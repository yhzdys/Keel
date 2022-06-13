package io.github.sinri.keel.core.json.scheme;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

/**
 * @since 2.7
 */
public class JsonNumberScheme extends JsonValueScheme {
    //private boolean withFractions;

    private boolean inclusiveMin;
    private Number min;
    private boolean inclusiveMax;
    private Number max;

    public JsonNumberScheme setMin(Number min, boolean inclusive) {
        this.min = min;
        this.inclusiveMin = inclusive;
        return this;
    }

    public JsonNumberScheme setMax(Number max, boolean inclusive) {
        this.max = max;
        this.inclusiveMax = inclusive;
        return this;
    }

    public Number getMax() {
        return max;
    }

    public Number getMin() {
        return min;
    }

    public boolean isInclusiveMin() {
        return inclusiveMin;
    }

    public boolean isInclusiveMax() {
        return inclusiveMax;
    }

    @Override
    public JsonElementSchemeType getJsonElementSchemeType() {
        return JsonElementSchemeType.JsonNumber;
    }

    @Override
    public JsonObject toJsonObject() {
        return super.toJsonObject()
                .put("scheme_type", getJsonElementSchemeType());
    }

    @Override
    public JsonElementScheme reloadDataFromJsonObject(JsonObject jsonObject) {
        super.reloadDataFromJsonObject(jsonObject);
        return this;
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return isNullable();
        }
        if (object instanceof Number) {
            if (object instanceof Double || object instanceof Float || object instanceof BigDecimal) {
                double v = ((Number) object).doubleValue();
                if (this.min != null) {
                    if (this.min.doubleValue() > v) return false;
                    if (!this.inclusiveMin && this.min.doubleValue() == v) return false;
                }
                if (this.max != null) {
                    if (this.max.doubleValue() < v) return false;
                    return this.inclusiveMax || this.max.doubleValue() != v;
                }
            } else {
                long v = ((Number) object).longValue();
                if (this.min != null) {
                    if (this.min.longValue() > v) return false;
                    if (!this.inclusiveMin && this.min.longValue() == v) return false;
                }
                if (this.max != null) {
                    if (this.max.longValue() < v) return false;
                    return this.inclusiveMax || this.max.longValue() != v;
                }
            }

            return true;
        }
        return false;
    }
}
