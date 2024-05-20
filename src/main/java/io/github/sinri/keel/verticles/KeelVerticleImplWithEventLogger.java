package io.github.sinri.keel.verticles;

import io.github.sinri.keel.logger.event.KeelEventLogger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import javax.annotation.Nonnull;

/**
 * @since 3.2.0
 */
abstract public class KeelVerticleImplWithEventLogger extends AbstractVerticle implements KeelVerticle {
    private @Nonnull KeelEventLogger logger;

    public KeelVerticleImplWithEventLogger() {
        super();
        this.logger = buildEventLogger();
    }

    @Nonnull
    public KeelEventLogger getLogger() {
        return logger;
    }

    abstract protected KeelEventLogger buildEventLogger();

    @Override
    public final void start(Promise<Void> startPromise) {
        this.logger = buildEventLogger();
        start();
        startPromise.complete();
    }

    @Override
    public final void start() {
        this.startAsKeelVerticle();
    }

    protected void startAsKeelVerticle(Promise<Void> startPromise) {
        startAsKeelVerticle();
        startPromise.complete();
    }

    abstract protected void startAsKeelVerticle();
}
