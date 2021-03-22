package net.gvsun.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
@ConfigurationProperties(prefix = "usession")
public class UnifySessionProperties {
    public static final String REDIS_REPOSITORY = "REDIS_REPOSITORY";
    public static final String MEMORY_REPOSITORY = "MEMORY_REPOSITORY";

    /**
     * 项目名，强烈建议和多数据源里配置的项目名一致，不然你是在给自己制造麻烦
     */
    private String projectName;

    /**
     * 存储session的仓库类型
     */
    private String repositoryType = REDIS_REPOSITORY;

    /**
     * session的过期时间，以秒为单位，建议使用默认配置，如果你确实想要修改，请和其他微服务商量保持一致
     */
    private Integer maxInactiveInterval = 1800;

    private Redis redis;

    public Integer getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(Integer maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return "UnifySessionProperties{" +
                "projectName='" + projectName + '\'' +
                ", repositoryType='" + repositoryType + '\'' +
                ", redis=" + redis +
                '}';
    }

    public static class Redis {
        /**
         * redis的地址
         */
        private String host = "localhost";

        /**
         * redis端口号
         */
        private Integer port = 6379;

        /**
         * redis密码
         */
        private String password;

        /**
         * redis数据库，建议保持默认，如果确实需要修改，请和其他微服务商量保持一致，不然你拿不到共享session
         */
        private Integer database = 2;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getDatabase() {
            return database;
        }

        public void setDatabase(Integer database) {
            this.database = database;
        }

        @Override
        public String toString() {
            return "Redis{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    ", password='" + "******" + '\'' +
                    ", database=" + database +
                    '}';
        }
    }
}
