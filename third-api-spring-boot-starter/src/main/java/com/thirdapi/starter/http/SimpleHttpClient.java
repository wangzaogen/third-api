package com.thirdapi.starter.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SimpleHttpClient {

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
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = read(input);
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

    private String read(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
