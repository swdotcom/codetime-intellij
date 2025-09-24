package com.software.codetime.managers;

import com.software.codetime.models.*;
import com.software.codetime.snowplow.entities.*;
import com.software.codetime.snowplow.events.*;
import com.software.codetime.snowplow.manager.TrackerManager;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class EventTrackerManager {
    public static final Logger log = Logger.getLogger("EventTrackerManager");

    private static EventTrackerManager instance = null;

    private TrackerManager trackerMgr;
    private boolean ready = false;
    private IdeProject ideProject;

    public static EventTrackerManager getInstance() {
        if (instance == null) {
            instance = new EventTrackerManager();
        }
        return instance;
    }

    private EventTrackerManager() {}

    public void init(IdeProject ideProject) {
        try {
            this.ideProject = ideProject;
            trackerMgr = new TrackerManager(
                    ConfigManager.metrics_endpoint, "CodeTime", ConfigManager.ide_name);
            ready = true;
        } catch (Exception e) {
            log.warning("Error initializing the " + ConfigManager.plugin_name + " tracker: " + e.getMessage());
        }
    }

    public void trackCodeTimeEvent(final CodeTime payload) {
        if (!this.ready) {
            return;
        }
        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            ResourceInfo resourceInfo = GitUtilManager.getResourceInfo(payload.getProject().getDirectory());

            Map<String, CodeTime.FileInfo> fileInfoDataSet = payload.getFileInfos();
            for ( CodeTime.FileInfo fileInfoData : fileInfoDataSet.values() ) {
                CodetimeEvent event = new CodetimeEvent();

                if (StringUtils.isBlank(event.authEntity.getJwt())) {
                    continue; // skip the iteration if no JWT
                }

                event.characters_added = fileInfoData.characters_added;
                event.characters_deleted = fileInfoData.characters_deleted;
                event.single_adds = fileInfoData.single_adds;
                event.single_deletes = fileInfoData.single_deletes;
                event.multi_deletes = fileInfoData.multi_deletes;
                event.multi_adds = fileInfoData.multi_adds;
                event.auto_indents = fileInfoData.auto_indents;
                event.replacements = fileInfoData.replacements;
                event.is_net_change = fileInfoData.is_net_change;

                event.keystrokes = fileInfoData.keystrokes;
                event.lines_added = fileInfoData.lines_added;
                event.lines_deleted = fileInfoData.lines_removed;

                event.ai_characters_added = fileInfoData.ai_characters_added;
                event.ai_lines_added = fileInfoData.ai_lines_added;
                event.ai_characters_reverted = fileInfoData.ai_characters_reverted;
                event.ai_lines_reverted = fileInfoData.ai_lines_reverted;

                Date startDate = new Date(fileInfoData.start * 1000);
                event.start_time = DateTimeFormatter.ISO_INSTANT.format(startDate.toInstant());
                Date endDate = new Date(fileInfoData.end * 1000);
                event.end_time = DateTimeFormatter.ISO_INSTANT.format(endDate.toInstant());

                // set the entities
                event.fileEntity = this.getFileEntity(fileInfoData);
                event.projectEntity = this.getProjectEntity();
                event.authEntity = this.getAuthEntity();
                event.pluginEntity = this.getPluginEntity();
                event.repoEntity = this.getRepoEntity(resourceInfo);

                trackerMgr.trackCodeTimeEvent(event);
            }
        }, 0);
    }

    public void trackEditorAction(String entity, String type) {
        trackEditorAction(entity, type, null);
    }

    public void trackEditorAction(final String entity, final String type, final String full_file_name) {
        if (!this.ready) {
            return;
        }

        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            EditorActionEvent event = new EditorActionEvent();
            event.entity = entity;
            event.type = type;

            // set the entities
            event.authEntity = this.getAuthEntity();
            event.pluginEntity = this.getPluginEntity();
            event.projectEntity = this.getProjectEntity();
            event.fileEntity = this.getFileEntityFromFileName(full_file_name);
            ResourceInfo resourceInfo = GitUtilManager.getResourceInfo(event.projectEntity.project_directory);
            event.repoEntity = this.getRepoEntity(resourceInfo);

            if (StringUtils.isBlank(event.authEntity.getJwt())) {
                System.out.println("editor_action sending a blank JWT: " + event);
            }

            trackerMgr.trackEditorAction(event);
        }, 0);
    }

    public ProjectEntity getProjectEntity() {
        ProjectEntity projectEntity = new ProjectEntity();
        Project activeProject = ideProject.getFirstActiveProject();
        projectEntity.project_directory = activeProject.getDirectory();
        projectEntity.project_name = activeProject.getName();
        return projectEntity;
    }


    // -------------------------
    // TRACK LOCAL EVENT
    // -------------------------
    public void trackGitLocalEvent(final GitEventType gitEventType, final String branch, final String commit_id) {
        if (!this.ready) {
            return;
        }

        ProjectEntity projectEntity = this.getProjectEntity();
        if (projectEntity == null || StringUtils.isBlank(projectEntity.project_directory)) {
            return;
        }

        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            Long commit_timestamp_seconds = GitUtilManager.getAuthoredUnixTimestamp(projectEntity.project_directory, commit_id);
            List<DiffNumStats> diffNumStats = GitUtilManager.getChangesForCommit(projectEntity.project_directory, commit_id);
            String actionCommitId = commit_id;
            if (gitEventType.equals(GitEventType.uncommitted_change)) {
                // track uncommited change git event
                commit_timestamp_seconds = null;
                actionCommitId = null;
            } else if (gitEventType.equals(GitEventType.local_commit) && StringUtils.isNotBlank(branch)) {

                if (StringUtils.isBlank(commit_id)) {
                    actionCommitId = GitUtilManager.getLatestCommitForBranch(projectEntity.project_directory, branch);
                }

                if (GitUtilManager.commitAlreadyOnRemote(projectEntity.project_directory, commit_id)) {
                    return;
                }

                if (GitUtilManager.isMergeCommit(projectEntity.project_directory, commit_id)) {
                    return;
                }
            }
            // track git event
            this.trackGitEventAction(
                    projectEntity, diffNumStats, gitEventType, actionCommitId, commit_timestamp_seconds);
        }, 0);
    }

    // -------------------------
    // TRACK UNCOMMITED EVENT
    // -------------------------
    public void trackUncommitedGitEvent() {
        if (!this.ready) {
            return;
        }
        ProjectEntity projectEntity = this.getProjectEntity();
        if (projectEntity == null || StringUtils.isBlank(projectEntity.project_directory)) {
            return;
        }

        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            List<DiffNumStats> diffNumStats = GitUtilManager.getLocalChanges(projectEntity.project_directory);

            this.trackGitEventAction(projectEntity, diffNumStats, GitEventType.uncommitted_change, null, null);
        }, 0);
    }

    // -------------------------
    // TRACK REMOTE EVENT
    // -------------------------
    public void trackGitRemoteEvent(final Path path) {
        if (!this.ready) {
            return;
        }
        ProjectEntity projectEntity = this.getProjectEntity();
        if (projectEntity == null || StringUtils.isBlank(projectEntity.project_directory)) {
            return;
        }

        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            // get the remote part of the path
            String remoteBranch = GitEventsManager.getBranchFromPath(path);
            String defaultBranch = GitUtilManager.getDefaultBranchFromRemoteBranch(projectEntity.project_directory, remoteBranch);
            List<String> gitAuthors = GitUtilManager.getGitConfigAuthors(projectEntity.project_directory);
            String lastTrackedRef = GitEventsManager.getLatestTrackedCommit(path);

            GitEventType gitEventName = GitEventType.branch_commit;
            if (remoteBranch.equals(defaultBranch)) {
                gitEventName = GitEventType.default_branch_commit;
            } else {
                // If we have not tracked this branch before, then pull all commits
                // based on the default branch being the parent. This may not be true,
                // but it will prevent us from pulling the entire commit history of the author.
                if (StringUtils.isBlank(lastTrackedRef)) {
                    lastTrackedRef = defaultBranch;
                }
            }

            List<AuthorCommit> authorCommits = GitUtilManager.getCommitsForAuthors(
                    projectEntity.project_directory, remoteBranch, lastTrackedRef, gitAuthors);

            for (AuthorCommit authorCommit : authorCommits) {
                List<DiffNumStats> diffNumStats = GitUtilManager.getChangesForCommit(
                        projectEntity.project_directory, authorCommit.commit);

                this.trackGitEventAction(
                        projectEntity, diffNumStats, gitEventName, authorCommit.commit, authorCommit.authoredTimestamp);
            }
        }, 0);
    }

    // -------------------------
    // PRIVATE METHODS
    // -------------------------

    private void trackGitEventAction(
            final ProjectEntity projectEntity,
            final List<DiffNumStats> diffNumStats,
            final GitEventType git_event_type,
            final String commit_id,
            final Long commit_timestamp_seconds) {
        // execute async
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            GitEvent event = new GitEvent();
            event.git_event_type = git_event_type;

            event.commit_id = commit_id;
            if (commit_timestamp_seconds != null) {
                Date git_event_timestamp = new Date(commit_timestamp_seconds * 1000);
                event.git_event_timestamp = DateTimeFormatter.ISO_INSTANT.format(git_event_timestamp.toInstant());
            }

            // set the entities
            event.authEntity = this.getAuthEntity();
            event.pluginEntity = this.getPluginEntity();
            event.projectEntity = projectEntity;
            ResourceInfo resourceInfo = GitUtilManager.getResourceInfo(
                    event.projectEntity.project_directory);
            event.repoEntity = this.getRepoEntity(resourceInfo);
            event.fileChangeEntities = getFileChangeEntities(diffNumStats);

            // execute async
            log.info("git event action event processed");

            trackerMgr.trackGitEventAction(event);
        }, 0);
    }

    private AuthEntity getAuthEntity() {
        AuthEntity authEntity = new AuthEntity();
        String jwt = FileUtilManager.getItem("jwt");
        if (StringUtils.isNotBlank(jwt)) {
            jwt = jwt.trim();
            if (jwt.contains(" ")) {
                jwt = jwt.split(" ")[1];
            }
            authEntity.setJwt(jwt);
        } else {
            authEntity.setJwt("");
        }
        return authEntity;
    }

    private FileEntity getFileEntityFromFileName(String fullFileName) {
        FileDetails fileDetails = UtilManager.getFileDetails(ideProject, fullFileName);
        FileEntity fileEntity = new FileEntity();
        fileEntity.character_count = fileDetails.character_count;
        fileEntity.file_name = fileDetails.project_file_name;
        fileEntity.file_path = fileDetails.full_file_name;
        fileEntity.line_count = fileDetails.line_count;
        fileEntity.syntax = fileDetails.syntax;
        return fileEntity;
    }

    private FileEntity getFileEntity(CodeTime.FileInfo fileInfo) {
        FileDetails fileDetails = UtilManager.getFileDetails(ideProject, fileInfo.fsPath);
        FileEntity fileEntity = new FileEntity();
        fileEntity.character_count = fileDetails.character_count;
        fileEntity.file_name = fileDetails.project_file_name;
        fileEntity.file_path = fileDetails.full_file_name;
        fileEntity.line_count = fileDetails.line_count;
        fileEntity.syntax = fileDetails.syntax;
        return fileEntity;
    }

    private List<FileChange> getFileChangeEntities(List<DiffNumStats> diffNumStats) {
        List<FileChange> fileChangeEntities = new ArrayList<>();
        if (diffNumStats != null) {
            for (DiffNumStats stats : diffNumStats) {
                FileChange fileChange = new FileChange();
                fileChange.deletions = stats.deletions;
                fileChange.insertions = stats.insertions;
                fileChange.file_name = stats.file_name;
                fileChangeEntities.add(fileChange);
            }
        }
        return fileChangeEntities;
    }

    private RepoEntity getRepoEntity(ResourceInfo resourceInfo) {
        RepoEntity repoEntity = new RepoEntity();
        if (resourceInfo != null) {
            repoEntity.git_branch = resourceInfo.getBranch();
            repoEntity.git_tag = resourceInfo.getTag();
            repoEntity.repo_identifier = resourceInfo.getIdentifier();
            repoEntity.owner_id = resourceInfo.getOwnerId();
            repoEntity.repo_name = resourceInfo.getRepoName();
        }
        return repoEntity;
    }

    private PluginEntity getPluginEntity() {
        PluginEntity pluginEntity = new PluginEntity();
        pluginEntity.plugin_name = ConfigManager.plugin_name;
        pluginEntity.plugin_version = ConfigManager.plugin_version;
        pluginEntity.plugin_id = ConfigManager.plugin_id;
        return pluginEntity;
    }
}
