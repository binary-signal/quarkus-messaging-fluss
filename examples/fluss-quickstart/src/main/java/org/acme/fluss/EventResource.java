package org.acme.fluss;

import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint that sends an event to Fluss on demand.
 *
 * <p>Usage:
 * <pre>
 *   curl -X POST "http://localhost:8080/events?id=my-event&amp;value=42"
 * </pre>
 */
@Path("/events")
@ApplicationScoped
public class EventResource {

    @Inject
    @Channel("events-out")
    Emitter<GenericRow> emitter;

    @POST
    public Response send(@QueryParam("id") String id, @QueryParam("value") int value) {
        GenericRow row = new GenericRow(3);
        row.setField(0, BinaryString.fromString(id));
        row.setField(1, value);
        row.setField(2, System.currentTimeMillis());

        emitter.send(row);

        return Response.accepted().entity("Sent: " + id + "=" + value + "\n").build();
    }
}
