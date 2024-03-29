package io.github.sinri.keel.test.lab.blocking;

import io.github.sinri.keel.facade.async.KeelAsyncKit;
import io.github.sinri.keel.tesuto.KeelTest;
import io.github.sinri.keel.tesuto.TestUnit;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncTest extends KeelTest {

    @TestUnit
    public Future<Void> foreachBreakableTest() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        var x = new ForeachBreakable<Integer>(list) {

            @Override
            protected Future<Void> handleItem(Integer item, AtomicBoolean breakAtomic) {
                getLogger().info("handle " + item);
                if (item % 5 == 4) {
                    breakAtomic.set(true);
                }
                return Future.succeededFuture();
            }
        };
        return x.start()
                .compose(Future::succeededFuture)
                .onSuccess(v -> {
                    getLogger().notice("FIN");
                })
                .onFailure(e -> {
                    getLogger().exception(e);
                });
    }

    public abstract static class ForeachBreakable<T> {
        private final Iterable<T> iterable;
        private final AtomicBoolean breakRef = new AtomicBoolean(false);

        public ForeachBreakable(Iterable<T> iterable) {
            this.iterable = iterable;
        }

        public Future<Void> start() {
            Iterator<T> iterator = iterable.iterator();
            return KeelAsyncKit.repeatedlyCall(routineResult -> {
                if (iterator.hasNext()) {
                    T next = iterator.next();
                    return handleItem(next, breakRef)
                            .compose(x -> {
                                if (breakRef.get()) {
                                    routineResult.stop();
                                }
                                return Future.succeededFuture();
                            });
                } else {
                    routineResult.stop();
                    return Future.succeededFuture();
                }
            });
        }

        abstract protected Future<Void> handleItem(T item, AtomicBoolean breakAtomic);

    }


}
