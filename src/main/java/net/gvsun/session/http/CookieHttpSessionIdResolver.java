package net.gvsun.session.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * SessionID解析器
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class CookieHttpSessionIdResolver implements HttpSessionIdResolver {
    private final CookieSerializer cookieSerializer = new DefaultCookieSerializer();

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        return cookieSerializer.readCookieValues(request);
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, sessionId));
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));
    }
}
