package io.github.sinri.keel.core.json.scheme;

import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;

/**
 * @since 2.7
 */
@Deprecated(since = "3.2.4")
abstract public class JsonValueScheme<T> implements JsonElementScheme<T> {

    protected T digested;
    private boolean nullable = false;
    private boolean optional = false;

    @Override
    public @Nonnull JsonObject toJsonObject() {
        return new JsonObject()
                .put("scheme_type", getJsonElementSchemeType())
                .put("nullable", nullable)
                .put("optional", optional)
                ;
    }


//    public void validate(Object object) throws JsonSchemeMismatchException {
//        if (object == null) {
//            if(!isNullable()){
//                throw new JsonSchemeMismatchException(JsonSchemeMismatchException.RuleNullableNotAllowed);
//            }
//        }
//    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    public JsonValueScheme<T> setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    @Override
    public @Nonnull JsonElementScheme<T> reloadDataFromJsonObject(@Nonnull JsonObject jsonObject) {
        this.nullable = jsonObject.getBoolean("nullable", false);
        this.optional = jsonObject.getBoolean("optional", false);
        return this;
    }

    @Override
    public void digest(T object) throws JsonSchemeMismatchException {
        if (object == null) {
            if (!isNullable()) {
                throw new JsonSchemeMismatchException(JsonSchemeMismatchException.RuleNullableNotAllowed);
            }
        }
        this.digested = object;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    public JsonValueScheme<T> setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    @Override
    public T getDigested() {
        return digested;
    }
}
