package io.github.sinri.keel.mysql.action;

import io.github.sinri.keel.mysql.NamedMySQLConnection;

import javax.annotation.Nonnull;

/**
 * @param <C>
 * @since 3.2.11 Refined for Mixin Style, extracted NamedActionInterface.
 */
public abstract class AbstractNamedMixinAction<C extends NamedMySQLConnection, W> implements NamedActionMixinInterface<C, W> {
    private final @Nonnull C namedSqlConnection;

    public AbstractNamedMixinAction(@Nonnull C namedSqlConnection) {
        this.namedSqlConnection = namedSqlConnection;
    }

    @Nonnull
    @Override
    public final C getNamedSqlConnection() {
        return namedSqlConnection;
    }
}
