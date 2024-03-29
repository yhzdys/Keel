package io.github.sinri.keel.mysql;

import io.github.sinri.keel.core.TechnicalPreview;
import io.github.sinri.keel.facade.configuration.KeelConfigElement;
import io.github.sinri.keel.mysql.matrix.ResultMatrix;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * KeelMySQLConfigure for connections and pool.
 * Commonly,
 * charset = "utf8";
 * useAffectedRows = true;
 * allowPublicKeyRetrieval = false;
 * poolMaxSize = 128;
 * poolShared = false;
 * tcpKeepAlive=false;
 */
public class KeelMySQLConfiguration extends KeelConfigElement {
    //private final @Nonnull String dataSourceName;

    public KeelMySQLConfiguration(@Nonnull KeelConfigElement base) {
        super(base);
    }


    @Nonnull
    public static KeelMySQLConfiguration loadConfigurationForDataSource(@Nonnull KeelConfigElement configCenter, @Nonnull String dataSourceName) {
        KeelConfigElement keelConfigElement = configCenter.extractConfigElement("mysql", dataSourceName);
        return new KeelMySQLConfiguration(Objects.requireNonNull(keelConfigElement));
    }

    @Nonnull
    public MySQLConnectOptions getConnectOptions() {
        // mysql.XXX.connect::database,host,password,port,user,charset,useAffectedRows,connectionTimeout
        MySQLConnectOptions mySQLConnectOptions = new MySQLConnectOptions()
                .setUseAffectedRows(true);
        mySQLConnectOptions.setHost(getHost())
                .setPort(getPort())
                .setUser(getUsername())
                .setPassword(getPassword());
        String charset = getCharset();
        if (charset != null) mySQLConnectOptions.setCharset(charset);
        String schema = getDatabase();
        if (schema != null) {
            mySQLConnectOptions.setDatabase(schema);
        }

        Integer connectionTimeout = getConnectionTimeout();
        if (connectionTimeout != null) {
            mySQLConnectOptions.setConnectTimeout(connectionTimeout);
        }

        return mySQLConnectOptions;
    }

    @Nonnull
    public PoolOptions getPoolOptions() {
        // mysql.XXX.pool::poolConnectionTimeout
        PoolOptions poolOptions = new PoolOptions();
        Integer poolMaxSize = getPoolMaxSize();
        if (poolMaxSize != null) {
            poolOptions.setMaxSize(poolMaxSize);
        }
        Integer poolConnectionTimeout = getPoolConnectionTimeout();
        if (poolConnectionTimeout != null) {
            poolOptions.setConnectionTimeout(poolConnectionTimeout);
            poolOptions.setConnectionTimeoutUnit(TimeUnit.SECONDS);
        }
        poolOptions.setShared(getPoolShared());
        poolOptions.setName("Keel-MySQL-Pool-" + this.getDataSourceName());
        return poolOptions;
    }

    public String getHost() {
        return getValueAsString(List.of("host"), null);
    }

    public Integer getPort() {
        return getValueAsInteger(List.of("port"), 3306);
    }

    public String getPassword() {
        return getValueAsString(List.of("password"), null);
    }

    public String getUsername() {
        var u = getValueAsString("username", null);
        if (u == null) {
            u = getValueAsString("user", null);
        }
        return u;
    }

    public String getDatabase() {
        String schema = getValueAsString("schema", null);
        if (schema == null) {
            schema = getValueAsString("database", null);
        }
        return Objects.requireNonNullElse(schema, "");
    }

    public String getCharset() {
        return getValueAsString("charset", null);
    }

    public Integer getPoolMaxSize() {
        var x = getChild("poolMaxSize");
        if (x == null) return null;
        return x.getValueAsInteger();
    }

    @Nonnull
    public String getDataSourceName() {
        return getName();
    }

    /**
     * The default value of connect timeout = 60000 ms
     *
     * @return connectTimeout - connect timeout, in ms
     * @since 3.0.1 let it be its original setting!
     */
    private Integer getConnectionTimeout() {
        var x = getChild("connectionTimeout");
        if (x == null) {
            return null;
        }
        return x.getValueAsInteger();
    }

    /**
     * Set the amount of time a client will wait for a connection from the pool.
     * If the time is exceeded without a connection available, an exception is provided.
     * TimeUnit would be set by `setConnectionTimeoutUnit`
     *
     * @see <a href="https://vertx.io/docs/apidocs/io/vertx/sqlclient/PoolOptions.html#setConnectionTimeout-int-">...</a>
     */
    public Integer getPoolConnectionTimeout() {
        KeelConfigElement keelConfigElement = extractConfigElement("poolConnectionTimeout");
        if (keelConfigElement == null) {
            return null;
        }
        return keelConfigElement.getValueAsInteger();
    }

    /**
     * @since 3.0.9
     * You can share a pool between multiple verticles or instances of the same verticle.
     * Such pool should be created outside a verticle otherwise it will be closed when the verticle
     * that created it is undeployed.
     */
    public boolean getPoolShared() {
        return getValueAsBoolean("poolShared", true);
    }


    /**
     * With Client to run SQL on target MySQL Database one-time.
     * The client is to be created, and then soon closed after the sql queried.
     *
     * @since 3.1.6
     */
    @TechnicalPreview(since = "3.1.6")
    public Future<ResultMatrix> instantQuery(String sql) {
        var sqlClient = MySQLBuilder.client()
                .with(this.getPoolOptions())
                .connectingTo(this.getConnectOptions())
                .using(Keel.getVertx())
                .build();
        return Future.succeededFuture()
                .compose(v -> sqlClient.preparedQuery(sql)
                        .execute()
                        .compose(rows -> {
                            return Future.succeededFuture(ResultMatrix.create(rows));
                        }))
                .andThen(ar -> sqlClient.close());
    }
}
