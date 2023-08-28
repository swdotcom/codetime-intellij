package com.software.codetime.snowplow.events;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.snowplow.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorActionEvent {
    private final List<SelfDescribingJson> contexts = new ArrayList<>();

    // editor action context
    public String entity = "";
    public String type = "";
    public int tz_offset_minutes = 0;

    // entities
    public AuthEntity authEntity = new AuthEntity();
    public FileEntity fileEntity = new FileEntity();
    public PluginEntity pluginEntity = new PluginEntity();
    public ProjectEntity projectEntity = new ProjectEntity();
    public RepoEntity repoEntity = new RepoEntity();

    public Unstructured buildContexts() {
        contexts.clear();

        SelfDescribingJson editorActionData = this.buildEditorActionContext();

        contexts.add(authEntity.buildContext());
        contexts.add(fileEntity.buildContext());
        contexts.add(pluginEntity.buildContext());
        contexts.add(projectEntity.buildContext());
        contexts.add(repoEntity.buildContext());

        return Unstructured.builder().eventData(editorActionData).customContext(contexts).build();
    }

    protected SelfDescribingJson buildEditorActionContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("entity", this.entity);
        eventData.put("type", this.type);
        eventData.put("tz_offset_minutes", this.tz_offset_minutes);
        return new SelfDescribingJson("iglu:com.software/editor_action/jsonschema/1-0-2", eventData);
    }

}
