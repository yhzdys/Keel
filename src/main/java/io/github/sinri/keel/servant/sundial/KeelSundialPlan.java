package io.github.sinri.keel.servant.sundial;

import io.github.sinri.keel.core.KeelCronExpression;
import io.vertx.core.Future;

import java.util.Calendar;

/**
 * @since 3.0.0
 * @since 3.2.4 change sync method `execute` to be async.
 */
public interface KeelSundialPlan {
    String key();

    KeelCronExpression cronExpression();

    Future<Void> execute(Calendar now);
}
