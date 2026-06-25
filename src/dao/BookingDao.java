package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import db.Db;
import db.SqlErrors;
import model.Booking;

public final class BookingDao implements IBookingDao {
    // Tóm tắt: Lấy danh sách tất cả booking
    public List<Booking> list() {
        String sql = """
                SELECT d.MaDatPhong, d.MaKH, kh.HoTen, kh.SoDienThoai,
                       d.MaBan, b.TenBan, d.ThoiGianBatDau,
                       d.ThoiGianKetThuc, d.TrangThai
                FROM dbo.DatPhong d
                INNER JOIN dbo.KhachHang kh ON kh.MaKH = d.MaKH
                INNER JOIN dbo.Ban b ON b.MaBan = d.MaBan
                ORDER BY d.ThoiGianBatDau DESC
                """;
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                bookings.add(new Booking(
                        result.getString("MaDatPhong"),
                        result.getString("MaKH"),
                        result.getNString("HoTen"),
                        result.getString("SoDienThoai"),
                        result.getString("MaBan"),
                        result.getNString("TenBan"),
                        result.getTimestamp("ThoiGianBatDau").toLocalDateTime(),
                        result.getTimestamp("ThoiGianKetThuc").toLocalDateTime(),
                        result.getNString("TrangThai")));
            }
            return bookings;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được booking", ex);
        }
    }

    // Tóm tắt: Tìm booking đã đặt sắp tới cho bàn
    public Optional<Booking> pendingForTable(String maBan) {
        String sql = """
                SELECT TOP (1)
                       d.MaDatPhong, d.MaKH, kh.HoTen, kh.SoDienThoai,
                       d.MaBan, b.TenBan, d.ThoiGianBatDau,
                       d.ThoiGianKetThuc, d.TrangThai
                FROM dbo.DatPhong d
                INNER JOIN dbo.KhachHang kh ON kh.MaKH = d.MaKH
                INNER JOIN dbo.Ban b ON b.MaBan = d.MaBan
                WHERE d.MaBan = ?
                  AND d.TrangThai = N'Đã đặt'
                  AND d.ThoiGianKetThuc >= SYSDATETIME()
                ORDER BY
                    CASE WHEN d.ThoiGianBatDau <= DATEADD(MINUTE, 30, SYSDATETIME())
                         THEN 0 ELSE 1 END,
                    d.ThoiGianBatDau
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maBan);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(readBooking(result));
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được booking của bàn", ex);
        }
    }

    // Tóm tắt: Thêm booking mới cho khách hàng
    public String create(
            String maKH, String maBan, LocalDateTime start, LocalDateTime end) {
        try (Connection connection = Db.getConnection()) {
            CustomerDao.ensureMembershipColumn(connection);
            return create(connection, maKH, maBan, start, end);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tạo được booking", ex);
        }
    }

    // Tóm tắt: Thêm booking cho khách vãng lai (tạo tài khoản khách nếu cần)
    public String createForGuest(
            String phone, String name, String maBan, LocalDateTime start, LocalDateTime end) {
        try (Connection connection = Db.getConnection()) {
            CustomerDao.ensureMembershipColumn(connection);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                String maKH = null;
                try (PreparedStatement find = connection.prepareStatement("""
                        SELECT MaKH
                        FROM dbo.KhachHang WITH (UPDLOCK, HOLDLOCK)
                        WHERE SoDienThoai = ?
                        """)) {
                    find.setString(1, phone);
                    try (ResultSet result = find.executeQuery()) {
                        if (result.next()) {
                            maKH = result.getString(1);
                        }
                    }
                }
                if (maKH == null) {
                    maKH = "KH" + java.util.UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 8).toUpperCase();
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT dbo.KhachHang
                                (MaKH, HoTen, SoDienThoai, HangThanhVien,
                                 DiemTichLuy, SoDuGio, LaThanhVien)
                            VALUES (?, ?, ?, N'Đồng', 0, 0, 0)
                            """)) {
                        insert.setString(1, maKH);
                        insert.setNString(2, name);
                        insert.setString(3, phone);
                        insert.executeUpdate();
                    }
                }
                String booking = create(connection, maKH, maBan, start, end);
                connection.commit();
                return booking;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw SqlErrors.wrap("Không tạo được booking cho khách", ex);
        }
    }

    // Tóm tắt: Hủy booking
    public void cancel(String maBooking) {
        try (Connection connection = Db.getConnection()) {
            String maBan = tableForBooking(connection, maBooking);
            try (CallableStatement statement = connection.prepareCall(
                    "{call dbo.usp_HuyBooking(?)}")) {
                statement.setString(1, maBooking);
                statement.execute();
            }
            if (maBan != null && hasPendingBooking(connection, maBan)) {
                markTableReserved(connection, maBan);
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không hủy được booking", ex);
        }
    }

    private static String create(
            Connection connection, String maKH, String maBan,
            LocalDateTime start, LocalDateTime end) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(
                "{call dbo.usp_TaoBooking(?,?,?,?,?)}")) {
            statement.setString(1, maKH);
            statement.setString(2, maBan);
            statement.setTimestamp(3, Timestamp.valueOf(start));
            statement.setTimestamp(4, Timestamp.valueOf(end));
            statement.registerOutParameter(5, Types.VARCHAR);
            statement.execute();
            markTableReserved(connection, maBan);
            return statement.getString(5);
        }
    }

    private static Booking readBooking(ResultSet result) throws SQLException {
        return new Booking(
                result.getString("MaDatPhong"),
                result.getString("MaKH"),
                result.getNString("HoTen"),
                result.getString("SoDienThoai"),
                result.getString("MaBan"),
                result.getNString("TenBan"),
                result.getTimestamp("ThoiGianBatDau").toLocalDateTime(),
                result.getTimestamp("ThoiGianKetThuc").toLocalDateTime(),
                result.getNString("TrangThai"));
    }

    private static void markTableReserved(Connection connection, String maBan)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dbo.Ban
                SET TrangThai = N'Đã đặt'
                WHERE MaBan = ?
                  AND TrangThai = N'Trống'
                  AND NOT EXISTS
                  (
                      SELECT 1
                      FROM dbo.PhienSuDung
                      WHERE MaBan = ?
                        AND TrangThai = N'Đang hoạt động'
                  )
                """)) {
            statement.setString(1, maBan);
            statement.setString(2, maBan);
            statement.executeUpdate();
        }
    }

    private static String tableForBooking(Connection connection, String maBooking)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT MaBan
                FROM dbo.DatPhong
                WHERE MaDatPhong = ?
                """)) {
            statement.setString(1, maBooking);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static boolean hasPendingBooking(Connection connection, String maBan)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM dbo.DatPhong
                WHERE MaBan = ?
                  AND TrangThai = N'Đã đặt'
                  AND ThoiGianKetThuc >= SYSDATETIME()
                """)) {
            statement.setString(1, maBan);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
