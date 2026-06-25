package controller;

import java.util.List;

import dao.IMenuDao;
import dao.MenuDao;
import model.LookupItem;
import model.MenuItem;
import security.Authorization;
import security.Permission;
import security.Session;

public final class MenuController {
    private final IMenuDao dao;

    public MenuController() {
        this(new MenuDao());
    }

    public MenuController(IMenuDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Tìm kiếm món ăn theo từ khóa
    public List<MenuItem> list(String keyword) {
        Authorization.require(Session.currentUser(), Permission.MENU_MANAGE);
        return dao.list(keyword);
    }

    // Tóm tắt: Lấy danh sách danh mục món ăn
    public List<LookupItem> categories() {
        Authorization.require(Session.currentUser(), Permission.MENU_MANAGE);
        return dao.categories();
    }

    // Tóm tắt: Lưu hoặc cập nhật danh mục
    public void saveCategory(LookupItem category, boolean insert) {
        Authorization.require(Session.currentUser(), Permission.MENU_MANAGE);
        if (category.id() == null || category.id().isBlank()
                || category.name() == null || category.name().isBlank()) {
            throw new IllegalArgumentException("Mã và tên danh mục là bắt buộc.");
        }
        dao.saveCategory(category, insert);
    }

    // Tóm tắt: Tạo mới món ăn
    public void create(MenuItem item) {
        Authorization.require(Session.currentUser(), Permission.MENU_MANAGE);
        validate(item);
        dao.insert(item);
    }

    // Tóm tắt: Cập nhật thông tin món ăn
    public void update(MenuItem item) {
        Authorization.require(Session.currentUser(), Permission.MENU_MANAGE);
        validate(item);
        dao.update(item);
    }

    private static void validate(MenuItem item) {
        if (item == null
                || item.maMon() == null || item.maMon().isBlank()
                || item.tenMon() == null || item.tenMon().isBlank()
                || item.donGia() == null || item.donGia().signum() < 0
                || item.maDanhMuc() == null || item.maDanhMuc().isBlank()) {
            throw new IllegalArgumentException("Thông tin món chưa đầy đủ hoặc không hợp lệ.");
        }
        boolean hourPackage = "Gói giờ".equals(item.loaiMon());
        if (hourPackage
                && (item.soGioQuyDoi() == null || item.soGioQuyDoi().signum() <= 0
                || item.hanSuDung() == null || item.hanSuDung() <= 0)) {
            throw new IllegalArgumentException(
                    "Gói giờ cần số giờ quy đổi và hạn sử dụng lớn hơn 0.");
        }
        if (!hourPackage
                && (item.soGioQuyDoi() != null || item.hanSuDung() != null)) {
            throw new IllegalArgumentException(
                    "Đồ ăn và đồ uống không sử dụng thông tin gói giờ.");
        }
    }
}
