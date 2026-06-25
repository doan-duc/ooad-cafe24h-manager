package ui;

// Tóm tắt: Đánh dấu panel cần tải lại dữ liệu mỗi khi được mở lại (tránh hiển thị dữ liệu cũ đã cache)
public interface Refreshable {
    // Tóm tắt: Được MainFrame gọi mỗi lần panel được chuyển tới hiển thị
    void onPageShown();
}
