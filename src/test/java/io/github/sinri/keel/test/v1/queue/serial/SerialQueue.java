package io.github.sinri.keel.test.v1.queue.serial;

import io.github.sinri.keel.Keel;
import io.github.sinri.keel.core.logger.KeelLogger;
import io.github.sinri.keel.syncqueue.KeelSyncQueueTask;
import io.github.sinri.keel.syncqueue.KeelSyncSerialQueue;
import io.github.sinri.keel.test.v1.queue.QueueTask;

public class SerialQueue extends KeelSyncSerialQueue {

    public SerialQueue() {

    }

    public static void main(String[] args) {
        new SerialQueue().run();
    }

    @Override
    protected KeelLogger getLogger() {
        return Keel.outputLogger("SerialQueue");
    }

    @Override
    protected boolean shouldStop() {
        return getSerialPool().getTaskCount() > 10;
    }

    @Override
    protected KeelSyncQueueTask getNextSerialTask() {
        double v = Math.random();
        if (v > 0.3) {
            return new QueueTask((int) (getSerialPool().getTaskCount() + 1), v);
        }
        return null;
    }
}
