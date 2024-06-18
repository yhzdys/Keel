package io.github.sinri.keel.web.http;


import io.github.sinri.keel.web.http.prehandler.PreHandlerChain;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @since 3.2.13
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PreHandlerChainMeta {
    Class<? extends PreHandlerChain> value() default PreHandlerChain.class;
}
