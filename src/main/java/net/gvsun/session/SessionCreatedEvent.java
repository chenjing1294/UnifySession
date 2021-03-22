package net.gvsun.session;

import net.gvsun.session.repository.Session;

/**
 * Session创建事件
 *
 * @author 陈敬
 * @since 1.1.0-SNAPSHOT
 */
public class SessionCreatedEvent extends AbstractSessionEvent {
    public SessionCreatedEvent(Object source, Session session) {
        super(source, session);
    }
}
