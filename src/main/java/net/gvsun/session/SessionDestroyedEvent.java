package net.gvsun.session;

import net.gvsun.session.repository.Session;

/**
 * Session销毁事件
 *
 * @author 陈敬
 * @since 1.1.0-SNAPSHOT
 */
public class SessionDestroyedEvent extends AbstractSessionEvent{
    public SessionDestroyedEvent(Object source, Session session) {
        super(source, session);
    }
}
