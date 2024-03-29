package io.github.sinri.keel.facade.async;

import io.vertx.core.Future;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.BiFunction;

/**
 * @since 3.2.3
 */
public class FutureForEachBreakable<T> {

    @Nonnull
    private final Iterable<T> iterable;
    @Nonnull
    private final BiFunction<T, FutureRepeat.RoutineResult, Future<Void>> itemProcessor;
    private FutureForEachBreakable(@Nonnull Iterable<T> iterable, @Nonnull BiFunction<T, FutureRepeat.RoutineResult, Future<Void>> itemProcessor) {
        this.iterable = iterable;
        this.itemProcessor = itemProcessor;
    }

    public static <R> Future<Void> call(@Nonnull Iterable<R> iterable, @Nonnull BiFunction<R, FutureRepeat.RoutineResult, Future<Void>> itemProcessor) {
        return new FutureForEachBreakable<>(iterable, itemProcessor).start();
    }

    private Future<Void> start() {
        Iterator<T> iterator = iterable.iterator();
        return KeelAsyncKit.repeatedlyCall(routineResult -> {
            if (iterator.hasNext()) {
                T next = iterator.next();
                return this.itemProcessor.apply(next, routineResult);
            } else {
                routineResult.stop();
                return Future.succeededFuture();
            }
        });
    }
}
