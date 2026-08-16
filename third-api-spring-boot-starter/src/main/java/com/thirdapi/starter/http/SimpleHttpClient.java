package com.thirdapi.starter.http;

import com.thirdapi.starter.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 基于 HttpURLConnection 的轻量 HTTP 客户端。
 */
public class SimpleHttpClient {

    /**
     * 执行一次 HTTP 请求；4xx/5xx 会被标记为 HTTP_xxx 错误，IO 异常标记为 IO。
     */
    public HttpCallResult execute(ApiRequest request, int connectTimeoutMs, int readTimeoutMs) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
            connection.setRequestMethod(request.getMethod());
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setInstanceFollowRedirects(true);
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", request.getContentType());
                byte[] bytes = request.getBody().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                OutputStream output = connection.getOutputStream();
                output.write(bytes);
                output.flush();
                output.close();
            }
            int status = connection.getResponseCode();
            // 4xx/5xx 读取错误流，其他状态读取正常响应流
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = Streams.readUtf8(input);
            HttpCallResult result = new HttpCallResult(status, body);
            if (status >= 400) {
                result.setErrorType("HTTP_" + status);
                result.setErrorMessage(body == null ? "" : body);
            }
            return result;
        } catch (IOException e) {
            HttpCallResult result = new HttpCallResult();
            result.setErrorType("IO");
            result.setErrorMessage(e.getMessage());
            return result;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
