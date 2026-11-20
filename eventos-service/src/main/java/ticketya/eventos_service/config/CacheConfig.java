package ticketya.eventos_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CATALOGO = "catalogo";

    @Autowired
    private RedisTolerantCacheErrorHandler errorHandler;

    @Bean
    @ConditionalOnProperty(name = "app.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration configuracion = configuracionCatalogo();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuracion)
                .withCacheConfiguration(CATALOGO, configuracion)
                .build();
    }

    RedisCacheConfiguration configuracionCatalogo() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return errorHandler;
    }
}
