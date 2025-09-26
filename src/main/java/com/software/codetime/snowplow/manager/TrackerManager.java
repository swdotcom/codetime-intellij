package com.software.codetime.snowplow.manager;

import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.Snowplow;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.tracker.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.tracker.configuration.TrackerConfiguration;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.managers.CacheManager;
import com.software.codetime.snowplow.client.Http;
import com.software.codetime.snowplow.client.Response;
import com.software.codetime.snowplow.events.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class TrackerManager {

    public static final Logger LOG = Logger.getLogger("TrackerManager");

    private final TrackerManager instance = null;
    private Tracker tracker = null;

    public TrackerManager(String swdcApiHost, String namespace, String appId) {
        // setup the http client
        Http.initialize(swdcApiHost);

        // fetch the tracker_api from the plugin config
        Response resp = Http.get("/plugins/config");

        // setup the snowplow emitter and tracker
        if (resp.ok && resp.responseData != null) {
            String track_api_host = resp.responseData.get("tracker_api").getAsString();
            String hostUrl = "https://" + track_api_host;
            tracker = Snowplow.createTracker(
                    new TrackerConfiguration(namespace, appId).base64Encoded(false).platform(DevicePlatform.Desktop),
                    new NetworkConfiguration(hostUrl),
                    new EmitterConfiguration().batchSize(10).callback(new TrackerEmitterCallback()));

            // start in 15 seconds, every 1 minutes
            AsyncManager.getInstance().scheduleService(
                    this::flushSnowplowEvents, "flushSnowplowEvents", 15, 60);

        }
    }

    private void flushSnowplowEvents() {
        if (tracker != null) {
            tracker.getEmitter().flushBuffer();
        }
    }

    public void trackCodeTimeEvent(CodetimeEvent event) {
        // extract the jwt and set into the cache manager
        CacheManager.jwt = event.authEntity != null ? event.authEntity.getJwt() : "";
        // build the contexts and send the event
        Unstructured contexts = event.buildContexts();
        sendEvent(contexts);
    }

    public void trackEditorAction(EditorActionEvent event) {
        // extract the jwt and set into the cache manager
        CacheManager.jwt = event.authEntity != null ? event.authEntity.getJwt() : "";
        // build the contexts and send the event
        Unstructured contexts = event.buildContexts();
        sendEvent(contexts);
    }

    public void trackGitEventAction(GitEvent event) {
        // extract the jwt and set into the cache manager
        CacheManager.jwt = event.authEntity != null ? event.authEntity.getJwt() : "";
        // build the contexts and send the event
        Unstructured contexts = event.buildContexts();
        sendEvent(contexts);
    }

    private void sendEvent(Unstructured payload) {
        if (tracker != null) {
            try {
                tracker.track(payload);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error tracking payload: " + e.getMessage());
            }
        }
    }
}
