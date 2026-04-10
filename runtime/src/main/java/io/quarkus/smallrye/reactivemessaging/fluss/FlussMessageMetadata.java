package io.quarkus.smallrye.reactivemessaging.fluss;

import org.apache.fluss.metadata.TableBucket;
import org.apache.fluss.metadata.TablePath;

/**
 * Metadata associated with a message received from a Fluss table.
 */
public class FlussMessageMetadata {

    private final TablePath tablePath;
    private final TableBucket tableBucket;
    private final long offset;

    public FlussMessageMetadata(TablePath tablePath, TableBucket tableBucket, long offset) {
        this.tablePath = tablePath;
        this.tableBucket = tableBucket;
        this.offset = offset;
    }

    public TablePath getTablePath() {
        return tablePath;
    }

    public TableBucket getTableBucket() {
        return tableBucket;
    }

    public int getBucketId() {
        return tableBucket.getBucket();
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "FlussMessageMetadata{" + "tablePath="
                + tablePath + ", bucket="
                + tableBucket.getBucket() + ", offset="
                + offset + '}';
    }
}
