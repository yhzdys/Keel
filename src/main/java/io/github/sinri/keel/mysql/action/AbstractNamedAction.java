package io.github.sinri.keel.mysql.action;

import io.github.sinri.keel.mysql.NamedMySQLConnection;

import javax.annotation.Nonnull;

/**
 * @param <C>
 * @since 3.2.11 Moved from `io.github.sinri.keel.mysql.AbstractNamedAction` and Refined.
 */
public abstract class AbstractNamedAction<C extends NamedMySQLConnection> implements NamedActionInterface<C> {
    private final @Nonnull C namedSqlConnection;

    public AbstractNamedAction(@Nonnull C namedSqlConnection) {
        this.namedSqlConnection = namedSqlConnection;
    }

    @Nonnull
    @Override
    public C getNamedSqlConnection() {
        return namedSqlConnection;
    }
}
