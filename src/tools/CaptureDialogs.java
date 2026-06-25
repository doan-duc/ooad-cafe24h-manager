package tools;

import ui.MainFrame;
import ui.Theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.TableController;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
import model.Employee;
import model.TableInfo;
import security.Session;

/**
 * Chụp ảnh thật của hộp thoại Gọi món (QuickCheckInDialog)
 * và Thanh toán (CheckoutDialog) từ app đang chạy.
 */
public final class CaptureDialogs {
    private CaptureDialogs() {}

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

        Session.login(new Employee(
                "admin", "Nguyễn Văn A", "0901234567", "owner@cafe24h.com",
                "", "Active", "VT01", "Chủ quán"));

        // Mở MainFrame - always on top để không bị che
        MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1400, 900);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            holder[0] = frame;
        });
        Thread.sleep(1800);
        MainFrame frame = holder[0];

        // Chuyển sang tab Sơ đồ bàn
        clickNav(frame, "Sơ đồ bàn");
        Thread.sleep(1500);
        JComponent tablePanel = pageOf(frame, "Sơ đồ bàn");

        // ─── HÌNH 4.4: QuickCheckInDialog (bàn Trống) ───────────────────────
        TableInfo emptyTable = null;
        for (TableInfo t : new TableController().list()) {
            if ("Trống".equals(t.trangThai())) {
                emptyTable = t;
                break;
            }
        }
        if (emptyTable == null) {
            System.err.println("Không tìm thấy bàn Trống để chụp màn hình gọi món.");
        } else {
            System.out.println("Bàn Trống: " + emptyTable.tenBan());
            final TableInfo et = emptyTable;
            Method handle = tablePanel.getClass().getDeclaredMethod("handle", TableInfo.class);
            handle.setAccessible(true);
            SwingUtilities.invokeLater(() -> {
                try { handle.invoke(tablePanel, et); } catch (Exception ex) { ex.printStackTrace(); }
            });
            Thread.sleep(2000);

            Window orderDlg = findShowingDialog("Đón khách");
            if (orderDlg == null) {
                System.err.println("Không mở được QuickCheckInDialog. Các cửa sổ đang hiện:");
                dumpWindows();
            } else {
                bringToFront(orderDlg);
                Thread.sleep(700);
                saveCapture(orderDlg, new File(outDir, "screen_order.png"));
                System.out.println("Đã lưu: screen_order.png (" + orderDlg.getClass().getSimpleName() + ")");
                // Đóng dialog gọi món
                SwingUtilities.invokeAndWait(orderDlg::dispose);
                Thread.sleep(500);
            }
        }

        // ─── HÌNH 4.5: CheckoutDialog (bàn Đang phục vụ) ───────────────────
        TableInfo occupiedTable = null;
        for (TableInfo t : new TableController().list()) {
            if ("Đang phục vụ".equals(t.trangThai())) {
                occupiedTable = t;
                break;
            }
        }
        if (occupiedTable == null) {
            System.err.println("Không tìm thấy bàn Đang phục vụ để chụp màn hình thanh toán.");
        } else {
            System.out.println("Bàn đang phục vụ: " + occupiedTable.tenBan());
            final TableInfo ot = occupiedTable;
            Method handle = tablePanel.getClass().getDeclaredMethod("handle", TableInfo.class);
            handle.setAccessible(true);
            SwingUtilities.invokeLater(() -> {
                try { handle.invoke(tablePanel, ot); } catch (Exception ex) { ex.printStackTrace(); }
            });
            Thread.sleep(2000);

            // Tìm SessionDialog
            Window sessionDlg = findShowingDialog("Phiên ");
            if (sessionDlg == null) {
                System.err.println("Không mở được SessionDialog.");
                dumpWindows();
            } else {
                bringToFront(sessionDlg);
                Thread.sleep(500);

                // Bấm nút Thanh toán
                AbstractButton payBtn = findButton(sessionDlg, "Thanh toán và kết thúc phiên");
                if (payBtn == null) {
                    System.err.println("Không tìm thấy nút Thanh toán.");
                } else {
                    SwingUtilities.invokeLater(payBtn::doClick);
                    Thread.sleep(2500);

                    // Dismiss error popup nếu loadPreview() thất bại
                    dismissOptionPanes();
                    Thread.sleep(600);

                    Window checkoutDlg = findShowingDialog("Thanh toán");
                    if (checkoutDlg == null) {
                        System.err.println("CheckoutDialog chưa hiển thị. Các cửa sổ đang hiện:");
                        dumpWindows();
                    } else {
                        bringToFront(checkoutDlg);
                        Thread.sleep(700);
                        saveCapture(checkoutDlg, new File(outDir, "screen_thanhtoan.png"));
                        System.out.println("Đã lưu: screen_thanhtoan.png");
                        SwingUtilities.invokeAndWait(checkoutDlg::dispose);
                    }
                    SwingUtilities.invokeAndWait(sessionDlg::dispose);
                }
            }
        }

        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Xong! Ảnh lưu trong: " + outDir.getAbsolutePath());
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private static void clickNav(MainFrame frame, String name) throws Exception {
        Field f = MainFrame.class.getDeclaredField("buttons");
        f.setAccessible(true);
        Map<String, JButton> buttons = (Map<String, JButton>) f.get(frame);
        JButton btn = buttons.get(name);
        SwingUtilities.invokeAndWait(btn::doClick);
    }

    @SuppressWarnings("unchecked")
    private static JComponent pageOf(MainFrame frame, String name) throws Exception {
        Field f = MainFrame.class.getDeclaredField("pages");
        f.setAccessible(true);
        Map<String, JComponent> pages = (Map<String, JComponent>) f.get(frame);
        return pages.get(name);
    }

    private static void bringToFront(Window w) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            w.setAlwaysOnTop(true);
            w.toFront();
            w.requestFocus();
        });
    }

    /** Đóng tất cả JOptionPane dialog đang hiện (để bỏ qua lỗi loadPreview). */
    private static void dismissOptionPanes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w instanceof javax.swing.JDialog dlg) {
                    // JOptionPane dialogs thường chứa JOptionPane component
                    if (findOptionPane(dlg) != null) {
                        System.out.println("Đóng JOptionPane: " + dlg.getTitle());
                        dlg.dispose();
                    }
                }
            }
        });
    }

    private static JOptionPane findOptionPane(Container c) {
        if (c instanceof JOptionPane p) return p;
        for (Component child : c.getComponents()) {
            if (child instanceof Container cc) {
                JOptionPane found = findOptionPane(cc);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Window findShowingDialog(String titlePrefix) {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof javax.swing.JDialog dlg
                    && dlg.getTitle() != null && dlg.getTitle().startsWith(titlePrefix)) {
                return w;
            }
        }
        return null;
    }

    private static AbstractButton findButton(Component root, String text) {
        if (root instanceof AbstractButton b && text.equals(b.getText())) return b;
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                AbstractButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void dumpWindows() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing()) {
                String title = (w instanceof javax.swing.JDialog d) ? d.getTitle()
                        : (w instanceof javax.swing.JFrame f) ? f.getTitle()
                        : w.getClass().getSimpleName();
                System.out.println("  [" + w.getClass().getSimpleName() + "] " + title);
            }
        }
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
