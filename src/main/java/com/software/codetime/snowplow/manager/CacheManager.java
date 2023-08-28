package com.software.codetime.snowplow.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheManager {

    public static String jwt = null;

    private static final Map<String, List<String>> hashMap = new HashMap<>();

    /**
     * Check to see if a hash value is found for a given data type
     * @param dataType (i.e. file_name)
     * @param matchingHash
     * @return boolean
     */
    public static boolean hasCachedValue(String dataType, String matchingHash) {
        List<String> hashValues = hashMap.get(dataType);
        if (hashValues != null) {
            for (String val : hashValues) {
                if (val.equalsIgnoreCase(matchingHash)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void updateCacheValues(String dataType, List<String> hashValues) {
        hashMap.put(dataType, hashValues);
    }

    public static void addCacheValue(String dataType, String hashValue) {
        List<String> hashValues = hashMap.get(dataType);
        if (hashValues == null) {
            hashValues = new ArrayList<>();
        }
        hashValues.add(hashValue);
        hashMap.put(dataType, hashValues);
    }

    public static List<String> getDataTypeValues(String dataType) {
        return hashMap.get(dataType);
    }
}
