package net.gvsun.session.repository;

import java.util.Set;

public interface Session {
    String getId();

    String changeSessionId();

    <T> T getAttribute(String name);

    default <T> T getAttributeOrDefault(String name, T defaultValue) {
        T result = this.getAttribute(name);
        return result == null ? null : defaultValue;
    }

    Set<String> getAttributeNames();

    void setAttribute(String name, Object value);

    void removeAttribute(String name);

    long getCreationTime();

    void setLastAccessedTime(long time);

    long getLastAccessedTime();

    void setMaxInactiveInterval(int interval);

    int getMaxInactiveInterval();

    boolean isExpired();
}
