package org.acme.fluss;

import java.util.concurrent.CompletionStage;

import org.apache.fluss.row.InternalRow;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.smallrye.reactivemessaging.fluss.FlussMessageMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Consumes InternalRow messages from the "events-in" channel and logs them.
 */
@ApplicationScoped
public class EventConsumer {

    @Incoming("events-in")
    public CompletionStage<Void> consume(Message<InternalRow> message) {
        InternalRow row = message.getPayload();

        String eventId = row.getString(0).toString();
        int value = row.getInt(1);
        long timestamp = row.getLong(2);

        message.getMetadata(FlussMessageMetadata.class).ifPresent(meta -> {
            System.out.printf(
                    "[bucket=%d offset=%d] event_id=%s value=%d timestamp=%d%n",
                    meta.getBucketId(), meta.getOffset(), eventId, value, timestamp);
        });

        return message.ack();
    }
}
