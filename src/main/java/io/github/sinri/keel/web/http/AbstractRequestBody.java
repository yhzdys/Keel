package io.github.sinri.keel.web.http;

import io.github.sinri.keel.core.json.SimpleJsonifiableEntity;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 3.0.1 JSON only.
 * @since 3.2.13 automatically support JSON and FORM.
 */
abstract public class AbstractRequestBody extends SimpleJsonifiableEntity {
    public AbstractRequestBody(RoutingContext routingContext) {
        //super(routingContext.body().asJsonObject());
        super();
        if (routingContext != null) {
            String contentType = routingContext.request().headers().get("Content-Type");
            if (contentType != null) {
                if (contentType.contains("multipart/form-data")) {
                    routingContext.request().setExpectMultipart(true);
                }
                if (contentType.contains("application/json")) {
                    this.reloadDataFromJsonObject(routingContext.body().asJsonObject());
                } else {
                    JsonObject requestObject = new JsonObject();
                    routingContext.request().formAttributes()
                            .forEach(entry -> requestObject.put(entry.getKey(), entry.getValue()));
                    this.reloadDataFromJsonObject(requestObject);
                }
            }
        }
    }
}
