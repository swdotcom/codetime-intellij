package com.software.codetime.snowplow.events;

import com.google.common.base.Preconditions;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public class Unstructured extends AbstractEvent {

    private final SelfDescribingJson eventData;
    private boolean base64Encode;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private SelfDescribingJson eventData;

        /**
         * @param selfDescribingJson The properties of the event. Has two field:
         *                  A "data" field containing the event properties and
         *                  A "schema" field identifying the schema against which the data is validated
         * @return itself
         */
        public T eventData(SelfDescribingJson selfDescribingJson) {
            this.eventData = selfDescribingJson;
            return self();
        }

        public Unstructured build() {
            return new Unstructured(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    protected Unstructured(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.eventData);

        this.eventData = builder.eventData;
    }

    /**
     * @param base64Encode whether to base64Encode the event data
     */
    public void setBase64Encode(boolean base64Encode) {
        this.base64Encode = base64Encode;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        SelfDescribingJson envelope = new SelfDescribingJson(
                Constants.SCHEMA_UNSTRUCT_EVENT, this.eventData.getMap()
        );
        payload.add(Parameter.EVENT, Constants.EVENT_UNSTRUCTURED);
        payload.addMap(envelope.getMap(), this.base64Encode, Parameter.UNSTRUCTURED_ENCODED, Parameter.UNSTRUCTURED);
        return putDefaultParams(payload);
    }
}
