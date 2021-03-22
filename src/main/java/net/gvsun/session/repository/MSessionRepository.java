package net.gvsun.session.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用Map实现的session存储仓库
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class MSessionRepository implements SessionRepository<MSession> {
    private static final Logger logger = LoggerFactory.getLogger(MSessionRepository.class);
    private Integer defaultMaxInactiveInterval; //s
    private final Map<String, Session> sessions;

    public MSessionRepository() {
        sessions = new ConcurrentHashMap<>();
    }

    public MSessionRepository(Map<String, Session> sessions) {
        if (sessions == null) {
            throw new IllegalArgumentException("sessions参数不能为null");
        } else {
            this.sessions = sessions;
        }
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    @Override
    public MSession createSession() {
        MSession result = new MSession();
        if (this.defaultMaxInactiveInterval != null) {
            result.setMaxInactiveInterval(defaultMaxInactiveInterval);
        }
        logger.debug("创建session:{}", result.getId());
        return result;
    }

    @Override
    public void save(MSession session) {
        if (!session.getId().equals(session.getOriginalId())) {
            this.sessions.remove(session.getOriginalId());
        }
        this.sessions.put(session.getId(), new MSession(session));
        logger.debug("保存session:{}", session.getId());
    }

    @Override
    public MSession findById(String sid) {
        logger.debug("查找session:{}", sid);
        Session saved = this.sessions.get(sid);
        if (saved == null) {
            return null;
        } else if (saved.isExpired()) {
            this.deleteById(saved.getId());
            return null;
        } else {
            return new MSession(saved);
        }
    }

    @Override
    public MSession findById(String sid, String projectName) {
        return null;
    }

    @Override
    public void deleteById(String sid) {
        this.sessions.remove(sid);
        logger.debug("删除session:{}", sid);
    }
}
