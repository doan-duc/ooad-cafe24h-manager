package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.sql.Date;
import java.time.LocalDate;

import db.Db;
import db.SqlErrors;
import model.Ingredient;
import model.PurchaseLine;
import model.PurchaseReceipt;
import model.StockCount;
import model.StockCountLine;

public final class InventoryDao implements IInventoryDao {
    // Tóm tắt: Lấy danh sách phiếu nhập kho trong khoảng ngày
    @Override
    public List<PurchaseReceipt> listPurchaseReceipts(LocalDate from, LocalDate to) {
        String sql = """
                SELECT pnk.MaPhieuNK, pnk.NgayNhap, pnk.NhaCungCap,
                       nv.HoTen AS TenNguoiLap, pnk.GhiChu,
                       COALESCE(SUM(ct.SoLuongNhap * ct.DonGiaNhap), 0) AS TongTien,
                       pnk.TrangThai
                FROM dbo.PhieuNhapKho pnk
                INNER JOIN dbo.NhanVien nv ON nv.MaNV = pnk.MaNV
                LEFT JOIN dbo.ChiTietPhieuNhapKho ct ON ct.MaPhieuNK = pnk.MaPhieuNK
                WHERE pnk.NgayNhap >= ?
                  AND pnk.NgayNhap < DATEADD(DAY, 1, ?)
                GROUP BY pnk.MaPhieuNK, pnk.NgayNhap, pnk.NhaCungCap,
                         nv.HoTen, pnk.GhiChu, pnk.TrangThai
                ORDER BY pnk.NgayNhap DESC
                """;
        List<PurchaseReceipt> list = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp ts = result.getTimestamp("NgayNhap");
                    list.add(new PurchaseReceipt(
                            result.getString("MaPhieuNK"),
                            ts == null ? null : ts.toLocalDateTime(),
                            result.getNString("NhaCungCap"),
                            result.getNString("TenNguoiLap"),
                            result.getNString("GhiChu"),
                            result.getBigDecimal("TongTien"),
                            result.getNString("TrangThai")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được lịch sử nhập kho", ex);
        }
    }

    // Tóm tắt: Tìm nguyên liệu theo từ khóa (hiển thị trạng thái cảnh báo tồn kho)
    public List<Ingredient> listIngredients(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        String sql = """
                SELECT MaNL, TenNL, DonViTinh, SoLuongTon, MucCanhBao, TrangThaiTon
                FROM dbo.vw_CanhBaoTonKho
                WHERE ? = '' OR MaNL LIKE ? OR TenNL LIKE ?
                ORDER BY
                    CASE TrangThaiTon WHEN N'Hết hàng' THEN 0
                                      WHEN N'Sắp hết' THEN 1 ELSE 2 END,
                    TenNL
                """;
        List<Ingredient> ingredients = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + value + "%";
            statement.setNString(1, value);
            statement.setNString(2, like);
            statement.setNString(3, like);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ingredients.add(new Ingredient(
                            result.getString("MaNL"),
                            result.getNString("TenNL"),
                            result.getNString("DonViTinh"),
                            result.getBigDecimal("SoLuongTon"),
                            result.getBigDecimal("MucCanhBao"),
                            result.getNString("TrangThaiTon")));
                }
            }
            return ingredients;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được tồn kho", ex);
        }
    }

    // Tóm tắt: Thêm nguyên liệu mới
    public void insertIngredient(Ingredient ingredient) {
        String sql = """
                INSERT dbo.NguyenLieu
                    (MaNL, TenNL, DonViTinh, SoLuongTon, MucCanhBao)
                VALUES (?, ?, ?, ?, ?)
                """;
        saveIngredient(sql, ingredient, true);
    }

    // Tóm tắt: Cập nhật thông tin nguyên liệu
    public void updateIngredient(Ingredient ingredient) {
        String sql = """
                UPDATE dbo.NguyenLieu
                SET TenNL = ?, DonViTinh = ?, MucCanhBao = ?
                WHERE MaNL = ?
                """;
        saveIngredient(sql, ingredient, false);
    }

    // Tóm tắt: Lập phiếu nhập kho mới
    public String createPurchaseReceipt(
            String supplier,
            String note,
            String maNV,
            List<PurchaseLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập phải có ít nhất một nguyên liệu.");
        }
        String receiptId = id("NK");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement header = connection.prepareStatement("""
                    INSERT dbo.PhieuNhapKho
                        (MaPhieuNK, NgayNhap, NhaCungCap, TrangThai, GhiChu, MaNV)
                    VALUES (?, SYSDATETIME(), ?, N'Hoạt động', ?, ?)
                    """);
                    PreparedStatement detail = connection.prepareStatement("""
                    INSERT dbo.ChiTietPhieuNhapKho
                        (MaPhieuNK, MaNL, SoLuongNhap, DonGiaNhap)
                    VALUES (?, ?, ?, ?)
                    """)) {
                header.setString(1, receiptId);
                header.setNString(2, supplier);
                header.setNString(3, note);
                header.setString(4, maNV);
                header.executeUpdate();

                for (PurchaseLine line : lines) {
                    detail.setString(1, receiptId);
                    detail.setString(2, line.maNL());
                    detail.setBigDecimal(3, line.soLuong());
                    detail.setBigDecimal(4, line.donGia());
                    detail.addBatch();
                }
                detail.executeBatch();
                connection.commit();
                return receiptId;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw SqlErrors.wrap("Không lập được phiếu nhập kho", ex);
        }
    }

    // Tóm tắt: Hủy phiếu nhập kho
    public void cancelPurchaseReceipt(String receiptId, String reason) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_HuyPhieuNhapKho(?,?)}")) {
            statement.setString(1, receiptId);
            statement.setNString(2, reason);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không hủy được phiếu nhập", ex);
        }
    }

    // Tóm tắt: Kiểm tra cơ sở dữ liệu có hỗ trợ vòng đời phiếu kiểm kê
    public boolean supportsStockCountLifecycle() {
        try (Connection connection = Db.getConnection()) {
            return LifecycleColumns.read(connection).supported();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không kiểm tra được cấu trúc phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Lập phiếu kiểm kê kho
    public String createStockCount(
            String maNV,
            String note,
            List<StockCountLine> lines,
            boolean submitForApproval) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phiếu kiểm kê phải có ít nhất một nguyên liệu.");
        }
        String countId = id("KK");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LifecycleColumns lifecycle = LifecycleColumns.read(connection);
                if (!submitForApproval && !lifecycle.supported()) {
                    throw new IllegalStateException(
                            "Cơ sở dữ liệu hiện tại chưa có cột TrangThai và LyDoTuChoi "
                                    + "cho phiếu kiểm kê nên chưa thể lưu nháp.");
                }
                String headerSql = lifecycle.supported()
                        ? """
                          INSERT dbo.PhieuKiemKe
                              (MaPhieuKK, NgayKiemKe, GhiChu, MaNV, TrangThai)
                          VALUES (?, SYSDATETIME(), ?, ?, ?)
                          """
                        : """
                          INSERT dbo.PhieuKiemKe
                              (MaPhieuKK, NgayKiemKe, GhiChu, MaNV)
                          VALUES (?, SYSDATETIME(), ?, ?)
                          """;
                try (PreparedStatement header = connection.prepareStatement(headerSql);
                    PreparedStatement detail = connection.prepareStatement("""
                            INSERT dbo.ChiTietPhieuKiemKe
                                (MaPhieuKK, MaNL, SoLuongSoSach, SoLuongThucTe, LyDo)
                            SELECT ?, MaNL, SoLuongTon, ?, ?
                            FROM dbo.NguyenLieu
                            WHERE MaNL = ?
                            """)) {
                    header.setString(1, countId);
                    header.setNString(2, clean(note));
                    header.setString(3, maNV);
                    if (lifecycle.supported()) {
                        header.setString(4, "Nhap");
                    }
                    header.executeUpdate();

                    for (StockCountLine line : lines) {
                        detail.setString(1, countId);
                        detail.setBigDecimal(2, line.soLuongThucTe());
                        detail.setNString(3, clean(line.lyDo()));
                        detail.setString(4, line.maNL());
                        if (detail.executeUpdate() == 0) {
                            throw new IllegalStateException(
                                    "Không tìm thấy nguyên liệu " + line.maNL() + ".");
                        }
                    }
                    if (submitForApproval) {
                        try (PreparedStatement submit = connection.prepareStatement("""
                                UPDATE dbo.PhieuKiemKe
                                SET TrangThai = 'ChoDuyet',
                                    ThoiGianGuiDuyet = SYSDATETIME()
                                WHERE MaPhieuKK = ? AND TrangThai = 'Nhap'
                                """)) {
                            submit.setString(1, countId);
                            submit.executeUpdate();
                        }
                    }
                }
                connection.commit();
                return countId;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw SqlErrors.wrap("Không tạo được phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Duyệt phiếu kiểm kê
    public void approveStockCount(String countId, String approverId) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_DuyetPhieuKiemKe(?,?)}")) {
            statement.setString(1, countId);
            statement.setString(2, approverId);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không duyệt được phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Gửi phiếu kiểm kê để duyệt
    public void submitStockCount(String countId, String employeeId) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_GuiDuyetPhieuKiemKe(?,?)}")) {
            statement.setString(1, countId);
            statement.setString(2, employeeId);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không gửi duyệt được phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Từ chối phiếu kiểm kê
    public void rejectStockCount(
            String countId, String approverId, String reason) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_TuChoiPhieuKiemKe(?,?,?)}")) {
            statement.setString(1, countId);
            statement.setString(2, approverId);
            statement.setNString(3, reason.trim());
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không từ chối được phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Lấy danh sách phiếu kiểm kê
    public List<StockCount> listStockCounts() {
        List<StockCount> counts = new ArrayList<>();
        try (Connection connection = Db.getConnection()) {
            LifecycleColumns lifecycle = LifecycleColumns.read(connection);
            String statusExpression = lifecycle.supported()
                    ? """
                      CASE p.TrangThai
                          WHEN 'Nhap' THEN N'Nháp'
                          WHEN 'ChoDuyet' THEN N'Chờ duyệt'
                          WHEN 'DaDuyet' THEN N'Đã duyệt'
                          WHEN 'TuChoi' THEN N'Từ chối'
                          WHEN 'DaHuy' THEN N'Đã hủy'
                      END
                      """
                    : """
                      CASE WHEN p.MaNVDuyet IS NULL
                           THEN N'Chờ duyệt' ELSE N'Đã duyệt' END
                      """;
            String rejectionExpression = lifecycle.supported()
                    ? "p.LyDoTuChoi"
                    : "CAST(NULL AS NVARCHAR(255))";
            String sql = """
                SELECT p.MaPhieuKK, p.NgayKiemKe, p.GhiChu, p.MaNV,
                       nv.HoTen AS TenNguoiKiem, p.MaNVDuyet,
                       duyet.HoTen AS TenNguoiDuyet,
                       COUNT(ct.MaNL) AS SoDong,
                       COALESCE(SUM(ct.ChenhLech), 0) AS TongChenhLech,
                       %s AS TrangThai,
                       %s AS LyDoTuChoi
                FROM dbo.PhieuKiemKe p
                INNER JOIN dbo.NhanVien nv ON nv.MaNV = p.MaNV
                LEFT JOIN dbo.NhanVien duyet ON duyet.MaNV = p.MaNVDuyet
                LEFT JOIN dbo.ChiTietPhieuKiemKe ct ON ct.MaPhieuKK = p.MaPhieuKK
                GROUP BY p.MaPhieuKK, p.NgayKiemKe, p.GhiChu, p.MaNV,
                         nv.HoTen, p.MaNVDuyet, duyet.HoTen, %s, %s
                ORDER BY p.NgayKiemKe DESC
                """.formatted(
                    statusExpression,
                    rejectionExpression,
                    statusExpression,
                    rejectionExpression);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp date = result.getTimestamp("NgayKiemKe");
                    counts.add(new StockCount(
                            result.getString("MaPhieuKK"),
                            date.toLocalDateTime(),
                            result.getNString("GhiChu"),
                            result.getString("MaNV"),
                            result.getNString("TenNguoiKiem"),
                            result.getString("MaNVDuyet"),
                            result.getNString("TenNguoiDuyet"),
                            result.getInt("SoDong"),
                            result.getBigDecimal("TongChenhLech"),
                            result.getNString("TrangThai"),
                            result.getNString("LyDoTuChoi")));
                }
            }
            return counts;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được phiếu kiểm kê", ex);
        }
    }

    // Tóm tắt: Lấy chi tiết từng dòng trong phiếu kiểm kê
    public List<StockCountLine> listStockCountLines(String countId) {
        String sql = """
                SELECT ct.MaNL, nl.TenNL, nl.DonViTinh,
                       ct.SoLuongSoSach, ct.SoLuongThucTe,
                       ct.ChenhLech, ct.LyDo
                FROM dbo.ChiTietPhieuKiemKe ct
                INNER JOIN dbo.NguyenLieu nl ON nl.MaNL = ct.MaNL
                WHERE ct.MaPhieuKK = ?
                ORDER BY nl.TenNL
                """;
        List<StockCountLine> lines = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    lines.add(new StockCountLine(
                            result.getString("MaNL"),
                            result.getNString("TenNL"),
                            result.getNString("DonViTinh"),
                            result.getBigDecimal("SoLuongSoSach"),
                            result.getBigDecimal("SoLuongThucTe"),
                            result.getBigDecimal("ChenhLech"),
                            result.getNString("LyDo")));
                }
            }
            return lines;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được chi tiết kiểm kê", ex);
        }
    }

    private void saveIngredient(String sql, Ingredient ingredient, boolean insert) {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (insert) {
                statement.setString(1, ingredient.maNL());
                statement.setNString(2, ingredient.tenNL());
                statement.setNString(3, ingredient.donViTinh());
                statement.setBigDecimal(4, ingredient.soLuongTon());
                statement.setBigDecimal(5, ingredient.mucCanhBao());
            } else {
                statement.setNString(1, ingredient.tenNL());
                statement.setNString(2, ingredient.donViTinh());
                statement.setBigDecimal(3, ingredient.mucCanhBao());
                statement.setString(4, ingredient.maNL());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap(insert
                    ? "Không thêm được nguyên liệu"
                    : "Không cập nhật được nguyên liệu", ex);
        }
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 15 - prefix.length()).toUpperCase();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /*
     * Stock count approval requires status and rejection-reason columns.
     * Submission and review timestamps are used when the database provides them.
     */
    private record LifecycleColumns(
            boolean status,
            boolean rejectionReason,
            boolean submittedAt,
            boolean reviewedAt) {

        private boolean supported() {
            return status && rejectionReason;
        }

        private static LifecycleColumns read(Connection connection) throws SQLException {
            return new LifecycleColumns(
                    hasColumn(connection, "TrangThai"),
                    hasColumn(connection, "LyDoTuChoi"),
                    hasColumn(connection, "ThoiGianGuiDuyet"),
                    hasColumn(connection, "ThoiGianDuyet"));
        }

        private static boolean hasColumn(Connection connection, String column)
                throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT CASE
                        WHEN COL_LENGTH(N'dbo.PhieuKiemKe', ?) IS NULL THEN 0
                        ELSE 1
                    END
                    """)) {
                statement.setNString(1, column);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getInt(1) == 1;
                }
            }
        }
    }
}
