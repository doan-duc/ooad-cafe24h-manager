package tools;
import ui.Theme;

import controller.AuthController;
import controller.OperationsController;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;
import model.OperationsSnapshot;

/** Exercises authentication and dashboard snapshot loading. */
public final class OperationsFlowSmokeTest {
    private OperationsFlowSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== OperationsFlowSmokeTest ===");
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

        OperationsSnapshot snapshot = new OperationsController().snapshot();
        System.out.println("3. Snapshot: " + snapshot.banDangDung() + " bàn đang phục vụ");

        System.out.println("PASS");
    }
}

