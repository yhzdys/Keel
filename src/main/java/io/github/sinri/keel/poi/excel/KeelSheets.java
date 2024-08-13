package io.github.sinri.keel.poi.excel;

import io.github.sinri.keel.core.ValueBox;
import io.vertx.core.Handler;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Objects;

/**
 * @since 3.0.13
 * @since 3.0.18 Finished Technical Preview.
 */
public class KeelSheets implements AutoCloseable {

    /**
     * @since 3.1.3
     */
    private final @Nullable FormulaEvaluator formulaEvaluator;
    protected @Nonnull Workbook autoWorkbook;

    /**
     * @param workbook The generated POI Workbook Implementation.
     * @since 3.0.20
     */
    public KeelSheets(@Nonnull Workbook workbook) {
        this(workbook, false);
    }

    /**
     * Create a new Sheets.
     */
    public KeelSheets() {
        this(null, false);
    }

    /**
     * Open an existed workbook or create.
     * Not use stream-write mode by default.
     *
     * @param workbook if null, create a new Sheets; otherwise, use it.
     * @since 3.1.3
     */
    public KeelSheets(@Nullable Workbook workbook, boolean withFormulaEvaluator) {
        autoWorkbook = Objects.requireNonNullElseGet(workbook, XSSFWorkbook::new);
        if (withFormulaEvaluator) {
            formulaEvaluator = autoWorkbook.getCreationHelper().createFormulaEvaluator();
        } else {
            formulaEvaluator = null;
        }
    }

    /**
     * @since 3.2.11
     */
    public static KeelSheets openFile(@Nonnull FileAccessOptions fileAccessOptions) {
        try {
            if (fileAccessOptions.isUseStreamReading()) {

                if (fileAccessOptions.getInputStream() != null) {
                    return new KeelSheets(fileAccessOptions.getStreamingReaderBuilder()
                            .open(fileAccessOptions.getInputStream())
                    );
                } else if (fileAccessOptions.getFile() != null) {
                    return new KeelSheets(fileAccessOptions.getStreamingReaderBuilder()
                            .open(fileAccessOptions.getFile())
                    );
                }
            } else {
                if (fileAccessOptions.getInputStream() != null) {
                    return new KeelSheets(WorkbookFactory.create(
                            fileAccessOptions.getFile()),
                            fileAccessOptions.isWithFormulaEvaluator()
                    );
                } else if (fileAccessOptions.getFile() != null) {
                    return new KeelSheets(WorkbookFactory.create(
                            fileAccessOptions.getFile()),
                            fileAccessOptions.isWithFormulaEvaluator()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("No input source!");
    }

    /**
     * @since 3.0.20 The great DAN and HONG discovered an issue with POI Factory Mode.
     */
    public static KeelSheets autoGenerate(@Nonnull InputStream inputStream) {
        return autoGenerate(inputStream, false);
    }

    /**
     * @since 3.1.4
     */
    public static KeelSheets autoGenerate(@Nonnull InputStream inputStream, boolean withFormulaEvaluator) {
        Workbook workbook;
        try {
            // XLSX
            workbook = new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            try {
                // XLS
                workbook = new HSSFWorkbook(inputStream);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return new KeelSheets(workbook, withFormulaEvaluator);
    }

    /**
     * @since 3.1.1
     */
    public static KeelSheets autoGenerateXLSX() {
        return new KeelSheets(new XSSFWorkbook());
    }

    /**
     * @since 3.1.4
     */
    public static KeelSheets autoGenerateXLSX(boolean withFormulaEvaluator) {
        return new KeelSheets(new XSSFWorkbook(), withFormulaEvaluator);
    }

    /**
     * @since 3.1.1
     */
    public static KeelSheets autoGenerateXLS() {
        return new KeelSheets(new HSSFWorkbook());
    }

    /**
     * @since 3.1.4
     */
    public static KeelSheets autoGenerateXLS(boolean withFormulaEvaluator) {
        return new KeelSheets(new HSSFWorkbook(), withFormulaEvaluator);
    }

    public KeelSheets useStreamWrite() {
        if (autoWorkbook instanceof XSSFWorkbook) {
            autoWorkbook = new SXSSFWorkbook((XSSFWorkbook) autoWorkbook);
        } else {
            throw new IllegalStateException("Now autoWorkbook is not an instance of XSSFWorkbook.");
        }
        return this;
    }

    @Deprecated(since = "3.2.16")
    public KeelSheetReader generateReaderForSheet(@Nonnull String sheetName) {
        return this.generateReaderForSheet(sheetName, readOptions -> {
        });
    }

    /**
     * @since 3.2.16
     */
    public KeelSheetReader generateReaderForSheet(@Nonnull String sheetName, @Nonnull Handler<SheetReadOptions> readOptionsHandler) {
        var sheet = this.getWorkbook().getSheet(sheetName);
        var readOptions = new SheetReadOptions();
        readOptions.setFormulaEvaluator(formulaEvaluator);
        readOptionsHandler.handle(readOptions);
        return new KeelSheetReader(sheet, readOptions);
    }

    /**
     * @since 3.1.4
     */
    @Deprecated(since = "3.2.16")
    public KeelSheetReader generateReaderForSheet(@Nonnull String sheetName, boolean parseFormulaCellToValue) {
        var sheet = this.getWorkbook().getSheet(sheetName);
        ValueBox<FormulaEvaluator> formulaEvaluatorValueBox = new ValueBox<>();
        if (parseFormulaCellToValue) {
            formulaEvaluatorValueBox.setValue(this.formulaEvaluator);
        }
        return new KeelSheetReader(sheet, new SheetReadOptions().setFormulaEvaluator(formulaEvaluator));
    }

    @Deprecated(since = "3.2.16")
    public KeelSheet generateReaderForSheet(int sheetIndex) {
        return this.generateReaderForSheet(sheetIndex, true);
    }

    /**
     * @since 3.2.16
     */
    public KeelSheetReader generateReaderForSheet(int sheetIndex, @Nonnull Handler<SheetReadOptions> readOptionsHandler) {
        var sheet = this.getWorkbook().getSheetAt(sheetIndex);
        var readOptions = new SheetReadOptions();
        readOptions.setFormulaEvaluator(formulaEvaluator);
        readOptionsHandler.handle(readOptions);
        return new KeelSheetReader(sheet, readOptions);
    }

    /**
     * @since 3.1.4
     */
    @Deprecated(since = "3.2.16")
    public KeelSheetReader generateReaderForSheet(int sheetIndex, boolean parseFormulaCellToValue) {
        var sheet = this.getWorkbook().getSheetAt(sheetIndex);
        ValueBox<FormulaEvaluator> formulaEvaluatorValueBox = new ValueBox<>();
        if (parseFormulaCellToValue) {
            formulaEvaluatorValueBox.setValue(this.formulaEvaluator);
        }
        return new KeelSheetReader(sheet, new SheetReadOptions().setFormulaEvaluator(formulaEvaluator));
    }

    public KeelSheetWriter generateWriterForSheet(@Nonnull String sheetName, Integer pos) {
        Sheet sheet = this.getWorkbook().createSheet(sheetName);
        if (pos != null) {
            this.getWorkbook().setSheetOrder(sheetName, pos);
        }
        return new KeelSheetWriter(sheet);
    }

    public KeelSheetWriter generateWriterForSheet(@Nonnull String sheetName) {
        return generateWriterForSheet(sheetName, null);
    }

    public int getSheetCount() {
        return autoWorkbook.getNumberOfSheets();
    }

    /**
     * @return Raw Apache POI Workbook instance.
     */
    @Nonnull
    public Workbook getWorkbook() {
        return autoWorkbook;
    }

    public void save(OutputStream outputStream) {
        try {
            autoWorkbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(File file) {
        try {
            save(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(String fileName) {
        save(new File(fileName));
    }

    @Override
    public void close() {
        try {
            autoWorkbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
