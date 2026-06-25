package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.BestSeller;
import model.RevenueSummary;
import model.TableUsage;

public final class ReportDao implements IReportDao {
    // Tóm tắt: Tính tổng doanh thu trong khoảng ngày
    public RevenueSummary revenue(LocalDate from, LocalDate to) {
        String sql = """
                SELECT COUNT_BIG(*) AS SoHoaDon,
                       COALESCE(SUM(TienGio), 0) AS DoanhThuGio,
                       COALESCE(SUM(CASE WHEN LoaiHoaDon <> 'NapGio'
                                         THEN TienMon ELSE 0 END), 0) AS DoanhThuFnb,
                       COALESCE(SUM(CASE WHEN LoaiHoaDon = 'NapGio'
                                         THEN TienMon ELSE 0 END), 0) AS DoanhThuGoiGio,
                       COALESCE(SUM(TienGiam), 0) AS TongGiamGia,
                       COALESCE(SUM(TongTien), 0) AS TongDoanhThu
                FROM dbo.HoaDon
                WHERE TrangThai = N'Đã thanh toán'
                  AND NgayLap >= ?
                  AND NgayLap < DATEADD(DAY, 1, ?)
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new RevenueSummary(
                        result.getLong("SoHoaDon"),
                        result.getBigDecimal("DoanhThuGio"),
                        result.getBigDecimal("DoanhThuFnb"),
                        result.getBigDecimal("DoanhThuGoiGio"),
                        result.getBigDecimal("TongGiamGia"),
                        result.getBigDecimal("TongDoanhThu"));
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tổng hợp được doanh thu", ex);
        }
    }

    // Tóm tắt: Lấy danh sách top 10 món bán chạy
    public List<BestSeller> bestSellers(LocalDate from, LocalDate to) {
        String sql = """
                SELECT TOP (10) m.MaMon, m.TenMon, m.LoaiMon,
                       SUM(CONVERT(BIGINT, ct.SoLuong)) AS SoLuongBan,
                       SUM(CONVERT(DECIMAL(18,2), ct.SoLuong * ct.DonGia)) AS DoanhThu
                FROM dbo.ChiTietHoaDon ct
                INNER JOIN dbo.HoaDon hd ON hd.MaHD = ct.MaHD
                INNER JOIN dbo.Mon m ON m.MaMon = ct.MaMon
                WHERE hd.TrangThai = N'Đã thanh toán'
                  AND hd.NgayLap >= ?
                  AND hd.NgayLap < DATEADD(DAY, 1, ?)
                  AND ct.TrangThaiMon <> 'DaHuy'
                GROUP BY m.MaMon, m.TenMon, m.LoaiMon
                ORDER BY SoLuongBan DESC, DoanhThu DESC
                """;
        List<BestSeller> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new BestSeller(
                            result.getString("MaMon"),
                            result.getNString("TenMon"),
                            result.getNString("LoaiMon"),
                            result.getLong("SoLuongBan"),
                            result.getBigDecimal("DoanhThu")));
                }
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được món bán chạy", ex);
        }
    }

    // Tóm tắt: Báo cáo thời gian sử dụng bàn trong khoảng ngày
    @Override
    public List<TableUsage> tableUsage(LocalDate from, LocalDate to) {
        String sql = """
                SELECT b.MaBan, b.TenBan, kv.TenKhuVuc,
                       COUNT(*) AS TongPhien,
                       COALESCE(SUM(p.TongThoiGian), 0) AS TongGio,
                       COALESCE(AVG(p.TongThoiGian), 0) AS TbGio
                FROM dbo.PhienSuDung p
                INNER JOIN dbo.Ban b ON b.MaBan = p.MaBan
                INNER JOIN dbo.KhuVuc kv ON kv.MaKhuVuc = b.MaKhuVuc
                WHERE p.ThoiGianVao >= ?
                  AND p.ThoiGianVao < DATEADD(DAY, 1, ?)
                  AND p.TrangThai = N'Đã kết thúc'
                GROUP BY b.MaBan, b.TenBan, kv.TenKhuVuc
                ORDER BY TongGio DESC
                """;
        List<TableUsage> list = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    list.add(new TableUsage(
                            result.getString("MaBan"),
                            result.getNString("TenBan"),
                            result.getNString("TenKhuVuc"),
                            result.getLong("TongPhien"),
                            result.getBigDecimal("TongGio"),
                            result.getBigDecimal("TbGio")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được báo cáo thời gian dùng bàn", ex);
        }
    }
}
