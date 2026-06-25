package security;

import java.util.Set;

import model.Employee;

/** Evaluates employee permissions through a {@link RolePermissionProvider}. */
public final class Authorization {

    private static RolePermissionProvider provider = new HardcodedRolePermissionProvider();

    private Authorization() {
    }

    // Tóm tắt: Thiết lập nhà cung cấp quyền hạn cho ứng dụng
    /** Replaces the permission provider before application startup. */
    public static void setProvider(RolePermissionProvider newProvider) {
        if (newProvider == null) {
            throw new IllegalArgumentException("RolePermissionProvider không được null.");
        }
        provider = newProvider;
    }

    // Tóm tắt: Kiểm tra nhân viên có quyền thực hiện tác vụ hay không
    /** Returns whether the employee has the requested permission. */
    public static boolean can(Employee employee, Permission permission) {
        if (employee == null || permission == null) {
            return false;
        }
        return provider.permissionsOf(employee.maVaiTro()).contains(permission);
    }

    // Tóm tắt: Yêu cầu quyền hạn, ném ngoại lệ nếu thiếu
    /** Throws {@link SecurityException} when the employee lacks permission. */
    public static void require(Employee employee, Permission permission) {
        if (!can(employee, permission)) {
            throw new SecurityException(
                    "Tài khoản không có quyền thực hiện chức năng này.");
        }
    }

    // Tóm tắt: Trả về toàn bộ quyền hạn của nhân viên
    /** Returns all permissions granted to the employee. */
    public static Set<Permission> permissionsOf(Employee employee) {
        if (employee == null) {
            return Set.of();
        }
        return provider.permissionsOf(employee.maVaiTro());
    }
}
