package com.software.codetime.snowplow.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.software.codetime.main.PluginInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SnowplowUtilManager {
    public static final Gson gson = new GsonBuilder().create();

    public static String currentDay = null;

    private static JavaTrackerInfo trackerInfo = null;

    public static class JavaTrackerInfo {
        public String artifactId = "";
        public String version = "";
    }

    public static JavaTrackerInfo getTrackerInfo() {
        trackerInfo = new JavaTrackerInfo();
        trackerInfo.artifactId = "codetime-intellij";
        trackerInfo.version = PluginInfo.getVersion();
        return trackerInfo;
    }

    public static boolean isNewDay() {
        String day = getTodayInStandardFormat();
        if (!day.equals(currentDay)) {
            currentDay = day;
            return true;
        }
        return false;
    }

    public static String getTodayInStandardFormat() {
        SimpleDateFormat formatDay = new SimpleDateFormat("YYYY-MM-dd");
        String day = formatDay.format(new Date());
        return day;
    }
}
