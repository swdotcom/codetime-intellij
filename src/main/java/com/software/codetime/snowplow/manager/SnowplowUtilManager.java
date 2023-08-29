package com.software.codetime.snowplow.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class SnowplowUtilManager {
    public static final Gson gson = new GsonBuilder().create();

    private static final Logger LOG = Logger.getLogger("UtilManager");

    public static String currentDay = null;

    private static JavaTrackerInfo trackerInfo = null;

    public static class JavaTrackerInfo {
        public String artifactId = "";
        public String version = "";
    }

    public static JavaTrackerInfo getTrackerInfo() {
        Model mavenModel = getMavenInfo();
        trackerInfo = new JavaTrackerInfo();
        trackerInfo.artifactId = mavenModel.getArtifactId();
        trackerInfo.version = mavenModel.getVersion();
        return trackerInfo;
    }

    private static Model getMavenInfo() {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try {
            if ((new File("pom.xml")).exists()) {
                model = reader.read(new FileReader("pom.xml"));
            } else {
                model = reader.read(
                        new InputStreamReader(
                                SnowplowUtilManager.class.getResourceAsStream("pom.xml")
                        )
                );
            }
        } catch (Exception e) {
            model = new Model();
            model.setArtifactId("swdc-java-ops");
            model.setGroupId("swdc.java.ops");
            model.setVersion("1.1.4");
        }
        return model;
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
