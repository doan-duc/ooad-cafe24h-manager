/*
    CAFE24H MANAGEMENT DATABASE - TEST DATA SEED SCRIPT

    Purpose:
    - Reset and populate the Cafe24hDB database with a rich set of test data.
    - Test credentials: admin / 111111 (Chủ quán), ql / 111111 (Quản lý),
      nv1 / 111111 (Thu ngân), nv2 / 111111 (Pha chế).
    - Safely resets all transactional and master tables to avoid FK conflicts.
    - Disables triggers temporarily to allow clean import of historical states.
*/

USE Cafe24hDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

PRINT N'====== BẮT ĐẦU THÊM DỮ LIỆU MẪU ======';

/* =========================================================
   1. TẠM VÔ HIỆU HÓA TRIGGERS ĐỂ NHẬP DỮ LIỆU LỊCH SỬ NHẤT QUÁN
   ========================================================= */
DISABLE TRIGGER dbo.trg_DatPhong_KhongTrungLich ON dbo.DatPhong;
DISABLE TRIGGER dbo.trg_DangKyCa_KhongTrungNgay ON dbo.DangKyCa;
DISABLE TRIGGER dbo.trg_ChiTietHoaDon_CapNhatTienMonVaKho ON dbo.ChiTietHoaDon;
DISABLE TRIGGER dbo.trg_ChiTietPhieuNhapKho_CapNhatTon ON dbo.ChiTietPhieuNhapKho;
DISABLE TRIGGER dbo.trg_PhieuNhapKho_HuyVaKhoiPhuc ON dbo.PhieuNhapKho;
DISABLE TRIGGER dbo.trg_PhieuKiemKe_DuyetDieuChinhTon ON dbo.PhieuKiemKe;
DISABLE TRIGGER dbo.trg_ChiTietPhieuKiemKe_KhoaSauGui ON dbo.ChiTietPhieuKiemKe;
PRINT N'1. Tắt triggers thành công.';

/* =========================================================
   2. XÓA DỮ LIỆU CŨ THEO THỨ TỰ NGƯỢC CỦA KHÓA NGOẠI
   ========================================================= */
DELETE FROM dbo.TieuHaoNguyenLieu;
DELETE FROM dbo.ChiTietHoaDon;
DELETE FROM dbo.LichSuNapGio;
DELETE FROM dbo.HoaDon;
DELETE FROM dbo.PhienSuDung;
DELETE FROM dbo.DatPhong;
DELETE FROM dbo.KhachHang;
DELETE FROM dbo.ChiTietPhieuKiemKe;
DELETE FROM dbo.PhieuKiemKe;
DELETE FROM dbo.ChiTietPhieuNhapKho;
DELETE FROM dbo.PhieuNhapKho;
DELETE FROM dbo.DinhMuc;
DELETE FROM dbo.Mon;
DELETE FROM dbo.DanhMuc;
DELETE FROM dbo.Ban;
DELETE FROM dbo.KhuVuc;
DELETE FROM dbo.DangKyCa;
DELETE FROM dbo.ChotCa;
DELETE FROM dbo.NhanVien;
DELETE FROM dbo.NguyenLieu;
DELETE FROM dbo.Voucher;
DELETE FROM dbo.CaLamViec;
DELETE FROM dbo.VaiTro;
PRINT N'2. Xóa sạch dữ liệu cũ thành công.';

/* =========================================================
   3. KHỞI TẠO CẤU HÌNH VAI TRÒ VÀ CA LÀM VIỆC
   ========================================================= */
-- Bảng VaiTro
INSERT INTO dbo.VaiTro (MaVaiTro, TenVaiTro, MoTa)
VALUES
    ('VT01', N'Chủ quán', N'Toàn quyền hệ thống'),
    ('VT02', N'Quản lý vận hành', N'Quản lý nhân sự, ca, báo cáo và vận hành'),
    ('VT03', N'Thu ngân/Lễ tân', N'Check-in, order, booking và thanh toán'),
    ('VT04', N'Pha chế/Kho', N'Pha chế, nhập kho và kiểm kê');

-- Bảng CaLamViec
INSERT INTO dbo.CaLamViec (MaCa, TenCa, GioBatDau, GioKetThuc, SucChua)
VALUES
    ('CA01', N'Ca sáng', '06:00:00', '14:00:00', 5),
    ('CA02', N'Ca chiều', '14:00:00', '22:00:00', 5),
    ('CA03', N'Ca đêm', '22:00:00', '06:00:00', 4);
PRINT N'3. Tạo Vai trò và Ca làm việc thành công.';

/* =========================================================
   4. THÊM TÀI KHOẢN NHÂN VIÊN (Mật khẩu mặc định: "password")
   ========================================================= */
-- Mật khẩu hash bên dưới tương ứng với chuỗi "password" được tạo bởi BCrypt
DECLARE @MatKhauHash VARCHAR(255) = '$2a$12$ma/7qU/y9ywLQeIpTwNwI.ujFAIr6lYzoiRi49g9gHq0vZjk3XfXi';

INSERT INTO dbo.NhanVien (MaNV, HoTen, SoDienThoai, Email, MatKhau, TrangThai, MaVaiTro)
VALUES
    ('admin', N'Nguyễn Văn A', '0901234567', 'owner@cafe24h.com', @MatKhauHash, 'Active', 'VT01'),
    ('ql', N'Trần Thị B', '0912345678', 'manager@cafe24h.com', @MatKhauHash, 'Active', 'VT02'),
    ('nv1', N'Lê Văn C', '0923456789', 'cashier@cafe24h.com', @MatKhauHash, 'Active', 'VT03'),
    ('nv2', N'Phạm Văn D', '0934567890', 'bartender@cafe24h.com', @MatKhauHash, 'Active', 'VT04');
PRINT N'4. Tạo tài khoản nhân viên thành công.';

/* =========================================================
   5. THÊM THÔNG TIN KHÁCH HÀNG
   ========================================================= */
INSERT INTO dbo.KhachHang (MaKH, HoTen, SoDienThoai, Email, NgaySinh, HangThanhVien, DiemTichLuy, SoDuGio, LaThanhVien)
VALUES
    ('KH001', N'Nguyễn Thị Hoa', '0987654321', 'hoa.nguyen@gmail.com', '1998-05-15', N'Vàng', 1200, 15.50, 1),
    ('KH002', N'Trần Minh Tuấn', '0976543210', 'tuan.tran@gmail.com', '2000-10-22', N'Bạc', 450, 5.00, 1),
    ('KH003', N'Phạm Lan Anh', '0965432109', 'lananh.pham@gmail.com', '1995-12-01', N'Đồng', 80, 0.00, 1),
    ('KH004', N'Khách Vãng Lai', NULL, NULL, NULL, N'Đồng', 0, 0.00, 0);
PRINT N'5. Tạo danh sách khách hàng thành công.';

/* =========================================================
   6. KHU VỰC VÀ BÀN TRONG QUÁN
   ========================================================= */
-- Bảng KhuVuc
INSERT INTO dbo.KhuVuc (MaKhuVuc, TenKhuVuc, MoTa)
VALUES
    ('KV01', N'Tầng trệt - Sân vườn', N'Khu vực ngoài trời thoáng đãng, nhiều cây xanh'),
    ('KV02', N'Lầu 1 - Máy lạnh', N'Khu vực yên tĩnh học tập, làm việc có điều hòa'),
    ('KV03', N'Sân thượng - Rooftop', N'Góc view đẹp lộng gió, thích hợp thư giãn ban đêm');

-- Bảng Ban (Trạng thái mặc định: Trống, Đang phục vụ, Đã đặt, Cần dọn)
INSERT INTO dbo.Ban (MaBan, TenBan, LoaiViTri, SucChua, TrangThai, MaKhuVuc)
VALUES
    -- Tầng trệt
    ('B001', N'Bàn Trệt 1', N'Cạnh cửa sổ', 4, N'Trống', 'KV01'),
    ('B002', N'Bàn Trệt 2', N'Trong góc', 2, N'Trống', 'KV01'),
    ('B003', N'Bàn Trệt 3', N'Trung tâm', 6, N'Trống', 'KV01'),
    -- Lầu 1 máy lạnh
    ('B101', N'Bàn Lầu 101', N'Cạnh cửa sổ', 4, N'Đang phục vụ', 'KV02'), -- Đang có khách sử dụng
    ('B102', N'Bàn Lầu 102 VIP', N'Phòng VIP kính', 8, N'Đã đặt', 'KV02'), -- Có booking trước
    ('B103', N'Bàn Lầu 103', N'Trung tâm', 4, N'Trống', 'KV02'),
    -- Sân thượng
    ('B201', N'Bàn Rooftop 201', N'Góc thoáng view phố', 4, N'Trống', 'KV03'),
    ('B202', N'Bàn Rooftop 202', N'Trung tâm sân khấu', 6, N'Trống', 'KV03');
PRINT N'6. Tạo Khu vực và Bàn thành công.';

/* =========================================================
   7. THỰC ĐƠN (DANH MỤC VÀ MÓN)
   ========================================================= */
-- Bảng DanhMuc
INSERT INTO dbo.DanhMuc (MaDanhMuc, TenDanhMuc, MoTa)
VALUES
    ('DM01', N'Cà phê truyền thống', N'Các món cà phê pha phin và pha máy'),
    ('DM02', N'Trà trái cây & Trà sữa', N'Đồ uống thanh mát và trà sữa trân châu'),
    ('DM03', N'Đồ ăn vặt', N'Hạt hướng dương, khô gà nhâm nhi học bài'),
    ('DM04', N'Bánh ngọt', N'Bánh sừng bò, tiramisu dùng kèm cà phê'),
    ('DM05', N'Gói giờ chơi', N'Các gói nạp giờ dùng máy học tập / phòng game');

-- Bảng Mon
-- Lưu ý ràng buộc CHECK gói giờ: LoaiMon = N'Gói giờ' yêu cầu SoGioQuyDoi > 0 và HanSuDung > 0.
-- LoaiMon khác bắt buộc SoGioQuyDoi và HanSuDung là NULL.
INSERT INTO dbo.Mon (MaMon, TenMon, DonGia, HinhAnh, TrangThai, MaDanhMuc, LoaiMon, SoGioQuyDoi, HanSuDung)
VALUES
    -- Cà phê (ảnh tải sẵn trong src/ui/images, load qua classpath - không cần mạng)
    ('M001', N'Cà phê đen đá', 25000.00, '/ui/images/caphe_den.jpg', N'Đang bán', 'DM01', N'Đồ uống', NULL, NULL),
    ('M002', N'Cà phê sữa đá', 29000.00, '/ui/images/caphe_sua.jpg', N'Đang bán', 'DM01', N'Đồ uống', NULL, NULL),
    ('M003', N'Bạc xỉu', 32000.00, '/ui/images/bac_xiu.jpg', N'Đang bán', 'DM01', N'Đồ uống', NULL, NULL),
    -- Trà
    ('M004', N'Trà đào cam sả', 35000.00, '/ui/images/tra_dao.jpg', N'Đang bán', 'DM02', N'Đồ uống', NULL, NULL),
    ('M005', N'Trà sữa trân châu', 39000.00, '/ui/images/tra_sua.jpg', N'Đang bán', 'DM02', N'Đồ uống', NULL, NULL),
    -- Đồ ăn vặt
    ('M101', N'Hạt hướng dương', 15000.00, '/ui/images/huong_duong.jpg', N'Đang bán', 'DM03', N'Đồ ăn', NULL, NULL),
    ('M102', N'Khô gà lá chanh', 25000.00, '/ui/images/kho_ga.jpg', N'Đang bán', 'DM03', N'Đồ ăn', NULL, NULL),
    -- Bánh
    ('M103', N'Bánh Croissant', 22000.00, '/ui/images/croissant.jpg', N'Đang bán', 'DM04', N'Đồ ăn', NULL, NULL),
    ('M104', N'Bánh Tiramisu', 35000.00, '/ui/images/tiramisu.jpg', N'Đang bán', 'DM04', N'Đồ ăn', NULL, NULL),
    -- Gói giờ (LoaiMon = Gói giờ)
    ('M201', N'Gói nạp 2 giờ chơi', 30000.00, '/ui/images/pack_2h.jpg', N'Đang bán', 'DM05', N'Gói giờ', 2.00, 30),
    ('M202', N'Gói nạp 5 giờ chơi', 70000.00, '/ui/images/pack_5h.jpg', N'Đang bán', 'DM05', N'Gói giờ', 5.00, 60),
    ('M203', N'Gói nạp 10 giờ chơi', 120000.00, '/ui/images/pack_10h.jpg', N'Đang bán', 'DM05', N'Gói giờ', 10.00, 90);
PRINT N'7. Tạo Thực đơn và Món thành công.';

/* =========================================================
   8. QUẢN LÝ KHO (NGUYÊN LIỆU VÀ ĐỊNH MỨC)
   ========================================================= */
-- Bảng NguyenLieu
INSERT INTO dbo.NguyenLieu (MaNL, TenNL, DonViTinh, SoLuongTon, MucCanhBao)
VALUES
    ('NL01', N'Hạt cà phê Robusta', 'g', 15000.0000, 2000.0000),
    ('NL02', N'Sữa đặc', 'g', 5000.0000, 1000.0000),
    ('NL03', N'Trà đen', 'g', 2000.0000, 500.0000),
    ('NL04', N'Sữa tươi thanh trùng', 'ml', 10000.0000, 2000.0000),
    ('NL05', N'Đào ngâm (miếng)', N'miếng', 120.0000, 20.0000),
    ('NL06', N'Đường nước', 'ml', 8000.0000, 1000.0000),
    ('NL07', N'Trân châu đen', 'g', 3000.0000, 500.0000);

-- Bảng DinhMuc (Công thức cấu tạo món)
INSERT INTO dbo.DinhMuc (MaMon, MaNL, SoLuongTieuHao)
VALUES
    -- Cà phê đen đá: 20g hạt cafe, 20ml đường nước
    ('M001', 'NL01', 20.0000),
    ('M001', 'NL06', 20.0000),
    -- Cà phê sữa đá: 20g hạt cafe, 30g sữa đặc
    ('M002', 'NL01', 20.0000),
    ('M002', 'NL02', 30.0000),
    -- Bạc xỉu: 10g hạt cafe, 40g sữa đặc, 80ml sữa tươi
    ('M003', 'NL01', 10.0000),
    ('M003', 'NL02', 40.0000),
    ('M003', 'NL04', 80.0000),
    -- Trà đào cam sả: 10g trà đen, 3 miếng đào ngâm, 30ml đường nước
    ('M004', 'NL03', 10.0000),
    ('M004', 'NL05', 3.0000),
    ('M004', 'NL06', 30.0000),
    -- Trà sữa trân châu: 15g trà đen, 120ml sữa tươi, 25ml đường nước, 50g trân châu đen
    ('M005', 'NL03', 15.0000),
    ('M005', 'NL04', 120.0000),
    ('M005', 'NL06', 25.0000),
    ('M005', 'NL07', 50.0000);
PRINT N'8. Khởi tạo nguyên liệu và công thức định mức thành công.';

/* =========================================================
   9. DANH SÁCH MÃ GIẢM GIÁ (VOUCHER)
   ========================================================= */
INSERT INTO dbo.Voucher (MaVoucher, TenVoucher, LoaiGiam, GiaTriGiam, NgayBatDau, NgayKetThuc, TrangThai)
VALUES
    ('VOUCHER10', N'Giảm 10% tổng hóa đơn', N'Phần trăm', 10.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', N'Hoạt động'),
    ('WELCOME50', N'Giảm 50.000đ tiền mặt', N'Tiền mặt', 50000.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', N'Hoạt động'),
    ('EXPIRED20', N'Voucher cũ đã hết hạn giảm 20%', N'Phần trăm', 20.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', N'Hết hạn');
PRINT N'9. Tạo Voucher thành công.';

/* =========================================================
   10. ĐĂNG KÝ CA LÀM VIỆC VÀ MỞ CA (CHỐT CA)
   ========================================================= */
-- Bảng DangKyCa (NV003 thu ngân đăng ký ca làm)
INSERT INTO dbo.DangKyCa (MaDangKy, MaNV, MaCa, NgayLam, TrangThai, MaNVDuyet, ThoiGianDangKy, ThoiGianDuyet, GhiChu)
VALUES
    -- Ca sáng ngày 20/06/2026 đã làm xong và đã duyệt
    ('DK062001', 'nv1', 'CA01', '2026-06-20', 'DaDuyet', 'ql', '2026-06-19 08:00:00', '2026-06-19 17:00:00', N'Ca làm việc thứ 7'),
    -- Ca chiều ngày 20/06/2026 đang làm và đã duyệt
    ('DK062002', 'nv1', 'CA02', '2026-06-20', 'DaDuyet', 'ql', '2026-06-19 08:15:00', '2026-06-19 17:01:00', N'Ca chiều thứ 7'),
    -- Đăng ký ca sáng chủ nhật đang chờ duyệt
    ('DK062101', 'nv1', 'CA01', '2026-06-21', 'ChoDuyet', NULL, '2026-06-20 09:00:00', NULL, N'Ca làm chủ nhật');

-- Bảng ChotCa
-- Lưu ý ràng buộc UNIQUE: Một nhân viên chỉ được phép có tối đa MỘT ca ở trạng thái "Đang mở".
INSERT INTO dbo.ChotCa (MaChotCa, ThoiGianMo, TienDauCa, TienThucTe, TienHeThong, ThoiGianChot, TrangThaiChot, MaCa, MaNV)
VALUES
    -- Ca sáng đã chốt bàn giao tiền đầy đủ
    ('CC062001', '2026-06-20 06:00:00', 1000000.00, 2550000.00, 2550000.00, '2026-06-20 14:00:00', N'Đã chốt', 'CA01', 'nv1'),
    -- Ca chiều đang làm việc, trạng thái Đang mở
    ('CC062002', '2026-06-20 14:00:00', 2550000.00, NULL, 0.00, NULL, N'Đang mở', 'CA02', 'nv1');
PRINT N'10. Tạo lịch sử ca và phiên mở ca (Chốt ca) thành công.';

/* =========================================================
   11. BOOKING ĐẶT TRƯỚC BÀN
   ========================================================= */
INSERT INTO dbo.DatPhong (MaDatPhong, MaKH, MaBan, ThoiGianBatDau, ThoiGianKetThuc, TrangThai)
VALUES
    -- Booking của khách KH001 tại bàn B102 VIP vào tối nay (Đang chờ nhận)
    ('DP00001', 'KH001', 'B102', '2026-06-20 19:00:00', '2026-06-20 22:00:00', N'Đã đặt'),
    -- Booking sáng nay của KH002 tại bàn B001 đã nhận khách thành công
    ('DP00002', 'KH002', 'B001', '2026-06-20 08:00:00', '2026-06-20 10:00:00', N'Đã nhận'),
    -- Booking ngày hôm qua của KH003 đã hoàn thành
    ('DP00003', 'KH003', 'B003', '2026-06-19 19:00:00', '2026-06-19 21:30:00', N'Đã nhận');
PRINT N'11. Tạo lịch sử Đặt bàn thành công.';

/* =========================================================
   12. PHIÊN SỬ DỤNG BÀN (SESSIONS)
   ========================================================= */
INSERT INTO dbo.PhienSuDung (MaPhien, ThoiGianVao, ThoiGianRa, TongThoiGian, TrangThai, MaBan, MaKH, MaNV, MaDatPhong)
VALUES
    -- Phiên sáng nay đã xong
    ('P00001', '2026-06-20 08:00:00', '2026-06-20 09:30:00', 1.50, N'Đã kết thúc', 'B001', 'KH002', 'nv1', 'DP00002'),
    -- Phiên tối qua đã xong
    ('P00003', '2026-06-19 19:00:00', '2026-06-19 21:30:00', 2.50, N'Đã kết thúc', 'B003', 'KH003', 'nv1', 'DP00003'),
    -- Phiên hiện tại đang mở trên bàn B101 để test POS
    ('P00002', '2026-06-20 16:30:00', NULL, NULL, N'Đang hoạt động', 'B101', 'KH003', 'nv1', NULL);
PRINT N'12. Tạo phiên sử dụng bàn thành công.';

/* =========================================================
   13. HÓA ĐƠN VÀ CHI TIẾT HÓA ĐƠN
   ========================================================= */
-- Bảng HoaDon
-- Lưu ý: TongTien là cột tính toán tự động dựa trên: (TienGio + TienMon - TienGiam)
INSERT INTO dbo.HoaDon (MaHD, NgayLap, TienGio, TienMon, TienGiam, TrangThai, LoaiHoaDon, MaPhien, MaNV, MaVoucher, MaChotCa, PhuongThucThanhToan)
VALUES
    -- Hóa đơn thanh toán bàn P00001 sáng nay (Tiền giờ tính 1.5h x 15000 = 22500đ, Tiền món 51000đ)
    ('HD00001', '2026-06-20 09:30:00', 22500.00, 51000.00, 0.00, N'Đã thanh toán', 'SuDungBan', 'P00001', 'nv1', NULL, 'CC062001', N'Tiền mặt'),
    -- Hóa đơn thanh toán bàn hôm qua P00003 (Tiền giờ tính 2.5h x 15000 = 37500đ, Tiền món 114000đ, Voucher giảm 10% = 15150đ)
    ('HD00003', '2026-06-19 21:30:00', 37500.00, 114000.00, 15150.00, N'Đã thanh toán', 'SuDungBan', 'P00003', 'nv1', 'VOUCHER10', NULL, N'Chuyển khoản'),
    -- Hóa đơn nạp giờ chơi trực tiếp của KH002 hôm nay (Gói 5 giờ = 70000đ)
    ('HD00004', '2026-06-20 10:00:00', 0.00, 70000.00, 0.00, N'Đã thanh toán', 'NapGio', NULL, 'nv1', NULL, 'CC062001', N'Tiền mặt'),
    -- Hóa đơn tạm tính hiện tại chưa thanh toán (Bàn B101/P00002)
    ('HD00002', NULL, 0.00, 35000.00, 0.00, N'Chưa thanh toán', 'SuDungBan', 'P00002', 'nv1', NULL, 'CC062002', NULL);

-- Bảng ChiTietHoaDon
-- Dùng IDENTITY_INSERT ON để chèn đúng mã ID phục vụ cho việc khớp bảng tiêu hao nguyên liệu.
SET IDENTITY_INSERT dbo.ChiTietHoaDon ON;

INSERT INTO dbo.ChiTietHoaDon (MaCTHD, MaHD, MaMon, SoLuong, DonGia, GhiChu, ThoiGianTao, TrangThaiMon)
VALUES
    -- HD00001: 1 Cà phê sữa (29k), 1 Bánh sừng bò (22k) -> Tổng 51k
    (1, 'HD00001', 'M002', 1, 29000.00, N'Ít đá', '2026-06-20 08:05:00', 'DaGiao'),
    (2, 'HD00001', 'M103', 1, 22000.00, NULL, '2026-06-20 08:10:00', 'DaGiao'),
    -- HD00003: 2 Bạc xỉu (64k), 1 Trà đào cam sả (35k), 1 Hướng dương (15k) -> Tổng 114k
    (3, 'HD00003', 'M003', 2, 32000.00, N'Nhiều sữa', '2026-06-19 19:05:00', 'DaGiao'),
    (4, 'HD00003', 'M004', 1, 35000.00, NULL, '2026-06-19 19:10:00', 'DaGiao'),
    (5, 'HD00003', 'M101', 1, 15000.00, NULL, '2026-06-19 19:30:00', 'DaGiao'),
    -- HD00004: Gói nạp 5 giờ chơi
    (6, 'HD00004', 'M202', 1, 70000.00, NULL, '2026-06-20 10:00:00', 'DaGiao'),
    -- HD00002: Bánh Tiramisu (35k) (Đang pha chế/chuẩn bị)
    (7, 'HD00002', 'M104', 1, 35000.00, NULL, '2026-06-20 16:35:00', 'ChoPhaChe');

SET IDENTITY_INSERT dbo.ChiTietHoaDon OFF;
PRINT N'13. Tạo Hóa đơn và Chi tiết hóa đơn thành công.';

/* =========================================================
   14. TIÊU HAO NGUYÊN LIỆU (CHI TIẾT PHA CHẾ HÓA ĐƠN)
   ========================================================= */
-- Khai báo chi tiết nguyên liệu thực tế đã tiêu hao cho các hóa đơn trong quá khứ để khớp báo cáo
INSERT INTO dbo.TieuHaoNguyenLieu (MaCTHD, MaNL, SoLuongTieuHao)
VALUES
    -- Cà phê sữa đá (MaCTHD = 1) -> 20g cà phê, 30g sữa đặc
    (1, 'NL01', 20.0000),
    (1, 'NL02', 30.0000),
    -- Bạc xỉu x2 (MaCTHD = 3) -> 20g cà phê, 80g sữa đặc, 160ml sữa tươi
    (3, 'NL01', 20.0000),
    (3, 'NL02', 80.0000),
    (3, 'NL04', 160.0000),
    -- Trà đào cam sả (MaCTHD = 4) -> 10g trà, 3 miếng đào, 30ml đường nước
    (4, 'NL03', 10.0000),
    (4, 'NL05', 3.0000),
    (4, 'NL06', 30.0000);
PRINT N'14. Tạo tiêu hao nguyên liệu chi tiết thành công.';

/* =========================================================
   15. LỊCH SỬ NẠP GIỜ CHƠI (HỒ SƠ KHÁCH HÀNG)
   ========================================================= */
INSERT INTO dbo.LichSuNapGio (MaNapGio, SoGioNap, SoGioConLai, NgayNap, NgayHetHan, MaKH, MaMon, MaHD)
VALUES
    -- Khách KH002 mua gói 5 giờ bằng hóa đơn HD00004
    ('NG00001', 5.00, 5.00, '2026-06-20 10:00:00', '2026-08-20 10:00:00', 'KH002', 'M202', 'HD00004');
PRINT N'15. Tạo lịch sử nạp giờ thành công.';

/* =========================================================
   16. NHẬP KHO NGUYÊN LIỆU (WAREHOUSE SHIPMENTS)
   ========================================================= */
-- Bảng PhieuNhapKho
INSERT INTO dbo.PhieuNhapKho (MaPhieuNK, NgayNhap, NhaCungCap, TrangThai, LyDoHuy, GhiChu, MaNV)
VALUES
    ('NK061501', '2026-06-15 10:00:00', N'Nhà cung cấp cà phê Trung Nguyên', N'Hoạt động', NULL, N'Nhập nguyên liệu hạt đầu tháng', 'ql'),
    ('NK061801', '2026-06-18 15:30:00', N'Tổng kho Mega Market', N'Hoạt động', NULL, N'Nhập sữa và các nguyên liệu phụ', 'ql');

-- Bảng ChiTietPhieuNhapKho
INSERT INTO dbo.ChiTietPhieuNhapKho (MaPhieuNK, MaNL, SoLuongNhap, DonGiaNhap)
VALUES
    -- Nhập cafe Robusta
    ('NK061501', 'NL01', 20000.0000, 150.00),
    -- Nhập sữa đặc và sữa tươi
    ('NK061801', 'NL02', 10000.0000, 60.00),
    ('NK061801', 'NL04', 15000.0000, 30.00);
PRINT N'16. Tạo Phiếu nhập kho nguyên liệu thành công.';

/* =========================================================
   17. PHIẾU KIỂM KÊ KHO VÀ ĐỐI CHIẾU
   ========================================================= */
-- Bảng PhieuKiemKe
INSERT INTO dbo.PhieuKiemKe (MaPhieuKK, NgayKiemKe, GhiChu, TrangThai, ThoiGianGuiDuyet, LyDoTuChoi, MaNV, MaNVDuyet, ThoiGianDuyet)
VALUES
    -- Đã duyệt: kiểm kê cuối ngày 19/06 → ql phê duyệt ngay trong đêm
    ('KK061901', '2026-06-19 23:00:00', N'Kiểm kê hao hụt cuối ngày', 'DaDuyet', '2026-06-19 23:15:00', NULL, 'nv2', 'ql', '2026-06-19 23:30:00'),
    -- Chờ duyệt: nv2 kiểm kê cuối ca tối 21/06, đã gửi duyệt nhưng ql chưa xử lý
    ('KK062101', '2026-06-21 22:45:00', N'Kiểm kê cuối ca chiều – phát hiện lệch trà đen và trân châu', 'ChoDuyet', '2026-06-21 23:00:00', NULL, 'nv2', NULL, NULL);

-- Bảng ChiTietPhieuKiemKe
INSERT INTO dbo.ChiTietPhieuKiemKe (MaPhieuKK, MaNL, SoLuongSoSach, SoLuongThucTe, LyDo)
VALUES
    -- KK061901 (Đã duyệt): hao hụt 20g hạt cà phê, sữa đặc khớp
    ('KK061901', 'NL01', 15020.0000, 15000.0000, N'Hao hụt tự nhiên do xay thử máy pha'),
    ('KK061901', 'NL02', 5000.0000, 5000.0000, NULL),
    -- KK062101 (Chờ duyệt): trà đen thiếu 30g, trân châu thiếu 150g, sữa tươi khớp
    ('KK062101', 'NL03', 2000.0000, 1970.0000, N'Đổ vỡ bình đong trong ca chiều'),
    ('KK062101', 'NL04', 10000.0000, 10000.0000, NULL),
    ('KK062101', 'NL07', 3000.0000, 2850.0000, N'Trân châu nấu dư bị hỏng do để quá giờ');
PRINT N'17. Tạo Phiếu kiểm kê kho thành công.';

/* =========================================================
   18. KÍCH HOẠT LẠI TRIGGERS SAU KHI NẠP DỮ LIỆU XONG
   ========================================================= */
ENABLE TRIGGER dbo.trg_DatPhong_KhongTrungLich ON dbo.DatPhong;
ENABLE TRIGGER dbo.trg_DangKyCa_KhongTrungNgay ON dbo.DangKyCa;
ENABLE TRIGGER dbo.trg_ChiTietHoaDon_CapNhatTienMonVaKho ON dbo.ChiTietHoaDon;
ENABLE TRIGGER dbo.trg_ChiTietPhieuNhapKho_CapNhatTon ON dbo.ChiTietPhieuNhapKho;
ENABLE TRIGGER dbo.trg_PhieuNhapKho_HuyVaKhoiPhuc ON dbo.PhieuNhapKho;
ENABLE TRIGGER dbo.trg_PhieuKiemKe_DuyetDieuChinhTon ON dbo.PhieuKiemKe;
ENABLE TRIGGER dbo.trg_ChiTietPhieuKiemKe_KhoaSauGui ON dbo.ChiTietPhieuKiemKe;
PRINT N'18. Bật lại triggers thành công.';

PRINT N'====== THÊM DỮ LIỆU MẪU HOÀN TẤT ======';
GO
