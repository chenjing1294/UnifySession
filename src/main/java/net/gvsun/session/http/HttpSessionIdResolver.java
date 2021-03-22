package net.gvsun.session.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 负责从客户端请求中解析出sessionId
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public interface HttpSessionIdResolver {
    List<String> resolveSessionIds(HttpServletRequest request);

    void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId);

    void expireSession(HttpServletRequest request, HttpServletResponse response);
}
