package com.software.codetime.snowplow.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.software.codetime.main.PluginInfo;

public class SnowplowUtilManager {
    public static final Gson gson = new GsonBuilder().create();

    public static class JavaTrackerInfo {
        public String artifactId = "";
        public String version = "";
    }

    public static JavaTrackerInfo getTrackerInfo() {
        JavaTrackerInfo trackerInfo = new JavaTrackerInfo();
        trackerInfo.artifactId = "codetime-intellij";
        trackerInfo.version = PluginInfo.getVersion();
        return trackerInfo;
    }
}
