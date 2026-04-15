# Quarkus SmallRye Reactive Messaging - Fluss Connector

A [Quarkus](https://quarkus.io/) extension that provides
a [SmallRye Reactive Messaging](https://smallrye.io/smallrye-reactive-messaging/)
connector for [Apache Fluss](https://fluss.apache.org/) (Incubating), a
streaming storage system built for real-time analytics.

Use standard `@Incoming` and `@Outgoing` annotations to consume from and produce
to Fluss Log Tables and Primary Key Tables.

## Prerequisites

- Java 17+
- Maven 3.9+
- A running Apache Fluss cluster (default: `localhost:9123`)

> **JVM flag required:** The Fluss client uses Apache Arrow internally, which
> requires
> `--add-opens=java.base/java.nio=ALL-UNNAMED` at JVM startup. Add this to your
> Quarkus Maven plugin config:
> ```xml
> <plugin>
>     <groupId>io.quarkus</groupId>
>     <artifactId>quarkus-maven-plugin</artifactId>
>     <configuration>
>         <jvmArgs>--add-opens=java.base/java.nio=ALL-UNNAMED</jvmArgs>
>     </configuration>
> </plugin>
> ```

## Installation

Build and install the extension locally:

```bash
mvn clean install
```

Then add the runtime dependency to your Quarkus application:

```xml

<dependency>
    <groupId>io.quarkiverse.reactivemessaging</groupId>
    <artifactId>quarkus-messaging-fluss</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configure channels in your `application.properties` using the `smallrye-fluss`
connector name.

### Incoming (consuming from Fluss)

```properties
mp.messaging.incoming.my-channel.connector=smallrye-fluss
mp.messaging.incoming.my-channel.bootstrap-servers=localhost:9123
mp.messaging.incoming.my-channel.database=my_db
mp.messaging.incoming.my-channel.table=events
```

### Outgoing (producing to Fluss)

```properties
mp.messaging.outgoing.my-channel.connector=smallrye-fluss
mp.messaging.outgoing.my-channel.bootstrap-servers=localhost:9123
mp.messaging.outgoing.my-channel.database=my_db
mp.messaging.outgoing.my-channel.table=events
```

### Configuration Reference

| Property            | Type   | Default          | Direction | Description                                             |
|---------------------|--------|------------------|-----------|---------------------------------------------------------|
| `connector`         | String |                  | Both      | Must be `smallrye-fluss`                                |
| `bootstrap-servers` | String | `localhost:9123` | Both      | Fluss cluster bootstrap address                         |
| `database`          | String | `fluss`          | Both      | Fluss database name                                     |
| `table`             | String | *(required)*     | Both      | Fluss table name                                        |
| `offset`            | String | `full`           | Incoming  | Startup mode: `full`, `earliest`, `latest`, `timestamp` |
| `offset.timestamp`  | long   |                  | Incoming  | Epoch millis, required when `offset=timestamp`          |
| `columns`           | String |                  | Incoming  | Comma-separated column names for projection             |
| `poll-timeout`      | int    | `100`            | Incoming  | Poll timeout in milliseconds                            |
| `batch-size`        | int    | `100`            | Outgoing  | Number of records before flushing                       |

## Usage

### Consuming messages

Incoming messages carry an `InternalRow` payload representing a row from a Fluss Table.

```java
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.fluss.row.InternalRow;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class FlussConsumer {

    @Incoming("events")
    public void consume(InternalRow row) {
        // Access fields by index
        String id    = row.getString(0).toString();
        int    value = row.getInt(1);
        System.out.println("Received: id=" + id + ", value=" + value);
    }
}
```

### Accessing Fluss metadata

Each message includes `FlussMessageMetadata` with the table path, bucket, offset
and change type.

```java
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.smallrye.reactivemessaging.fluss.FlussMessageMetadata;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class FlussConsumerWithMetadata {

    @Incoming("events")
    public CompletionStage<Void> consume(Message<InternalRow> message) {
        message.getMetadata(FlussMessageMetadata.class).ifPresent(meta -> {
            System.out.println("Table: " + meta.getTablePath());
            System.out.println("Bucket: " + meta.getBucketId());
            System.out.println("Offset: " + meta.getOffset());
            System.out.println("ChangeType:" + meta.getChangeType());
        });
        return message.ack();
    }
}
```

### Producing messages

Outgoing messages must carry a `GenericRow` (or any `InternalRow`) payload
matching the target table schema.

```java
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

import java.time.Duration;

@ApplicationScoped
public class FlussProducer {

    @Outgoing("output")
    public Multi<GenericRow> produce() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .map(tick -> {
                        GenericRow row = new GenericRow(3);
                        row.setField(0, BinaryString.fromString(
                                "event-" + tick));
                        row.setField(1, tick.intValue());
                        row.setField(2, System.currentTimeMillis());
                        return row;
                    });
    }
}
```

### Processing (incoming to outgoing)

```java
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.fluss.row.BinaryString;
import org.apache.fluss.row.GenericRow;
import org.apache.fluss.row.InternalRow;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class FlussProcessor {

    @Incoming("input")
    @Outgoing("output")
    public GenericRow process(InternalRow row) {
        // Transform: read from input table, write to output table
        GenericRow out = new GenericRow(2);
        out.setField(0, row.getString(0));  // pass through field
        out.setField(1, row.getInt(1) * 2); // double the value
        return out;
    }
}
```

### Start reading position

The `offset` property controls where the connector begins reading when it
starts.
This applies to both Log Tables and Primary Key Tables.

| Mode        | Description                                                                                                                                                                                                       |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `full`      | **Default.** For log tables, reads from the earliest offset. For PK tables, reads from the earliest changelog offset (**full snapshot read is not yet supported — for now this behaves the same as `earliest`**). |
| `earliest`  | Starts reading from the earliest available offset.                                                                                                                                                                |
| `latest`    | Starts reading from the latest offset — only new records arriving after startup will be consumed.                                                                                                                 |
| `timestamp` | Starts reading from the offset closest to a given timestamp. Requires `offset.timestamp` (epoch milliseconds).                                                                                                    |

Examples:

```properties
# Read only new records
mp.messaging.incoming.events.offset=latest
# Read from a specific point in time
mp.messaging.incoming.events.offset=timestamp
mp.messaging.incoming.events.offset.timestamp=1713200000000
```

### Column projection

Reduce network overhead by fetching only the columns you need:

```properties
mp.messaging.incoming.events.connector=smallrye-fluss
mp.messaging.incoming.events.table=sensor_readings
mp.messaging.incoming.events.columns=sensor_id,temperature,timestamp
```

The `InternalRow` you receive will only contain the projected columns (indexed
starting at 0 in projection order).

## How It Works

- **Incoming:** The connector creates a `LogScanner` that subscribes to all
  buckets of the configured table at the position determined by the `offset`
  property (`full`, `earliest`, `latest`, or `timestamp`). It polls for
  `ScanRecords` on a worker thread and emits each row as a
  `Message<InternalRow>`.

- **Outgoing:** The connector creates an `AppendWriter` for the configured Log
  Table. Each incoming message payload (`InternalRow`/`GenericRow`) is appended
  and flushed in batches based on `batch-size`.

## Fluss vs Kafka

If you are coming from Kafka, note these key differences:

| Kafka                            | Fluss                                        |
|----------------------------------|----------------------------------------------|
| Topics (unstructured bytes)      | Tables (schematized, typed rows)             |
| Consumer groups with rebalancing | LogScanner subscribing to individual buckets |
| `ProducerRecord<K,V>`            | `GenericRow` with typed fields               |
| `ConsumerRecord<K,V>`            | `InternalRow` with typed field accessors     |
| Serializers/Deserializers        | Schema-native (no SerDe needed)              |

## Current Limitations

- **No full snapshot read for PK Tables** -- the `full` offset mode does not yet
  read the initial snapshot for Primary Key Tables; it falls back to `earliest`
  (changelog only)
- **No consumer offset tracking** -- offsets are not persisted between restarts
- **No health checks** -- MicroProfile Health integration is not yet implemented
- **No dev services** -- no automatic Fluss container startup in dev mode

## Building from Source

```bash
git clone https://github.com/binary-signal/quarkus-messaging-fluss.git
cd quarkus-messaging-fluss
mvn clean install
```

The build produces two artifacts:

- `quarkus-messaging-fluss` -- runtime JAR (add this to your app)
- `quarkus-messaging-fluss-deployment` -- build-time processing (resolved
  automatically by Quarkus)

## License

Apache License 2.0
