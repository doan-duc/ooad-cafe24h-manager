package ui;



import javax.swing.SwingUtilities;

import dao.AuthDao;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            start();
        });
    }

    public static void start() {
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null || !Db.test(config)) {
            new ConnectionSetupFrame().setVisible(true);
            return;
        }

        Db.configure(config);
        try {
            AuthDao authDao = new AuthDao();
            int employeeCount = authDao.countEmployees();
            if (employeeCount == 0) {
                new FirstRunFrame().setVisible(true);
            } else if (authDao.countUsableAccounts() == 0) {
                new DatabaseRecoveryFrame(employeeCount).setVisible(true);
            } else {
                new LoginFrame().setVisible(true);
            }
        } catch (RuntimeException ex) {
            new ConnectionSetupFrame(ex.getMessage()).setVisible(true);
        }
    }
}
