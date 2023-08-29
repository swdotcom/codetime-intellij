package com.software.codetime.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.managers.IntellijProjectManager;
import com.software.codetime.models.*;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class UtilManager {
    public static final Logger LOG = Logger.getLogger("UtilManager");

    public static final Gson gson = new GsonBuilder().create();
    public final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static final long DAYS_IN_SECONDS = 60 * 60 * 24;

    // Unnamed project name
    public final static String unnamed_project_name = "Unnamed";
    // Untitled file name or directory
    public final static String untitled_file_name = "Untitled";

    private static final long CACHE_EXPIRE_MILLIS = 1000 * 60 * 2; // 2 minutes
    private static final Map<String, CacheData> resultCache = new HashMap<>();

    // cached result of OS detection
    protected static OSType detectedOS;

    public enum OSType {
        Windows, MacOS, Linux, Other
    }

    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public static boolean isWindows() {
        return getOperatingSystemType() == OSType.Windows;
    }

    public static boolean isMac() {
        return getOperatingSystemType() == OSType.MacOS;
    }

    public static boolean isLinux() {
        return (!isMac() && !isWindows());
    }

    public static String getHostname() {
        List<String> cmd = new ArrayList<>();
        cmd.add("hostname");
        return getSingleLineResult(cmd, 1);
    }

    public static String getOsUsername() {
        String username = System.getProperty("user.name");
        if (username == null || username.trim().isEmpty()) {
            try {
                List<String> cmd = new ArrayList<>();
                if (UtilManager.isWindows()) {
                    cmd.add("cmd");
                    cmd.add("/c");
                    cmd.add("whoami");
                } else {
                    cmd.add("/bin/sh");
                    cmd.add("-c");
                    cmd.add("whoami");
                }
                username = getSingleLineResult(cmd, 1);
            } catch (Exception e) {
                //
            }
        }
        return username;
    }

    public static OSType getOperatingSystemType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin"))) {
                detectedOS = OSType.MacOS;
            } else if (OS.contains("win")) {
                detectedOS = OSType.Windows;
            } else if (OS.contains("nux")) {
                detectedOS = OSType.Linux;
            } else {
                detectedOS = OSType.Other;
            }
        }
        return detectedOS;
    }

    public static String getOs() {
        String osInfo = "";
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String osArch = System.getProperty("os.arch");

            if (osArch != null) {
                osInfo += osArch;
            }
            if (osInfo.isEmpty()) {
                osInfo += "_";
            }
            if (osVersion != null) {
                osInfo += osVersion;
            }
            if (osName != null) {
                osInfo += osName;
            }
        } catch (Exception e) {
            //
        }

        return osInfo;
    }

    public static Icon getResourceIcon(String iconName, ClassLoader loader) {
        Icon icon = null;
        if (loader == null) {
            loader = UtilManager.class.getClassLoader();
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(loader.getResource("assets/" + iconName));
            icon = new ImageIcon(bufferedImage);
        } catch (Exception e) {
            //getResourceIcon
        }
        return icon;
    }

    private static String getSingleLineResult(List<String> cmd, int maxLen) {
        String result = null;
        String[] cmdArgs = Arrays.copyOf(cmd.toArray(), cmd.size(), String[].class);
        String content = runCommand(cmdArgs, null);

        // for now just get the 1st one found
        if (content != null) {
            String[] contentList = content.split("\n");
            if (contentList != null && contentList.length > 0) {
                int len = (maxLen != -1) ? Math.min(maxLen, contentList.length) : contentList.length;
                for (int i = 0; i < len; i++) {
                    String line = contentList[i];
                    if (line != null && line.trim().length() > 0) {
                        result = line.trim();
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String runCommand(String[] args, String dir) {
        String command = Arrays.toString(args);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (StringUtils.isNotBlank(dir)) {
            processBuilder.directory(new File(dir));
        }

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            StringBuilder processOutput = new StringBuilder();
            BufferedReader processOutputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String readLine;

            while ((readLine = processOutputReader.readLine()) != null) {
                processOutput.append(readLine).append(System.lineSeparator());
            }

            process.waitFor();
            String result = processOutput.toString().trim();
            return result;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to complete command request: {0}", command);
        }

        return "";
    }

    public static List<String> getResultsForCommandArgs(String[] args, String dir) {
        String cacheId = String.join("_", args);

        CacheData resultData = resultCache.get(cacheId);
        if (resultData != null && resultData.results != null) {
            if (System.currentTimeMillis() - resultData.timestamp > CACHE_EXPIRE_MILLIS) {
                resultCache.remove(cacheId);
            }
            return resultData.results;
        }

        List<String> results = new ArrayList<>();
        try {
            String result = runCommand(args, dir);
            if (result == null || result.trim().length() == 0) {
                return results;
            }
            String[] contentList = result.split("\n");
            if (contentList != null && contentList.length > 0) {
                // remove "warning:" lines
                for (String content : contentList) {
                    if (content.toLowerCase().indexOf("warning:") == -1 && !results.contains(content)) {
                        results.add(content);
                    }
                }
                CacheData cd = new CacheData();
                cd.results = results;
                cd.timestamp = System.currentTimeMillis();
                resultCache.put(cacheId, cd);
            }
        } catch (Exception e) {
            if (results == null) {
                results = new ArrayList<>();
            }
        }
        return results;
    }

    public static Date atStartOfWeek(long local_now) {
        // find out how many days to go back
        int daysBack = 0;
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            while (dayOfWeek != Calendar.SUNDAY) {
                daysBack++;
                dayOfWeek -= 1;
            }
        } else {
            daysBack = 7;
        }

        long startOfDayInSec = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        long startOfWeekInSec = startOfDayInSec - (DAYS_IN_SECONDS * daysBack);

        return new Date(startOfWeekInSec * 1000);
    }

    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // the timestamps are all in seconds
    public static class TimesData {
        public Integer offset;
        public long now;
        public long local_now;
        public String timezone;
        public long local_start_day;
        public long local_start_yesterday;
        public Date local_start_of_week_date;
        public Date local_start_of_yesterday_date;
        public Date local_start_today_date;
        public long local_start_of_week;
        public long local_end_day;
        public long utc_end_day;

        public TimesData() {
            offset = ZonedDateTime.now().getOffset().getTotalSeconds();
            now = System.currentTimeMillis() / 1000;
            local_now = now + offset;
            timezone = TimeZone.getDefault().getID();
            local_start_day = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            local_start_yesterday = local_start_day - DAYS_IN_SECONDS;
            local_start_of_week_date = atStartOfWeek(local_now);
            local_start_of_yesterday_date = new Date(local_start_yesterday * 1000);
            local_start_today_date = new Date(local_start_day * 1000);
            local_start_of_week = local_start_of_week_date.toInstant().getEpochSecond();
            local_end_day = atEndOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            utc_end_day = atEndOfDay(new Date(now * 1000)).toInstant().getEpochSecond();
        }
    }

    public static TimesData getTimesData() {
        TimesData timesData = new TimesData();
        return timesData;
    }

    public static String getTodayInStandardFormat() {
        SimpleDateFormat formatDay = new SimpleDateFormat("YYYY-MM-dd");
        String day = formatDay.format(new Date());
        return day;
    }

    public static boolean isNewDay() {
        String currentDay = FileUtilManager.getItem("currentDay", "");
        final String day = getTodayInStandardFormat();
        boolean dayChanged = !day.equals(currentDay);
        if (dayChanged) {
            // update the current day stats and refresh the sidebar
            SwingUtilities.invokeLater(() -> {

                FileUtilManager.setItem("currentDay", day);

                // update the last payload timestamp
                FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

                SessionSummaryManager.updateSessionSummaryFromServer();

                if (ConfigManager.tree_refresh_runnable != null) {
                    ConfigManager.tree_refresh_runnable.run();
                }
            });
        }
        return dayChanged;
    }

    public static String buildQueryString(JsonObject obj, boolean prependQuestionMark) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = obj.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!obj.get(key).isJsonNull()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }

                String val = obj.get(key).getAsString();
                try {
                    val = URLEncoder.encode(val, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unable to url encode value, error: {0}", e.getMessage());
                }
                sb.append(key).append("=").append(val);
            }
        }
        if (prependQuestionMark) {
            return "?" + sb;
        }
        return sb.toString();
    }

    public static void submitIntellijIssue() {
        launchUrl(ConfigManager.intellij_issues_url);
    }

    public static void launchUrl(String url) {
        boolean useExec = false;
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (Exception e) {
                useExec = true;
            }
        } else {
            useExec = true;
        }

        if (useExec) {
            // try using the runtime
            Runtime runtime = Runtime.getRuntime();
            try {
                if (isWindows()) {
                    try {
                        runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
                    } catch (Exception e1) {
                        runtime.exec(new String[]{"cmd", "/c", "start", url});
                    }
                } else if (isLinux()) {
                    try {
                        // Do a best guess on unix until we get a platform independent way
                        // Build a list of browsers to try, in this order.
                        String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};

                        // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
                        StringBuffer cmd = new StringBuffer();
                        for (int i = 0; i < browsers.length; i++) {
                            cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + url + "\" ");
                        }

                        runtime.exec(new String[]{"sh", "-c", cmd.toString()});

                    } catch (Exception e1) {
                        runtime.exec("xdg-open " + url);
                    }
                } else {
                    runtime.exec("open " + url);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Unable to open url: {0}", e.getMessage());
            }
        }
    }

    public static String humanizeMinutes(long minutes) {
        String str = "";
        if (minutes == 60) {
            str = "1h";
        } else if (minutes > 60) {
            double hours = Math.floor(minutes / 60);
            float remaining_minutes = minutes % 60;
            String hourStr = String.format("%.0f", Math.floor(hours)) + "h";
            if ((remaining_minutes / 60) % 1 == 0) {
                str = hourStr;
            } else {
                str = hourStr + " " + String.format("%.0f", remaining_minutes) + "m";
            }
        } else if (minutes == 1) {
            str = "1m";
        } else {
            str = minutes + "m";
        }
        return str;
    }

    public static boolean isGitProject(String projectDir) {
        if (projectDir == null || projectDir.equals("")) {
            return false;
        }

        String gitFile = projectDir + File.separator + ".git";
        File f = new File(gitFile);
        return f.exists();
    }

    public static int getLineCount(String fileName) {
        Stream<String> stream = null;
        try {
            Path path = Paths.get(fileName);
            stream = Files.lines(path);
            return (int) stream.count();
        } catch (Exception e) {
            return 0;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    public static FileDetails getFileDetails(IdeProject ideProject, String fullFileName) {
        FileDetails fileDetails = new FileDetails();
        if (StringUtils.isNotBlank(fullFileName)) {
            fileDetails.full_file_name = fullFileName;
            Project p = ideProject.getProjectForPath(fullFileName);
            if (p != null) {
                fileDetails.project_directory = p.getDirectory();
                fileDetails.project_name = p.getName();
            }

            File f = new File(fullFileName);

            if (f.exists()) {
                fileDetails.character_count = f.length();
                fileDetails.file_name = f.getName();
                if (StringUtils.isNotBlank(fileDetails.project_directory) && fullFileName.indexOf(fileDetails.project_directory) != -1) {
                    // strip out the project_file_name
                    String[] parts = fullFileName.split(fileDetails.project_directory);
                    if (parts.length > 1) {
                        fileDetails.project_file_name = parts[1];
                    } else {
                        fileDetails.project_file_name = fullFileName;
                    }
                } else {
                    fileDetails.project_file_name = fullFileName;
                }
                if (fileDetails.line_count == 0) {
                    fileDetails.line_count = getLineCount(fullFileName);
                }

                fileDetails.syntax = ideProject.getFileSyntax(f);
            }
        }

        return fileDetails;
    }

    public static void launchFile(String fsPath) {
        com.intellij.openapi.project.Project p = IntellijProjectManager.getOpenProject();
        if (p == null) {
            return;
        }
        File f = new File(fsPath);
        if (f.exists()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
                    OpenFileDescriptor descriptor = new OpenFileDescriptor(p, vFile);
                    FileEditorManager mgr = FileEditorManager.getInstance(p);
                    if (mgr != null) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                mgr.openTextEditor(descriptor, true);
                            } catch (Exception e) {
                                System.out.println("Error opening file: " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {

                }
            });
        }
    }
}
