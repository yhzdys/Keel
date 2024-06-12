package io.github.sinri.keel.helper;

import io.vertx.ext.auth.VertxContextPRNG;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * @since 3.0.1
 */
public class KeelRandomHelper {
    private static final KeelRandomHelper instance = new KeelRandomHelper();
    private final AtomicReference<VertxContextPRNG> prngRef;

    private KeelRandomHelper() {
        prngRef = new AtomicReference<>();
    }

    static KeelRandomHelper getInstance() {
        return instance;
    }

    /**
     * @return Pseudo Random Number Generator
     * @since 3.2.11 build when first get
     */
    @Nonnull
    public VertxContextPRNG getPRNG() {
        if (prngRef.get() == null) {
            synchronized (prngRef) {
                if (prngRef.get() == null) {
                    if (Keel.isVertxInitialized()) {
                        prngRef.set(VertxContextPRNG.current(Keel.getVertx()));
                    } else {
                        prngRef.set(VertxContextPRNG.current());
                    }
                }
            }
        }
        VertxContextPRNG prng = prngRef.get();
        Objects.requireNonNull(prng);
        return prng;
    }
}
