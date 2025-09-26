package com.software.codetime.models;

import com.software.codetime.managers.ConfigManager;
import com.software.codetime.utils.UtilManager;

import java.util.HashMap;
import java.util.Map;

public class CodeTime implements Cloneable {
    private final String type = "Events";

    // non-hardcoded attributes
    private Map<String, FileInfo> source = new HashMap<>();
    private String version;
    private final int pluginId;
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

    @Override
    public CodeTime clone() throws CloneNotSupportedException {
        return (CodeTime) super.clone();
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
        public int add = 0;
        public int paste = 0;
        public int open = 0;
        public int close = 0;
        public int delete = 0;
        public int length = 0;
        public int lines = 0;
        public int lines_added = 0;
        public int ai_lines_added = 0;
        public int ai_lines_reverted = 0;
        public int lines_removed = 0;
        public int keystrokes = 0;
        public String syntax = "";
        public long start = 0;
        public long end = 0;
        public long local_start = 0;
        public long local_end = 0;
        public String fsPath = "";
        public String name = "";
        // new attributes for snowplow
        public int characters_added = 0; // chars added
        public int ai_characters_added = 0; // ai chars added
        public int ai_characters_reverted = 0; // ai chars reverted
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
            return "FileInfo {" +
                    "\n  add = " + add +
                    "\n  paste = " + paste +
                    "\n  open = " + open +
                    "\n  close = " + close +
                    "\n  delete = " + delete +
                    "\n  length = " + length +
                    "\n  lines = " + lines +
                    "\n  lines_added = " + lines_added +
                    "\n  ai_lines_added = " + ai_lines_added +
                    "\n  ai_lines_reverted = " + ai_lines_reverted +
                    "\n  lines_removed = " + lines_removed +
                    "\n  keystrokes = " + keystrokes +
                    "\n  syntax = '" + syntax + '\'' +
                    "\n  start = " + start +
                    "\n  end = " + end +
                    "\n  local_start = " + local_start +
                    "\n  local_end = " + local_end +
                    "\n  fsPath = '" + fsPath + '\'' +
                    "\n  name = '" + name + '\'' +
                    "\n  characters_added = " + characters_added +
                    "\n  ai_characters_added = " + ai_characters_added +
                    "\n  ai_characters_reverted = " + ai_characters_reverted +
                    "\n  characters_deleted = " + characters_deleted +
                    "\n  single_deletes = " + single_deletes +
                    "\n  multi_deletes = " + multi_deletes +
                    "\n  single_adds = " + single_adds +
                    "\n  multi_adds = " + multi_adds +
                    "\n  auto_indents = " + auto_indents +
                    "\n  replacements = " + replacements +
                    "\n  is_net_change = " + is_net_change +
                    "\n}";
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
