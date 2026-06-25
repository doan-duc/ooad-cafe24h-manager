package db;

/**
 * Immutable SQL Server connection settings.
 *
 * <p>Persistence is handled by {@link DatabaseConfigRepository}.
 */
public record DatabaseConfig(
        String server,
        int port,
        String database,
        String authentication,
        String username,
        String password,
        boolean encrypt,
        boolean trustServerCertificate) {

    // Tóm tắt: Trả về cấu hình mặc định ban đầu cho biểu mẫu kết nối
    /** Returns the initial settings shown by the connection form. */
    public static DatabaseConfig defaults() {
        return new DatabaseConfig(
                "localhost", 1433, "Cafe24hDB", "windows", "", "", true, true);
    }

    // Tóm tắt: Xây dựng JDBC URL từ cấu hình hiện tại
    /** Builds the JDBC URL for these settings. */
    public String jdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:sqlserver://")
                .append(server)
                .append(':')
                .append(port)
                .append(";databaseName=")
                .append(database)
                .append(";encrypt=")
                .append(encrypt)
                .append(";trustServerCertificate=")
                .append(trustServerCertificate)
                .append(';');

        if (usesWindowsAuthentication()) {
            url.append("integratedSecurity=true;authenticationScheme=NativeAuthentication;");
        }
        return url.toString();
    }

    // Tóm tắt: Kiểm tra xem có sử dụng xác thực Windows hay không
    /** Returns whether Windows Authentication is selected. */
    public boolean usesWindowsAuthentication() {
        return "windows".equalsIgnoreCase(authentication);
    }
}
