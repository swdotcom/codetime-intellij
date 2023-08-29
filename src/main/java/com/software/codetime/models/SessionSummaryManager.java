package com.software.codetime.models;

import com.google.gson.reflect.TypeToken;
import com.software.codetime.http.ClientResponse;
import com.software.codetime.http.OpsHttpClient;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import com.software.codetime.websockets.handlers.StatsUpdateHandler;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionSummaryManager {
    public static final Logger LOG = Logger.getLogger("SessionSummaryManager");

    private final Map<WatchKey, Path> keys = new HashMap<>();
    private WatchService sessionSummaryWatcher = null;

    public void setSessionSummaryChangeListener() {
        SwingUtilities.invokeLater(() -> {
            try {
                sessionSummaryWatcher = FileSystems.getDefault().newWatchService();

                Path path = new File(FileUtilManager.getSoftwareDir(true)).toPath();

                WatchKey key = path.register(
                        sessionSummaryWatcher,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                keys.put(key, path);

                // start in 15 seconds, every 2 minutes
                AsyncManager.getInstance().scheduleService(
                        () -> pollSessionSummaryFileChanges(), "pollSessionSummaryFileChanges", 15, 120);

            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error creating file watch service: " + e.getMessage());
            }
        });
    }

    private void pollSessionSummaryFileChanges() {
        try {
            for (WatchKey key : keys.keySet()) {
                Path dir = keys.get(key);
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path name = ((WatchEvent<Path>)event).context();

                    if (name == null || FileUtilManager.getSessionDataSummaryFile().indexOf(name.toString()) == -1) {
                        // update the status for session summary json file changes only
                        continue;
                    }

                    WatchEvent.Kind kind = event.kind();

                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // update the status
                        try {
                            Type type = new TypeToken<SessionSummary>() {}.getType();
                            SessionSummary summary = UtilManager.gson.fromJson(
                                    FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile()), type);
                            StatsUpdateHandler.handleStatsUpdate(summary);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "Unable to update stats from session data file: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to poll session summary file changes: " + e.getMessage());
        }
    }

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static void updateSessionSummaryFromServer() {
        SessionSummary summary = fetchSessionSummary();
        updateFileSummaryAndStatsBar(summary);
    }

    public static SessionSummary fetchSessionSummary() {
        String api = "/api/v1/user/session_summary";
        ClientResponse resp = OpsHttpClient.appGet(api);
        if (resp.isOk()) {
            try {
                Type type = new TypeToken<SessionSummary>() {}.getType();
                return UtilManager.gson.fromJson(resp.getJsonObj(), type);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[CodeTime] error reading session summary: " + e.getMessage());
            }
        }
        return new SessionSummary();
    }

    public static void updateFileSummaryAndStatsBar(SessionSummary sessionSummary) {
        if (sessionSummary != null) {
            SwingUtilities.invokeLater(() -> {
                if (ConfigManager.status_bar_update_handler != null) {
                    ConfigManager.status_bar_update_handler.updateEditorStatus(sessionSummary);
                }

                if (ConfigManager.tree_refresh_runnable != null) {
                    ConfigManager.tree_refresh_runnable.run();
                }
            });
        }
    }
}
