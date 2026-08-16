package com.thirdapi.starter.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 少量 IO 工具方法，供 HTTP 客户端与配置、鉴权模块共用。
 */
public final class Streams {

    private Streams() {
    }

    /**
     * 按 UTF-8 读取输入流为字符串；空流返回空串。
     */
    public static String readUtf8(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
