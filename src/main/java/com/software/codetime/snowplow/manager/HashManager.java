package com.software.codetime.snowplow.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.software.codetime.managers.CacheManager;
import com.software.codetime.snowplow.client.Http;
import com.software.codetime.snowplow.client.Response;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.digest.Blake2b;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class HashManager {

    private static final Logger LOG = Logger.getLogger("HashManager");

    private static Blake2b.Blake2b512 blake2b = null;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String hashValue(String value, String dataType) {
        // return an empty string if the jwt is null or empty
        if (StringUtils.isBlank(CacheManager.jwt)) {
            return "";
        }

        // check the cache first
        if (StringUtils.isNotBlank(value)) {
            try {
                // create the hash value
                String hashedValue = produceHash(value);
                boolean isCached = CacheManager.hasCachedValue(dataType, hashedValue);
                if (!isCached) {
                    // doesn't exist yet, encrypt it then update the cache
                    encryptValue(value, hashedValue, dataType);

                    // populate it with the single value
                    CacheManager.addCacheValue(dataType, hashedValue);

                    // populate the hashed values cache
                    populateHashValues();
                }
                return hashedValue;
            } catch (Exception e) {
                LOG.warning("[jtrack] Hashing error: " + e);
            }
        }
        return "";
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for(int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 15];
        }

        return new String(hexChars);
    }

    /**
     * This is separate to allow testing to verify the hashed value
     * @param value
     * @return
     */
    public static String produceHash(String value) {
        if (blake2b == null) {

            // initialize the caches
            HashManager.populateHashValues();

            blake2b = new Blake2b.Blake2b512();

            // update the isNewDay value
            SnowplowUtilManager.isNewDay();
        }

        try {
            // create the hash value

            byte[] hashedBytes = blake2b.digest(value.getBytes(StandardCharsets.UTF_8));
            String hashedValue = bytesToHex(hashedBytes).toLowerCase();
            return hashedValue;
        } catch (Exception e) {
            LOG.warning("[jtrack] Hashing error: " + e);
        }
        return "";
    }

    private static void encryptValue(String value, String hashedValue, String dataType) {
        JsonObject params = new JsonObject();
        params.addProperty("value", value);
        params.addProperty("hashed_value", hashedValue);
        params.addProperty("data_type", dataType);

        // normal response {"message":"success"}
        try {
            Http.post("/user_encrypted_data", params);
        } catch (Exception e) {
            LOG.warning("[jtrack] Error posting encryption information: " + e);
        }
    }

    public static void populateHashValues() {
        Response resp = Http.get("/hashed_values");

        if (resp.ok && resp.responseData != null) {
            // data will return like this: {"data": {data_type: ["hashedVal1", hashedVal2"]}}
            JsonObject responseData = resp.responseData;

            if (responseData != null && responseData.has("data")) {

                JsonObject dataTypeJson = responseData.get("data").getAsJsonObject();

                // { data_type: ["hashedval1", "hashedval2"],}
                Set<String> keys = dataTypeJson.keySet();

                // go through each key and set cache
                for (String key : keys) {
                    List<String> hashValueList = null;

                    JsonArray list = (JsonArray) dataTypeJson.get(key);
                    if (list != null && list.size() > 0) {
                        Type type = new TypeToken<List<String>>() {
                        }.getType();
                        hashValueList = SnowplowUtilManager.gson.fromJson(SnowplowUtilManager.gson.toJson(list), type);
                    }

                    CacheManager.updateCacheValues(key, hashValueList);
                }
            }
        }
    }
}
