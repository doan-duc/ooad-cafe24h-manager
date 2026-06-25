package controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dao.IInventoryDao;
import dao.InventoryDao;
import model.Ingredient;
import model.PurchaseLine;
import model.PurchaseReceipt;
import model.StockCount;
import model.StockCountLine;
import security.Authorization;
import security.Permission;
import security.Session;

public final class BanhangController {
    private final IInventoryDao dao;

    public BanhangController() {
        this(new InventoryDao());
    }

    public BanhangController(IInventoryDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách phiếu nhập trong khoảng thời gian
    public List<PurchaseReceipt> listPurchaseReceipts(LocalDate from, LocalDate to) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_VIEW);
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        }
        return dao.listPurchaseReceipts(from, to);
    }

    // Tóm tắt: Tìm kiếm nguyên liệu theo từ khóa
    public List<Ingredient> list(String keyword) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_VIEW);
        return dao.listIngredients(keyword);
    }

    // Tóm tắt: Tạo mới nguyên liệu
    public void createIngredient(Ingredient ingredient) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        validate(ingredient);
        dao.insertIngredient(ingredient);
    }

    // Tóm tắt: Cập nhật thông tin nguyên liệu
    public void updateIngredient(Ingredient ingredient) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        validate(ingredient);
        dao.updateIngredient(ingredient);
    }

    // Tóm tắt: Tạo phiếu nhập hàng mới
    public String createPurchaseReceipt(
            String supplier, String note, List<PurchaseLine> lines) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        if (supplier == null || supplier.isBlank()) {
            throw new IllegalArgumentException("Nhà cung cấp là bắt buộc.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phiếu nhập phải có ít nhất một nguyên liệu.");
        }
        Set<String> ingredientIds = new HashSet<>();
        for (PurchaseLine line : lines) {
            if (line == null || line.maNL() == null || line.maNL().isBlank()
                    || line.soLuong() == null || line.soLuong().signum() <= 0
                    || line.donGia() == null || line.donGia().signum() < 0) {
                throw new IllegalArgumentException(
                        "Dòng nguyên liệu trong phiếu nhập không hợp lệ.");
            }
            if (!ingredientIds.add(line.maNL())) {
                throw new IllegalArgumentException(
                        "Mỗi nguyên liệu chỉ được xuất hiện một lần trong phiếu nhập.");
            }
        }
        return dao.createPurchaseReceipt(
                supplier.trim(), note, Session.currentUser().maNV(), lines);
    }

    // Tóm tắt: Hủy phiếu nhập hàng
    public void cancelPurchaseReceipt(String receiptId, String reason) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        if (receiptId == null || receiptId.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã phiếu nhập và lý do hủy là bắt buộc.");
        }
        dao.cancelPurchaseReceipt(receiptId.trim(), reason.trim());
    }

    // Tóm tắt: Tạo phiếu kiểm kê cho một nguyên liệu
    public String createStockCount(
            String maNL, BigDecimal actual, String reason, String note) {
        return createStockCount(
                note,
                List.of(new StockCountLine(
                        maNL, null, null, null, actual, null, reason)),
                true);
    }

    // Tóm tắt: Tạo phiếu kiểm kê với nhiều nguyên liệu
    public String createStockCount(
            String note, List<StockCountLine> lines, boolean submitForApproval) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        validateStockCountLines(lines);
        if (!submitForApproval && !dao.supportsStockCountLifecycle()) {
            throw new IllegalStateException(
                    "Cơ sở dữ liệu hiện tại chưa hỗ trợ lưu nháp phiếu kiểm kê.");
        }
        return dao.createStockCount(
                Session.currentUser().maNV(), note, lines, submitForApproval);
    }

    // Tóm tắt: Kiểm tra hỗ trợ chu kỳ phiếu kiểm kê
    public boolean supportsStockCountLifecycle() {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_VIEW);
        return dao.supportsStockCountLifecycle();
    }

    // Tóm tắt: Lấy danh sách tất cả phiếu kiểm kê
    public List<StockCount> stockCounts() {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_VIEW);
        return dao.listStockCounts();
    }

    // Tóm tắt: Lấy chi tiết dòng trong phiếu kiểm kê
    public List<StockCountLine> stockCountLines(String countId) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_VIEW);
        requireCountId(countId);
        return dao.listStockCountLines(countId);
    }

    // Tóm tắt: Gửi phiếu kiểm kê để duyệt
    public void submit(String countId) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_OPERATE);
        StockCount count = findStockCount(countId);
        if (!count.isDraft() && !count.isRejected()) {
            throw new IllegalStateException(
                    "Chỉ phiếu Nháp hoặc Từ chối mới có thể gửi duyệt.");
        }
        if (!Session.currentUser().maNV().equals(count.maNV())) {
            throw new SecurityException("Chỉ người lập phiếu mới có thể gửi duyệt.");
        }
        dao.submitStockCount(countId, Session.currentUser().maNV());
    }

    // Tóm tắt: Duyệt phiếu kiểm kê
    public void approve(String countId) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_APPROVE);
        StockCount count = findStockCount(countId);
        requirePendingForReview(count);
        dao.approveStockCount(countId, Session.currentUser().maNV());
    }

    // Tóm tắt: Từ chối phiếu kiểm kê
    public void reject(String countId, String reason) {
        Authorization.require(Session.currentUser(), Permission.INVENTORY_APPROVE);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc.");
        }
        StockCount count = findStockCount(countId);
        requirePendingForReview(count);
        dao.rejectStockCount(
                countId, Session.currentUser().maNV(), reason.trim());
    }

    private StockCount findStockCount(String countId) {
        requireCountId(countId);
        return dao.listStockCounts().stream()
                .filter(count -> count.maPhieuKK().equals(countId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu kiểm kê " + countId + "."));
    }

    private static void requirePendingForReview(StockCount count) {
        if (!count.isPending()) {
            throw new IllegalStateException(
                    "Chỉ phiếu Chờ duyệt mới có thể được duyệt hoặc từ chối.");
        }
        if (Session.currentUser().maNV().equals(count.maNV())) {
            throw new SecurityException("Người lập phiếu không được tự phê duyệt phiếu.");
        }
    }

    private static void requireCountId(String countId) {
        if (countId == null || countId.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu kiểm kê không hợp lệ.");
        }
    }

    private static void validateStockCountLines(List<StockCountLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phiếu kiểm kê phải có ít nhất một nguyên liệu.");
        }
        Set<String> ingredientIds = new HashSet<>();
        for (StockCountLine line : lines) {
            if (line == null || line.maNL() == null || line.maNL().isBlank()
                    || line.soLuongThucTe() == null
                    || line.soLuongThucTe().signum() < 0) {
                throw new IllegalArgumentException(
                        "Dòng nguyên liệu trong phiếu kiểm kê không hợp lệ.");
            }
            if (!ingredientIds.add(line.maNL())) {
                throw new IllegalArgumentException(
                        "Mỗi nguyên liệu chỉ được xuất hiện một lần trong phiếu kiểm kê.");
            }
        }
    }

    private static void validate(Ingredient ingredient) {
        if (ingredient == null
                || ingredient.maNL() == null || ingredient.maNL().isBlank()
                || ingredient.tenNL() == null || ingredient.tenNL().isBlank()
                || ingredient.donViTinh() == null || ingredient.donViTinh().isBlank()
                || ingredient.soLuongTon() == null || ingredient.soLuongTon().signum() < 0
                || ingredient.mucCanhBao() == null || ingredient.mucCanhBao().signum() < 0) {
            throw new IllegalArgumentException(
                    "Thông tin nguyên liệu không đầy đủ hoặc số lượng không hợp lệ.");
        }
    }
}
