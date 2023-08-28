package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AuthEntity {
    // Auth related attributes
    private String jwt = "";

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    /**
     * Return empty if the value is null or blank
     * @return
     */
    public String getJwt() {
        if (StringUtils.isBlank(this.jwt)) {
            return "";
        }
        return this.jwt;
    }

    public SelfDescribingJson buildContext() {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("jwt", this.getJwt());
        return new SelfDescribingJson("iglu:com.software/auth/jsonschema/1-0-0", eventData);
    }
}
