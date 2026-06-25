package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.MenuItem;
import model.OrderLine;

public final class OrderDao implements IOrderDao {
    // Tóm tắt: Tìm menu đang bán theo từ khóa và loại
    public List<MenuItem> searchMenu(String keyword, String type) {
        String value = keyword == null ? "" : keyword.trim();
        String typeValue = type == null ? "" : type.trim();
        String sql = """
                SELECT m.MaMon, m.TenMon, m.DonGia, m.HinhAnh, m.TrangThai,
                       m.MaDanhMuc, dm.TenDanhMuc, m.LoaiMon,
                       m.SoGioQuyDoi, m.HanSuDung
                FROM dbo.Mon m
                INNER JOIN dbo.DanhMuc dm ON dm.MaDanhMuc = m.MaDanhMuc
                WHERE m.TrangThai = N'Đang bán'
                  AND (? = N'' OR m.TenMon LIKE ? OR m.MaMon LIKE ?)
                  AND (? = N'' OR m.LoaiMon = ?)
                ORDER BY dm.TenDanhMuc, m.TenMon
                """;
        List<MenuItem> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + value + "%";
            statement.setNString(1, value);
            statement.setNString(2, like);
            statement.setNString(3, like);
            statement.setNString(4, typeValue);
            statement.setNString(5, typeValue);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new MenuItem(
                            result.getString("MaMon"),
                            result.getNString("TenMon"),
                            result.getBigDecimal("DonGia"),
                            result.getString("HinhAnh"),
                            result.getNString("TrangThai"),
                            result.getString("MaDanhMuc"),
                            result.getNString("TenDanhMuc"),
                            result.getNString("LoaiMon"),
                            result.getBigDecimal("SoGioQuyDoi"),
                            (Integer) result.getObject("HanSuDung")));
                }
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được menu", ex);
        }
    }

    // Tóm tắt: Lấy chi tiết các dòng trong hóa đơn
    public List<OrderLine> listInvoiceLines(String maHD) {
        String sql = """
                SELECT ct.MaCTHD, ct.MaHD, p.MaBan, b.TenBan, ct.MaMon,
                       m.TenMon, ct.SoLuong, ct.DonGia, ct.GhiChu,
                       ct.TrangThaiMon, ct.ThoiGianTao
                FROM dbo.ChiTietHoaDon ct
                INNER JOIN dbo.Mon m ON m.MaMon = ct.MaMon
                INNER JOIN dbo.HoaDon hd ON hd.MaHD = ct.MaHD
                LEFT JOIN dbo.PhienSuDung p ON p.MaPhien = hd.MaPhien
                LEFT JOIN dbo.Ban b ON b.MaBan = p.MaBan
                WHERE ct.MaHD = ?
                ORDER BY ct.MaCTHD
                """;
        return queryLines(sql, maHD);
    }

    // Tóm tắt: Lấy danh sách món cần xử lý cho bếp
    public List<OrderLine> listKitchenLines() {
        String sql = """
                SELECT v.MaCTHD, v.MaHD, v.MaBan, v.TenBan, v.MaMon,
                       v.TenMon, v.SoLuong, ct.DonGia, v.GhiChu,
                       v.TrangThaiMon, v.ThoiGianTao
                FROM dbo.vw_OrderPhaChe v
                INNER JOIN dbo.ChiTietHoaDon ct ON ct.MaCTHD = v.MaCTHD
                ORDER BY v.ThoiGianTao, v.MaCTHD
                """;
        return queryLines(sql, null);
    }

    // Tóm tắt: Thêm món vào hóa đơn
    public void addItem(String maHD, String maMon, int quantity, String note) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_ThemMonVaoHoaDon(?,?,?,?)}")) {
            statement.setString(1, maHD);
            statement.setString(2, maMon);
            statement.setInt(3, quantity);
            if (note == null || note.isBlank()) {
                statement.setNull(4, Types.NVARCHAR);
            } else {
                statement.setNString(4, note.trim());
            }
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không thêm được món vào order", ex);
        }
    }

    // Tóm tắt: Cập nhật số lượng và ghi chú cho dòng trong order
    public void updateLine(int maCTHD, int quantity, String note) {
        String sql = """
                UPDATE dbo.ChiTietHoaDon
                SET SoLuong = ?, GhiChu = ?
                WHERE MaCTHD = ?
                  AND TrangThaiMon IN ('ChoPhaChe', 'DangPha')
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            if (note == null || note.isBlank()) {
                statement.setNull(2, Types.NVARCHAR);
            } else {
                statement.setNString(2, note.trim());
            }
            statement.setInt(3, maCTHD);
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Món đã pha/giao nên không thể sửa.");
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không sửa được món", ex);
        }
    }

    // Tóm tắt: Cập nhật trạng thái xử lý (chờ/đang pha/đã pha) của món
    public void updateStatus(int maCTHD, String status) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_CapNhatTrangThaiMon(?,?)}")) {
            statement.setInt(1, maCTHD);
            statement.setString(2, status);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không cập nhật được trạng thái món", ex);
        }
    }

    private List<OrderLine> queryLines(String sql, String maHD) {
        List<OrderLine> lines = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (maHD != null) {
                statement.setString(1, maHD);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    lines.add(new OrderLine(
                            result.getInt("MaCTHD"),
                            result.getString("MaHD"),
                            result.getString("MaBan"),
                            result.getNString("TenBan"),
                            result.getString("MaMon"),
                            result.getNString("TenMon"),
                            result.getInt("SoLuong"),
                            result.getBigDecimal("DonGia"),
                            result.getNString("GhiChu"),
                            result.getString("TrangThaiMon"),
                            result.getTimestamp("ThoiGianTao").toLocalDateTime(),
                            true));
                }
            }
            return lines;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được chi tiết order", ex);
        }
    }
}
