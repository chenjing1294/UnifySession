package net.gvsun.session;

import net.gvsun.session.repository.Session;
import org.springframework.context.ApplicationEvent;

/**
 * Session事件
 *
 * @author 陈敬
 * @since 1.1.0-SNAPSHOT
 */
public abstract class AbstractSessionEvent extends ApplicationEvent {
    private final String sessionId;
    private final Session session;

    public AbstractSessionEvent(Object source, Session session) {
        super(source);
        this.session = session;
        this.sessionId = session.getId();
    }

    @SuppressWarnings("unchecked")
    public <S extends Session> S getSession() {
        return (S) this.session;
    }

    public String getSessionId() {
        return this.sessionId;
    }
}
