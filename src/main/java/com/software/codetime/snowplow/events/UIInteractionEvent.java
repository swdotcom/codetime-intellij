package com.software.codetime.snowplow.events;


import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.snowplow.entities.AuthEntity;
import com.software.codetime.snowplow.entities.PluginEntity;
import com.software.codetime.snowplow.entities.UIElementEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIInteractionEvent {
    private final List<SelfDescribingJson> contexts = new ArrayList<>();

    public UIInteractionType interaction_type = UIInteractionType.click;

    // entities
    public AuthEntity authEntity = new AuthEntity();
    public PluginEntity pluginEntity = new PluginEntity();
    public UIElementEntity uiElementEntity = new UIElementEntity();

    public Unstructured buildContexts() {
        contexts.clear();

        SelfDescribingJson uiInteractionData = this.buildUIInteractionContext();

        contexts.add(authEntity.buildContext());
        contexts.add(pluginEntity.buildContext());
        contexts.add(uiElementEntity.buildContext());

        return Unstructured.builder().eventData(uiInteractionData).customContext(contexts).build();
    }

    protected SelfDescribingJson buildUIInteractionContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("interaction_type", this.interaction_type);
        return new SelfDescribingJson("iglu:com.software/ui_interaction/jsonschema/1-0-0", eventData);
    }

}
