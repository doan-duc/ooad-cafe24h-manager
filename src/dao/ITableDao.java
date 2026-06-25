package dao;

import java.math.BigDecimal;
import java.util.List;

import model.CheckoutPreview;
import model.CheckoutReceipt;
import model.LookupItem;
import model.OrderRequestLine;
import model.TableInfo;

/** Defines data access operations for tables and checkout. */
public interface ITableDao {

    List<TableInfo> listTableMap();

    String[] checkIn(String maBan, String maKH, String maNV, String maBooking);

    String[] checkInWithOrder(
            String maBan,
            String maKH,
            String maNV,
            String maBooking,
            List<OrderRequestLine> lines);

    CheckoutPreview previewCheckout(String maBan, BigDecimal hourlyRate);

    CheckoutPreview previewCheckout(String maBan, String voucher, BigDecimal hourlyRate);

    CheckoutReceipt checkout(
            String maBan,
            String maNV,
            String paymentMethod,
            String voucher,
            BigDecimal hourlyRate);

    void markClean(String maBan);

    void updateStatus(String maBan, String newStatus, String requiredCurrentStatus);

    List<LookupItem> listAreas();

    List<LookupItem> listTables();
}
