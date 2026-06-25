package controller;

import java.util.List;

import dao.ConfigDao;
import dao.IConfigDao;
import model.Area;
import model.RecipeLine;
import model.TableConfig;
import model.Voucher;
import security.Authorization;
import security.Permission;
import security.Session;

public final class ConfigController {
    private final IConfigDao dao;

    public ConfigController() {
        this(new ConfigDao());
    }

    public ConfigController(IConfigDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách khu vực
    public List<Area> areas() {
        require();
        return dao.listAreas();
    }

    // Tóm tắt: Lưu hoặc cập nhật khu vực
    public void saveArea(Area area, boolean insert) {
        require();
        if (area == null
                || area.maKhuVuc() == null || area.maKhuVuc().isBlank()
                || area.tenKhuVuc() == null || area.tenKhuVuc().isBlank()) {
            throw new IllegalArgumentException("Mã và tên khu vực là bắt buộc.");
        }
        dao.saveArea(area, insert);
    }

    // Tóm tắt: Lấy danh sách cấu hình bàn
    public List<TableConfig> tables() {
        require();
        return dao.listTables();
    }

    // Tóm tắt: Lưu hoặc cập nhật cấu hình bàn
    public void saveTable(TableConfig table, boolean insert) {
        require();
        if (table == null
                || table.maBan() == null || table.maBan().isBlank()
                || table.tenBan() == null || table.tenBan().isBlank()
                || table.maKhuVuc() == null || table.maKhuVuc().isBlank()
                || table.sucChua() <= 0) {
            throw new IllegalArgumentException("Thông tin bàn chưa đầy đủ hoặc không hợp lệ.");
        }
        dao.saveTable(table, insert);
    }

    // Tóm tắt: Lấy danh sách voucher
    public List<Voucher> vouchers() {
        require();
        return dao.listVouchers();
    }

    // Tóm tắt: Lưu hoặc cập nhật voucher
    public void saveVoucher(Voucher voucher, boolean insert) {
        require();
        if (voucher == null
                || voucher.maVoucher() == null || voucher.maVoucher().isBlank()
                || voucher.tenVoucher() == null || voucher.tenVoucher().isBlank()
                || voucher.giaTriGiam() == null || voucher.giaTriGiam().signum() < 0
                || voucher.ngayBatDau() == null || voucher.ngayKetThuc() == null
                || !voucher.ngayKetThuc().isAfter(voucher.ngayBatDau())) {
            throw new IllegalArgumentException("Thông tin voucher không hợp lệ.");
        }
        if ("Phần trăm".equals(voucher.loaiGiam())
                && voucher.giaTriGiam().compareTo(
                        java.math.BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Voucher phần trăm không được vượt quá 100%.");
        }
        dao.saveVoucher(voucher, insert);
    }

    // Tóm tắt: Lấy công thức nguyên liệu cho món
    public List<RecipeLine> recipe(String maMon) {
        require();
        return dao.listRecipe(maMon);
    }

    // Tóm tắt: Lưu dòng công thức
    public void saveRecipe(RecipeLine line) {
        require();
        if (line == null
                || line.maMon() == null || line.maMon().isBlank()
                || line.maNL() == null || line.maNL().isBlank()
                || line.soLuongTieuHao() == null
                || line.soLuongTieuHao().signum() <= 0) {
            throw new IllegalArgumentException("Định mức phải lớn hơn 0.");
        }
        dao.saveRecipe(line);
    }

    // Tóm tắt: Xóa dòng công thức
    public void deleteRecipe(String maMon, String maNL) {
        require();
        dao.deleteRecipe(maMon, maNL);
    }

    private static void require() {
        Authorization.require(Session.currentUser(), Permission.CONFIG_MANAGE);
    }
}
