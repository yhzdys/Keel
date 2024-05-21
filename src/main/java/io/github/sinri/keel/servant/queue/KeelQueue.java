package io.github.sinri.keel.servant.queue;

import io.github.sinri.keel.facade.async.KeelAsyncKit;
import io.github.sinri.keel.verticles.KeelVerticleImplWithIssueRecorder;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;

import javax.annotation.Nonnull;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * 标准的队列服务实现。
 * <p>
 * 仅用于单节点模式。
 *
 * @since 2.1
 */
public abstract class KeelQueue extends KeelVerticleImplWithIssueRecorder<QueueManageIssueRecord> {
    private KeelQueueNextTaskSeeker nextTaskSeeker;
    private QueueWorkerPoolManager queueWorkerPoolManager;
    private SignalReader signalReader;
    private QueueStatus queueStatus = QueueStatus.INIT;

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }

    protected KeelQueue setQueueStatus(QueueStatus queueStatus) {
        this.queueStatus = queueStatus;
        return this;
    }

    /**
     * Create a new instance of QueueWorkerPoolManager when routine starts.
     * By default, it uses an unlimited pool, this could be override if needed.
     *
     * @since 3.0.9
     */
    protected @Nonnull QueueWorkerPoolManager getQueueWorkerPoolManager() {
        return new QueueWorkerPoolManager(0);
    }

    /**
     * Create a new instance of KeelQueueNextTaskSeeker when routine starts.
     */
    abstract protected @Nonnull KeelQueueNextTaskSeeker getNextTaskSeeker();

    /**
     * Create a new instance of SignalReader when routine starts.
     *
     * @since 3.0.1
     */
    abstract protected @Nonnull SignalReader getSignalReader();

    @Override
    protected void startAsKeelVerticle() {
        this.queueStatus = QueueStatus.RUNNING;

        try {
            routine();
        } catch (Exception e) {
            getIssueRecorder().exception(e, r -> r.message("Exception in routine"));
            undeployMe();
        }
    }

    protected final void routine() {
        getIssueRecorder().debug(r -> r.message("KeelQueue::routine start"));
        this.signalReader = getSignalReader();
        this.queueWorkerPoolManager = getQueueWorkerPoolManager();
        this.nextTaskSeeker = getNextTaskSeeker();

        Future.succeededFuture()
                .compose(v -> {
                    return signalReader.readSignal();
                })
                .recover(throwable -> {
                    getIssueRecorder().debug(r -> r.message("AS IS. Failed to read signal: " + throwable.getMessage()));
                    if (getQueueStatus() == QueueStatus.STOPPED) {
                        return Future.succeededFuture(QueueSignal.STOP);
                    } else {
                        return Future.succeededFuture(QueueSignal.RUN);
                    }
                })
                .compose(signal -> {
                    if (signal == QueueSignal.STOP) {
                        return this.whenSignalStopCame();
                    } else if (signal == QueueSignal.RUN) {
                        return this.whenSignalRunCame(nextTaskSeeker);
                    } else {
                        return Future.failedFuture("Unknown Signal");
                    }
                })
                .eventually(() -> {
                    long waitingMs = nextTaskSeeker.waitingMs();
                    getIssueRecorder().debug(r -> r.message("set timer for next routine after " + waitingMs + " ms"));
                    Keel.getVertx().setTimer(waitingMs, timerID -> routine());
                    return Future.succeededFuture();
                })
        ;
    }

    private Future<Void> whenSignalStopCame() {
        if (getQueueStatus() == QueueStatus.RUNNING) {
            this.queueStatus = QueueStatus.STOPPED;
            getIssueRecorder().notice(r -> r.message("Signal Stop Received"));
        }
        return Future.succeededFuture();
    }

    private Future<Void> whenSignalRunCame(KeelQueueNextTaskSeeker nextTaskSeeker) {
        this.queueStatus = QueueStatus.RUNNING;

        return KeelAsyncKit.repeatedlyCall(routineResult -> {
                    if (this.queueWorkerPoolManager.isBusy()) {
                        return KeelAsyncKit.sleep(1_000L);
                    }

                    return Future.succeededFuture()
                            .compose(v -> nextTaskSeeker.get())
                            .compose(task -> {
                                if (task == null) {
                                    // 队列里已经空了，不必再找
                                    getIssueRecorder().debug(r -> r.message("No more task todo"));
                                    // 通知 FutureUntil 结束
                                    routineResult.stop();
                                    return Future.succeededFuture();
                                }

                                // 队列里找出来一个task, deploy it (至于能不能跑起来有没有锁就不管了)
                                getIssueRecorder().info(r -> r.message("To run task: " + task.getTaskReference()));
                                getIssueRecorder().info(r -> r.message("Trusted that task is already locked by seeker: " + task.getTaskReference()));

                                // since 3.0.9
                                task.setQueueWorkerPoolManager(this.queueWorkerPoolManager);

                                return Future.succeededFuture()
                                        .compose(v -> task.deployMe(new DeploymentOptions()
                                                .setThreadingModel(ThreadingModel.WORKER)
                                        ))
                                        .compose(
                                                deploymentID -> {
                                                    getIssueRecorder().info(r -> r.message("TASK [" + task.getTaskReference() + "] VERTICLE DEPLOYED: " + deploymentID));
                                                    // 通知 FutureUntil 继续下一轮
                                                    return Future.succeededFuture();
                                                },
                                                throwable -> {
                                                    getIssueRecorder().exception(throwable, r -> r.message("CANNOT DEPLOY TASK [" + task.getTaskReference() + "] VERTICLE"));
                                                    // 通知 FutureUntil 继续下一轮
                                                    return Future.succeededFuture();
                                                }
                                        );

                            });
                })
                .recover(throwable -> {
                    getIssueRecorder().exception(throwable, r -> r.message("KeelQueue 递归找活干里出现了奇怪的故障"));
                    return Future.succeededFuture();
                });
    }

    @Override
    public void stop() {
        this.queueStatus = QueueStatus.STOPPED;
    }

    public enum QueueSignal {
        RUN,
        STOP
    }

    public enum QueueStatus {
        INIT,
        RUNNING,
        STOPPED
    }

    public interface SignalReader {
        Future<KeelQueue.QueueSignal> readSignal();
    }
}
