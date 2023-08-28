package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.managers.HashManager;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FileChange {
    public String file_name = "";
    public int insertions = 0;
    public int deletions = 0;

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        // standardize the file_name path to unix style for "file_name"
        if (StringUtils.isNotBlank(this.file_name)) {
            this.file_name = this.file_name.replace("\\", "/");
        }
        eventData.put("file_name", HashManager.hashValue(this.file_name, "file_name"));
        eventData.put("insertions", this.insertions);
        eventData.put("deletions", this.deletions);
        return new SelfDescribingJson("iglu:com.software/file_change/jsonschema/1-0-0", eventData);
    }
}
