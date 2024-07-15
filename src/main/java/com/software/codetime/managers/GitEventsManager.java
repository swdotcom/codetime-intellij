package com.software.codetime.managers;

import com.google.gson.JsonObject;
import com.software.codetime.snowplow.events.GitEventType;
import com.software.codetime.utils.FileUtilManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitEventsManager {
    public static final Logger LOG = Logger.getLogger("GitEventsManager");
    private final Map<WatchKey, Path> keys = new HashMap();
    private WatchService watcher = null;

    public GitEventsManager() {
    }

    public void setUpGitFileListener(String basePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                this.watcher = FileSystems.getDefault().newWatchService();
                Path headsPath = Paths.get(basePath, ".git", "refs", "heads");
                this.walkAndRegisterDirectories(headsPath);
                Path remotesPath = Paths.get(basePath, ".git", "refs", "remotes");
                this.walkAndRegisterDirectories(remotesPath);
                AsyncManager.getInstance().scheduleService(() -> {
                    this.pollGitChanges();
                }, "pollGitChanges", 15, 120);
            } catch (Exception var4) {
                LOG.log(Level.WARNING, "Error creating file watch service: " + var4.getMessage());
            }

        });
    }

    private void walkAndRegisterDirectories(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                GitEventsManager.this.registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        this.keys.put(key, dir);
    }

    private void pollGitChanges() {
        try {
            Iterator var1 = this.keys.keySet().iterator();

            while(var1.hasNext()) {
                WatchKey key = (WatchKey)var1.next();
                Path dir = this.keys.get(key);
                Iterator var4 = key.pollEvents().iterator();

                while(var4.hasNext()) {
                    WatchEvent<?> event = (WatchEvent)var4.next();
                    Path name = (Path)event.context();
                    if (name != null) {
                        WatchEvent.Kind kind = event.kind();
                        Path child = dir.resolve(name);
                        if (Files.exists(child) && kind != StandardWatchEventKinds.ENTRY_DELETE) {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                try {
                                    if (Files.isDirectory(child)) {
                                        this.walkAndRegisterDirectories(child);
                                    }
                                } catch (Exception var10) {
                                    LOG.log(Level.WARNING, "Error registering newly created sub directory");
                                }

                                this.onCommitHandler(child);
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                this.onCommitHandler(child);
                            }
                        } else {
                            this.onRemoveBranchFromTrackingHistory(child);
                        }
                    }
                }
            }
        } catch (Exception var11) {
            LOG.log(Level.WARNING, "Unable to poll git event changes: " + var11.getMessage());
        }

    }

    public void trackUncommittedChanges(String path) {
        EventTrackerManager.getInstance().trackUncommitedGitEvent();
    }

    private void onCommitHandler(Path path) {
        if (!Files.isDirectory(path)) {
            String branch = getBranchFromPath(path);
            String commit_id = this.getCommitFromBranchFile(path);
            String filePath = path.toString();
            if (filePath.indexOf(this.getRefsHeadsDirEndPath()) != -1) {
                EventTrackerManager.getInstance().trackGitLocalEvent(GitEventType.local_commit, branch, commit_id);
            } else if (filePath.indexOf(this.getRefsRemotesDirEndPath()) != -1) {
                EventTrackerManager.getInstance().trackGitRemoteEvent(path);
            }

            this.setLatestTrackedCommit(path, commit_id);
        }
    }

    private void onRemoveBranchFromTrackingHistory(Path path) {
        JsonObject obj = FileUtilManager.getFileContentAsJson(FileUtilManager.getGitEventFile());
        if (obj != null && obj.has(path.toString())) {
            obj.remove(path.toString());
        }

        FileUtilManager.writeData(FileUtilManager.getGitEventFile(), obj);
    }

    public static boolean commitExistsInGitEventFile(Path path, String latestTrackedCommit) {
        JsonObject obj = FileUtilManager.getFileContentAsJson(FileUtilManager.getGitEventFile());
        if (obj != null && obj.has(path.toString())) {
            JsonObject commit = (JsonObject)obj.get(path.toString());
            return commit != null && commit.has(latestTrackedCommit);
        }

        return false;
    }

    private void setLatestTrackedCommit(Path path, String latestTrackedCommit) {
        JsonObject obj = FileUtilManager.getFileContentAsJson(FileUtilManager.getGitEventFile());
        JsonObject commit = new JsonObject();
        commit.addProperty("latestTrackedCommit", latestTrackedCommit);
        obj.add(path.toString(), commit);
        FileUtilManager.writeData(FileUtilManager.getGitEventFile(), obj);
    }

    public static String getLatestTrackedCommit(Path path) {
        JsonObject obj = FileUtilManager.getFileContentAsJson(FileUtilManager.getGitEventFile());
        JsonObject commit = obj.getAsJsonObject(path.toString());
        return commit != null ? commit.get("latestTrackedCommit").getAsString() : "";
    }

    private String getRefsHeadsDirEndPath() {
        return File.separator + ".git" + File.separator + "refs" + File.separator + "heads" + File.separator;
    }

    private String getRefsRemotesDirEndPath() {
        return File.separator + ".git" + File.separator + "refs" + File.separator + "remotes" + File.separator;
    }

    private String getCommitFromBranchFile(Path path) {
        try {
            String commit = FileUtilManager.getFileContent(path.toString());
            commit = StringUtils.isNotBlank(commit) ? commit.replace("\\", "").replace("\n", "").replace("\r", "") : null;
            return commit;
        } catch (Exception var3) {
            return null;
        }
    }

    public static String getBranchFromPath(Path path) {
        try {
            String branch = path.toString().split(".git" + File.separator)[1];
            branch = StringUtils.isNotBlank(branch) ? branch.replace("\n", "").replace("\r", "") : null;
            return branch;
        } catch (Exception var2) {
            return null;
        }
    }
}
