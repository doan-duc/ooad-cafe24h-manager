package controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import dao.AuthDao;
import dao.IAuthDao;
import model.Employee;
import security.PasswordHasher;
import security.Session;

public final class AuthController {
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(30);
    private static final ConcurrentHashMap<String, LoginAttempt> LOGIN_ATTEMPTS =
            new ConcurrentHashMap<>();

    private static final int OTP_MAX_WRONG = 3;
    private static final Duration OTP_DURATION = Duration.ofMinutes(5);
    private static final ConcurrentHashMap<String, OtpRecord> OTP_STORE =
            new ConcurrentHashMap<>();

    private final IAuthDao dao;

    /** Creates a controller backed by the application DAO. */
    public AuthController() {
        this(new AuthDao());
    }

    /** Creates a controller with an injected DAO. */
    public AuthController(IAuthDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Xác thực người dùng, kiểm tra số lần đăng nhập sai, khóa tài khoản tạm thời nếu cần
    public Employee xuLyDangNhap(String login, char[] password) {
        try {
            if (login == null || login.isBlank()
                    || password == null || password.length == 0) {
                throw new IllegalArgumentException("Hãy nhập tài khoản và mật khẩu.");
            }

            String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
            LoginAttempt currentAttempt = LOGIN_ATTEMPTS.get(normalizedLogin);
            if (currentAttempt != null && currentAttempt.lockedUntil() != null
                    && Instant.now().isBefore(currentAttempt.lockedUntil())) {
                long seconds = Math.max(1,
                        Duration.between(Instant.now(), currentAttempt.lockedUntil()).toSeconds());
                throw new SecurityException(
                        "Tài khoản tạm khóa do nhập sai nhiều lần. Thử lại sau "
                                + seconds + " giây.");
            }

            Employee employee = dao.findForLogin(login.trim());
            if (employee == null || !PasswordHasher.matches(password, employee.matKhau())) {
                LoginAttempt updated = LOGIN_ATTEMPTS.compute(
                        normalizedLogin, (key, previous) -> {
                            int failures = previous == null ? 1 : previous.failures() + 1;
                            Instant lockedUntil = failures >= MAX_FAILED_ATTEMPTS
                                    ? Instant.now().plus(LOCK_DURATION) : null;
                            return new LoginAttempt(failures, lockedUntil);
                        });
                int remaining = Math.max(0, MAX_FAILED_ATTEMPTS - updated.failures());
                if (updated.lockedUntil() != null) {
                    throw new SecurityException(
                            "Đã nhập sai 3 lần. Tài khoản tạm khóa trong 30 giây.");
                }
                throw new SecurityException(
                        "Tài khoản hoặc mật khẩu không đúng. Còn "
                                + remaining + " lần thử.");
            }
            if (!"Active".equals(employee.trangThai())) {
                throw new SecurityException(
                        "Tài khoản đang bị khóa. Hãy liên hệ Chủ quán.");
            }

            LOGIN_ATTEMPTS.remove(normalizedLogin);
            Employee principal = withoutPassword(employee);
            Session.login(principal);
            return principal;
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
        }
    }

    // Tóm tắt: Tạo tài khoản chủ quán đầu tiên với kiểm tra mật khẩu, email, điện thoại
    public void createFirstOwner(
            String maNV,
            String hoTen,
            String phone,
            String email,
            char[] password,
            char[] confirmation) {
        try {
            validateEmployeeFields(maNV, hoTen, phone);
            if (password == null || password.length < 6) {
                throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
            }
            if (confirmation == null) {
                throw new IllegalArgumentException("Hãy nhập lại mật khẩu xác nhận.");
            }
            if (!java.util.Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Xác nhận mật khẩu không khớp.");
            }
            dao.createFirstOwner(
                    maNV.trim(),
                    hoTen.trim(),
                    phone.trim(),
                    blankToNull(email),
                    PasswordHasher.hash(password));
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                java.util.Arrays.fill(confirmation, '\0');
            }
        }
    }

    // Tóm tắt: Kiểm tra tính hợp lệ của mã nhân viên, họ tên, số điện thoại
    public static void validateEmployeeFields(String maNV, String name, String phone) {
        if (maNV == null || maNV.isBlank()
                || name == null || name.isBlank()
                || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã nhân viên, họ tên và số điện thoại là bắt buộc.");
        }
        if (maNV.trim().length() > 10) {
            throw new IllegalArgumentException("Mã nhân viên tối đa 10 ký tự.");
        }
        if (!phone.trim().matches("\\d{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại phải có 9 đến 15 chữ số.");
        }
    }

    /**
     * UC01.3: Finds an active account by phone or email for password reset.
     * Returns the Employee (without password hash in the result).
     */
    // Tóm tắt: Tìm tài khoản nhân viên để reset mật khẩu dựa trên số điện thoại hoặc email
    public Employee findForReset(String contact) {
        if (contact == null || contact.isBlank()) {
            throw new IllegalArgumentException(
                    "Nhập số điện thoại hoặc email đã đăng ký.");
        }
        Employee emp = dao.findByContact(contact.trim());
        if (emp == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy tài khoản với thông tin đã nhập.");
        }
        return withoutPassword(emp);
    }

    /**
     * UC01.5: Generates a 6-digit OTP for the given employee and returns it
     * (caller shows the code to the user — simulated delivery).
     */
    // Tóm tắt: Tạo mã OTP 6 chữ số, lưu vào store với hạn 5 phút, trả về code để hiển thị
    public String startOtp(String maNV) {
        String code = String.format("%06d",
                new java.util.Random().nextInt(1_000_000));
        OTP_STORE.put(maNV, new OtpRecord(code, Instant.now().plus(OTP_DURATION), 0));
        return code;
    }

    /**
     * UC01.5: Validates the OTP entered by the user.
     * Throws SecurityException with descriptive message on failure.
     */
    // Tóm tắt: Xác thực mã OTP, kiểm tra hạn, số lần nhập sai, xóa record khi đúng
    public void verifyOtp(String maNV, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Hãy nhập mã OTP.");
        }
        OtpRecord record = OTP_STORE.get(maNV);
        if (record == null) {
            throw new IllegalStateException("Không có yêu cầu OTP đang chờ xử lý.");
        }
        if (Instant.now().isAfter(record.expiresAt())) {
            OTP_STORE.remove(maNV);
            throw new SecurityException("Mã OTP đã hết hạn. Hãy yêu cầu mã mới.");
        }
        if (!record.code().equals(code.trim())) {
            int wrong = record.wrongCount() + 1;
            if (wrong >= OTP_MAX_WRONG) {
                OTP_STORE.remove(maNV);
                throw new SecurityException(
                        "Nhập sai OTP quá " + OTP_MAX_WRONG
                                + " lần. Hãy yêu cầu mã mới.");
            }
            OTP_STORE.put(maNV, new OtpRecord(record.code(), record.expiresAt(), wrong));
            throw new SecurityException(
                    "Mã OTP không đúng. Còn " + (OTP_MAX_WRONG - wrong) + " lần thử.");
        }
        OTP_STORE.remove(maNV);
    }

    /**
     * UC01.3 step 5-7: Updates password after OTP verification.
     * Caller must have already called verifyOtp successfully.
     */
    // Tóm tắt: Cập nhật mật khẩu sau khi xác thực OTP thành công
    public void resetPassword(String maNV, char[] newPwd, char[] confirm) {
        try {
            validateNewPassword(newPwd, confirm);
            dao.updatePassword(maNV, PasswordHasher.hash(newPwd));
        } finally {
            clearChars(newPwd);
            clearChars(confirm);
        }
    }

    /**
     * UC01.4 step 2-3: Verifies the current password for the logged-in user,
     * then generates and returns an OTP code.
     */
    // Tóm tắt: Xác thực mật khẩu hiện tại, khởi tạo quy trình đổi mật khẩu bằng OTP
    public String initiatePasswordChange(char[] currentPwd) {
        try {
            Employee user = Session.currentUser();
            if (user == null) {
                throw new SecurityException("Chưa đăng nhập.");
            }
            Employee withHash = dao.findForLogin(user.maNV());
            if (withHash == null || !PasswordHasher.matches(currentPwd, withHash.matKhau())) {
                throw new SecurityException("Mật khẩu hiện tại không đúng.");
            }
            return startOtp(user.maNV());
        } finally {
            clearChars(currentPwd);
        }
    }

    private static void validateNewPassword(char[] pwd, char[] confirm) {
        if (pwd == null || pwd.length < 6) {
            throw new IllegalArgumentException("Mật khẩu mới cần ít nhất 6 ký tự.");
        }
        if (!java.util.Arrays.equals(pwd, confirm)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp.");
        }
    }

    private static void clearChars(char[] arr) {
        if (arr != null) {
            java.util.Arrays.fill(arr, '\0');
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Employee withoutPassword(Employee employee) {
        return new Employee(
                employee.maNV(),
                employee.hoTen(),
                employee.soDienThoai(),
                employee.email(),
                null,
                employee.trangThai(),
                employee.maVaiTro(),
                employee.tenVaiTro());
    }

    private record LoginAttempt(int failures, Instant lockedUntil) {
    }

    private record OtpRecord(String code, Instant expiresAt, int wrongCount) {
    }
}
