package io.quarkus.smallrye.reactivemessaging.fluss.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.fluss.FlussConnector;

class FlussProcessor {

    private static final String FEATURE = "messaging-fluss";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerFlussConnector() {
        return AdditionalBeanBuildItem.unremovableOf(FlussConnector.class);
    }

    @BuildStep
    ReflectiveClassBuildItem registerReflection() {
        return ReflectiveClassBuildItem.builder(
                        "org.apache.fluss.row.GenericRow",
                        "org.apache.fluss.row.InternalRow",
                        "org.apache.fluss.config.Configuration",
                        "org.apache.fluss.client.ConnectionFactory",
                        "org.apache.fluss.metadata.TablePath",
                        "org.apache.fluss.metadata.TableBucket")
                .methods(true)
                .fields(true)
                .build();
    }
}
