package db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

public final class Db {
    private static volatile DatabaseConfig config;
    private static final int POOL_SIZE = 5;
    private static final LinkedBlockingQueue<Connection> pool =
            new LinkedBlockingQueue<>(POOL_SIZE);

    private Db() {
    }

    // Tóm tắt: Cấu hình và khởi tạo pool kết nối cơ sở dữ liệu
    public static void configure(DatabaseConfig databaseConfig) {
        if (databaseConfig == null) {
            throw new IllegalArgumentException("Cấu hình kết nối không được null.");
        }
        config = databaseConfig;
        loadDriver();
        Connection old;
        while ((old = pool.poll()) != null) {
            try {
                old.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // Tóm tắt: Lấy kết nối từ pool hoặc tạo kết nối mới
    public static Connection getConnection() throws SQLException {
        ensureConfigured();
        Connection conn = pool.poll();
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    return wrap(conn);
                }
            } catch (SQLException ignored) {
            }
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
        return wrap(open(config));
    }

    // Tóm tắt: Kiểm tra xem cấu hình kết nối có hợp lệ hay không
    public static boolean test(DatabaseConfig databaseConfig) {
        try {
            try (Connection connection = open(databaseConfig)) {
                return connection.isValid(3);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    // Tóm tắt: Kiểm tra kết nối và ném ngoại lệ nếu không hợp lệ
    public static void testOrThrow(DatabaseConfig databaseConfig) {
        try {
            try (Connection connection = open(databaseConfig)) {
                if (!connection.isValid(3)) {
                    throw new SQLException("SQL Server không phản hồi.");
                }
            }
        } catch (Exception ex) {
            throw SqlErrors.wrap("Không kết nối được SQL Server", ex);
        }
    }

    private static Connection wrap(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[] { Connection.class },
                new PoolHandler(real));
    }

    private static final class PoolHandler implements InvocationHandler {
        private final Connection real;

        PoolHandler(Connection real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                returnToPool();
                return null;
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }

        private void returnToPool() {
            try {
                if (!real.isClosed()) {
                    if (!real.getAutoCommit()) {
                        real.rollback();
                        real.setAutoCommit(true);
                    }
                    if (!pool.offer(real)) {
                        real.close();
                    }
                }
            } catch (SQLException ex) {
                try {
                    real.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static Connection open(DatabaseConfig databaseConfig) throws SQLException {
        if (databaseConfig == null) {
            throw new SQLException("Chưa cấu hình kết nối SQL Server.");
        }
        loadDriver();
        if (databaseConfig.usesWindowsAuthentication()) {
            return DriverManager.getConnection(databaseConfig.jdbcUrl());
        }
        return DriverManager.getConnection(
                databaseConfig.jdbcUrl(),
                databaseConfig.username(),
                databaseConfig.password());
    }

    private static void ensureConfigured() throws SQLException {
        if (config == null) {
            DatabaseConfig loaded = DatabaseConfigRepository.load();
            if (loaded == null) {
                throw new SQLException("Chưa cấu hình kết nối SQL Server.");
            }
            configure(loaded);
        }
    }

    private static synchronized void loadDriver() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Thiếu thư viện Microsoft JDBC Driver.", ex);
        }
    }
}
