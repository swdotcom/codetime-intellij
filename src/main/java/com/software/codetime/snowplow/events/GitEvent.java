package com.software.codetime.snowplow.events;


import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.snowplow.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitEvent {
    private final List<SelfDescribingJson> contexts = new ArrayList<>();

    // editor action context
    public GitEventType git_event_type = GitEventType.local_commit;
    public String git_event_timestamp = null;
    public String commit_id = null;

    // entities
    public AuthEntity authEntity = new AuthEntity();
    public PluginEntity pluginEntity = new PluginEntity();
    public ProjectEntity projectEntity = new ProjectEntity();
    public RepoEntity repoEntity = new RepoEntity();
    public List<FileChange> fileChangeEntities = new ArrayList<>();

    public Unstructured buildContexts() {
        contexts.clear();

        SelfDescribingJson data = this.buildGitContext();

        contexts.add(authEntity.buildContext());
        contexts.add(pluginEntity.buildContext());
        contexts.add(projectEntity.buildContext());
        contexts.add(repoEntity.buildContext());
        for (FileChange fileChange : fileChangeEntities) {
            contexts.add(fileChange.buildContext());
        }

        return Unstructured.builder().eventData(data).customContext(contexts).build();
    }

    protected SelfDescribingJson buildGitContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("git_event_type", this.git_event_type);
        eventData.put("git_event_timestamp", this.git_event_timestamp);
        eventData.put("commit_id", this.commit_id);
        return new SelfDescribingJson("iglu:com.software/git_event/jsonschema/1-0-0", eventData);
    }

}
