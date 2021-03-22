package net.gvsun.session.repository;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Map的session
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class MSession implements Session, Serializable {
    private String id;
    private final String originalId;
    private Map<String, Object> sessionAttrs = new ConcurrentHashMap<>();
    private long creationTime = System.currentTimeMillis();
    private long lastAccessedTime = this.creationTime;
    //默认的session的最大过期时间（秒）
    private int maxInactiveInterval = 1800;

    public MSession() {
        this(MSession.generateId());
    }

    public MSession(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session不能为null");
        }
        this.id = session.getId();
        this.originalId = this.id;
        this.sessionAttrs = new ConcurrentHashMap<>(session.getAttributeNames().size());
        for (String name : session.getAttributeNames()) {
            Object val = session.getAttribute(name);
            if (val != null) {
                this.sessionAttrs.put(name, val);
            }
        }
        this.lastAccessedTime = session.getLastAccessedTime();
        this.creationTime = session.getCreationTime();
        this.maxInactiveInterval = session.getMaxInactiveInterval();
    }

    public MSession(String id) {
        this.id = id;
        this.originalId = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getOriginalId() {
        return this.originalId;
    }

    @Override
    public String changeSessionId() {
        String changedId = generateId();
        setId(changedId);
        return changedId;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) this.sessionAttrs.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        //返回副本
        return new HashSet<>(this.sessionAttrs.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null)
            removeAttribute(name);
        else
            this.sessionAttrs.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        this.sessionAttrs.remove(name);
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public void setLastAccessedTime(long time) {
        this.lastAccessedTime = time;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public boolean isExpired() {
        return this.isExpired(System.currentTimeMillis());
    }

    boolean isExpired(long now) {
        if (this.maxInactiveInterval <= 0)
            return false;
        return (now - this.lastAccessedTime) / 1000 > this.maxInactiveInterval;
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Session && this.id.equals(((Session) obj).getId());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
