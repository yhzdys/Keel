package io.github.sinri.keel.poi.excel;

import java.text.SimpleDateFormat;

public class SheetColumnReadOptions {
    private boolean formatDateTime;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public boolean isFormatDateTime() {
        return formatDateTime;
    }

    public SheetColumnReadOptions setFormatDateTime(boolean formatDateTime) {
        this.formatDateTime = formatDateTime;
        return this;
    }

    public SimpleDateFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    public SheetColumnReadOptions setDateTimeFormat(SimpleDateFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        return this;
    }
}
