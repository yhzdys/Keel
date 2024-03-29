package io.github.sinri.keel.mysql;

import io.vertx.sqlclient.SqlConnection;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

public class KeelMySQLDataSourceProvider {

    @Nonnull
    public static String defaultMySQLDataSourceName() {
        return Objects.requireNonNull(Keel.getConfiguration().getValueAsString(List.of("mysql", "default_data_source_name"), "default"));
    }

    /**
     * @since 3.0.11 Technical Preview.
     * @since 3.0.18 Finished Technical Preview.
     */
    public static <C extends NamedMySQLConnection> NamedMySQLDataSource<C> initializeNamedMySQLDataSource(
            @Nonnull String dataSourceName,
            Function<SqlConnection, C> sqlConnectionWrapper
    ) {
        var configuration = Keel.getConfiguration().extractConfigElement("mysql", dataSourceName);
        Objects.requireNonNull(configuration);
        KeelMySQLConfiguration mySQLConfigure = new KeelMySQLConfiguration(configuration);
        return new NamedMySQLDataSource<>(mySQLConfigure, sqlConnectionWrapper);
    }

    /**
     * @since 3.0.11 Technical Preview.
     * @since 3.0.18 Finished Technical Preview.
     */
    public static NamedMySQLDataSource<DynamicNamedMySQLConnection> initializeDynamicNamedMySQLDataSource(@Nonnull String dataSourceName) {
        return initializeNamedMySQLDataSource(dataSourceName, sqlConnection -> new DynamicNamedMySQLConnection(sqlConnection, dataSourceName));
    }
}
