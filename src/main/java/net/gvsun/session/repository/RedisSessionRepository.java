package net.gvsun.session.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gvsun.session.SessionCreatedEvent;
import net.gvsun.session.SessionDestroyedEvent;
import net.gvsun.session.ShareAttribute;
import net.gvsun.session.UnifySessionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用Redis实现的session存储仓库
 *
 * @author 陈敬
 * @since 0.0.2-SNAPSHOT
 */
public class RedisSessionRepository implements SessionRepository<RedisSession> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSessionRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Integer defaultMaxInactiveInterval; //s
    private final RedisTemplate<String, Object> jdkRedisTemplate;
    private final RedisTemplate<String, String> jsonRedisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UnifySessionProperties properties;
    public static final String PUBLIC = ":";
    public static final String PRIVATE = "";

    public RedisSessionRepository(RedisTemplate<String, Object> jdkRedisTemplate,
                                  RedisTemplate<String, String> jsonRedisTemplate,
                                  UnifySessionProperties properties,
                                  ApplicationEventPublisher applicationEventPublisher) {
        this.jdkRedisTemplate = jdkRedisTemplate;
        this.jsonRedisTemplate = jsonRedisTemplate;
        this.properties = properties;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    @Override
    public RedisSession createSession() {
        RedisSession result = new RedisSession();
        if (this.defaultMaxInactiveInterval != null) {
            result.setMaxInactiveInterval(defaultMaxInactiveInterval);
        }
        logger.debug("创建session:{}", result.getId());
        applicationEventPublisher.publishEvent(new SessionCreatedEvent(this, result));
        return result;
    }

    @Override
    public void save(RedisSession session) {
        long start = System.currentTimeMillis();
        if (!session.getId().equals(session.getOriginalId())) {
            Set<String> keys = jdkRedisTemplate.keys(session.getOriginalId() + "*");
            if (keys != null && keys.size() > 0) {
                for (String ori : keys) {
                    String newKey = session.getId();
                    if (ori.length() > session.getOriginalId().length()) {
                        newKey += ori.substring(session.getOriginalId().length());
                    }
                    jdkRedisTemplate.rename(ori, newKey);
                }
            }
        }

        Set<String> attributeNames = session.getAttributeNames();
        Map<String, String> shareAttributeMap = new HashMap<>();
        Map<String, Object> privateAttributeMap = new HashMap<>();
        for (String name : attributeNames) {
            Object attribute = session.getAttribute(name);
            if (name.startsWith(PUBLIC)) {
                if (attribute instanceof ShareAttribute) {
                    try {
                        shareAttributeMap.put(name, objectMapper.writeValueAsString(attribute));
                    } catch (JsonProcessingException e) {
                        logger.error("序列化失败", e);
                    }
                }
            } else if (attribute instanceof Serializable) {
                privateAttributeMap.put(name, attribute);
            }
        }
        String key = session.getId() + ":" + properties.getProjectName();
        jsonRedisTemplate.opsForHash().putAll(key, shareAttributeMap);
        jdkRedisTemplate.opsForHash().putAll(key, privateAttributeMap);

        jdkRedisTemplate.opsForHash().put(session.getId(), "creationTime", session.getCreationTime());
        jdkRedisTemplate.opsForHash().put(session.getId(), "lastAccessedTime", session.getLastAccessedTime());
        long residueTime = (long) session.getMaxInactiveInterval() * 1000 - (session.getLastAccessedTime() - session.getCreationTime());
        if (residueTime <= 0)
            residueTime = 0;
        jdkRedisTemplate.expire(session.getId(), residueTime, TimeUnit.MILLISECONDS);
        jdkRedisTemplate.expire(key, residueTime, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();
        logger.debug("保存session:{}耗时{}ms", session.getId(), end - start);
    }

    @Override
    public RedisSession findById(String sid, String projectName) {
        long start = System.currentTimeMillis();
        int attributeCount = 1000;
        String key = sid + ":" + projectName;
        Boolean hasSession = jdkRedisTemplate.hasKey(sid);
        if (hasSession == null || !hasSession) {
            return null;
        }

        Boolean hasKey = jdkRedisTemplate.hasKey(key);
        Map<String, String> shareAttributeMap = new HashMap<>();
        Map<String, Object> privateAttributeMap = new HashMap<>();
        if (hasKey != null && hasKey) {
            //match只支持glob通配符，不支持正则表达式，
            //[^abc]匹配除了a, b, c以外的字符。
            ScanOptions shareBuild = ScanOptions.scanOptions().match("[^:]*").count(attributeCount).build();
            try (Cursor<Map.Entry<Object, Object>> cursor = jdkRedisTemplate.opsForHash().scan(key, shareBuild)) {
                while (cursor.hasNext()) {
                    Map.Entry<Object, Object> next = cursor.next();
                    String k = (String) next.getKey();
                    privateAttributeMap.put(k, next.getValue());
                }
            } catch (IOException e) {
                logger.error("jsonRedisTemplate.opsForHash().scan失败", e);
            }

            ScanOptions publicShareBuild = ScanOptions.scanOptions().match(":*").count(attributeCount).build();
            try (Cursor<Map.Entry<Object, Object>> cursor = jsonRedisTemplate.opsForHash().scan(key, publicShareBuild)) {
                while (cursor.hasNext()) {
                    Map.Entry<Object, Object> next = cursor.next();
                    String k = (String) next.getKey();
                    shareAttributeMap.put(k, (String) next.getValue());
                }
            } catch (IOException e) {
                logger.error("jsonRedisTemplate.opsForHash().scan失败", e);
            }
        }

        Object creationTime = jdkRedisTemplate.opsForHash().get(sid, "creationTime");
        Object lastAccessedTime = jdkRedisTemplate.opsForHash().get(sid, "lastAccessedTime");

        RedisSession redisSession = new RedisSession(sid);
        if (this.defaultMaxInactiveInterval != null) {
            redisSession.setMaxInactiveInterval(defaultMaxInactiveInterval);
        }
        for (Map.Entry<String, String> e : shareAttributeMap.entrySet()) {
            redisSession.setAttribute(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Object> e : privateAttributeMap.entrySet()) {
            redisSession.setAttribute(e.getKey(), e.getValue());
        }
        if (creationTime == null) {
            logger.error("未找到session:{}的创建时间", sid);
            deleteById(sid);
            return null;
        }
        redisSession.setCreationTime((Long) creationTime);
        if (lastAccessedTime != null) {
            redisSession.setLastAccessedTime((Long) lastAccessedTime);
        } else {
            redisSession.setLastAccessedTime(redisSession.getCreationTime());
        }
        if (redisSession.isExpired()) {
            deleteById(sid);
            return null;
        }
        long end = System.currentTimeMillis();
        logger.debug("查找session:{}耗时{}ms", sid, end - start);
        return redisSession;
    }

    @Override
    public RedisSession findById(String sid) {
        return findById(sid, properties.getProjectName());
    }

    @Override
    public void deleteById(String sid) {
        applicationEventPublisher.publishEvent(new SessionDestroyedEvent(this, findById(sid)));
        Set<String> keys = jdkRedisTemplate.keys(sid + "*");
        if (keys != null && keys.size() > 0) {
            jdkRedisTemplate.delete(keys);
        }
        logger.debug("删除session:{}", sid);
    }
}
