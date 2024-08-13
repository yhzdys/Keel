package io.github.sinri.keel.poi.excel.entity;

import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

/**
 * It is designed to be overridden for customized row reader.
 *
 * @since 3.0.14
 * @since 3.0.18 Finished Technical Preview.
 * @since 3.1.1 Use BigDecimal to handle the number value of cells.
 */
public class KeelSheetMatrixRow {
    private final List<String> rawRow;

    public KeelSheetMatrixRow(List<String> rawRow) {
        this.rawRow = rawRow;
    }

    @Nonnull
    public String readValue(int i) {
        return rawRow.get(i);
    }

    /**
     * @since 3.1.1
     */
    public BigDecimal readValueToBigDecimal(int i) {
        return new BigDecimal(readValue(i));
    }

    /**
     * @since 3.1.1
     */
    public BigDecimal readValueToBigDecimalStrippedTrailingZeros(int i) {
        return new BigDecimal(readValue(i)).stripTrailingZeros();
    }

    @Nullable
    public Integer readValueToInteger(int i) {
        try {
            return readValueToBigDecimal(i).intValueExact();
        } catch (ArithmeticException arithmeticException) {
            return null;
        }
//        double v = readValueToDouble(i);
//        return (int) v;
    }

    @Nullable
    public Long readValueToLong(int i) {
        try {
            return readValueToBigDecimal(i).longValueExact();
        } catch (ArithmeticException arithmeticException) {
            return null;
        }
//        double v = readValueToDouble(i);
//        return (long) v;
    }

    public double readValueToDouble(int i) {
        return readValueToBigDecimal(i).doubleValue();
//        return Double.parseDouble(readValue(i));
    }

    /**
     * @param columns definitions of columns as list
     * @return a json object with each column in defined type
     * @since 3.2.16
     */
    public JsonObject toJsonObject(@Nonnull List<Column> columns) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Object value;
            switch (column.getColumnType()) {
                case Long:
                    value = readValueToLong(i);
                    break;
                case Double:
                    value = readValueToDouble(i);
                    break;
                case Integer:
                    value = readValueToInteger(i);
                    break;
                case BigDecimal:
                    value = readValueToBigDecimal(i);
                    break;
                case String:
                default:
                    value = readValue(i);
                    break;
            }
            jsonObject.put(column.getName(), value);
        }
        return jsonObject;
    }

    /**
     * @since 3.2.16
     */
    public <T> T toBoundDataEntity(@Nonnull List<Column> columns, Class<T> tClass) {
        return toJsonObject(columns).mapTo(tClass);
    }
}
