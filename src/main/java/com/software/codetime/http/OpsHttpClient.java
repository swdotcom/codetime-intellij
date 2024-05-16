package com.software.codetime.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.JsonTypeInfo;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpsHttpClient {
    public static final Logger LOG = Logger.getLogger("Software");

    private static final String slack_endpoint = "https://slack.com/api";

    private static final String spotify_endpoint = "https://api.spotify.com";
    private static final String spotify_account_endpoint = "https://accounts.spotify.com";

    public static ClientResponse softwareGet(String api, String token) {
        return makeApiCall(HttpGet.METHOD_NAME, ConfigManager.metrics_endpoint + api, token, null, null);
    }

    public static ClientResponse softwarePost(String api, String token, JsonObject payload) {
        return makeApiCall(HttpPost.METHOD_NAME, ConfigManager.metrics_endpoint + api, token, payload, null);
    }

    public static ClientResponse appGet(String api) {
        return makeApiCall(HttpGet.METHOD_NAME, ConfigManager.app_url + api, getBearerToken(), null, null);
    }

    public static ClientResponse appPost(String api, JsonObject payload) {
        return makeApiCall(HttpPost.METHOD_NAME, ConfigManager.app_url + api, getBearerToken(), payload, null);
    }

    public static ClientResponse appPut(String api, JsonObject payload) {
        return makeApiCall(HttpPut.METHOD_NAME, ConfigManager.app_url + api, getBearerToken(), payload, null);
    }

    public static ClientResponse appDelete(String api, JsonObject payload) {
        return makeApiCall(HttpDelete.METHOD_NAME, ConfigManager.app_url + api, getBearerToken(), payload, null);
    }

    public static ClientResponse slackGet(String api, String access_token) {
        return makeApiCall(HttpGet.METHOD_NAME, slack_endpoint + api, "Bearer " + access_token, null, null);
    }

    public static ClientResponse slackPost(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpPost.METHOD_NAME, slack_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static ClientResponse slackPut(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpPut.METHOD_NAME, slack_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static ClientResponse slackDelete(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpDelete.METHOD_NAME, slack_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static ClientResponse spotifyGet(String api, String access_token) {
        return makeApiCall(HttpGet.METHOD_NAME, spotify_endpoint + api, "Bearer " + access_token, null, null);
    }

    public static ClientResponse spotifyPost(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpPost.METHOD_NAME, spotify_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static ClientResponse spotifyPut(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpPut.METHOD_NAME, spotify_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static ClientResponse spotifyDelete(String api, String access_token, JsonObject payload) {
        return makeApiCall(HttpDelete.METHOD_NAME, spotify_endpoint + api, "Bearer " + access_token, payload, null);
    }

    public static String spotifyTokenRefresh(String refreshToken, String clientId, String clientSecret) {
        String api = "/api/token";
        String authPayload = clientId + ":" + clientSecret;
        byte[] bytesEncoded = Base64.encodeBase64(authPayload.getBytes());
        String encodedAuthPayload = "Basic " + new String(bytesEncoded);
        JsonObject params = new JsonObject();
        params.addProperty("grant_type", "refresh_token");
        params.addProperty("refresh_token", refreshToken);
        String url = spotify_account_endpoint + api + UtilManager.buildQueryString(params, true);
        ClientResponse resp = makeApiCall(HttpPost.METHOD_NAME, url, encodedAuthPayload, null, null);
        if (resp != null && resp.getJsonObj() != null && resp.getJsonObj().has("access_token")) {
            return resp.getJsonObj().get("access_token").getAsString();
        }
        return null;
    }

    public static ClientResponse executePostRequest(String url, JsonObject payload, JsonObject headers) {
        return makeApiCall(HttpPost.METHOD_NAME, url, null, payload, headers);
    }

    public static ClientResponse executeGetRequest(String url, JsonObject headers) {
        return makeApiCall(HttpGet.METHOD_NAME, url, null, null, headers);
    }

    private static ClientResponse makeApiCall(
            String method,
            String api,
            String auth_token,
            JsonObject payload,
            JsonObject headers) {
        ClientResponse softwareResponse = new ClientResponse();

        OpsHttpManager httpMgr = new OpsHttpManager(method, api, auth_token, payload, headers);

        Future<HttpResponse> response = UtilManager.EXECUTOR_SERVICE.submit(httpMgr);

        //
        // Handle the Future if it exist
        //
        if (response != null) {
            try {
                HttpResponse httpResponse = response.get();
                if (httpResponse != null) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode < 400) {
                        softwareResponse.setIsOk(true);
                    }
                    HttpEntity entity = httpResponse.getEntity();

                    ContentType contentType = ContentType.getOrDefault(entity);
                    String mimeType = contentType.getMimeType();
                    boolean isPlainText = mimeType.indexOf("text/plain") != -1 || mimeType.indexOf("text/html") != -1;

                    if (entity != null) {
                        try {
                            JsonTypeInfo typeInfo = null;
                            String str = FileUtilManager.getStringRepresentation(entity, isPlainText);
                            if (!isPlainText) {
                                // validate/clean the json
                                typeInfo = FileUtilManager.cleanJsonString(str);
                            }
                            softwareResponse.setJsonStr(str);

                            if (typeInfo != null && !isPlainText) {
                                try {
                                    if (typeInfo.el.isJsonPrimitive()) {
                                        if (statusCode < 400) {
                                            softwareResponse.setDataMessage(typeInfo.el.getAsString());
                                        } else {
                                            softwareResponse.setErrorMessage(typeInfo.el.getAsString());
                                        }
                                    } else if (typeInfo.el.isJsonArray()) {
                                        softwareResponse.setJsonArray(typeInfo.el.getAsJsonArray());
                                    } else {
                                        softwareResponse.setJsonObj(typeInfo.el.getAsJsonObject());
                                    }
                                } catch (Exception e) {
                                    String errorMessage = "swdc.java.ops: Unable to parse response data:" + e.getMessage();
                                    LOG.log(Level.WARNING, errorMessage);
                                    softwareResponse.setErrorMessage(errorMessage);
                                }
                            }
                        } catch (Exception e) {
                            String errorMessage = "swdc.java.ops: Unable to get the response from the http request, error: " + e.getMessage();
                            softwareResponse.setErrorMessage(errorMessage);
                            LOG.log(Level.WARNING, errorMessage);
                        }
                    }

                }
            } catch (Exception e) {
                String errorMessage = "swdc.java.ops: Unable to get the response from the http request, error: " + e.getMessage();
                softwareResponse.setErrorMessage(errorMessage);
                LOG.log(Level.WARNING, errorMessage);
            }
        }

        return softwareResponse;
    }


    protected static class OpsHttpManager implements Callable<HttpResponse> {
        private static final int timeout = 10;
        private static final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();
        private static final CloseableHttpClient httpClient =
                HttpClientBuilder.create().setDefaultRequestConfig(config).build();

        private static String api;
        private static String auth_token;
        private static String httpMethodName;
        private static JsonObject payloadObj;
        private static JsonObject headersObj;

        public OpsHttpManager(String method, String api_val, String auth_token_val, JsonObject payload, JsonObject headers) {
            api = api_val;
            if (payload != null && payload.keySet().size() > 0) {
                payloadObj = payload;
            }
            auth_token = auth_token_val;
            httpMethodName = method;
            if (headers != null && headers.keySet().size() > 0) {
                headersObj = headers;
            }
        }

        @Override
        public HttpResponse call() {
            //boolean isUrlEncoded = payloadObj != null && api.contains("?");
            boolean isUrlEncoded = payloadObj != null;
            HttpUriRequest req = null;
            try {
                HttpResponse response = null;

                UrlEncodedFormEntity entity = null;
                if (payloadObj != null) {
                    // add the payload to the entity
                    List<NameValuePair> form = new ArrayList<>();
                    for (String key : payloadObj.keySet()) {
                        JsonElement el = payloadObj.get(key);
                        if (el.isJsonPrimitive()) {
                            form.add(new BasicNameValuePair(key, payloadObj.get(key).getAsString()));
                        } else {
                            form.add(new BasicNameValuePair(key, payloadObj.get(key).toString()));
                        }
                    }

                    entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
                }

                switch (httpMethodName) {
                    case HttpPost.METHOD_NAME:
                        req = new HttpPost(api);
                        if (entity != null) {
                            ((HttpPost) req).setEntity(entity);
                        }
                        break;
                    case HttpPut.METHOD_NAME:
                        req = new HttpPut(api);
                        if (entity != null) {
                            ((HttpPut) req).setEntity(entity);
                        }
                        break;
                    case HttpDelete.METHOD_NAME:
                        if (entity != null) {
                            req = new HttpDeleteWithBody(api);
                            ((HttpDeleteWithBody) req).setEntity(entity);
                        } else {
                            req = new HttpDelete(api);
                        }
                        break;
                    default:
                        req = new HttpGet(api);
                        break;
                }

                UtilManager.TimesData timesData = UtilManager.getTimesData();
                if (api.indexOf(ConfigManager.metrics_endpoint) != -1 ||
                        api.indexOf(ConfigManager.app_url) != -1) {
                    req.addHeader("X-SWDC-Plugin-Id", String.valueOf(ConfigManager.plugin_id));
                    req.addHeader("X-SWDC-Plugin-Name", ConfigManager.plugin_name);
                    req.addHeader("X-SWDC-Plugin-Version", ConfigManager.plugin_version);
                    req.addHeader("X-SWDC-Plugin-OS", UtilManager.getOs());
                    req.addHeader("X-SWDC-Plugin-TZ", timesData.timezone);
                    req.addHeader("X-SWDC-Plugin-Offset", String.valueOf(timesData.offset));
                    req.addHeader("X-SWDC-Plugin-Ide-Name", ConfigManager.ide_name);
                    req.addHeader("X-SWDC-Plugin-Ide-Version", ConfigManager.ide_version);
                    req.addHeader("X-SWDC-Plugin-Type", ConfigManager.plugin_type);
                    req.addHeader("X-SWDC-Plugin-Editor", ConfigManager.plugin_editor);
                    if (ConfigManager.theme_mode_handler != null) {
                        req.addHeader("X-SWDC-Is-Light-Mode", String.valueOf(ConfigManager.theme_mode_handler.isLightMode()));
                    } else {
                        req.addHeader("X-SWDC-Is-Light-Mode", "false");
                    }
                }

                boolean isRefreshAccessToken = (StringUtils.isNotBlank(auth_token) && auth_token.contains("Basic"));

                if (isUrlEncoded || isRefreshAccessToken) {
                    req.addHeader("Content-Type", "application/x-www-form-urlencoded");
                } else {
                    req.addHeader("Content-Type", "application/json; charset=utf-8");
                }

                if (headersObj != null) {
                    for (String key : headersObj.keySet()) {
                        req.addHeader(key, headersObj.get(key).getAsString());
                    }
                }

                if (StringUtils.isNotBlank(auth_token)) {
                    req.addHeader("Authorization", auth_token);
                }

                // execute the request
                response = httpClient.execute(req);

                //
                // Return the response
                //
                return response;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "swdc.java.ops: Unable to make api request.{0}", e.getMessage());
            }

            return null;
        }
    }

    public static String getBearerToken() {
        String jwt = FileUtilManager.getItem("jwt");
        if (StringUtils.isNotBlank(jwt) && jwt.indexOf("JWT ") == 0) {
            // convert it to bearer
            return "Bearer " + jwt.split("JWT ")[1];
        }
        return jwt;
    }
}
