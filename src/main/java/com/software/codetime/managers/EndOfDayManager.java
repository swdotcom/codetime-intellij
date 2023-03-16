package com.software.codetime.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.AsyncManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

public class EndOfDayManager {

    private static final int MIN_IN_SEC = 60;
    private static final int HOUR_IN_SEC = MIN_IN_SEC * 60;

    public static void setEndOfDayNotification() {
        ClientResponse resp = OpsHttpClient.softwareGet("/users/profile", FileUtilManager.getItem("jwt"));
        if (resp.isOk() && !resp.getJsonObj().isJsonNull()) {

            // get the profile
            JsonElement workHours = null;
            long secondsUntilEndOfTheDay = 0;

            try {
                workHours = resp.getJsonObj().get("work_hours").getAsJsonObject();
                secondsUntilEndOfTheDay = getSecondsDelayUsingV2Format((JsonObject) workHours);
            } catch (Exception e) {
                // the work hours may come in this format as well
                // [[118800,147600],[205200,234000],[291600,320400],[378000,406800],[464400,493200]]
                // just give a default of 5pm
                secondsUntilEndOfTheDay = getSecondsUntilEndOfTheDay(new Date(), HOUR_IN_SEC * 17);
            }

            if (secondsUntilEndOfTheDay > 0) {
                // schedule it
                AsyncManager.getInstance().executeOnceInSeconds(() -> showEndOfDayNotification(), secondsUntilEndOfTheDay);
            }
        }
    }

    private static void showEndOfDayNotification() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Object[] options = {"Show me the data", "Settings"};
            String title = "Code Time Dashboard";
            String msg = "It's the end of your work day!\nWould you like to see your code time stats for today?";

            Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", EndOfDayManager.class.getClassLoader());

            int choice = JOptionPane.showOptionDialog(
                    null, msg, title, JOptionPane.OK_OPTION,
                    JOptionPane.QUESTION_MESSAGE, icon, options, options[0]);

            if (choice == 1) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/preferences");
                });
            } else if (choice == 0) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/code_time?view=summary");
                });
            }
        });
    }

    private static long getSecondsDelayUsingV2Format(JsonObject workHours) {
        if (workHours != null && !workHours.isJsonNull()) {
            Date d = new Date();
            String dow_lc = new SimpleDateFormat("EEE").format(d).toLowerCase(Locale.ROOT);
            // get today's work hours
            JsonObject workHoursToday = workHours.get(dow_lc).getAsJsonObject();
            if (workHoursToday.get("active").getAsBoolean()) {
                // it's active, get the largest end range
                List<Long> endTimes = new ArrayList<>();
                JsonArray ranges = workHoursToday.get("ranges").getAsJsonArray();
                if (ranges != null && !ranges.isJsonNull()) {
                    for (JsonElement el : ranges) {
                        long endSeconds = getEndTimeSeconds(((JsonObject)el).get("end").getAsString());
                        endTimes.add(endSeconds);
                    }

                    // sort seconds in descending order
                    endTimes.sort(Collections.reverseOrder());
                    return getSecondsUntilEndOfTheDay(d, endTimes.get(0));
                }
            }
        }
        return 0;
    }

    private static long getEndTimeSeconds(String end) {
        String[] hourMinParts = end.split(":");
        return (Integer.parseInt(hourMinParts[0], 10) * HOUR_IN_SEC) + (Integer.parseInt(hourMinParts[1], 10) * MIN_IN_SEC);
    }

    private static long getSecondsUntilEndOfTheDay(Date d, long endSeconds) {
        LocalTime now = LocalTime.now(ZoneId.systemDefault());
        return endSeconds - now.toSecondOfDay();
    }
}
