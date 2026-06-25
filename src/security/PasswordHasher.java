package security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    // Tóm tắt: Mã hóa mật khẩu sử dụng BCrypt
    public static String hash(char[] password) {
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(12));
    }

    // Tóm tắt: Xác minh mật khẩu có khớp với hash không
    public static boolean matches(char[] password, String hash) {
        if (hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(new String(password), hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
