package com.software.codetime.snowplow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.software.codetime.managers.CacheManager;
import com.software.codetime.snowplow.manager.SnowplowUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Http {

    private static final Logger LOG = Logger.getLogger("Http");

    private static CloseableHttpClient client;
    private static String apiHost = "";
    private static SnowplowUtilManager.JavaTrackerInfo trackerInfo;

    static {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .setSocketTimeout(30000)
                .build();

        client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }

    public static void initialize(String host) {
        apiHost = host;
    }

    public static void initialize(String host, CloseableHttpClient httpClient) {
        client = httpClient;
        apiHost = host;
    }

    public static Response get(String api) {
        HttpUriRequest httpGet = new HttpGet(apiHost + api);
        return execute(httpGet, null);
    }

    public static Response post(String api, Object payload) throws UnsupportedEncodingException {
        String jsonStr = null;

        // convert to a json string
        if (payload != null) {
            jsonStr = SnowplowUtilManager.gson.toJson(payload);
        }

        HttpUriRequest httpPost = new HttpPost(apiHost + api);
        return execute(httpPost, jsonStr);
    }

    private static Response execute(HttpUriRequest req, String payload) {
        Response response = new Response();
        trackerInfo = SnowplowUtilManager.getTrackerInfo();

        req.setHeader("Content-type", "application/json");
        req.addHeader("X-SWDC-Tracker-Version", trackerInfo.version);
        req.addHeader("X-SWDC-Tracker-Id", trackerInfo.artifactId);

        // add the jwt if we have it
        if (StringUtils.isNotBlank(CacheManager.jwt)) {
            req.addHeader("Authorization", CacheManager.jwt);
        }

        if (payload != null && req.getMethod().equals(HttpPost.METHOD_NAME)) {
            // add the payload to the request
            try {
                StringEntity params = new StringEntity(payload);
                ((HttpPost) req).setEntity(params);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "post payload format error: " + e);
                return new Response();
            }
        }

        try (final CloseableHttpResponse httpResp = client.execute(req)) {
            if (httpResp != null && httpResp.getStatusLine() != null) {
                response.statusCode = httpResp.getStatusLine().getStatusCode();
                if (response.statusCode < 400) {
                    HttpEntity entity = httpResp.getEntity();
                    if (entity != null) {
                        Reader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
                        response.responseData = (JsonObject) UtilManager.gson.fromJson(reader, JsonElement.class);
                        response.ok = true;
                    }
                } else {
                    LOG.log(Level.WARNING, "java tracker request issue, status: " + response.statusCode);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "java tracker request error: " + e);
            response.ok = false;

        }
        return response;
    }

}
