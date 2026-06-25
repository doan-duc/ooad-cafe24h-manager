package db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/** Loads and stores {@link DatabaseConfig} in a properties file. */
public final class DatabaseConfigRepository {

    private static final Path CONFIG_FILE = Paths.get(
            System.getProperty("cafe24h.config", "config/database.properties"));

    private DatabaseConfigRepository() {
    }

    // Tóm tắt: Tải cấu hình kết nối từ file properties
    /** Returns the stored settings, or {@code null} when they are unavailable. */
    public static DatabaseConfig load() {
        if (!Files.exists(CONFIG_FILE)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);
            return from(properties);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    // Tóm tắt: Lưu cấu hình kết nối mà không lưu mật khẩu
    /**
     * Stores connection settings without persisting the SQL Server password.
     *
     * @throws IllegalStateException when the settings cannot be written
     */
    public static void save(DatabaseConfig config) {
        Properties properties = new Properties();
        properties.setProperty("server", config.server());
        properties.setProperty("port", Integer.toString(config.port()));
        properties.setProperty("database", config.database());
        properties.setProperty("authentication", config.authentication());
        properties.setProperty("username", config.username() == null ? "" : config.username());
        // Never persist the SQL Server password as plaintext.
        properties.setProperty("password", "");
        properties.setProperty("encrypt", Boolean.toString(config.encrypt()));
        properties.setProperty(
                "trustServerCertificate", Boolean.toString(config.trustServerCertificate()));

        try {
            Path parent = CONFIG_FILE.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "Cafe24h SQL Server connection");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Không lưu được cấu hình kết nối.", ex);
        }
    }

    private static DatabaseConfig from(Properties properties) {
        return new DatabaseConfig(
                properties.getProperty("server", "localhost").trim(),
                Integer.parseInt(properties.getProperty("port", "1433").trim()),
                properties.getProperty("database", "Cafe24hDB").trim(),
                properties.getProperty("authentication", "windows")
                        .trim().toLowerCase(Locale.ROOT),
                properties.getProperty("username", "").trim(),
                properties.getProperty("password", ""),
                Boolean.parseBoolean(properties.getProperty("encrypt", "true")),
                Boolean.parseBoolean(properties.getProperty("trustServerCertificate", "true")));
    }
}
