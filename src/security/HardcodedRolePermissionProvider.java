package security;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Provides the default in-memory role-to-permission mapping. */
public final class HardcodedRolePermissionProvider implements RolePermissionProvider {

    private static final Set<Permission> ALL = EnumSet.allOf(Permission.class);

    private static final Map<String, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            "VT01", ALL,
            "VT02", EnumSet.of(
                    Permission.TABLE_VIEW,
                    Permission.INVENTORY_VIEW,
                    Permission.INVENTORY_APPROVE,
                    Permission.EMPLOYEE_VIEW,
                    Permission.REPORT_VIEW,
                    Permission.SHIFT_MANAGE),
            "VT03", EnumSet.of(
                    Permission.TABLE_VIEW,
                    Permission.TABLE_OPERATE,
                    Permission.ORDER_CREATE,
                    Permission.CUSTOMER_MANAGE,
                    Permission.PAYMENT,
                    Permission.BOOKING_MANAGE,
                    Permission.SHIFT_OPERATE,
                    Permission.HOUR_PACKAGE_SELL),
            "VT04", EnumSet.of(
                    Permission.KITCHEN_OPERATE,
                    Permission.INVENTORY_VIEW,
                    Permission.INVENTORY_OPERATE,
                    Permission.SHIFT_OPERATE));

    // Tóm tắt: Trả về tập hợp quyền hạn cho vai trò cụ thể
    @Override
    public Set<Permission> permissionsOf(String maVaiTro) {
        return ROLE_PERMISSIONS.getOrDefault(maVaiTro, Set.of());
    }
}
