package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.managers.HashManager;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FileEntity {
    // Editor file related attributes
    public String file_name = "";
    public String file_path = "";
    public String syntax = "";
    public int line_count = 0;
    public long character_count = 0;

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        // standardize the file_name path to unix style for "file_name"
        if (StringUtils.isNotBlank(this.file_name)) {
            this.file_name = this.file_name.replace("\\", "/");
        }
        eventData.put("file_name", HashManager.hashValue(this.file_name, "file_name"));
        eventData.put("file_path", HashManager.hashValue(this.file_path, "file_path"));
        eventData.put("syntax", this.syntax);
        eventData.put("line_count", this.line_count);
        eventData.put("character_count", this.character_count);
        return new SelfDescribingJson("iglu:com.software/file/jsonschema/1-0-1", eventData);
    }
}
