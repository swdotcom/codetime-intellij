package com.software.codetime.snowplow.events;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.snowplow.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodetimeEvent {
    private final List<SelfDescribingJson> contexts = new ArrayList<>();

    public int keystrokes = 0;
    public int characters_added = 0;
    public int ai_characters_added = 0;
    public int ai_characters_reverted = 0;
    public int characters_deleted = 0;
    public int single_deletes = 0;
    public int multi_deletes = 0;
    public int single_adds = 0;
    public int multi_adds = 0;
    public int auto_indents = 0;
    public int replacements = 0;
    public int lines_added = 0;
    public int ai_lines_added = 0;
    public int ai_lines_reverted = 0;
    public int lines_deleted = 0;
    public String start_time = "";
    public String end_time = "";
    public boolean is_net_change = false;

    // entities
    public AuthEntity authEntity = new AuthEntity();
    public FileEntity fileEntity = new FileEntity();
    public PluginEntity pluginEntity = new PluginEntity();
    public ProjectEntity projectEntity = new ProjectEntity();
    public RepoEntity repoEntity = new RepoEntity();

    public Unstructured buildContexts() {
        contexts.clear();

        SelfDescribingJson codetimeEventData = this.buildCodetimeContext();

        contexts.add(authEntity.buildContext());
        contexts.add(fileEntity.buildContext());
        contexts.add(pluginEntity.buildContext());
        contexts.add(projectEntity.buildContext());
        contexts.add(repoEntity.buildContext());

        return Unstructured.builder().eventData(codetimeEventData).customContext(contexts).build();
    }

    protected SelfDescribingJson buildCodetimeContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("keystrokes", this.keystrokes);
        eventData.put("characters_added", this.characters_added);
        eventData.put("ai_characters_added", this.ai_characters_added);
        eventData.put("ai_characters_reverted", this.ai_characters_reverted);
        eventData.put("characters_deleted", this.characters_deleted);
        eventData.put("single_deletes", this.single_deletes);
        eventData.put("multi_deletes", this.multi_deletes);
        eventData.put("single_adds", this.single_adds);
        eventData.put("multi_adds", this.multi_adds);
        eventData.put("auto_indents", this.auto_indents);
        eventData.put("replacements", this.replacements);
        eventData.put("lines_added", this.lines_added);
        eventData.put("ai_lines_added", this.ai_lines_added);
        eventData.put("ai_lines_reverted", this.ai_lines_reverted);
        eventData.put("lines_deleted", this.lines_deleted);
        eventData.put("start_time", this.start_time);
        eventData.put("end_time", this.end_time);
        eventData.put("is_net_change", this.is_net_change);

        return new SelfDescribingJson("iglu:com.software/codetime/jsonschema/1-0-3", eventData);
    }
}
