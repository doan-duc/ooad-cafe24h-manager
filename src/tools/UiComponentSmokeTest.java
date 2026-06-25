package tools;
import ui.MainFrame;
import ui.Theme;

import javax.swing.SwingUtilities;

import controller.AuthController;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;
/** Opens the main UI after authenticating with command-line credentials. */
public final class UiComponentSmokeTest {
    private UiComponentSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== UiComponentSmokeTest ===");
        Theme.install();
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Chưa cấu hình kết nối.");
            System.exit(1);
        }
        Db.configure(config);

        String login = args.length > 0 ? args[0] : "NV001";
        String password = args.length > 1 ? args[1] : "password";
        AuthController auth = new AuthController();
        Employee user = auth.xuLyDangNhap(login, password.toCharArray());
        System.out.println("Đăng nhập: " + user.displayName());

        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            System.out.println("UI khởi động OK");
            frame.dispose();
        });
        System.out.println("PASS");
    }
}

