package dao;

import java.util.List;

import model.MenuItem;
import model.OrderLine;

/** Defines data access operations for orders and kitchen workflow. */
public interface IOrderDao {

    List<MenuItem> searchMenu(String keyword, String type);

    List<OrderLine> listInvoiceLines(String maHD);

    List<OrderLine> listKitchenLines();

    void addItem(String maHD, String maMon, int quantity, String note);

    void updateLine(int maCTHD, int quantity, String note);

    void updateStatus(int maCTHD, String status);
}
