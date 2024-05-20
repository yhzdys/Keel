package io.github.sinri.keel.verticles;

import io.github.sinri.keel.logger.issue.record.KeelIssueRecord;
import io.github.sinri.keel.logger.issue.recorder.KeelIssueRecorder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import javax.annotation.Nonnull;

abstract public class KeelVerticleImplWithIssueRecorder<T extends KeelIssueRecord<T>> extends AbstractVerticle implements KeelVerticle {
    private @Nonnull KeelIssueRecorder<T> issueRecorder;

    public KeelVerticleImplWithIssueRecorder() {
        this.issueRecorder = buildIssueRecorder();
    }

    @Nonnull
    public KeelIssueRecorder<T> getIssueRecorder() {
        return issueRecorder;
    }

    abstract protected @Nonnull KeelIssueRecorder<T> buildIssueRecorder();

    @Override
    public final void start(Promise<Void> startPromise) {
        this.issueRecorder = buildIssueRecorder();
        this.start();
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
