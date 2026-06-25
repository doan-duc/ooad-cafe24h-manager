package tools;

import dao.BookingDao;
import dao.CustomerDao;
import dao.OrderDao;
import dao.TableDao;
import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;

/** Exercises read-only DAO flows without requiring an application login. */
public final class ReadOnlyDaoSmokeTest {
    private ReadOnlyDaoSmokeTest() {
    }

    public static void main(String[] args) {
        System.out.println("=== ReadOnlyDaoSmokeTest ===");
        DatabaseConfig config = DatabaseConfigRepository.load();
        if (config == null) {
            System.err.println("Chưa cấu hình kết nối.");
            System.exit(1);
        }
        Db.configure(config);

        int tables = new TableDao().listTableMap().size();
        int menu = new OrderDao().searchMenu("", "").size();
        int bookings = new BookingDao().list().size();
        int members = new CustomerDao().search("").size();

        System.out.println("1. Bàn: " + tables);
        System.out.println("2. Menu đang bán: " + menu);
        System.out.println("3. Booking: " + bookings);
        System.out.println("4. Thành viên: " + members);
        System.out.println("PASS");
    }
}
