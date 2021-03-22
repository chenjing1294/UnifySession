package net.gvsun.session.http;

import net.gvsun.session.repository.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * HttpSession包装其，执行附加逻辑，主要是做写入检查和绑定到session的监听器的执行
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class HttpSessionAdapter<S extends Session> implements HttpSession {
    private static final Logger logger = LoggerFactory.getLogger(HttpSessionAdapter.class);
    private S session;
    private final ServletContext servletContext;
    private boolean invalidated = false;
    private boolean old = false;

    private static final HttpSessionContext NOOP_SESSION_CONTEXT = new HttpSessionContext() {
        @Override
        public HttpSession getSession(String sessionId) {
            return null;
        }

        @Override
        public Enumeration<String> getIds() {
            return HttpSessionAdapter.EMPTY_ENUMERATION;
        }
    };

    private static final Enumeration<String> EMPTY_ENUMERATION = new Enumeration<String>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public String nextElement() {
            throw new NoSuchElementException();
        }
    };


    public HttpSessionAdapter(S session, ServletContext servletContext) {
        if (session == null)
            throw new IllegalArgumentException("session参数不能为null");
        else if (servletContext == null)
            throw new IllegalArgumentException("servletContext参数不能为null");
        else {
            this.session = session;
            this.servletContext = servletContext;
        }
    }

    public S getSession() {
        return this.session;
    }

    @Override
    public long getCreationTime() {
        this.checkState();
        return this.session.getCreationTime();
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        this.checkState();
        return this.session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.session.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.session.getMaxInactiveInterval();
    }

    /**
     * 该方法已被servlet标准废弃，由于他说的安全原因
     */
    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        return NOOP_SESSION_CONTEXT;
    }

    @Override
    public Object getAttribute(String name) {
        this.checkState();
        return this.session.getAttribute(name);
    }

    /**
     * 该方法已被servlet标准废弃
     */
    @Override
    @Deprecated
    public Object getValue(String name) {
        return null;
    }

    /**
     * 该方法已被servlet标准废弃
     */
    @Override
    @Deprecated
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        this.checkState();
        return Collections.enumeration(this.session.getAttributeNames());
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.checkState();
        Object oldValue = this.session.getAttribute(name);
        this.session.setAttribute(name, value);
        if (value != oldValue) {
            if (oldValue instanceof HttpSessionBindingListener) {
                try {
                    ((HttpSessionBindingListener) oldValue).valueUnbound(
                            new HttpSessionBindingEvent(this, name, oldValue)
                    );
                } catch (Exception e) {
                    logger.error("调用HttpSessionBindingListener的方法时出错", e);
                }
            }
            if (value instanceof HttpSessionBindingListener) {
                try {
                    ((HttpSessionBindingListener) value).valueBound(
                            new HttpSessionBindingEvent(this, name, value)
                    );
                } catch (Exception e) {
                    logger.error("调用HttpSessionBindingListener的方法时出错", e);
                }
            }
        }
    }

    /**
     * 该方法已被servlet标准废弃
     */
    @Override
    @Deprecated
    public void putValue(String name, Object value) {

    }

    @Override
    public void removeAttribute(String name) {
        this.checkState();
        Object oldValue = this.session.getAttribute(name);
        this.session.removeAttribute(name);
        if (oldValue instanceof HttpSessionBindingListener) {
            try {
                ((HttpSessionBindingListener) oldValue).valueUnbound(
                        new HttpSessionBindingEvent(this, name, oldValue)
                );
            } catch (Exception e) {
                logger.error("调用HttpSessionBindingListener的方法时出错", e);
            }
        }
    }

    /**
     * 该方法已被servlet标准废弃
     */
    @Override
    @Deprecated
    public void removeValue(String name) {
    }

    @Override
    public void invalidate() {
        this.checkState();
        this.invalidated = true;
    }

    @Override
    public boolean isNew() {
        this.checkState();
        return !this.old;
    }

    public void markNotNew() {
        this.old = true;
    }

    private void checkState() {
        if (this.invalidated)
            throw new IllegalStateException("HttpSession已经过期了");
    }
}
