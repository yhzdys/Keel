package io.github.sinri.keel.test.lab.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

public class Config1 {

    public static void main(String[] args) {
        Keel.initializeVertxStandalone(new VertxOptions());
        ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject()
                        .put("path", "config.properties")
                );
        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(configStoreOptions);
        ConfigRetriever configRetriever = ConfigRetriever.create(Keel.getVertx(), configRetrieverOptions);
        JsonObject cachedConfig = configRetriever.getCachedConfig();
        Keel.getLogger().fatal("cachedConfig: ", cachedConfig);

        configRetriever.getConfig()
                .compose(config -> {
                    Keel.getLogger().fatal("Retrieved: ", config);
                    return Future.succeededFuture();
                });

        Keel.getVertx().setTimer(3000L, v -> {
            JsonObject cachedConfig2 = configRetriever.getCachedConfig();
            Keel.getLogger().fatal("cachedConfig2: ", cachedConfig);
            Keel.close();
        });
    }
}
