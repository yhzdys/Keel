package io.github.sinri.keel.poi.excel;

import io.github.sinri.keel.core.ValueBox;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;

/**
 * @since 3.2.1
 */
public class SheetReadOptions {
    /**
     * Load sheet with 3 kinds of cell formula evaluator: None, Cached, and Evaluate.
     */
    private final @Nonnull ValueBox<FormulaEvaluator> formulaEvaluatorBox = new ValueBox<>();
    private boolean formatDateTime;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * SET cell formula evaluator to Cached or Evaluate.
     *
     * @param formulaEvaluator Null as Cached, or Evaluate.
     */
    public SheetReadOptions setFormulaEvaluator(@Nullable FormulaEvaluator formulaEvaluator) {
        this.formulaEvaluatorBox.setValue(formulaEvaluator);
        return this;
    }

    /**
     * SET cell formula evaluator to None,
     */
    public SheetReadOptions removeFormulaEvaluator() {
        this.formulaEvaluatorBox.clear();
        return this;
    }

    @Nonnull
    public ValueBox<FormulaEvaluator> getFormulaEvaluatorBox() {
        return formulaEvaluatorBox;
    }

    public boolean isFormatDateTime() {
        return formatDateTime;
    }

    public SheetReadOptions setFormatDateTime(boolean formatDateTime) {
        this.formatDateTime = formatDateTime;
        return this;
    }

    public SimpleDateFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    public SheetReadOptions setDateTimeFormat(SimpleDateFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        return this;
    }
}
