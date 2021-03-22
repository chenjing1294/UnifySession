package net.gvsun.session.repository;

/**
 * 存储session的仓库
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public interface SessionRepository<S extends Session> {
    S createSession();

    void save(S session);

    S findById(String sid);

    S findById(String sid, String projectName);

    void deleteById(String sid);
}
