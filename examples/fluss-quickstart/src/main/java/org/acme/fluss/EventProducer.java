package org.acme.fluss;

import java.time.Duration;

import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Produces a GenericRow every 2 seconds to the "ticker-out" channel.
 *
 * <p>The target table schema is assumed to be:
 * <pre>
 *   event_id   STRING
 *   value      INT
 *   timestamp  BIGINT
 * </pre>
 */
@ApplicationScoped
public class EventProducer {

    @Outgoing("ticker-out")
    public Multi<GenericRow> produce() {
        return Multi.createFrom()
                .ticks()
                .every(Duration.ofSeconds(2))
                .map(tick -> {
                    GenericRow row = new GenericRow(3);
                    row.setField(0, BinaryString.fromString("evt-" + tick));
                    row.setField(1, tick.intValue());
                    row.setField(2, System.currentTimeMillis());
                    return row;
                });
    }
}
