# Fluss Quickstart

A minimal Quarkus application demonstrating the SmallRye Reactive Messaging Fluss connector.

## What it does

- **EventProducer** -- emits a `GenericRow` to the `events` table every 2 seconds via `@Outgoing`
- **EventConsumer** -- reads `InternalRow` messages from the `events` table via `@Incoming` and logs them with bucket/offset metadata
- **EventResource** -- REST endpoint to send an event on demand via `POST /events?id=foo&value=42`

## Prerequisites

1. Java 17+
2. A running Apache Fluss cluster on `localhost:9123`
3. A database and log table created in Fluss:

```sql
CREATE DATABASE IF NOT EXISTS quickstart_db;

CREATE TABLE quickstart_db.events (
    `event_id` STRING,
    `value`    INT,
    `timestamp` BIGINT
);
```

## Run

First, install the connector from the project root:

```bash
mvn clean install -DskipTests
```

Then start the example:

```bash
cd examples/fluss-quickstart
mvn quarkus:dev
```

> **Note:** The Fluss client uses Apache Arrow which requires `--add-opens=java.base/java.nio=ALL-UNNAMED`.
> This is already configured in the `pom.xml` via the Quarkus Maven plugin's `<jvmArgs>`.
> If you run the app outside of Maven (e.g. `java -jar`), add that flag manually.

## Send an event via REST

```bash
curl -X POST "http://localhost:8080/events?id=hello&value=99"
```

## Expected output

The consumer logs each row as it arrives:

```
[bucket=0 offset=0] event_id=evt-0 value=0 timestamp=1712765432100
[bucket=0 offset=1] event_id=evt-1 value=1 timestamp=1712765434100
[bucket=0 offset=2] event_id=hello value=99 timestamp=1712765435200
```
