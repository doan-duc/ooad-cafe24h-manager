package tools;
import ui.MainFrame;
import ui.Theme;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import controller.AuthController;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;

/** Captures the main window to the output path supplied on the command line. */
public final class UiPreviewCapture {
    private UiPreviewCapture() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Expected: login password output.png");
        }
        Theme.install();
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Chưa cấu hình kết nối.");
            System.exit(1);
        }
        Db.configure(config);

        AuthController auth = new AuthController();
        Employee user = auth.xuLyDangNhap(args[0], args[1].toCharArray());
        System.out.println("Đăng nhập: " + user.displayName());

        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1400, 900);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            holder[0] = frame;
        });
        Thread.sleep(1500);

        MainFrame frame = holder[0];
        Rectangle bounds = new Rectangle(
                frame.getLocationOnScreen().x,
                frame.getLocationOnScreen().y,
                frame.getWidth(),
                frame.getHeight());
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", new File(args[2]));
        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Preview saved to " + args[2]);
    }
}

