package dao;

import java.util.List;

import java.time.LocalDate;

import model.Ingredient;
import model.PurchaseLine;
import model.PurchaseReceipt;
import model.StockCount;
import model.StockCountLine;

/** Defines data access operations for inventory and stock counts. */
public interface IInventoryDao {

    List<PurchaseReceipt> listPurchaseReceipts(LocalDate from, LocalDate to);

    List<Ingredient> listIngredients(String keyword);

    void insertIngredient(Ingredient ingredient);

    void updateIngredient(Ingredient ingredient);

    String createPurchaseReceipt(
            String supplier,
            String note,
            String maNV,
            List<PurchaseLine> lines);

    void cancelPurchaseReceipt(String receiptId, String reason);

    boolean supportsStockCountLifecycle();

    String createStockCount(
            String maNV,
            String note,
            List<StockCountLine> lines,
            boolean submitForApproval);

    List<StockCount> listStockCounts();

    List<StockCountLine> listStockCountLines(String countId);

    void submitStockCount(String countId, String employeeId);

    void approveStockCount(String countId, String approverId);

    void rejectStockCount(String countId, String approverId, String reason);
}
