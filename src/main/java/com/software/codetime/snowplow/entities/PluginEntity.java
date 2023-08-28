package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;

public class PluginEntity {
    // Plugin related attributes
    public int plugin_id = 0;
    public String plugin_version = "";
    public String plugin_name = "";

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("plugin_id", this.plugin_id);
        eventData.put("plugin_version", this.plugin_version);
        eventData.put("plugin_name", this.plugin_name);
        return new SelfDescribingJson("iglu:com.software/plugin/jsonschema/1-0-1", eventData);
    }
}
