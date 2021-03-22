package net.gvsun.session.config;

import net.gvsun.session.UnifyHttpSessionListenerAdapter;
import net.gvsun.session.UnifySessionFilter;
import net.gvsun.session.UnifySessionProperties;
import net.gvsun.session.repository.MSession;
import net.gvsun.session.repository.MSessionRepository;
import net.gvsun.session.repository.RedisSession;
import net.gvsun.session.repository.RedisSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * UnifySession的主配置类
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
@EnableConfigurationProperties(UnifySessionProperties.class)
public class UnifySessionConfiguration implements ApplicationContextAware {
    private final static Logger logger = LoggerFactory.getLogger(UnifySessionConfiguration.class);
    public static final String FILTER_NAME = "unifySessionFilter";
    private List<HttpSessionListener> httpSessionListeners = new ArrayList<>();
    private ApplicationContext applicationContext;

    @Bean
    public FilterRegistrationBean<Filter> someFilterRegistration(
            UnifySessionProperties properties,
            @Qualifier("jdkRedisTemplate") RedisTemplate<String, Object> jdkRedisTemplate,
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, String> jsonRedisTemplate) {
        logger.info("使用UnifySession接管session的生命周期管理,配置详情{}", properties.toString());
        boolean find = false;
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        if (properties.getRepositoryType().equals(UnifySessionProperties.MEMORY_REPOSITORY)) {
            UnifySessionFilter<MSession> filter = new UnifySessionFilter<>(new MSessionRepository(), properties);
            registration.setFilter(filter);
            find = true;
        } else if (properties.getRepositoryType().equals(UnifySessionProperties.REDIS_REPOSITORY)) {
            RedisSessionRepository redisSessionRepository = new RedisSessionRepository(
                    jdkRedisTemplate,
                    jsonRedisTemplate,
                    properties,
                    applicationContext);
            if (properties.getMaxInactiveInterval() > 0) {
                redisSessionRepository.setDefaultMaxInactiveInterval(properties.getMaxInactiveInterval());
            }
            UnifySessionFilter<RedisSession> filter = new UnifySessionFilter<>(
                    redisSessionRepository,
                    properties);
            registration.setFilter(filter);
            find = true;
        }
        if (find) {
            registration.setDispatcherTypes(getSessionDispatcherTypes());
            registration.addUrlPatterns("/*");
            registration.setName(FILTER_NAME);
            registration.setMatchAfter(false);
            registration.setOrder(Integer.MIN_VALUE + 50);
            return registration;
        } else {
            throw new IllegalArgumentException("未找到指定的repositoryType=" + properties.getRepositoryType());
        }
    }

    @Bean
    public UnifyHttpSessionListenerAdapter unifyHttpSessionListenerAdapter() {
        return new UnifyHttpSessionListenerAdapter(this.httpSessionListeners);
    }

    @Autowired(required = false)
    public void setHttpSessionListeners(List<HttpSessionListener> listeners) {
        this.httpSessionListeners = listeners;
    }

    protected EnumSet<DispatcherType> getSessionDispatcherTypes() {
        return EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
