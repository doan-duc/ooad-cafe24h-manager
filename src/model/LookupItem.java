package model;

// Tóm tắt: Dữ liệu tra cứu chung (mã, tên)
public record LookupItem(String id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
