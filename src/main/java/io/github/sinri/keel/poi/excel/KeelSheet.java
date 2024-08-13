package io.github.sinri.keel.poi.excel;

import org.apache.poi.ss.usermodel.Sheet;

import javax.annotation.Nonnull;

/**
 * @since 3.0.13
 * @since 3.0.18 Finished Technical Preview.
 */
public abstract class KeelSheet {
    private final Sheet sheet;

    /**
     * @since 3.2.16
     */
    public KeelSheet(@Nonnull Sheet sheet) {
        this.sheet = sheet;
    }

    /**
     * @return Raw Apache POI Sheet instance.
     */
    public Sheet getSheet() {
        return sheet;
    }

    public String getName() {
        return getSheet().getSheetName();
    }
}
