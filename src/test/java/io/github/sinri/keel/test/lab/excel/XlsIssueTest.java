package io.github.sinri.keel.test.lab.excel;

import io.github.sinri.keel.poi.excel.KeelSheets;
import io.github.sinri.keel.poi.excel.reader.KeelSheetReader;
import io.github.sinri.keel.tesuto.KeelTest;
import io.github.sinri.keel.tesuto.TestUnit;
import io.github.sinri.keel.tesuto.TestUnitResult;
import io.vertx.core.Future;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class XlsIssueTest extends KeelTest {
    @Nonnull
    @Override
    protected Future<Void> starting() {
        return Future.succeededFuture();
    }

    @Nonnull
    @Override
    protected Future<Void> ending(List<TestUnitResult> testUnitResults) {
        return Future.succeededFuture();
    }

    @TestUnit
    public Future<Void> giveMeFive() {
        try {
            var path = "/Users/leqee/code/Keel/src/test/resources/excel/excel_5.xls";
            var fs = new FileInputStream(new File(path));
            KeelSheets keelSheets = KeelSheets.autoGenerate(fs);
            KeelSheetReader keelSheet = keelSheets.generateReaderForSheet(1, s -> {
            });
            return keelSheet.readAllRowsToMatrix()
                    .compose(matrix -> {
                        matrix.getRawRowList().forEach(rawRow -> {
                            getLogger().info(r -> r.message("row: " + rawRow.toString()));
                        });
                        return Future.succeededFuture();
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
