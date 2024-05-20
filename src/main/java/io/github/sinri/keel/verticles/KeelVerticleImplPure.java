package io.github.sinri.keel.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * @since 3.2.0
 */
public abstract class KeelVerticleImplPure extends AbstractVerticle implements KeelVerticle {
    @Override
    public final void start(Promise<Void> startPromise) {
        this.startAsPureKeelVerticle(startPromise);
    }

    @Override
    public final void start() {
        this.startAsPureKeelVerticle();
    }

    protected void startAsPureKeelVerticle(Promise<Void> startPromise) {
        startAsPureKeelVerticle();
        startPromise.complete();
    }

    abstract protected void startAsPureKeelVerticle();
}
