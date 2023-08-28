package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.managers.HashManager;

import java.util.HashMap;
import java.util.Map;

public class ProjectEntity {
    // Project related attributes
    public String project_name = "";
    public String project_directory = "";

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("project_name", HashManager.hashValue(this.project_name, "project_name"));
        eventData.put("project_directory", HashManager.hashValue(this.project_directory, "project_directory"));
        return new SelfDescribingJson("iglu:com.software/project/jsonschema/1-0-0", eventData);
    }

}
