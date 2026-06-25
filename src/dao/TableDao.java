package dao;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.CheckoutPreview;
import model.CheckoutReceipt;
import model.LookupItem;
import model.OrderRequestLine;
import model.TableInfo;

public final class TableDao implements ITableDao {
    // Tóm tắt: Lấy sơ đồ bàn với trạng thái và thông tin phiên phục vụ
    public List<TableInfo> listTableMap() {
        String sql = """
                SELECT MaBan, TenBan, LoaiViTri, SucChua, TrangThai, MaKhuVuc,
                       TenKhuVuc, MaPhien, ThoiGianVao, MaKH, TenKhachHang,
                       SoDienThoai, MaHD, TienMon, SoPhutDaDung
                FROM dbo.vw_SoDoBan
                ORDER BY TenKhuVuc, TenBan
                """;
        List<TableInfo> tables = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Timestamp checkIn = result.getTimestamp("ThoiGianVao");
                Object minutes = result.getObject("SoPhutDaDung");
                tables.add(new TableInfo(
                        result.getString("MaBan"),
                        result.getNString("TenBan"),
                        result.getNString("LoaiViTri"),
                        result.getInt("SucChua"),
                        result.getNString("TrangThai"),
                        result.getString("MaKhuVuc"),
                        result.getNString("TenKhuVuc"),
                        result.getString("MaPhien"),
                        checkIn == null ? null : checkIn.toLocalDateTime(),
                        result.getString("MaKH"),
                        result.getNString("TenKhachHang"),
                        result.getString("SoDienThoai"),
                        result.getString("MaHD"),
                        result.getBigDecimal("TienMon"),
                        minutes == null ? null : result.getInt("SoPhutDaDung")));
            }
            return tables;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được sơ đồ bàn", ex);
        }
    }

    // Tóm tắt: Bắt đầu phục vụ bàn (check-in)
    public String[] checkIn(String maBan, String maKH, String maNV, String maBooking) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = prepareCheckIn(
                        connection, maBan, maKH, maNV, maBooking)) {
            statement.execute();
            return checkInResult(statement);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không check-in được bàn", ex);
        }
    }

    // Tóm tắt: Bắt đầu phục vụ bàn và thêm các món ngay
    public String[] checkInWithOrder(
            String maBan,
            String maKH,
            String maNV,
            String maBooking,
            List<OrderRequestLine> lines) {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String[] result;
                try (CallableStatement checkIn = prepareCheckIn(
                        connection, maBan, maKH, maNV, maBooking)) {
                    checkIn.execute();
                    result = checkInResult(checkIn);
                }
                try (CallableStatement add = connection.prepareCall(
                        "{call dbo.usp_ThemMonVaoHoaDon(?,?,?,?)}")) {
                    for (OrderRequestLine line : lines) {
                        add.setString(1, result[1]);
                        add.setString(2, line.maMon());
                        add.setInt(3, line.soLuong());
                        setNullableNvarchar(add, 4, line.ghiChu());
                        add.execute();
                    }
                }
                connection.commit();
                return result;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw SqlErrors.wrap("Không thể bắt đầu phục vụ và ghi nhận món", ex);
        }
    }

    // Tóm tắt: Xem trước tạm tính thanh toán bàn
    public CheckoutPreview previewCheckout(String maBan, BigDecimal hourlyRate) {
        return previewCheckout(maBan, null, hourlyRate);
    }

    // Tóm tắt: Xem trước tạm tính với voucher
    public CheckoutPreview previewCheckout(
            String maBan, String voucher, BigDecimal hourlyRate) {
        try (Connection connection = Db.getConnection()) {
            CustomerDao.ensureMembershipColumn(connection);
            MembershipSnapshot member = loadMembershipSnapshot(connection, maBan);
            if (hasCheckoutPreviewProcedure(connection)) {
                return previewCheckoutWithProcedure(
                        connection, maBan, voucher, hourlyRate, member);
            }
            return previewCheckoutFromQueries(connection, maBan, voucher, hourlyRate, member);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tính được tạm tính checkout", ex);
        }
    }

    private CheckoutPreview previewCheckoutWithProcedure(
            Connection connection,
            String maBan,
            String voucher,
            BigDecimal hourlyRate,
            MembershipSnapshot member) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(
                "{call dbo.usp_XemTruocCheckout(?,?,?,?)}")) {
            statement.setString(1, maBan);
            setNullable(statement, 2, voucher);
            statement.setBigDecimal(3, hourlyRate);
            statement.setNull(4, Types.TIMESTAMP);
            boolean hasResult = statement.execute();
            while (!hasResult && statement.getUpdateCount() != -1) {
                hasResult = statement.getMoreResults();
            }
            if (!hasResult) {
                throw new SQLException(
                        "usp_XemTruocCheckout không trả về dữ liệu tạm tính.");
            }
            try (ResultSet result = statement.getResultSet()) {
                if (!result.next()) {
                    throw new SQLException(
                            "Không tìm thấy phiên đang hoạt động để tạm tính.");
                }
                BigDecimal food = result.getBigDecimal("TienMon");
                BigDecimal hourly = result.getBigDecimal("TienGio");
                BigDecimal total;
                if (hasColumn(result, "TongTien")) {
                    total = result.getBigDecimal("TongTien");
                } else if (hasColumn(result, "TamTinh")) {
                    total = result.getBigDecimal("TamTinh");
                } else {
                    BigDecimal discount = hasColumn(result, "TienGiam")
                            ? result.getBigDecimal("TienGiam") : BigDecimal.ZERO;
                    total = food.add(hourly).subtract(
                            discount == null ? BigDecimal.ZERO : discount);
                }
                int earned = member.member() ? Math.max(0,
                        food.divideToIntegralValue(BigDecimal.valueOf(10_000)).intValue()) : 0;
                return new CheckoutPreview(
                        result.getTimestamp("ThoiGianVao").toLocalDateTime(),
                        result.getTimestamp("ThoiGianRa").toLocalDateTime(),
                        result.getLong("TongPhut"),
                        food,
                        hourly,
                        total,
                        earned);
            }
        }
    }

    private CheckoutPreview previewCheckoutFromQueries(
            Connection connection,
            String maBan,
            String voucher,
            BigDecimal hourlyRate,
            MembershipSnapshot member) throws SQLException {
        String sql = """
                SELECT p.ThoiGianVao, SYSDATETIME() AS ThoiGianRa,
                       DATEDIFF(MINUTE, p.ThoiGianVao, SYSDATETIME()) AS TongPhut,
                       CONVERT(DECIMAL(10, 2),
                           DATEDIFF(SECOND, p.ThoiGianVao, SYSDATETIME())
                           / 3600.0) AS TongGio,
                       hd.TienMon, p.MaKH,
                       COALESCE(goi.SoGioConLai, 0) AS SoGioConLai
                FROM dbo.PhienSuDung p
                INNER JOIN dbo.HoaDon hd ON hd.MaPhien = p.MaPhien
                OUTER APPLY
                (
                    SELECT SUM(n.SoGioConLai) AS SoGioConLai
                    FROM dbo.LichSuNapGio n
                    WHERE n.MaKH = p.MaKH
                      AND n.SoGioConLai > 0
                      AND n.NgayHetHan >= SYSDATETIME()
                ) goi
                WHERE p.MaBan = ?
                  AND p.TrangThai = N'Đang hoạt động'
                  AND hd.TrangThai = N'Chưa thanh toán'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maBan);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException(
                            "Bàn không có phiên đang hoạt động để thanh toán.");
                }
                BigDecimal food = result.getBigDecimal("TienMon");
                BigDecimal totalHours = result.getBigDecimal("TongGio");
                BigDecimal packageHours = result.getBigDecimal("SoGioConLai");
                BigDecimal billableHours = totalHours.subtract(packageHours)
                        .max(BigDecimal.ZERO);
                BigDecimal hourly = billableHours.multiply(hourlyRate)
                        .setScale(0, RoundingMode.HALF_UP);
                BigDecimal discount = loadVoucherDiscount(
                        connection, voucher, food.add(hourly));
                int earned = member.member() ? Math.max(0,
                        food.divideToIntegralValue(BigDecimal.valueOf(10_000)).intValue()) : 0;
                return new CheckoutPreview(
                        result.getTimestamp("ThoiGianVao").toLocalDateTime(),
                        result.getTimestamp("ThoiGianRa").toLocalDateTime(),
                        result.getLong("TongPhut"),
                        food,
                        hourly,
                        food.add(hourly).subtract(discount),
                        earned);
            }
        }
    }

    // Tóm tắt: Thực hiện thanh toán và kết thúc phiên phục vụ
    public CheckoutReceipt checkout(
            String maBan,
            String maNV,
            String tenPT,
            String voucher,
            BigDecimal hourlyRate) {
        try (Connection connection = Db.getConnection()) {
            CustomerDao.ensureMembershipColumn(connection);
            MembershipSnapshot member = loadMembershipSnapshot(connection, maBan);
            try (CallableStatement statement = connection.prepareCall(
                    "{call dbo.usp_ThanhToanCheckout(?,?,?,?,?,?)}")) {
                statement.setString(1, maBan);
                statement.setString(2, maNV);
                statement.setNString(3, tenPT);
                statement.registerOutParameter(4, Types.VARCHAR);
                setNullable(statement, 5, voucher);
                statement.setBigDecimal(6, hourlyRate);
                statement.execute();
                CheckoutReceipt base = loadReceipt(connection, statement.getString(4));
                syncFoodPoints(connection, member, base.tienMon());
                int earned = member.member() ? Math.max(0,
                        (base.tienMon() == null ? BigDecimal.ZERO : base.tienMon())
                            .divideToIntegralValue(BigDecimal.valueOf(10_000)).intValue()) : 0;
                String newTier = member.member() ? tierFor(member.pointsBefore() + earned) : null;
                return new CheckoutReceipt(
                        base.maHD(), base.thoiGianVao(), base.thoiGianRa(),
                        base.tongPhut(), base.tienMon(), base.tienGio(),
                        base.tienGiam(), base.tongTien(),
                        earned, newTier);
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không hoàn tất được thanh toán", ex);
        }
    }

    // Tóm tắt: Đánh dấu bàn cần dọn
    public void markClean(String maBan) {
        updateStatus(maBan, "Trống", "Cần dọn");
    }

    private record MembershipSnapshot(String maKH, int pointsBefore, boolean member) {
    }

    private static MembershipSnapshot loadMembershipSnapshot(
            Connection connection, String maBan) throws SQLException {
        String sql = """
                SELECT TOP (1) p.MaKH, kh.DiemTichLuy, kh.LaThanhVien
                FROM dbo.PhienSuDung p
                LEFT JOIN dbo.KhachHang kh ON kh.MaKH = p.MaKH
                WHERE p.MaBan = ?
                  AND p.TrangThai = N'Đang hoạt động'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maBan);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getString("MaKH") == null) {
                    return new MembershipSnapshot(null, 0, false);
                }
                return new MembershipSnapshot(
                        result.getString("MaKH"),
                        result.getInt("DiemTichLuy"),
                        result.getBoolean("LaThanhVien"));
            }
        }
    }

    private static void syncFoodPoints(
            Connection connection, MembershipSnapshot member, BigDecimal foodAmount)
            throws SQLException {
        if (member == null || !member.member() || member.maKH() == null) {
            return;
        }
        int earned = (foodAmount == null ? BigDecimal.ZERO : foodAmount)
                .divideToIntegralValue(BigDecimal.valueOf(10_000))
                .intValue();
        int nextPoints = member.pointsBefore() + Math.max(0, earned);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dbo.KhachHang
                SET DiemTichLuy = ?,
                    HangThanhVien = CASE
                        WHEN ? >= 500 THEN N'Vàng'
                        WHEN ? >= 200 THEN N'Bạc'
                        ELSE HangThanhVien
                    END
                WHERE MaKH = ?
                  AND LaThanhVien = 1
                """)) {
            statement.setInt(1, nextPoints);
            statement.setInt(2, nextPoints);
            statement.setInt(3, nextPoints);
            statement.setString(4, member.maKH());
            statement.executeUpdate();
        }
    }

    // Tóm tắt: Cập nhật trạng thái bàn
    public void updateStatus(String maBan, String newStatus, String requiredCurrentStatus) {
        String sql = "UPDATE dbo.Ban SET TrangThai = ? WHERE MaBan = ?";
        if (requiredCurrentStatus != null) {
            sql += " AND TrangThai = ?";
        }
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, newStatus);
            statement.setString(2, maBan);
            if (requiredCurrentStatus != null) {
                statement.setNString(3, requiredCurrentStatus);
            }
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Trạng thái bàn đã thay đổi. Hãy tải lại.");
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không cập nhật được trạng thái bàn", ex);
        }
    }

    // Tóm tắt: Lấy danh sách khu vực
    public List<LookupItem> listAreas() {
        return lookup("SELECT MaKhuVuc, TenKhuVuc FROM dbo.KhuVuc ORDER BY TenKhuVuc");
    }

    // Tóm tắt: Lấy danh sách bàn
    public List<LookupItem> listTables() {
        return lookup("SELECT MaBan, TenBan FROM dbo.Ban ORDER BY TenBan");
    }

    private List<LookupItem> lookup(String sql) {
        List<LookupItem> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(new LookupItem(result.getString(1), result.getNString(2)));
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được dữ liệu danh mục", ex);
        }
    }

    private static void setNullable(CallableStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setNullableNvarchar(
            CallableStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.NVARCHAR);
        } else {
            statement.setNString(index, value.trim());
        }
    }

    private static CallableStatement prepareCheckIn(
            Connection connection,
            String maBan,
            String maKH,
            String maNV,
            String maBooking) throws SQLException {
        CallableStatement statement = connection.prepareCall(
                "{call dbo.usp_CheckInBan(?,?,?,?,?,?)}");
        statement.setString(1, maBan);
        setNullable(statement, 2, maKH);
        statement.setString(3, maNV);
        setNullable(statement, 4, maBooking);
        statement.registerOutParameter(5, Types.VARCHAR);
        statement.registerOutParameter(6, Types.VARCHAR);
        return statement;
    }

    private static String[] checkInResult(CallableStatement statement) throws SQLException {
        return new String[] {statement.getString(5), statement.getString(6)};
    }

    private static CheckoutReceipt loadReceipt(Connection connection, String invoiceId)
            throws SQLException {
        String sql = """
                SELECT hd.MaHD, p.ThoiGianVao, p.ThoiGianRa,
                       DATEDIFF(MINUTE, p.ThoiGianVao, p.ThoiGianRa) AS TongPhut,
                       hd.TienMon, hd.TienGio, hd.TienGiam, hd.TongTien
                FROM dbo.HoaDon hd
                INNER JOIN dbo.PhienSuDung p ON p.MaPhien = hd.MaPhien
                WHERE hd.MaHD = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoiceId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Không đọc lại được hóa đơn vừa thanh toán.");
                }
                LocalDateTime checkIn = result.getTimestamp("ThoiGianVao").toLocalDateTime();
                LocalDateTime checkOut = result.getTimestamp("ThoiGianRa").toLocalDateTime();
                return new CheckoutReceipt(
                        result.getString("MaHD"),
                        checkIn,
                        checkOut,
                        result.getLong("TongPhut"),
                        result.getBigDecimal("TienMon"),
                        result.getBigDecimal("TienGio"),
                        result.getBigDecimal("TienGiam"),
                        result.getBigDecimal("TongTien"),
                        0,
                        null);
            }
        }
    }

    private static String tierFor(int points) {
        if (points >= 500) return "Vàng";
        if (points >= 200) return "Bạc";
        return "Đồng";
    }

    private static boolean hasCheckoutPreviewProcedure(Connection connection)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT OBJECT_ID(N'dbo.usp_XemTruocCheckout', N'P')");
                ResultSet result = statement.executeQuery()) {
            return result.next() && result.getObject(1) != null;
        }
    }

    private static BigDecimal loadVoucherDiscount(
            Connection connection, String voucher, BigDecimal subtotal)
            throws SQLException {
        if (voucher == null || voucher.isBlank()) {
            return BigDecimal.ZERO;
        }
        String sql = """
                SELECT LoaiGiam, GiaTriGiam
                FROM dbo.Voucher
                WHERE MaVoucher = ?
                  AND TrangThai = N'Hoạt động'
                  AND SYSDATETIME() BETWEEN NgayBatDau AND NgayKetThuc
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, voucher.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Voucher không tồn tại, bị khóa hoặc hết hạn.");
                }
                BigDecimal discount = "Phần trăm".equals(
                        result.getNString("LoaiGiam"))
                                ? subtotal.multiply(
                                        result.getBigDecimal("GiaTriGiam"))
                                        .divide(BigDecimal.valueOf(100), 0,
                                                RoundingMode.HALF_UP)
                                : result.getBigDecimal("GiaTriGiam");
                return discount.min(subtotal);
            }
        }
    }

    private static boolean hasColumn(ResultSet result, String name)
            throws SQLException {
        ResultSetMetaData metadata = result.getMetaData();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            if (name.equalsIgnoreCase(metadata.getColumnLabel(index))) {
                return true;
            }
        }
        return false;
    }
}
