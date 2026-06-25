package tools;

import ui.LoginFrame;
import ui.MainFrame;
import ui.Theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;
import security.Session;

/** Chụp ảnh tất cả màn hình chính của ứng dụng và lưu vào thư mục chỉ định. */
public final class CaptureAllScreens {
    private CaptureAllScreens() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: outputDir");
        }
        File outDir = new File(args[0]);
        outDir.mkdirs();

        Theme.install();
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Chưa cấu hình kết nối.");
            System.exit(1);
        }
        Db.configure(config);

        // Chụp màn hình đăng nhập trước khi set session
        LoginFrame[] lholder = new LoginFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame lf = new LoginFrame();
            lf.setSize(1000, 650);
            lf.setAlwaysOnTop(true);
            lf.setVisible(true);
            lf.toFront();
            lholder[0] = lf;
        });
        Thread.sleep(1200);
        saveCapture(lholder[0], new File(outDir, "screen_dangnhap.png"));
        System.out.println("Đã lưu: screen_dangnhap.png");
        SwingUtilities.invokeAndWait(() -> lholder[0].dispose());

        // Tạo session với tài khoản admin (VT01 = toàn quyền) để mở MainFrame
        Employee adminUser = new Employee(
                "admin", "Nguyễn Văn A", "0901234567", "owner@cafe24h.com",
                "", "Active", "VT01", "Chủ quán");
        Session.login(adminUser);
        System.out.println("Session: " + adminUser.displayName());

        // Mở cửa sổ chính
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1400, 900);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            holder[0] = frame;
        });
        Thread.sleep(2000);

        MainFrame frame = holder[0];

        // Truy cập map buttons qua reflection
        Field buttonsField = MainFrame.class.getDeclaredField("buttons");
        buttonsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, JButton> buttons =
                (LinkedHashMap<String, JButton>) buttonsField.get(frame);

        // Ánh xạ tên tab → tên file output
        Map<String, String> pageToFile = new LinkedHashMap<>();
        pageToFile.put("Tổng quan", "screen_tongquan.png");
        pageToFile.put("Sơ đồ bàn", "screen_sodoban.png");
        pageToFile.put("Pha chế", "screen_phache.png");
        pageToFile.put("Thành viên", "screen_thanhvien.png");
        pageToFile.put("Booking", "screen_booking.png");
        pageToFile.put("Kho nguyên liệu", "screen_kho.png");
        pageToFile.put("Menu và định mức", "screen_menu.png");
        pageToFile.put("Nhân viên", "screen_nhanvien.png");
        pageToFile.put("Báo cáo", "screen_baocao.png");
        pageToFile.put("Ca làm việc", "screen_calamviec.png");
        pageToFile.put("Thiết lập cửa hàng", "screen_thietlap.png");

        for (Map.Entry<String, JButton> entry : buttons.entrySet()) {
            String pageName = entry.getKey();
            JButton btn = entry.getValue();

            String fileName = pageToFile.getOrDefault(pageName,
                    "screen_" + pageName.replaceAll("[^a-zA-Z0-9]", "_") + ".png");

            SwingUtilities.invokeAndWait(btn::doClick);
            Thread.sleep(1500);

            saveCapture(frame, new File(outDir, fileName));
            System.out.println("Đã lưu: " + fileName + " (" + pageName + ")");
        }

        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Xong! Ảnh lưu trong: " + outDir.getAbsolutePath());
    }

    private static void saveCapture(Window window, File out) throws Exception {
        BufferedImage[] result = {null};
        SwingUtilities.invokeAndWait(() -> {
            int w = window.getWidth();
            int h = window.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            window.printAll(g2);
            g2.dispose();
            result[0] = img;
        });
        ImageIO.write(result[0], "png", out);
    }
}
