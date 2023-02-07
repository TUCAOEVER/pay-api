package com.promc.payapi.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HTTP工具
 */
public final class HttpUtil {

    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";

    /**
     * 工具类隐藏构造方法
     */
    private HttpUtil() {
        throw new UnsupportedOperationException("此类不允许实例化!");
    }

    public static String get(@NotNull String urlString) {
        return get(urlString, StandardCharsets.UTF_8);
    }

    public static String get(@NotNull String urlString, @NotNull Charset charset) {
        try {
            return sendRequest(urlString, GET_METHOD, charset);
        } catch (IOException e) {
            return "";
        }
    }

    public static String post(@NotNull String urlString) {
        return post(urlString, StandardCharsets.UTF_8);
    }

    public static String post(@NotNull String urlString, @NotNull Charset charset) {
        try {
            return sendRequest(urlString, POST_METHOD, charset);
        } catch (IOException e) {
            return "";
        }
    }

    public static String post(@NotNull String urlString, @NotNull String body) {
        try {
            return sendPostRequest(urlString, body);
        } catch (IOException e) {
            return "";
        }
    }

    @NotNull
    public static String sendRequest(@NotNull String urlString, @NotNull String requestMethod, Charset charset) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(requestMethod);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), charset))) {
            StringBuilder builder = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }
            return builder.toString();
        }
    }

    @NotNull
    public static String sendPostRequest(@NotNull String urlString, @NotNull String body) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(POST_METHOD);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(body);
            writer.flush();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            StringBuilder builder = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }
            return builder.toString();
        }
    }
}