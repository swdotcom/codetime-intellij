package com.software.codetime.snowplow.entities;


import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;

public class UIElementEntity {
    // UI Element
    public String element_name = "";
    public String element_location = "";
    public String color = "";
    public String icon_name = "";
    public String cta_text = "";

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("element_name", this.element_name);
        eventData.put("element_location", this.element_location);
        eventData.put("color", this.color);
        eventData.put("icon_name", this.icon_name);
        eventData.put("cta_text", this.cta_text);
        return new SelfDescribingJson("iglu:com.software/ui_element/jsonschema/1-0-5", eventData);
    }
}
