package tools;
import ui.Theme;

import controller.AuthController;
import controller.TableController;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;
import model.TableInfo;

import java.util.List;

/** Exercises authentication and table loading for quick service. */
public final class QuickServiceSmokeTest {
    private QuickServiceSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== QuickServiceSmokeTest ===");
        Theme.install();
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Chưa cấu hình kết nối.");
            System.exit(1);
        }
        Db.configure(config);
        System.out.println("1. Kết nối OK");

        String login = args.length > 0 ? args[0] : "NV001";
        String password = args.length > 1 ? args[1] : "password";
        AuthController auth = new AuthController();
        Employee user = auth.xuLyDangNhap(login, password.toCharArray());
        System.out.println("2. Đăng nhập OK: " + user.displayName());

        List<TableInfo> tables = new TableController().list();
        System.out.println("3. Bàn: " + tables.size());

        System.out.println("PASS");
    }
}

