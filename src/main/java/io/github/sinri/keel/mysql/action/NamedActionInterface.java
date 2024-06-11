package io.github.sinri.keel.mysql.action;

import io.github.sinri.keel.mysql.NamedMySQLConnection;

import javax.annotation.Nonnull;

/**
 * @param <C>
 * @since 3.2.11
 */
public interface NamedActionInterface<C extends NamedMySQLConnection> {
    @Nonnull
    C getNamedSqlConnection();
}
