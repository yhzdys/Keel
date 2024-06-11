package io.github.sinri.keel.mysql;

import javax.annotation.Nonnull;

/**
 * @param <C>
 * @since 3.0.11 Technical Preview
 * @since 3.0.18 Finished Technical Preview.
 * @since 3.2.11 Deprecated. Use `io.github.sinri.keel.mysql.action.AbstractNamedAction` or `io.github.sinri.keel.mysql.action.AbstractNamedMixinAction`.
 */
@Deprecated(since = "3.2.11", forRemoval = true)
public class AbstractNamedAction<C extends NamedMySQLConnection> {
    private final @Nonnull C namedSqlConnection;

    public AbstractNamedAction(@Nonnull C namedSqlConnection) {
        this.namedSqlConnection = namedSqlConnection;
    }

    @Nonnull
    public final C getNamedSqlConnection() {
        return namedSqlConnection;
    }
}
