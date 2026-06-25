package dao;

import java.util.List;

import model.Customer;
import model.InvoiceHistory;
import model.MenuItem;

/** Defines data access operations for customers and invoice history. */
public interface ICustomerDao {

    List<Customer> search(String keyword);

    void insert(Customer customer);

    void update(Customer customer);

    String[] topUpHours(String maKH, String maMon, String maNV, String maPT);

    List<MenuItem> listHourPackages();

    List<InvoiceHistory> invoiceHistory(String maKH);

    Customer findByPhone(String phone);
}
