package tools;
import ui.LoginFrame;
import ui.Theme;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class LoginPreviewCapture {
    private LoginPreviewCapture() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output PNG path.");
        }
        Theme.install();
        LoginFrame[] holder = new LoginFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setSize(1000, 650);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            holder[0] = frame;
        });
        Thread.sleep(900);

        LoginFrame frame = holder[0];
        Rectangle bounds = new Rectangle(
                frame.getLocationOnScreen().x,
                frame.getLocationOnScreen().y,
                frame.getWidth(),
                frame.getHeight());
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", new File(args[0]));
        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Login preview saved to " + args[0]);
    }
}
