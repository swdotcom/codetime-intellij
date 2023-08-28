package com.software.codetime.snowplow.events;

import com.google.common.base.Preconditions;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Utils;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractEvent implements Event {

    protected final List<SelfDescribingJson> context;

    protected long deviceCreatedTimestamp;

    /**
     * The true timestamp may be null if none is set.
     */
    protected Long trueTimestamp;

    protected final String eventId;
    protected final Subject subject;

    public static abstract class Builder<T extends Builder<T>> {

        private List<SelfDescribingJson> context = new LinkedList<>();
        private long deviceCreatedTimestamp = System.currentTimeMillis();
        protected Long trueTimestamp = null;
        private String eventId = Utils.getEventId();
        private Subject subject = null;

        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @param context the list of contexts
         * @return itself
         */
        public T customContext(List<SelfDescribingJson> context) {
            this.context = context;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @param timestamp the event timestamp as
         *                  unix epoch
         * @return itself
         * Use {@link #trueTimestamp} or {@link #deviceCreatedTimestamp}
         */
        @Deprecated
        public T timestamp(long timestamp) {
            return deviceCreatedTimestamp(timestamp);
        }

        /**
         * Adjust the device-created timestamp. This is usually not what you want, check {@link #trueTimestamp}.
         *
         * @param timestamp the event timestamp as
         *                  unix epoch
         * @return itself
         */
        public T deviceCreatedTimestamp(long timestamp) {
            this.deviceCreatedTimestamp = timestamp;
            return self();
        }

        /**
         * The true timestamp of that event (as determined by the user).
         *
         * @param timestamp the event timestamp as
         *                  unix epoch
         * @return itself
         */
        public T trueTimestamp(Long timestamp) {
            this.trueTimestamp = timestamp;
            return self();
        }

        /**
         * A custom eventId for the event.
         *
         * @param eventId the eventId
         * @return itself
         */
        public T eventId(String eventId) {
            this.eventId = eventId;
            return self();
        }

        /**
         * A custom subject for the event.
         *
         * @param subject the eventId
         * @return itself
         */
        public T subject(Subject subject) {
            this.subject = subject;
            return self();
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

    protected AbstractEvent(Builder<?> builder) {

        // Precondition checks
        Preconditions.checkNotNull(builder.context);
        Preconditions.checkNotNull(builder.eventId);
        Preconditions.checkArgument(!builder.eventId.isEmpty(), "eventId cannot be empty");

        this.context = builder.context;
        this.deviceCreatedTimestamp = builder.deviceCreatedTimestamp;
        this.trueTimestamp = builder.trueTimestamp;
        this.eventId = builder.eventId;
        this.subject = builder.subject;
    }

    /**
     * @return the events custom context
     */
    @Override
    public List<SelfDescribingJson> getContext() {
        return new ArrayList<>(this.context);
    }

    /**
     * @return the event's device created timestamp.
     */
    public long getDeviceCreatedTimestamp() {
        return deviceCreatedTimestamp;
    }

    /**
     * @return the event's true timestamp.
     */
    @Override
    public Long getTrueTimestamp() {
        return trueTimestamp;
    }

    /**
     * @return the event id
     */
    public String getEventId() {
        return this.eventId;
    }

    /**
     * @return the event subject
     */
    @Override
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * @return the event payload
     */
    @Override
    public abstract Payload getPayload();

    /**
     * Adds the default parameters to a TrackerPayload object.
     *
     * @param payload the payload to add too.
     * @return the TrackerPayload with appended values.
     */
    protected TrackerPayload putDefaultParams(TrackerPayload payload) {
        payload.add(Parameter.EID, getEventId());
        if (getTrueTimestamp()!=null) {
            payload.add(Parameter.TRUE_TIMESTAMP, Long.toString(getTrueTimestamp()));
        }
        payload.add(Parameter.DEVICE_CREATED_TIMESTAMP, Long.toString(getDeviceCreatedTimestamp()));
        return payload;
    }
}
