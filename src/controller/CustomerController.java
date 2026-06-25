package controller;

import java.util.List;

import dao.CustomerDao;
import dao.ICustomerDao;
import model.Customer;
import model.InvoiceHistory;
import model.MenuItem;
import security.Authorization;
import security.Permission;
import security.Session;

public final class CustomerController {
    private final ICustomerDao dao;

    public CustomerController() {
        this(new CustomerDao());
    }

    public CustomerController(ICustomerDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Tìm kiếm khách hàng theo từ khóa
    public List<Customer> search(String keyword) {
        Authorization.require(Session.currentUser(), Permission.CUSTOMER_MANAGE);
        return dao.search(keyword);
    }

    // Tóm tắt: Tạo mới khách hàng
    public void create(Customer customer) {
        Authorization.require(Session.currentUser(), Permission.CUSTOMER_MANAGE);
        if (customer != null
                && (customer.maKH() == null || customer.maKH().isBlank())) {
            customer = new Customer(
                    newCustomerId(),
                    customer.hoTen(),
                    customer.soDienThoai(),
                    customer.email(),
                    customer.ngaySinh(),
                    customer.hangThanhVien(),
                    customer.diemTichLuy(),
                    customer.soDuGio());
        }
        validate(customer);
        if (customer.diemTichLuy() != 0 || customer.soDuGio().signum() != 0) {
            throw new IllegalArgumentException(
                    "Thành viên mới phải bắt đầu với 0 điểm và 0 giờ.");
        }
        dao.insert(customer);
    }

    // Tóm tắt: Tìm khách hàng thành viên qua số điện thoại
    public Customer findMemberByPhone(String phone) {
        Authorization.require(Session.currentUser(), Permission.TABLE_OPERATE);
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = phone.trim();
        if (!normalized.matches("\\d{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại thành viên không hợp lệ.");
        }
        return dao.findByPhone(normalized);
    }

    // Tóm tắt: Cập nhật thông tin khách hàng
    public void update(Customer customer) {
        Authorization.require(Session.currentUser(), Permission.CUSTOMER_MANAGE);
        validate(customer);
        dao.update(customer);
    }

    // Tóm tắt: Lấy danh sách gói giờ bán
    public List<MenuItem> hourPackages() {
        Authorization.require(Session.currentUser(), Permission.HOUR_PACKAGE_SELL);
        return dao.listHourPackages();
    }

    // Tóm tắt: Nạp giờ sử dụng cho khách hàng
    public String[] topUp(String maKH, String maMon, String tenPT) {
        Authorization.require(Session.currentUser(), Permission.HOUR_PACKAGE_SELL);
        if (maKH == null || maKH.isBlank()
                || maMon == null || maMon.isBlank()
                || tenPT == null || tenPT.isBlank()) {
            throw new IllegalArgumentException("Thông tin nạp giờ chưa đầy đủ.");
        }
        return dao.topUpHours(
                maKH.trim(), maMon.trim(), Session.currentUser().maNV(), tenPT.trim());
    }

    // Tóm tắt: Lấy lịch sử hoá đơn của khách hàng
    public List<InvoiceHistory> history(String maKH) {
        Authorization.require(Session.currentUser(), Permission.CUSTOMER_MANAGE);
        if (maKH == null || maKH.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn thành viên cần xem lịch sử.");
        }
        return dao.invoiceHistory(maKH.trim());
    }

    private void validate(Customer customer) {
        if (customer == null
                || customer.maKH() == null || customer.maKH().isBlank()
                || customer.hoTen() == null || customer.hoTen().isBlank()
                || customer.soDienThoai() == null || customer.soDienThoai().isBlank()) {
            throw new IllegalArgumentException(
                    "Họ tên và số điện thoại thành viên là bắt buộc.");
        }
        if (!customer.soDienThoai().matches("\\d{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (customer.diemTichLuy() < 0
                || customer.soDuGio() == null
                || customer.soDuGio().signum() < 0) {
            throw new IllegalArgumentException(
                    "Điểm tích lũy và số dư giờ không được âm.");
        }
    }

    private static String newCustomerId() {
        return "KH" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
    }
}
