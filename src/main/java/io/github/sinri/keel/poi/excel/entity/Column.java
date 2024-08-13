package io.github.sinri.keel.poi.excel.entity;

/**
 * @since 3.2.16
 */
public class Column {
    private String name;
    private ColumnType columnType;

    public Column(String name, ColumnType columnType) {
        this.name = name;
        this.columnType = columnType;
    }

    public ColumnType getColumnType() {
        return columnType;
    }

    public Column setColumnType(ColumnType columnType) {
        this.columnType = columnType;
        return this;
    }

    public String getName() {
        return name;
    }

    public Column setName(String name) {
        this.name = name;
        return this;
    }
}
