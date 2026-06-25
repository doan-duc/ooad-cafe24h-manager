package controller;

import java.util.List;

import dao.EmployeeDao;
import dao.IEmployeeDao;
import model.Employee;
import model.LookupItem;
import security.Authorization;
import security.PasswordHasher;
import security.Permission;
import security.Session;

public final class EmployeeController {

    private final IEmployeeDao dao;

    /** Creates a controller backed by the application DAO. */
    public EmployeeController() {
        this(new EmployeeDao());
    }

    /** Creates a controller with an injected DAO. */
    public EmployeeController(IEmployeeDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Tìm kiếm nhân viên theo từ khóa
    public List<Employee> list(String keyword) {
        Authorization.require(Session.currentUser(), Permission.EMPLOYEE_VIEW);
        return dao.listEmployees(keyword);
    }

    // Tóm tắt: Lấy danh sách vai trò nhân viên
    public List<LookupItem> roles() {
        Authorization.require(Session.currentUser(), Permission.EMPLOYEE_VIEW);
        return dao.listRoles();
    }

    // Tóm tắt: Tạo mới nhân viên
    public void create(Employee employee, char[] password) {
        try {
            Authorization.require(Session.currentUser(), Permission.EMPLOYEE_MANAGE);
            Employee normalized = normalize(employee);
            if (password == null || password.length < 6) {
                throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
            }
            dao.insertEmployee(normalized, PasswordHasher.hash(password));
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    // Tóm tắt: Cập nhật thông tin nhân viên
    public void update(Employee employee) {
        Authorization.require(Session.currentUser(), Permission.EMPLOYEE_MANAGE);
        Employee normalized = normalize(employee);
        if (Session.currentUser().maNV().equals(normalized.maNV())
                && (!"Active".equals(normalized.trangThai())
                        || !Session.currentUser().maVaiTro().equals(normalized.maVaiTro()))) {
            throw new IllegalArgumentException(
                    "Không thể tự khóa hoặc tự đổi vai trò của tài khoản đang dùng.");
        }
        dao.updateEmployee(normalized);
    }

    // Tóm tắt: Đặt lại mật khẩu nhân viên
    public void resetPassword(String maNV, char[] password) {
        try {
            Authorization.require(Session.currentUser(), Permission.EMPLOYEE_MANAGE);
            if (maNV == null || maNV.isBlank()) {
                throw new IllegalArgumentException("Hãy chọn nhân viên cần đổi mật khẩu.");
            }
            if (password == null || password.length < 6) {
                throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
            }
            dao.updatePassword(maNV.trim(), PasswordHasher.hash(password));
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    private static Employee normalize(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Thông tin nhân viên không được để trống.");
        }
        AuthController.validateEmployeeFields(
                employee.maNV(), employee.hoTen(), employee.soDienThoai());
        if (employee.trangThai() == null || employee.trangThai().isBlank()
                || employee.maVaiTro() == null || employee.maVaiTro().isBlank()) {
            throw new IllegalArgumentException("Trạng thái và vai trò nhân viên là bắt buộc.");
        }
        return new Employee(
                employee.maNV().trim(),
                employee.hoTen().trim(),
                employee.soDienThoai().trim(),
                blankToNull(employee.email()),
                employee.matKhau(),
                employee.trangThai().trim(),
                employee.maVaiTro().trim(),
                employee.tenVaiTro());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
