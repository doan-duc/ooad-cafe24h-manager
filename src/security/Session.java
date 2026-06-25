package security;

import model.Employee;

public final class Session {
    private static Employee currentUser;

    private Session() {
    }

    // Tóm tắt: Trả về nhân viên hiện tại đang đăng nhập
    public static Employee currentUser() {
        return currentUser;
    }

    // Tóm tắt: Lưu nhân viên đăng nhập vào phiên
    public static void login(Employee employee) {
        currentUser = employee;
    }

    // Tóm tắt: Xóa phiên đăng nhập hiện tại
    public static void logout() {
        currentUser = null;
    }
}
