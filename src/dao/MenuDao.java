package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.LookupItem;
import model.MenuItem;

public final class MenuDao implements IMenuDao {
    // Tóm tắt: Tìm danh sách món ăn theo từ khóa
    public List<MenuItem> list(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        String like = "%" + value + "%";
        String sql = """
                SELECT m.MaMon, m.TenMon, m.DonGia, m.HinhAnh, m.TrangThai,
                       m.MaDanhMuc, dm.TenDanhMuc, m.LoaiMon,
                       m.SoGioQuyDoi, m.HanSuDung
                FROM dbo.Mon m
                INNER JOIN dbo.DanhMuc dm ON dm.MaDanhMuc = m.MaDanhMuc
                WHERE ? = N'' OR m.TenMon LIKE ? OR m.MaMon LIKE ?
                ORDER BY dm.TenDanhMuc, m.TenMon
                """;
        List<MenuItem> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, value);
            statement.setNString(2, like);
            statement.setNString(3, like);
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
            throw SqlErrors.wrap("Không tải được danh sách món", ex);
        }
    }

    // Tóm tắt: Lấy danh sách danh mục món
    public List<LookupItem> categories() {
        List<LookupItem> items = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT MaDanhMuc, TenDanhMuc FROM dbo.DanhMuc ORDER BY TenDanhMuc");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                items.add(new LookupItem(result.getString(1), result.getNString(2)));
            }
            return items;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được danh mục món", ex);
        }
    }

    // Tóm tắt: Lưu hoặc cập nhật danh mục món
    public void saveCategory(LookupItem item, boolean insert) {
        String sql = insert
                ? "INSERT dbo.DanhMuc (MaDanhMuc, TenDanhMuc) VALUES (?, ?)"
                : "UPDATE dbo.DanhMuc SET TenDanhMuc = ? WHERE MaDanhMuc = ?";
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (insert) {
                statement.setString(1, item.id());
                statement.setNString(2, item.name());
            } else {
                statement.setNString(1, item.name());
                statement.setString(2, item.id());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không lưu được danh mục", ex);
        }
    }

    // Tóm tắt: Thêm món ăn mới
    public void insert(MenuItem item) {
        String sql = """
                INSERT dbo.Mon
                    (MaMon, TenMon, DonGia, HinhAnh, TrangThai, MaDanhMuc,
                     LoaiMon, SoGioQuyDoi, HanSuDung)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        saveItem(sql, item, true);
    }

    // Tóm tắt: Cập nhật thông tin món ăn
    public void update(MenuItem item) {
        String sql = """
                UPDATE dbo.Mon
                SET TenMon = ?, DonGia = ?, HinhAnh = ?, TrangThai = ?,
                    MaDanhMuc = ?, LoaiMon = ?, SoGioQuyDoi = ?, HanSuDung = ?
                WHERE MaMon = ?
                """;
        saveItem(sql, item, false);
    }

    private void saveItem(String sql, MenuItem item, boolean insert) {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (insert) {
                statement.setString(index++, item.maMon());
            }
            statement.setNString(index++, item.tenMon());
            statement.setBigDecimal(index++, item.donGia());
            setNullable(statement, index++, item.hinhAnh(), Types.VARCHAR);
            statement.setNString(index++, item.trangThai());
            statement.setString(index++, item.maDanhMuc());
            statement.setNString(index++, item.loaiMon());
            if (item.soGioQuyDoi() == null) {
                statement.setNull(index++, Types.DECIMAL);
            } else {
                statement.setBigDecimal(index++, item.soGioQuyDoi());
            }
            if (item.hanSuDung() == null) {
                statement.setNull(index++, Types.INTEGER);
            } else {
                statement.setInt(index++, item.hanSuDung());
            }
            if (!insert) {
                statement.setString(index, item.maMon());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap(insert ? "Không thêm được món" : "Không cập nhật được món", ex);
        }
    }

    private static void setNullable(
            PreparedStatement statement, int index, String value, int sqlType)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, sqlType);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
