package me.pggsnap.demos.rediscluster.reactive.config;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * @author pggsnap
 * @date 2020/6/9
 */
@Configuration
public class RedisReactiveConfig {
    @Bean
    public RedisCacheConfiguration defaultRedisCacheConfiguration() {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                ))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                ));

        return redisCacheConfiguration;
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory, RedisCacheConfiguration redisCacheConfiguration) {
        RedisSerializationContext<String, Object> redisSerializationContext = RedisSerializationContext.<String, Object>newSerializationContext()
                .key(redisCacheConfiguration.getKeySerializationPair())
                .value(redisCacheConfiguration.getValueSerializationPair())
                .hashKey(redisCacheConfiguration.getKeySerializationPair())
                .hashValue(redisCacheConfiguration.getValueSerializationPair())
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, redisSerializationContext);
    }

    @Bean(name = "reactiveRedisConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory(ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
                                                             ClientResources clientResources,
                                                             RedisProperties redisProperties) {
        // initial LettuceClientConfiguration
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout(redisProperties.getTimeout());
        }
        if (redisProperties.isSsl()) {
            builder.useSsl();
        }
        if (redisProperties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(redisProperties.getLettuce().getShutdownTimeout());
            }
            if (redisProperties.getLettuce().getPool() != null) {
                builder.poolConfig(getPoolConfig(redisProperties.getLettuce().getPool()));
            }
        }

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.builder()
                .enableAdaptiveRefreshTrigger(
                        ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT,
                        ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS
                )
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(10))
                .build();
        builder.clientResources(clientResources)
                .clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(options).build());
        builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        LettucePoolingClientConfiguration configuration = builder.build();

        // initial RedisClusterConfiguration
        RedisProperties.Cluster clusterProperties = redisProperties.getCluster();
        RedisClusterConfiguration config = new RedisClusterConfiguration(clusterProperties.getNodes());
        if (clusterProperties.getMaxRedirects() != null) {
            config.setMaxRedirects(clusterProperties.getMaxRedirects());
        }
        if (redisProperties.getPassword() != null) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, configuration);
        return factory;
    }

    private GenericObjectPoolConfig<?> getPoolConfig(RedisProperties.Pool properties) {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(properties.getMaxActive());
        config.setMaxIdle(properties.getMaxIdle());
        config.setMinIdle(properties.getMinIdle());
        if (properties.getMaxWait() != null) {
            config.setMaxWaitMillis(properties.getMaxWait().toMillis());
        }
        return config;
    }
}
