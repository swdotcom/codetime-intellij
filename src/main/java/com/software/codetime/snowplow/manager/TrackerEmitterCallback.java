package com.software.codetime.snowplow.manager;

import com.snowplowanalytics.snowplow.tracker.emitter.EmitterCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.FailureType;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

import java.util.List;
import java.util.logging.Logger;

public class TrackerEmitterCallback implements EmitterCallback {
    public static final Logger LOG = Logger.getLogger("TrackerEmitterCallback");

    @Override
    public void onSuccess(List<TrackerPayload> payloads) {
    }

    @Override
    public void onFailure(FailureType failureType, boolean willRetry, List<TrackerPayload> payloads) {
        LOG.warning("Failed sending event. Will retry: " + willRetry + ". Error code: " + failureType.toString() + ". Event count: " + payloads.size());
    }
}