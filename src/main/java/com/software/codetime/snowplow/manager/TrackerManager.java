package com.software.codetime.snowplow.manager;

import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.emitter.BatchEmitter;
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter;
import com.snowplowanalytics.snowplow.tracker.emitter.EmitterCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.FailureType;
import com.snowplowanalytics.snowplow.tracker.http.HttpClientAdapter;
import com.snowplowanalytics.snowplow.tracker.http.OkHttpClientAdapter;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.software.codetime.managers.CacheManager;
import com.software.codetime.snowplow.client.Http;
import com.software.codetime.snowplow.client.Response;
import com.software.codetime.snowplow.events.*;
import okhttp3.OkHttpClient;

import java.util.Calendar;
import java.util.List;
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
            String urlScheme = resp.responseData.has("tracker_url_scheme")
                    ? resp.responseData.get("tracker_url_scheme").getAsString() : "https";

            String hostUrl = !Pattern.matches("^http[s]?:\\/\\/.*$", track_api_host) ? urlScheme + "://" + track_api_host : track_api_host;
            OkHttpClient client = new OkHttpClient();
            HttpClientAdapter adapter = OkHttpClientAdapter.builder()
                    .url(hostUrl)
                    .httpClient(client)
                    .build();

            Emitter emitter = BatchEmitter.builder()
                    .callback(new EmitterCallback() {
                        @Override
                        public void onSuccess(List<TrackerPayload> payloads) {

                        }

                        @Override
                        public void onFailure(FailureType failureType, boolean willRetry, List<TrackerPayload> payloads) {
                            LOG.warning("Failed sending event. Error code: " + failureType.toString() + ". Event List: " + payloads.toString());
                        }
                    })
                    .httpClientAdapter(adapter)
                    .bufferCapacity(1)
                    .build();

            // set the timezone
            String tzId = Calendar.getInstance().getTimeZone().getID();
            Subject subject = new Subject();
            subject.setTimezone(tzId);

            tracker = new Tracker.TrackerBuilder(emitter, namespace, appId)
                    .platform(DevicePlatform.InternetOfThings)
                    .base64(false)
                    .subject(subject)
                    .build();

        }
    }

    public String getSubjectTimezoneId() {
        return tracker.getSubject().getSubject().get("tz");
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

    public void trackUIInteraction(UIInteractionEvent event) {
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
