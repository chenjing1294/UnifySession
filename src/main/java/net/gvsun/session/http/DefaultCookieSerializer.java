package net.gvsun.session.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultCookieSerializer implements CookieSerializer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCookieSerializer.class);
    private final String cookieName = "USESSIONID";
    private boolean useBase64Encoding = false;
    private static final Pattern compile = Pattern.compile("http[s]*://([\\w\\.]+)[:\\d]*.*");

    @Override
    public void writeCookieValue(CookieValue cookieValue) {
        HttpServletRequest request = cookieValue.getRequest();
        HttpServletResponse response = cookieValue.getResponse();
        StringBuilder sb = new StringBuilder();
        sb.append(this.cookieName).append('=');
        String value = cookieValue.getCookieValue();
        if (value != null && value.length() > 0) {
            if (useBase64Encoding) {
                value = base64Encode(value);
            }
            sb.append(value);
        }
        int cookieMaxAge = cookieValue.getCookieMaxAge();
        if (cookieMaxAge > -1) {
            sb.append("; Max-Age=").append(cookieMaxAge);
        }
        String domainName = getDomainName(request);
        sb.append("; Domain=").append(domainName);
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        String s = sb.toString();
        logger.debug("写入cookie:{}到客户端", s);
        response.addHeader("Set-Cookie", s);
    }

    @Override
    public List<String> readCookieValues(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        List<String> matchingCookieValues = new ArrayList<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (this.cookieName.equals(cookie.getName())) {
                    String sessionId = cookie.getValue();
                    if (sessionId != null) {
                        sessionId = this.useBase64Encoding ? base64Decode(sessionId) : sessionId;
                        matchingCookieValues.add(sessionId);
                    }
                }
            }
        }
        return matchingCookieValues;
    }

    private String base64Decode(String base64Value) {
        try {
            byte[] decodedCookieBytes = Base64.getDecoder().decode(base64Value);
            return new String(decodedCookieBytes);
        } catch (Exception e) {
            logger.warn("Base64解码错误:{}", base64Value);
            return null;
        }
    }

    private String getDomainName(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        Matcher matcher = compile.matcher(requestURL);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String base64Encode(String value) {
        byte[] encodedCookieBytes = Base64.getEncoder().encode(value.getBytes());
        return new String(encodedCookieBytes);
    }
}
