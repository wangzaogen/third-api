package com.thirdapi.starter.executor;

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
import com.thirdapi.starter.config.ApiConfig;
import com.thirdapi.starter.config.ConfigSnapshot;
import com.thirdapi.starter.config.ConfigStore;
import com.thirdapi.starter.http.RequestBuilder;
import com.thirdapi.starter.http.SimpleHttpClient;
import com.thirdapi.starter.logging.CallLogger;
import com.thirdapi.starter.logging.CallMetrics;
import com.thirdapi.starter.logging.InMemoryCallMetrics;
import com.thirdapi.starter.logging.Slf4jCallLogger;
import com.thirdapi.starter.resilience.ResiliencePolicy;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ApiInvocationExecutorTest {

    private HttpServer server;

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void executesHttpGetAndMapsStringResponse() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = startServer(exchange -> {
            requests.incrementAndGet();
            write(exchange, 200, "hello");
        });

        ApiInvocationExecutor executor = buildExecutor(server.getAddress().getPort(), 1, 10, 1);
        ApiInvocation invocation = invocation(server.getAddress().getPort(), new Object[]{"demo"});

        Object result = executor.execute(invocation);

        assertEquals("hello", ((String) result).trim());
        assertEquals(1, requests.get());
    }

    @Test
    public void retriesOnceThenSucceeds() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = startServer(exchange -> {
            if (requests.incrementAndGet() == 1) {
                write(exchange, 500, "boom");
            } else {
                write(exchange, 200, "recovered");
            }
        });

        ApiInvocationExecutor executor = buildExecutor(server.getAddress().getPort(), 1, 10, 2);

        Object result = executor.execute(invocation(server.getAddress().getPort(), new Object[]{"demo"}));

        assertEquals("recovered", ((String) result).trim());
        assertEquals(2, requests.get());
    }

    @Test
    public void opensCircuitAfterFailures() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = startServer(exchange -> {
            requests.incrementAndGet();
            write(exchange, 500, "boom");
        });

        ApiInvocationExecutor executor = buildExecutor(server.getAddress().getPort(), 0, 10, 1);
        executor.execute(invocation(server.getAddress().getPort(), new Object[]{"demo"}));
        Object rejected = executor.execute(invocation(server.getAddress().getPort(), new Object[]{"demo"}));

        assertEquals(null, rejected);
        assertEquals(1, requests.get());
    }

    private ApiInvocationExecutor buildExecutor(int port, int maxRetries, long backoffMs, int minCalls) {
        ThirdApiProperties properties = new ThirdApiProperties();
        properties.setAppName("test");
        properties.getDefaultRetry().setMaxAttempts(maxRetries);
        properties.getDefaultRetry().setBackoffMs(backoffMs);
        properties.getDefaultCircuitBreaker().setMinCalls(minCalls);
        properties.getDefaultCircuitBreaker().setFailureRatioThreshold(50);
        properties.getDefaultCircuitBreaker().setOpenTimeoutMs(5000);

        ObjectMapper objectMapper = new ObjectMapper();
        ConfigStore store = new ConfigStore();
        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setVersion(1);
        ApiConfig config = new ApiConfig();
        config.setProvider("demo");
        config.setChannel("local");
        config.setEndpoint("ping");
        config.setBaseUrl("http://127.0.0.1:" + port);
        config.setPath("/ping");
        config.setHttpMethod("GET");
        config.setTimeoutMs(3000);
        config.setMaxRetries(maxRetries);
        config.setRetryBackoffMs(backoffMs);
        config.setAuthType("NONE");
        config.setCircuitBreakerMinCalls(minCalls);
        snapshot.setConfigs(Collections.singletonList(config));
        store.update(snapshot);

        CallLogger logger = new Slf4jCallLogger();
        CallMetrics metrics = new InMemoryCallMetrics();
        return new ApiInvocationExecutor(
                store,
                new RequestBuilder(objectMapper),
                new AuthProcessor(new OAuth2TokenProvider(objectMapper), new HmacSha256ApiSigner()),
                new SimpleHttpClient(),
                new ResiliencePolicy(),
                logger,
                metrics,
                properties,
                objectMapper);
    }

    private ApiInvocation invocation(int port, Object[] args) throws Exception {
        Method method = TestClient.class.getMethod("ping", String.class);
        ApiInvocation invocation = new ApiInvocation();
        invocation.setProvider("demo");
        invocation.setChannel("local");
        invocation.setEndpoint("ping");
        invocation.setBaseUrl("http://127.0.0.1:" + port);
        invocation.setPath("/ping");
        invocation.setHttpMethod(HttpMethod.GET);
        invocation.setArgs(args);
        invocation.setMethod(method);
        invocation.setReturnType(method.getReturnType());
        invocation.setStartedAtMillis(System.currentTimeMillis());
        return invocation;
    }

    private HttpServer startServer(Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    private void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @ThirdPartyApi(provider = "demo", channel = "local")
    private interface TestClient {

        @ApiMethod(name = "ping", path = "/ping", method = HttpMethod.GET)
        String ping(@ApiParam(name = "msg", location = ParamLocation.QUERY) String msg);
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
