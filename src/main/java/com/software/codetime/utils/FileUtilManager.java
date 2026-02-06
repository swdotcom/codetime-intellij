package com.software.codetime.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.JsonTypeInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

public class FileUtilManager {
    public static final Logger log = Logger.getLogger("FileManager");

    private static JsonObject cachedSessionJson = null;
    private static long last_session_json_millis = 0;

    private static String getOsSpecificName(String fileName) {
        return File.separator + fileName;
    }

    public static String getSoftwareDir(boolean autoCreate) {
        String softwareDataDir = UtilManager.getUserHomeDir() + getOsSpecificName(ConfigManager.software_dir);

        File f = new File(softwareDataDir);
        if (autoCreate && !f.exists()) {
            // make the directory
            f.mkdirs();
        }

        return softwareDataDir;
    }

    public static String getSoftwareSessionFile(boolean autoCreate) {
        return getSoftwareDir(autoCreate) + getOsSpecificName("session.json");
    }

    private static String getDeviceFile() {
        return getSoftwareDir(true) + getOsSpecificName("device.json");
    }

    public static String getSessionDataSummaryFile() {
        return getSoftwareDir(true) + getOsSpecificName("sessionSummary.json");
    }

    public static String getCodeTimeDashboardFile() {
        return getSoftwareDir(true) + getOsSpecificName("CodeTime.txt");
    }

    public static String getCodeTimeDashboardHtmlFile() {
        return getSoftwareDir(true) + getOsSpecificName("CodeTimeDashboard.html");
    }

    public static String getCodeTimeViewHtmlFile() {
        return getSoftwareDir(true) + getOsSpecificName("CodeTimeView.html");
    }

    public static String getEditorOpsExplorerHtmlFile() {
        return getSoftwareDir(true) + getOsSpecificName("EditorOpsExplorer.html");
    }

    public static String getEditorOpsAutomationConfigurationHtmlFile() {
        return getSoftwareDir(true) + getOsSpecificName("EditorOpsAutomationConfiguration.html");
    }

    public static String getAutomationHistoryFile() {
        return getSoftwareDir(true) + getOsSpecificName("automationHistory.json");
    }

    public static String getAutomationsConfigFile() {
        return getSoftwareDir(true) + getOsSpecificName("automationsConfig.json");
    }

    public static String getFlowChangeFile() {
        return getSoftwareDir(true) + getOsSpecificName("flowChange.json");
    }

    public static String getGitEventFile() {
        return getSoftwareDir(true) + getOsSpecificName("gitEvents.json");
    }

    public static synchronized void writeData(String file, Object o) {
        if (o == null) {
            return;
        }
        File f = new File(file);
        final String content = UtilManager.gson.toJson(o);

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(content);
        } catch (IOException e) {
            log.warning("swdc.java.ops: Error writing content: " + e.getMessage());
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    /**
     * Writes JSON content only when it changed.
     * This reduces disk churn and helps avoid watch-service feedback loops.
     */
    public static synchronized void writeDataIfChanged(String file, Object o) {
        if (o == null) {
            return;
        }

        final String content = UtilManager.gson.toJson(o);
        try {
            Path path = Paths.get(file);
            if (Files.exists(path)) {
                String existing = Files.readString(path, StandardCharsets.UTF_8);
                if (content.equals(existing)) {
                    return;
                }
            }
        } catch (Exception e) {
            // If we fail to read/compare, fall back to writing.
        }

        writeData(file, o);
    }

    public static void appendData(String file, Object o) {
        if (o == null) {
            return;
        }
        File f = new File(file);
        String content = UtilManager.gson.toJson(o);

        if (UtilManager.isWindows()) {
            content += "\r\n";
        } else {
            content += "\n";
        }
        final String contentToWrite = content;
        try {
            Writer output;
            output = new BufferedWriter(new FileWriter(f, true));  //clears file every time
            output.append(contentToWrite);
            output.close();
        } catch (Exception e) {
            log.warning("swdc.java.ops: Error appending content: " + e.getMessage());
        }
    }

    public static JsonArray getFileContentAsJsonArray(String file) {
        return getJsonArrayFromFile(file);
    }

    public static JsonObject getFileContentAsJson(String file) {
        return getJsonObjectFromFile(file);
    }

    public static void saveFileContent(String file, String content) {
        File f = new File(file);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(content);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    private synchronized static void writeSessionJsonContent(JsonObject obj) {
        cachedSessionJson = obj;
        File f = new File(getSoftwareSessionFile(true));
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(obj.toString());
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static void writeStringContent(File f, String json) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(json);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static String getItem(String key) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            try {
                return sessionJson.get(key).getAsString();
            } catch (Exception e) {/*ignore*/}
        }
        return null;
    }

    public static String getItem(String key, String defaultVal) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            try {
                return sessionJson.get(key).getAsString();
            } catch (Exception e) {/*ignore*/}
        }
        return defaultVal;
    }

    public static long getNumericItem(String key, long defaultVal) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsLong();
        }
        return defaultVal;
    }


    public static boolean getBooleanItem(String key) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has(key) && !sessionJson.get(key).isJsonNull()) {
            return sessionJson.get(key).getAsBoolean();
        }
        return false;
    }

    public static void setItem(String key, String val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static void setBooleanItem(String key, boolean val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static void setNumericItem(String key, long val) {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        sessionJson.addProperty(key, val);
        writeSessionJsonContent(sessionJson);
    }

    public static JsonObject getSoftwareSessionAsJson() {
        String sessionFile = getSoftwareSessionFile(true);
        if (cachedSessionJson == null || System.currentTimeMillis() - last_session_json_millis > 2000) {
            cachedSessionJson = getJsonObjectFromFile(sessionFile);
            last_session_json_millis = System.currentTimeMillis();
        }
        if (cachedSessionJson == null) {
            // data doesn't exist, create the file
            writeSessionJsonContent(new JsonObject());
            cachedSessionJson = getJsonObjectFromFile(sessionFile);
        }
        return cachedSessionJson;
    }

    public static JsonObject getJsonObjectFromFile(String fileName) {
        JsonObject jsonObject = new JsonObject();
        String content = getFileContent(fileName);

        if (content != null) {
            // json parse it
            jsonObject = readAsJsonObject(content);
        }

        if (jsonObject == null) {
            jsonObject = new JsonObject();
        }
        return jsonObject;
    }

    public static JsonArray getJsonArrayFromFile(String fileName) {
        JsonArray jsonArray = new JsonArray();
        String content = getFileContent(fileName);

        if (content != null) {
            // json parse it
            jsonArray = readAsJsonArray(content);
        }

        if (jsonArray == null) {
            jsonArray = new JsonArray();
        }
        return jsonArray;
    }

    public static String getFileContent(String file) {
        String content = null;

        File f = new File(file);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(file));
                content = new String(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warning("Error trying to read and parse: " + e.getMessage());
            }
        }
        return content;
    }

    public static String getPluginUuid() {
        String plugin_uuid = null;
        JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
        if (deviceJson.has("plugin_uuid") && !deviceJson.get("plugin_uuid").isJsonNull()) {
            plugin_uuid = deviceJson.get("plugin_uuid").getAsString();
        } else {
            // set it for the 1st and only time (sha1 + ":" + uuid)
            String name = UtilManager.getHostname() + UtilManager.getOsUsername();
            plugin_uuid = DigestUtils.sha1Hex(name.trim()) + ":" + UUID.randomUUID();
            deviceJson.addProperty("plugin_uuid", plugin_uuid);
            String content = deviceJson.toString();
            saveFileContent(getDeviceFile(), content);
        }
        return plugin_uuid;
    }

    public static String getAuthCallbackState(boolean autoCreate) {
        String auth_callback_state = null;
        JsonObject deviceJson = getJsonObjectFromFile(getDeviceFile());
        boolean hasAuthCallbackState = (deviceJson.has("auth_callback_state") && !deviceJson.get("auth_callback_state").isJsonNull());
        if (!hasAuthCallbackState && autoCreate) {
            auth_callback_state = UUID.randomUUID().toString();
            setAuthCallbackState(auth_callback_state);
        } else if (!hasAuthCallbackState) {
            return "";
        } else {
            auth_callback_state = deviceJson.get("auth_callback_state").getAsString();
        }
        return auth_callback_state;
    }

    public static void setAuthCallbackState(String value) {
        String deviceFile = getDeviceFile();
        JsonObject deviceJson = getJsonObjectFromFile(deviceFile);
        deviceJson.addProperty("auth_callback_state", value);

        String content = deviceJson.toString();

        saveFileContent(deviceFile, content);
    }

    public static JsonArray readAsJsonArray(String data) {
        try {
            JsonArray jsonArray = UtilManager.gson.fromJson(buildJsonArrayReader(data), JsonArray.class);
            return jsonArray;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject readAsJsonObject(String data) {
        try {
            JsonObject jsonObject = UtilManager.gson.fromJson(buildJsonReader(data), JsonObject.class);
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonReader buildJsonReader(String data) {
        return new JsonReader(new StringReader(data));
    }

    public static JsonReader buildJsonArrayReader(String data) {
        // Clean the data
        return new JsonReader(new StringReader(cleanJsonArrayString(data)));
    }

    /**
     * Replace byte order mark, new lines, and trim
     * @param data
     * @return clean data
     */
    public static JsonTypeInfo cleanJsonString(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        JsonTypeInfo typeInfo = new JsonTypeInfo();
        try {
            JsonObject obj = convertToJsonObject(data);
            typeInfo.str = obj.toString();
            typeInfo.el = obj;
        } catch (Exception e) {
            try {
                JsonArray arr = convertToJsonArrayObject(data);
                typeInfo.str = arr.toString();
                typeInfo.el = arr;
            } catch (Exception e1) {
                return null;
            }
        }
        return typeInfo;
    }

    public static String cleanJsonArrayString(String data) {
        if (StringUtils.isBlank(data)) {
            return "";
        }

        JsonArray obj = convertToJsonArrayObject(data);
        return obj.toString();
    }

    public static JsonObject convertToJsonObject(String data) {
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        return jsonObject;
    }

    public static JsonArray convertToJsonArrayObject(String data) {
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        JsonArray jsonArray = gson.fromJson(data, JsonArray.class);
        return jsonArray;
    }

    public static String getStringRepresentation(HttpEntity res, boolean isPlainText) throws IOException {
        if (res == null) {
            return null;
        }

        InputStream inputStream = res.getContent();

        // Timing information--- verified that the data is still streaming
        // when we are called (this interval is about 2s for a large response.)
        // So in theory we should be able to do somewhat better by interleaving
        // parsing and reading, but experiments didn't show any improvement.
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader;
        reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        try (BufferedReader br = new BufferedReader(reader)) {
            boolean done = false;
            while (!done) {
                String aLine = br.readLine();
                if (aLine != null) {
                    sb.append(aLine);
                    if (isPlainText) {
                        sb.append("\n");
                    }
                } else {
                    done = true;
                }
            }
        }

        return sb.toString();
    }

    public static boolean getFlowChangeState() {
        JsonObject flowChange = getJsonObjectFromFile(getFlowChangeFile());
        if (flowChange != null && flowChange.has("in_flow")) {
            try {
                return flowChange.get("in_flow").getAsBoolean();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static void updateFlowChangeState(boolean state) {
        JsonObject flowChange = getJsonObjectFromFile(getFlowChangeFile());
        if (flowChange == null || !flowChange.has("in_flow")) {
            flowChange = new JsonObject();
        }
        flowChange.addProperty("in_flow", state);

        File f = new File(getFlowChangeFile());
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(flowChange.toString());
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {/*ignore*/}
        }
    }

    public static boolean hasActiveVscodeEditorOpsEditors() {
        JsonObject sessionJson = getSoftwareSessionAsJson();
        if (sessionJson != null && sessionJson.has("vc_eops_editors") && !sessionJson.get("vc_eops_editors").isJsonNull()) {
            return sessionJson.get("vc_eops_editors").getAsInt() > 0;
        }
        return false;
    }
}
