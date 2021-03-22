package net.gvsun.session.config;

import net.gvsun.session.UnifySessionProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 配置私有和公有session属性的RedisTemplate
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
public class RedisConfig {
    private JedisConnectionFactory jedisConnectionFactory;

    private JedisConnectionFactory getJedisConnectionFactory(UnifySessionProperties properties) {
        if (jedisConnectionFactory == null) {
            JedisPoolConfig jpc = new JedisPoolConfig();
            RedisStandaloneConfiguration rdsc = new RedisStandaloneConfiguration();
            rdsc.setHostName(properties.getRedis().getHost());
            rdsc.setPort(properties.getRedis().getPort());
            rdsc.setPassword(properties.getRedis().getPassword());
            rdsc.setDatabase(properties.getRedis().getDatabase());
            JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
            JedisClientConfiguration build = builder.usePooling().poolConfig(jpc).build();
            jedisConnectionFactory = new JedisConnectionFactory(rdsc, build);
        }
        return jedisConnectionFactory;
    }

    @Bean("jdkRedisTemplate")
    @Qualifier("jdkRedisTemplate")
    public RedisTemplate<String, Object> jdkRedisTemplate(UnifySessionProperties properties) {
        JedisConnectionFactory factory = getJedisConnectionFactory(properties);
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean("jsonRedisTemplate")
    @Qualifier("jsonRedisTemplate")
    public RedisTemplate<String, String> jsonRedisTemplate(UnifySessionProperties properties) {
        JedisConnectionFactory factory = getJedisConnectionFactory(properties);
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
