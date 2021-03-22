package net.gvsun.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gvsun.session.http.CookieHttpSessionIdResolver;
import net.gvsun.session.http.HttpSessionAdapter;
import net.gvsun.session.http.HttpSessionIdResolver;
import net.gvsun.session.http.OnCommittedResponseWrapper;
import net.gvsun.session.repository.RedisSessionRepository;
import net.gvsun.session.repository.Session;
import net.gvsun.session.repository.SessionRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 替换tomcat提供的HttpSession
 * <p>
 * SessionFilter必须放在访问HttpSession或可能提交响应的任何过滤器之前，
 * 以确保session被正确覆盖和持久。
 * <p>
 * 扩展{@link OncePerRequestFilter}是有必要的，因为session过滤代码在一次请求中只需要被执行一次即可
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class UnifySessionFilter<S extends Session> extends OncePerRequestFilter {
    //存储session的仓库，目前有两种：MSessionRepository和RedisSessionRepository
    private final SessionRepository<S> sessionRepository;
    //sessionId解析器
    private final HttpSessionIdResolver httpSessionIdResolver = new CookieHttpSessionIdResolver();
    public static final String SESSION_REPOSITORY_ATTR = SessionRepository.class.getName();
    //在HttpServletRequest中标记一个session
    public static final String CURRENT_SESSION_ATTR = SESSION_REPOSITORY_ATTR + ".CURRENT_SESSION";
    public static final String INVALID_SESSION_ID_ATTR = SESSION_REPOSITORY_ATTR + ".invalidSessionId";
    private final UnifySessionProperties properties;

    public UnifySessionFilter(SessionRepository<S> sessionRepository, UnifySessionProperties properties) {
        if (sessionRepository == null) {
            throw new IllegalArgumentException("sessionRepository不能为null");
        }
        if (StringUtils.isEmpty(properties.getProjectName())) {
            throw new IllegalArgumentException("usession.projectName不能为空");
        }
        this.properties = properties;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //使用包装器模式，包装原始的request和response用于替换tomcat容器提供的session管理机制
        UnifyRequestWrapper unifyRequestWrapper = new UnifyRequestWrapper(request, response);
        UnifyResponseWrapper unifyResponseWrapper = new UnifyResponseWrapper(unifyRequestWrapper, response);
        try {
            filterChain.doFilter(unifyRequestWrapper, unifyResponseWrapper);
        } finally {
            unifyRequestWrapper.commitSession();
        }
    }

    /**
     * {@link HttpServletRequest}的包装器
     */
    public final class UnifyRequestWrapper extends HttpServletRequestWrapper {
        //用于标记请求中指定的session是否在服务端已存在
        private Boolean requestedSessionIdValid;
        //用于标记服务器端存储的session是否已经过期或失效
        private boolean requestedSessionInvalidated;
        //标记session是否在类内缓存
        private boolean requestedSessionCached;
        //标记客户端发过来的sessionId
        private String requestedSessionId;
        //缓存的session
        private S requestedSession;
        private final HttpServletResponse response;

        public UnifyRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        @Override
        public String changeSessionId() {
            HttpSessionWrapper sessionWrapper = getSession(false);
            if (sessionWrapper == null) {
                throw new IllegalStateException("无法改变sessionID，与该请求相关联的session还未创建");
            }
            return sessionWrapper.getSession().changeSessionId();
        }

        @Override
        public HttpSessionWrapper getSession(boolean create) {
            //先尝试从request中获取session
            HttpSessionWrapper currentSession = getCurrentSession();
            if (currentSession != null) {
                return currentSession;
            }

            //如果在request中无法获取session，再尝试从SessionRepository中获取
            S requestedSession = getRequestedSession();
            if (requestedSession != null) {
                requestedSession.setLastAccessedTime(System.currentTimeMillis());
                this.requestedSessionIdValid = true;
                currentSession = new HttpSessionWrapper(requestedSession, getServletContext());
                currentSession.markNotNew();
                setCurrentSession(currentSession);
                return currentSession;
            }

            if (!create) {
                return null;
            }
            S session = sessionRepository.createSession();
            session.setLastAccessedTime(System.currentTimeMillis());
            currentSession = new HttpSessionWrapper(session, getServletContext());
            setCurrentSession(currentSession);
            return currentSession;
        }

        @Override
        public HttpSessionWrapper getSession() {
            return getSession(true);
        }

        @Override
        public String getRequestedSessionId() {
            if (this.requestedSessionId == null) {
                getRequestedSession();
            }
            return this.requestedSessionId;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            if (this.requestedSessionIdValid == null) {
                S requestedSession = getRequestedSession();
                if (requestedSession != null) {
                    requestedSession.setLastAccessedTime(System.currentTimeMillis());
                }
                return isRequestedSessionIdValid(requestedSession);
            }
            return this.requestedSessionIdValid;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            RequestDispatcher requestDispatcher = super.getRequestDispatcher(path);
            //用SessionCommittingRequestDispatcher对原始的RequestDispatcher进行包装，确保session在include之前被提交
            return new SessionCommittingRequestDispatcher(requestDispatcher);
        }

        private boolean isRequestedSessionIdValid(S session) {
            if (this.requestedSessionIdValid == null) {
                this.requestedSessionIdValid = session != null;
            }
            return this.requestedSessionIdValid;
        }

        private boolean isInvalidateClientSession() {
            return getCurrentSession() == null && this.requestedSessionInvalidated;
        }

        private S getRequestedSession() {
            /*if (!this.requestedSessionCached) {
                List<String> sessionIds = httpSessionIdResolver.resolveSessionIds(this);
                for (String sessionId : sessionIds) {
                    if (this.requestedSessionId == null) {
                        this.requestedSessionId = sessionId;
                    }
                    S session = sessionRepository.findById(sessionId);
                    if (session != null) {
                        this.requestedSession = session;
                        this.requestedSessionId = sessionId;
                        break;
                    }
                }
                this.requestedSessionCached = true;
            }*/
            List<String> sessionIds = httpSessionIdResolver.resolveSessionIds(this);
            for (String sessionId : sessionIds) {
                if (this.requestedSessionId == null) {
                    this.requestedSessionId = sessionId;
                }
                S session = sessionRepository.findById(sessionId);
                if (session != null) {
                    this.requestedSession = session;
                    this.requestedSessionId = sessionId;
                    break;
                }
            }
            return this.requestedSession;
        }

        private void setCurrentSession(HttpSessionWrapper currentSession) {
            if (currentSession == null) {
                removeAttribute(CURRENT_SESSION_ATTR);
            } else {
                setAttribute(CURRENT_SESSION_ATTR, currentSession);
            }
        }

        /**
         * 从request中获取session
         */
        @SuppressWarnings("unchecked")
        private HttpSessionWrapper getCurrentSession() {
            return (HttpSessionWrapper) getAttribute(CURRENT_SESSION_ATTR);
        }

        private void clearRequestedSessionCache() {
            this.requestedSessionCached = false;
            this.requestedSession = null;
            this.requestedSessionId = null;
        }

        private void commitSession() {
            HttpSessionWrapper currentSession = getCurrentSession();
            if (currentSession == null) {
                if (isInvalidateClientSession()) {
                    httpSessionIdResolver.expireSession(this, this.response);
                }
            } else {
                S session = currentSession.getSession();
                clearRequestedSessionCache();
                sessionRepository.save(session);
                String sessionId = session.getId();
                //没有必要在每个响应中都设置sessionId，只在最初的一个响应中设置即可
                if (!isRequestedSessionIdValid() || !sessionId.equals(getRequestedSessionId())) {
                    httpSessionIdResolver.setSessionId(this, response, sessionId);
                }
            }
        }

        /**
         * HttpSession包装器，在主动使session失效时做一些额外的清理工作，提供获取和设置共享属性的方法
         * <p>
         * ':'作为UnifySession的保留字符，在session中的属性名中不能使用
         */
        public final class HttpSessionWrapper extends HttpSessionAdapter<S> {
            private final ObjectMapper objectMapper = new ObjectMapper();

            HttpSessionWrapper(S session, ServletContext servletContext) {
                super(session, servletContext);
            }

            /**
             * 获取其他项目的共享session属性
             *
             * @param projectName 项目名
             * @param name        属性名
             */
            @SuppressWarnings("unchecked")
            public <T> T getShareAttribute(String projectName, String name, TypeReference<T> typeReference) throws IOException {
                String attrName = RedisSessionRepository.PUBLIC + name;
                Object val = super.getAttribute(attrName);
                if (val != null) {
                    if (val instanceof ShareAttribute) {
                        return (T) val;
                    } else {
                        return objectMapper.readValue((String) val, typeReference);
                    }
                }
                String sessionId = getId();
                S session = sessionRepository.findById(sessionId, projectName);
                Object attribute = session.getAttribute(RedisSessionRepository.PUBLIC + name);
                return attribute != null ? objectMapper.readValue((String) attribute, typeReference) : null;
            }

            /**
             * 获取共享session属性
             *
             * @param name 属性名
             */
            public <T> T getShareAttribute(String name, TypeReference<T> typeReference) throws IOException {
                return getShareAttribute(properties.getProjectName(), name, typeReference);
            }

            /**
             * 设置本项目的共享session属性
             *
             * @param name  属性名
             * @param value 属性值
             */
            public void setShareAttribute(String name, ShareAttribute value) {
                super.setAttribute(RedisSessionRepository.PUBLIC + name, value);
            }

            @Override
            public void setAttribute(String name, Object value) {
                super.setAttribute(RedisSessionRepository.PRIVATE + name, value);
            }

            @Override
            public Object getAttribute(String name) {
                return super.getAttribute(RedisSessionRepository.PRIVATE + name);
            }

            @Override
            public void invalidate() {
                super.invalidate();
                requestedSessionInvalidated = true;
                setCurrentSession(null);
                clearRequestedSessionCache();
                sessionRepository.deleteById(getId());
            }
        }

        /**
         * RequestDispatcher包装器，因为要在include之前进行提交session信息，
         * 如果在include之后在提交的话，容器会忽略对响应头的修改
         */
        private final class SessionCommittingRequestDispatcher implements RequestDispatcher {
            private final RequestDispatcher dispatcher;

            private SessionCommittingRequestDispatcher(RequestDispatcher dispatcher) {
                this.dispatcher = dispatcher;
            }

            @Override
            public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                this.dispatcher.forward(request, response);
            }

            @Override
            public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                commitSession();
                dispatcher.include(request, response);
            }
        }
    }

    /**
     * {@link HttpServletResponse}的包装器
     */
    private final class UnifyResponseWrapper extends OnCommittedResponseWrapper {
        private final UnifyRequestWrapper request;

        UnifyResponseWrapper(UnifyRequestWrapper request, HttpServletResponse response) {
            super(response);
            if (request == null) {
                throw new IllegalArgumentException("request不能为null");
            }
            this.request = request;
        }

        @Override
        protected void onResponseCommitted() {
            this.request.commitSession();
        }
    }
}
