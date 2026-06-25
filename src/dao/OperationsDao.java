package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import db.Db;
import db.SqlErrors;
import model.OperationsSnapshot;

public final class OperationsDao implements IOperationsDao {
    // Tóm tắt: Lấy tổng quan vận hành hiện tại (bàn, món, cảnh báo, doanh thu)
    public OperationsSnapshot snapshot(boolean includeRevenue) {
        String revenueExpression = includeRevenue
                ? """
                  (SELECT COALESCE(SUM(TongTien), 0)
                   FROM dbo.HoaDon
                   WHERE TrangThai = N'Đã thanh toán'
                     AND NgayLap >= CONVERT(DATE, SYSDATETIME())
                     AND NgayLap < DATEADD(DAY, 1, CONVERT(DATE, SYSDATETIME())))
                  """
                : "CONVERT(DECIMAL(18, 2), 0)";
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM dbo.Ban
                     WHERE TrangThai = N'Đang phục vụ') AS BanDangDung,
                    (SELECT COUNT(*) FROM dbo.ChiTietHoaDon
                     WHERE TrangThaiMon IN ('ChoPhaChe', 'DangPha', 'DaPha'))
                        AS MonChoXuLy,
                    (SELECT COUNT(*) FROM dbo.NguyenLieu
                     WHERE SoLuongTon <= MucCanhBao) AS NguyenLieuSapHet,
                    (SELECT COUNT(*) FROM dbo.ChotCa
                     WHERE TrangThaiChot = N'Đang mở') AS CaDangMo,
                    %s AS DoanhThuHomNay
                """.formatted(revenueExpression);
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return new OperationsSnapshot(
                    result.getInt("BanDangDung"),
                    result.getInt("MonChoXuLy"),
                    result.getInt("NguyenLieuSapHet"),
                    result.getInt("CaDangMo"),
                    result.getBigDecimal("DoanhThuHomNay"),
                    includeRevenue);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được tổng quan vận hành", ex);
        }
    }
}
