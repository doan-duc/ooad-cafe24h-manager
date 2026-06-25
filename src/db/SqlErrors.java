package db;

import java.sql.SQLException;

public final class SqlErrors {
    private SqlErrors() {
    }

    // Tóm tắt: Bao gói ngoại lệ SQL thành RuntimeException với mô tả
    public static RuntimeException wrap(String action, Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String message = sqlException.getMessage();
                if (message != null && !message.isBlank()) {
                    return new IllegalStateException(action + ": " + clean(message), exception);
                }
            }
            current = current.getCause();
        }
        return new IllegalStateException(action + ": " + exception.getMessage(), exception);
    }

    private static String clean(String message) {
        return message
                .replaceAll("(?s)com\\.microsoft\\.sqlserver\\.jdbc\\.[^:]+:", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
