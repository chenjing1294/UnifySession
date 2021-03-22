package net.gvsun.session;

import net.gvsun.session.http.HttpSessionAdapter;
import net.gvsun.session.repository.Session;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.List;

/**
 * Session事件监听器，负责转发session的创建和销毁事件，执行客户端代码
 *
 * @author 陈敬
 * @since 1.1.0-SNAPSHOT
 */
public class UnifyHttpSessionListenerAdapter implements ApplicationListener<AbstractSessionEvent>, ServletContextAware {
    private ServletContext context;
    private final List<HttpSessionListener> listeners;

    public UnifyHttpSessionListenerAdapter(List<HttpSessionListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onApplicationEvent(AbstractSessionEvent event) {
        if (this.listeners.isEmpty()) {
            return;
        }

        HttpSessionEvent httpSessionEvent = createHttpSessionEvent(event);

        for (HttpSessionListener listener : this.listeners) {
            if (event instanceof SessionDestroyedEvent) {
                listener.sessionDestroyed(httpSessionEvent);
            } else if (event instanceof SessionCreatedEvent) {
                listener.sessionCreated(httpSessionEvent);
            }
        }
    }

    private HttpSessionEvent createHttpSessionEvent(AbstractSessionEvent event) {
        Session session = event.getSession();
        HttpSession httpSession = new HttpSessionAdapter<>(session, this.context);
        return new HttpSessionEvent(httpSession);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.context = servletContext;
    }
}
