package security;

import java.util.Set;

/** Supplies permissions for each application role. */
public interface RolePermissionProvider {

    // Tóm tắt: Lấy tập hợp quyền hạn của một vai trò
    /**
     * Returns permissions assigned to a role.
     *
     * @param maVaiTro role identifier, such as {@code VT01}
     * @return assigned permissions, or an empty set for an unknown role
     */
    Set<Permission> permissionsOf(String maVaiTro);
}
