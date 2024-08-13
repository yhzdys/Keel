package io.github.sinri.keel.poi.excel;

import io.github.sinri.keel.core.ValueBox;
import io.vertx.core.Handler;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.2.1
 */
public class SheetReadOptions {
    /**
     * Load sheet with 3 kinds of cell formula evaluator: None, Cached, and Evaluate.
     */
    private final @Nonnull ValueBox<FormulaEvaluator> formulaEvaluatorBox = new ValueBox<>();

    private final SheetColumnReadOptions defaultColumnReadOptions = new SheetColumnReadOptions();

    private final Map<Integer, SheetColumnReadOptions> columnReadOptionsMap = new HashMap<>();

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

    public SheetColumnReadOptions getDefaultColumnReadOptions() {
        return defaultColumnReadOptions;
    }

    public SheetReadOptions maintainDefaultColumnReadOptions(Handler<SheetColumnReadOptions> handler) {
        handler.handle(this.defaultColumnReadOptions);
        return this;
    }

    public Map<Integer, SheetColumnReadOptions> getColumnReadOptionsMap() {
        return columnReadOptionsMap;
    }

    public SheetReadOptions setColumnReadOptions(Integer index, SheetColumnReadOptions columnReadOptions) {
        this.columnReadOptionsMap.put(index, columnReadOptions);
        return this;
    }

    @Nullable
    public SheetColumnReadOptions getColumnReadOptions(Integer index) {
        return columnReadOptionsMap.get(index);
    }
}
