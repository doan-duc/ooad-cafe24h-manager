package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.Area;
import model.RecipeLine;
import model.TableConfig;
import model.Voucher;

public final class ConfigDao implements IConfigDao {
    // Tóm tắt: Lấy danh sách khu vực
    public List<Area> listAreas() {
        List<Area> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT MaKhuVuc, TenKhuVuc, MoTa FROM dbo.KhuVuc ORDER BY TenKhuVuc");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(new Area(
                        result.getString(1), result.getNString(2), result.getNString(3)));
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được khu vực", ex);
        }
    }

    // Tóm tắt: Lưu hoặc cập nhật khu vực
    public void saveArea(Area area, boolean insert) {
        String sql = insert
                ? "INSERT dbo.KhuVuc (MaKhuVuc, TenKhuVuc, MoTa) VALUES (?, ?, ?)"
                : "UPDATE dbo.KhuVuc SET TenKhuVuc = ?, MoTa = ? WHERE MaKhuVuc = ?";
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (insert) {
                statement.setString(1, area.maKhuVuc());
                statement.setNString(2, area.tenKhuVuc());
                statement.setNString(3, area.moTa());
            } else {
                statement.setNString(1, area.tenKhuVuc());
                statement.setNString(2, area.moTa());
                statement.setString(3, area.maKhuVuc());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không lưu được khu vực", ex);
        }
    }

    // Tóm tắt: Lấy danh sách bàn với thông tin cấu hình
    public List<TableConfig> listTables() {
        String sql = """
                SELECT b.MaBan, b.TenBan, b.LoaiViTri, b.SucChua,
                       b.TrangThai, b.MaKhuVuc, k.TenKhuVuc
                FROM dbo.Ban b
                INNER JOIN dbo.KhuVuc k ON k.MaKhuVuc = b.MaKhuVuc
                ORDER BY k.TenKhuVuc, b.TenBan
                """;
        List<TableConfig> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(new TableConfig(
                        result.getString(1),
                        result.getNString(2),
                        result.getNString(3),
                        result.getInt(4),
                        result.getNString(5),
                        result.getString(6),
                        result.getNString(7)));
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được bàn", ex);
        }
    }

    // Tóm tắt: Lưu hoặc cập nhật cấu hình bàn
    public void saveTable(TableConfig table, boolean insert) {
        String sql = insert
                ? """
                  INSERT dbo.Ban
                      (MaBan, TenBan, LoaiViTri, SucChua, TrangThai, MaKhuVuc)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """
                : """
                  UPDATE dbo.Ban
                  SET TenBan = ?, LoaiViTri = ?, SucChua = ?, MaKhuVuc = ?
                  WHERE MaBan = ?
                  """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (insert) {
                statement.setString(index++, table.maBan());
            }
            statement.setNString(index++, table.tenBan());
            statement.setNString(index++, table.loaiViTri());
            statement.setInt(index++, table.sucChua());
            if (insert) {
                statement.setNString(index++, table.trangThai());
            }
            statement.setString(index++, table.maKhuVuc());
            if (!insert) {
                statement.setString(index, table.maBan());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không lưu được bàn", ex);
        }
    }

    // Tóm tắt: Lấy danh sách voucher
    public List<Voucher> listVouchers() {
        List<Voucher> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT MaVoucher, TenVoucher, LoaiGiam, GiaTriGiam,
                               NgayBatDau, NgayKetThuc, TrangThai
                        FROM dbo.Voucher ORDER BY NgayKetThuc DESC
                        """);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(new Voucher(
                        result.getString(1),
                        result.getNString(2),
                        result.getNString(3),
                        result.getBigDecimal(4),
                        result.getTimestamp(5).toLocalDateTime(),
                        result.getTimestamp(6).toLocalDateTime(),
                        result.getNString(7)));
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được voucher", ex);
        }
    }

    // Tóm tắt: Lưu hoặc cập nhật voucher
    public void saveVoucher(Voucher voucher, boolean insert) {
        String sql = insert
                ? """
                  INSERT dbo.Voucher
                      (MaVoucher, TenVoucher, LoaiGiam, GiaTriGiam,
                       NgayBatDau, NgayKetThuc, TrangThai)
                  VALUES (?, ?, ?, ?, ?, ?, ?)
                  """
                : """
                  UPDATE dbo.Voucher
                  SET TenVoucher = ?, LoaiGiam = ?, GiaTriGiam = ?,
                      NgayBatDau = ?, NgayKetThuc = ?, TrangThai = ?
                  WHERE MaVoucher = ?
                  """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (insert) {
                statement.setString(index++, voucher.maVoucher());
            }
            statement.setNString(index++, voucher.tenVoucher());
            statement.setNString(index++, voucher.loaiGiam());
            statement.setBigDecimal(index++, voucher.giaTriGiam());
            statement.setTimestamp(index++, Timestamp.valueOf(voucher.ngayBatDau()));
            statement.setTimestamp(index++, Timestamp.valueOf(voucher.ngayKetThuc()));
            statement.setNString(index++, voucher.trangThai());
            if (!insert) {
                statement.setString(index, voucher.maVoucher());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không lưu được voucher", ex);
        }
    }

    // Tóm tắt: Lấy định mức nguyên liệu cho một món
    public List<RecipeLine> listRecipe(String maMon) {
        String sql = """
                SELECT d.MaMon, m.TenMon, d.MaNL, n.TenNL,
                       n.DonViTinh, d.SoLuongTieuHao
                FROM dbo.DinhMuc d
                INNER JOIN dbo.Mon m ON m.MaMon = d.MaMon
                INNER JOIN dbo.NguyenLieu n ON n.MaNL = d.MaNL
                WHERE d.MaMon = ?
                ORDER BY n.TenNL
                """;
        List<RecipeLine> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maMon);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new RecipeLine(
                            result.getString(1),
                            result.getNString(2),
                            result.getString(3),
                            result.getNString(4),
                            result.getNString(5),
                            result.getBigDecimal(6)));
                }
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được định mức", ex);
        }
    }

    // Tóm tắt: Lưu định mức tiêu hao nguyên liệu cho một món
    public void saveRecipe(RecipeLine line) {
        String sql = """
                MERGE dbo.DinhMuc AS target
                USING (SELECT ? AS MaMon, ? AS MaNL) AS source
                ON target.MaMon = source.MaMon AND target.MaNL = source.MaNL
                WHEN MATCHED THEN
                    UPDATE SET SoLuongTieuHao = ?
                WHEN NOT MATCHED THEN
                    INSERT (MaMon, MaNL, SoLuongTieuHao)
                    VALUES (source.MaMon, source.MaNL, ?);
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, line.maMon());
            statement.setString(2, line.maNL());
            statement.setBigDecimal(3, line.soLuongTieuHao());
            statement.setBigDecimal(4, line.soLuongTieuHao());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không lưu được định mức", ex);
        }
    }

    // Tóm tắt: Xóa định mức nguyên liệu
    public void deleteRecipe(String maMon, String maNL) {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE dbo.DinhMuc WHERE MaMon = ? AND MaNL = ?")) {
            statement.setString(1, maMon);
            statement.setString(2, maNL);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không xóa được định mức", ex);
        }
    }
}
