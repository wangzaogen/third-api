package com.thirdapi.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdapi.sdk.core.annotation.ThirdPartyApi;
import com.thirdapi.starter.auth.ApiSigner;
import com.thirdapi.starter.auth.AuthProcessor;
import com.thirdapi.starter.auth.HmacSha256ApiSigner;
import com.thirdapi.starter.auth.OAuth2TokenProvider;
import com.thirdapi.starter.auth.TokenProvider;
import com.thirdapi.starter.client.ThirdApiClientFactory;
import com.thirdapi.starter.config.AdminConfigSource;
import com.thirdapi.starter.config.ConfigSource;
import com.thirdapi.starter.config.ConfigStore;
import com.thirdapi.starter.config.ConfigSyncService;
import com.thirdapi.starter.config.LocalConfigSource;
import com.thirdapi.starter.executor.ApiInvocationExecutor;
import com.thirdapi.starter.http.RequestBuilder;
import com.thirdapi.starter.http.SimpleHttpClient;
import com.thirdapi.starter.logging.CallLogger;
import com.thirdapi.starter.logging.CallMetrics;
import com.thirdapi.starter.logging.InMemoryCallMetrics;
import com.thirdapi.starter.logging.Slf4jCallLogger;
import com.thirdapi.starter.registry.ThirdApiRegistry;
import com.thirdapi.starter.resilience.ResiliencePolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto configuration for the third-api starter.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ThirdPartyApi.class)
@EnableConfigurationProperties(ThirdApiProperties.class)
@ConditionalOnProperty(prefix = "third-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ThirdApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper thirdApiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThirdApiRegistry thirdApiRegistry() {
        return new ThirdApiRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigStore thirdApiConfigStore() {
        return new ConfigStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestBuilder requestBuilder(ObjectMapper objectMapper) {
        return new RequestBuilder(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenProvider tokenProvider(ObjectMapper objectMapper) {
        return new OAuth2TokenProvider(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiSigner apiSigner() {
        return new HmacSha256ApiSigner();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthProcessor authProcessor(TokenProvider tokenProvider, ApiSigner apiSigner) {
        return new AuthProcessor(tokenProvider, apiSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleHttpClient simpleHttpClient() {
        return new SimpleHttpClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResiliencePolicy resiliencePolicy() {
        return new ResiliencePolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    public CallLogger callLogger() {
        return new Slf4jCallLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public CallMetrics callMetrics() {
        return new InMemoryCallMetrics();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ConfigSyncService configSyncService(ConfigStore configStore,
                                               ThirdApiProperties properties,
                                               ObjectMapper objectMapper) {
        List<ConfigSource> sources = new ArrayList<ConfigSource>();
        sources.add(new LocalConfigSource(properties));
        boolean adminMode = "admin".equalsIgnoreCase(properties.getMode())
                && properties.getAdminUrl() != null
                && !properties.getAdminUrl().isEmpty();
        if (adminMode) {
            sources.add(new AdminConfigSource(objectMapper, properties));
        }
        return new ConfigSyncService(sources, configStore, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiInvocationExecutor apiInvocationExecutor(ConfigStore configStore,
                                                       RequestBuilder requestBuilder,
                                                       AuthProcessor authProcessor,
                                                       SimpleHttpClient httpClient,
                                                       ResiliencePolicy resiliencePolicy,
                                                       CallLogger callLogger,
                                                       CallMetrics callMetrics,
                                                       ThirdApiProperties properties,
                                                       ObjectMapper objectMapper) {
        return new ApiInvocationExecutor(configStore, requestBuilder, authProcessor,
                httpClient, resiliencePolicy, callLogger, callMetrics, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThirdApiClientFactory thirdApiClientFactory(ThirdApiRegistry registry,
                                                       ApiInvocationExecutor executor) {
        return new ThirdApiClientFactory(registry, executor);
    }
}
