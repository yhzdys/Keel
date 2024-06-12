package io.github.sinri.keel.test;

import io.github.sinri.keel.logger.KeelLogLevel;
import io.github.sinri.keel.logger.issue.center.KeelIssueRecordCenter;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;

import java.util.concurrent.TimeUnit;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

public class SharedTestBootstrap {

    public static void bootstrap(Handler<Void> handler) {
        Keel.getConfiguration().loadPropertiesFile("config.properties");
        Keel.initializeVertx(new VertxOptions()
                        .setEventLoopPoolSize(4) // default 2 * number of cores on the machine
                        .setWorkerPoolSize(2)//default 20
                        .setMaxWorkerExecuteTime(60_000_000_000L) // 1s;  default 60_000_000_000 ns = 60s
                        .setMaxWorkerExecuteTimeUnit(TimeUnit.NANOSECONDS)
                        .setBlockedThreadCheckInterval(1000L) // default 1000 ms = 1s
                        .setBlockedThreadCheckIntervalUnit(TimeUnit.MILLISECONDS)
                        .setMaxEventLoopExecuteTime(2000000000L)//default 2000000000 ns = 2s
                        .setMaxEventLoopExecuteTimeUnit(TimeUnit.NANOSECONDS)
                )
                .onSuccess(v -> {
                    Keel.getLogger().setVisibleLevel(KeelLogLevel.DEBUG);
                    handler.handle(null);
                })
                .onFailure(throwable -> {
                    KeelIssueRecordCenter.outputCenter().generateEventLogger("SharedTestBootstrap")
                            .exception(throwable, "Keel Initialize Failure");
                });
    }
}
