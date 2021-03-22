package net.gvsun.session;

import net.gvsun.session.config.RedisConfig;
import net.gvsun.session.config.UnifySessionConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 导入配置类，启用UnifySession
 *
 * @author 陈敬
 * @since 0.0.1-SNAPSHOT
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({UnifySessionConfiguration.class, RedisConfig.class})
@ServletComponentScan(basePackages = "net.gvsun.session")
public @interface EnableUnifySession {
}
