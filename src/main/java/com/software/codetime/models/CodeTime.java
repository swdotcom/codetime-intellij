package com.software.codetime.models;

import com.software.codetime.managers.ConfigManager;
import com.software.codetime.utils.UtilManager;

import java.util.HashMap;
import java.util.Map;

public class CodeTime {
    private String type = "Events";

    // non-hardcoded attributes
    private Map<String, FileInfo> source = new HashMap<>();
    private String version;
    private int pluginId;
    public int keystrokes = 0;
    // start and end are in seconds
    public long start;
    private long local_start;
    private final String os;
    private String timezone;
    private Project project;

    public long cumulative_editor_seconds = 0;
    public long cumulative_session_seconds = 0;
    public long elapsed_seconds = 0;
    public String workspace_name = "";
    public String hostname = "";
    public String project_null_error = "";

    public CodeTime() {
        String appVersion = ConfigManager.plugin_version;
        this.pluginId = ConfigManager.plugin_id;
        this.os = UtilManager.getOs();
    }

    public CodeTime(String version) {
        this.version = version;
        this.pluginId = ConfigManager.plugin_id;
        this.os = UtilManager.getOs();
    }

    public CodeTime clone() {
        CodeTime kc = new CodeTime();
        kc.keystrokes = this.keystrokes;
        kc.start = this.start;
        kc.local_start = this.local_start;
        kc.version = this.version;
        kc.pluginId = this.pluginId;
        kc.project = this.project;
        kc.type = this.type;
        kc.source = this.source;
        kc.timezone = this.timezone;

        kc.cumulative_editor_seconds = this.cumulative_editor_seconds;
        kc.cumulative_session_seconds = this.cumulative_session_seconds;
        kc.elapsed_seconds = this.elapsed_seconds;
        kc.workspace_name = this.workspace_name;
        kc.hostname = this.hostname;
        kc.project_null_error = this.project_null_error;

        return kc;
    }

    public void resetData() {
        this.keystrokes = 0;
        this.source = new HashMap<>();
        if (this.project != null) {
            this.project = new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
        }
        this.start = 0L;
        this.local_start = 0L;
        this.timezone = "";
        this.cumulative_editor_seconds = 0;
        this.cumulative_session_seconds = 0;
        this.elapsed_seconds = 0;
        this.workspace_name = "";
        this.project_null_error = "";
    }


    public static class FileInfo {
        public Integer add = 0;
        public Integer paste = 0;
        public Integer open = 0;
        public Integer close = 0;
        public Integer delete = 0;
        public Integer length = 0;
        public Integer netkeys = 0;
        public Integer lines = 0;
        public Integer linesAdded = 0;
        public Integer linesRemoved = 0;
        public Integer keystrokes = 0;
        public String syntax = "";
        public long start = 0;
        public long end = 0;
        public long local_start = 0;
        public long local_end = 0;
        public long duration_seconds = 0;
        public String fsPath = "";
        public String name = "";
        // new attributes for snowplow
        public int characters_added = 0; // chars added
        public int characters_deleted = 0; // chars deleted
        public int single_deletes = 0; // single char or single line delete
        public int multi_deletes = 0; // multi char or multi line delete
        public int single_adds = 0; // single char or single line add
        public int multi_adds = 0; // multi char or multi line add
        public int auto_indents = 0;
        public int replacements = 0;
        public boolean is_net_change = false;

        @Override
        public String toString() {
            return "FileInfo [add=" + add + ", paste=" + paste + ", open=" + open
                    + "\n, close=" + close + ", delete=" + delete + ", length=" + length + ", lines=" + lines
                    + "\n, linesAdded=" + linesAdded + ", linesRemoved=" + linesRemoved + ", keystrokes=" + keystrokes
                    + "\n, syntax=" + syntax + ", characters_added=" + characters_added + ", characters_deleted="
                    + characters_deleted + "\n, single_deletes=" + single_deletes + ", multi_deletes=" + multi_deletes
                    + "\n, single_adds=" + single_adds + ", multi_adds=" + multi_adds + ", auto_indents=" + auto_indents
                    + "\n, replacements=" + replacements + ", is_net_change=" + is_net_change + "]";
        }
    }

    public Map<String, FileInfo> getFileInfos() {
        return this.source;
    }

    // update each source with it's true amount of keystrokes
    public boolean hasData() {
        return this.keystrokes > 0;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public String toString() {
        return "CodeTime{" +
                "type='" + type + '\'' +
                ", pluginId=" + pluginId +
                ", source=" + source +
                ", keystrokes='" + keystrokes + '\'' +
                ", start=" + start +
                ", local_start=" + local_start +
                ", timezone='" + timezone + '\'' +
                ", project=" + project +
                '}';
    }

    public FileInfo getSourceByFileName(String fileName) {

        // Fetch the FileInfo
        if (source != null && source.get(fileName) != null) {
            return source.get(fileName);
        }

        if (source == null) {
            source = new HashMap<>();
        }

        UtilManager.TimesData timesData = UtilManager.getTimesData();

        // Keystrokes metadata needs to be initialized
        if (this.start == 0) {
            this.start = timesData.now;
            this.local_start = timesData.local_now;
            this.timezone = timesData.timezone;
        }

        // create one and return the one just created
        CodeTime.FileInfo fileInfoData = new CodeTime.FileInfo();
        fileInfoData.start = timesData.now;
        fileInfoData.local_start = timesData.local_now;
        source.put(fileName, fileInfoData);
        fileInfoData.fsPath = fileName;

        return fileInfoData;
    }

    public void endPreviousModifiedFiles(String fileName) {
        UtilManager.TimesData timesData = UtilManager.getTimesData();
        if (this.source != null) {
            for (String key : this.source.keySet()) {
                FileInfo fileInfo = this.source.get(key);
                if (key.equals(fileName)) {
                    fileInfo.end = 0;
                    fileInfo.local_end = 0;
                } else {
                    fileInfo.end = timesData.now;
                    fileInfo.local_end = timesData.local_now;
                }
            }
        }
    }

    public Map<String, FileInfo> getSource() {
        return this.source;
    }
}
