package com.thirdapi.starter.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thirdapi.sdk.core.annotation.ApiMethod;
import com.thirdapi.sdk.core.annotation.ApiParam;
import com.thirdapi.sdk.core.annotation.ThirdPartyApi;
import com.thirdapi.sdk.core.model.ApiInvocation;
import com.thirdapi.sdk.core.model.HttpMethod;
import com.thirdapi.sdk.core.model.ParamLocation;
import com.thirdapi.starter.auth.AuthProcessor;
import com.thirdapi.starter.auth.HmacSha256ApiSigner;
import com.thirdapi.starter.auth.OAuth2TokenProvider;
import com.thirdapi.starter.autoconfigure.ThirdApiProperties;
import com.thirdapi.starter.config.AdminConfigSource;
import com.thirdapi.starter.config.ConfigSnapshot;
import com.thirdapi.starter.config.ConfigStore;
import com.thirdapi.starter.executor.ApiInvocationExecutor;
import com.thirdapi.starter.http.RequestBuilder;
import com.thirdapi.starter.http.SimpleHttpClient;
import com.thirdapi.starter.logging.CallLogger;
import com.thirdapi.starter.logging.CallMetrics;
import com.thirdapi.starter.logging.InMemoryCallMetrics;
import com.thirdapi.starter.logging.Slf4jCallLogger;
import com.thirdapi.starter.resilience.ResiliencePolicy;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class EndToEndTest {

    private static final int MOCK_THIRD_PARTY_PORT = 8920;

    private HttpServer thirdPartyServer;

    @After
    public void tearDown() {
        if (thirdPartyServer != null) {
            thirdPartyServer.stop(0);
        }
    }

    @Test
    public void pullConfigFromSqliteAdminAndInvokeThirdParty() throws Exception {
        Assume.assumeTrue("E2E_RUN=true", "true".equalsIgnoreCase(System.getenv("E2E_RUN")));

        startThirdPartyServer();

        ThirdApiProperties properties = new ThirdApiProperties();
        properties.setAppName("e2e");
        properties.setAppId("order-service");
        properties.setAppSecret("secret");
        properties.setAdminUrl(System.getenv("E2E_ADMIN_URL") == null
                ? "http://127.0.0.1:8080" : System.getenv("E2E_ADMIN_URL"));
        properties.setLongPollTimeoutSeconds(1);

        AdminConfigSource source = new AdminConfigSource(new ObjectMapper(), properties);
        ConfigSnapshot snapshot = source.load();
        assertNotNull("Admin config snapshot should not be null", snapshot);
        assertFalse("Admin config should contain endpoints", snapshot.getConfigs().isEmpty());

        ConfigStore store = new ConfigStore();
        store.update(snapshot);

        ApiInvocationExecutor executor = buildExecutor(store, properties);
        ApiInvocation invocation = invocation();

        Object result = executor.execute(invocation);
        assertEquals("pong", String.valueOf(result).trim());
    }

    private ApiInvocationExecutor buildExecutor(ConfigStore store, ThirdApiProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new ApiInvocationExecutor(
                store,
                new RequestBuilder(objectMapper),
                new AuthProcessor(new OAuth2TokenProvider(objectMapper), new HmacSha256ApiSigner()),
                new SimpleHttpClient(),
                new ResiliencePolicy(),
                new Slf4jCallLogger(),
                new InMemoryCallMetrics(),
                properties,
                objectMapper);
    }

    private ApiInvocation invocation() throws Exception {
        Method method = E2eClient.class.getMethod("ping", String.class);
        ApiInvocation invocation = new ApiInvocation();
        invocation.setProvider("sms");
        invocation.setChannel("sqlite-sms");
        invocation.setEndpoint("ping");
        invocation.setBaseUrl("http://127.0.0.1:" + MOCK_THIRD_PARTY_PORT);
        invocation.setPath("/ping");
        invocation.setHttpMethod(HttpMethod.GET);
        invocation.setArgs(new Object[]{"e2e"});
        invocation.setMethod(method);
        invocation.setReturnType(method.getReturnType());
        invocation.setStartedAtMillis(System.currentTimeMillis());
        return invocation;
    }

    private void startThirdPartyServer() throws IOException {
        thirdPartyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", MOCK_THIRD_PARTY_PORT), 0);
        thirdPartyServer.createContext("/ping", this::handlePing);
        thirdPartyServer.start();
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        byte[] bytes = "pong".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @ThirdPartyApi(provider = "sms", channel = "sqlite-sms")
    private interface E2eClient {

        @ApiMethod(name = "ping", path = "/ping", method = HttpMethod.GET)
        String ping(@ApiParam(name = "q", location = ParamLocation.QUERY) String q);
    }
}
