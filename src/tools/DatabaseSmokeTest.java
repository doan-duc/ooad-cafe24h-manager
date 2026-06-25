package tools;

import java.sql.Connection;

import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;

/** Verifies that the application can open a database connection. */
public final class DatabaseSmokeTest {
    private DatabaseSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== DatabaseSmokeTest ===");
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Không tìm thấy cấu hình kết nối. Hãy chạy ứng dụng và cấu hình trước.");
            System.exit(1);
        }
        System.out.println("Server: " + config.server() + ":" + config.port());
        System.out.println("Database: " + config.database());
        System.out.println("Authentication: " + config.authentication());

        Db.configure(config);
        try (Connection connection = Db.getConnection()) {
            System.out.println("Kết nối thành công.");
        }
        System.out.println("PASS");
    }
}
