package net.gvsun.session.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Cookie序列化器，从客户端读取cookie和向客户端写入cookie
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public interface CookieSerializer {
    void writeCookieValue(CookieValue cookieValue);

    List<String> readCookieValues(HttpServletRequest request);

    class CookieValue {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final String cookieValue;
        private int cookieMaxAge = -1;

        public CookieValue(HttpServletRequest request, HttpServletResponse response, String cookieValue) {
            this.request = request;
            this.response = response;
            this.cookieValue = cookieValue;
            if ("".equals(this.cookieValue)) {
                this.cookieMaxAge = 0;
            }
        }

        public HttpServletRequest getRequest() {
            return this.request;
        }

        public HttpServletResponse getResponse() {
            return this.response;
        }

        public String getCookieValue() {
            return this.cookieValue;
        }

        public int getCookieMaxAge() {
            return this.cookieMaxAge;
        }

        public void setCookieMaxAge(int cookieMaxAge) {
            this.cookieMaxAge = cookieMaxAge;
        }
    }
}
