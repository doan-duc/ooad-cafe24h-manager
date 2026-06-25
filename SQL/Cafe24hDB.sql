/*
    CAFE24H MANAGEMENT DATABASE

    Requirements source:
    - Chapter 1: Requirement collection and classification
    - Chapter 2: Use cases, class diagram, and ERD
    - Chapter 3: Architecture, relational model, and data dictionary

    Initialization rules:
    - Preserve the 25 normalized business tables.
    - Do not insert sample business data.
    - Initialize NhanVien without records.
    - Insert only required configuration data.

    Run Reset_Cafe24hDB.sql before recreating an existing database.
*/

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE master;
GO

IF DB_ID(N'Cafe24hDB') IS NULL
BEGIN
    CREATE DATABASE Cafe24hDB;
END;
GO

USE Cafe24hDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.VaiTro', N'U') IS NOT NULL
BEGIN
    RAISERROR(
        N'Cafe24hDB đã có cấu trúc. Hãy chạy Reset_Cafe24hDB.sql nếu muốn tạo lại.',
        16,
        1
    );
    SET NOEXEC ON;
END;
GO

/* =========================================================
   1. INDEPENDENT TABLES
   ========================================================= */

CREATE TABLE dbo.VaiTro
(
    MaVaiTro VARCHAR(10) NOT NULL,
    TenVaiTro NVARCHAR(50) NOT NULL,
    MoTa NVARCHAR(255) NULL,
    CONSTRAINT PK_VaiTro PRIMARY KEY (MaVaiTro),
    CONSTRAINT UQ_VaiTro_TenVaiTro UNIQUE (TenVaiTro)
);
GO

CREATE TABLE dbo.KhachHang
(
    MaKH VARCHAR(10) NOT NULL,
    HoTen NVARCHAR(100) NOT NULL,
    SoDienThoai VARCHAR(15) NULL,
    Email VARCHAR(100) NULL,
    NgaySinh DATE NULL,
    HangThanhVien NVARCHAR(20) NOT NULL
        CONSTRAINT DF_KhachHang_HangThanhVien DEFAULT N'Đồng',
    DiemTichLuy INT NOT NULL
        CONSTRAINT DF_KhachHang_DiemTichLuy DEFAULT 0,
    SoDuGio DECIMAL(10, 2) NOT NULL
        CONSTRAINT DF_KhachHang_SoDuGio DEFAULT 0,
    LaThanhVien BIT NOT NULL
        CONSTRAINT DF_KhachHang_LaThanhVien DEFAULT 1,
    CONSTRAINT PK_KhachHang PRIMARY KEY (MaKH),
    CONSTRAINT CK_KhachHang_DiemTichLuy CHECK (DiemTichLuy >= 0),
    CONSTRAINT CK_KhachHang_SoDuGio CHECK (SoDuGio >= 0),
    CONSTRAINT CK_KhachHang_HangThanhVien
        CHECK (HangThanhVien IN (N'Đồng', N'Bạc', N'Vàng'))
);
GO

CREATE UNIQUE INDEX UX_KhachHang_SoDienThoai
ON dbo.KhachHang (SoDienThoai)
WHERE SoDienThoai IS NOT NULL;
GO

CREATE UNIQUE INDEX UX_KhachHang_Email
ON dbo.KhachHang (Email)
WHERE Email IS NOT NULL;
GO

CREATE TABLE dbo.KhuVuc
(
    MaKhuVuc VARCHAR(10) NOT NULL,
    TenKhuVuc NVARCHAR(50) NOT NULL,
    MoTa NVARCHAR(255) NULL,
    CONSTRAINT PK_KhuVuc PRIMARY KEY (MaKhuVuc),
    CONSTRAINT UQ_KhuVuc_TenKhuVuc UNIQUE (TenKhuVuc)
);
GO

CREATE TABLE dbo.DanhMuc
(
    MaDanhMuc VARCHAR(10) NOT NULL,
    TenDanhMuc NVARCHAR(50) NOT NULL,
    MoTa NVARCHAR(255) NULL,
    CONSTRAINT PK_DanhMuc PRIMARY KEY (MaDanhMuc),
    CONSTRAINT UQ_DanhMuc_TenDanhMuc UNIQUE (TenDanhMuc)
);
GO

CREATE TABLE dbo.NguyenLieu
(
    MaNL VARCHAR(10) NOT NULL,
    TenNL NVARCHAR(100) NOT NULL,
    DonViTinh NVARCHAR(20) NOT NULL,
    SoLuongTon DECIMAL(18, 4) NOT NULL
        CONSTRAINT DF_NguyenLieu_SoLuongTon DEFAULT 0,
    MucCanhBao DECIMAL(18, 4) NOT NULL
        CONSTRAINT DF_NguyenLieu_MucCanhBao DEFAULT 0,
    CONSTRAINT PK_NguyenLieu PRIMARY KEY (MaNL),
    CONSTRAINT UQ_NguyenLieu_TenNL UNIQUE (TenNL),
    CONSTRAINT CK_NguyenLieu_SoLuongTon CHECK (SoLuongTon >= 0),
    CONSTRAINT CK_NguyenLieu_MucCanhBao CHECK (MucCanhBao >= 0)
);
GO

CREATE TABLE dbo.CaLamViec
(
    MaCa VARCHAR(10) NOT NULL,
    TenCa NVARCHAR(50) NOT NULL,
    GioBatDau TIME(0) NOT NULL,
    GioKetThuc TIME(0) NOT NULL,
    SucChua INT NOT NULL
        CONSTRAINT DF_CaLamViec_SucChua DEFAULT 5,
    CONSTRAINT PK_CaLamViec PRIMARY KEY (MaCa),
    CONSTRAINT UQ_CaLamViec_TenCa UNIQUE (TenCa),
    CONSTRAINT CK_CaLamViec_Gio CHECK (GioBatDau <> GioKetThuc),
    CONSTRAINT CK_CaLamViec_SucChua CHECK (SucChua > 0)
);
GO


CREATE TABLE dbo.Voucher
(
    MaVoucher VARCHAR(15) NOT NULL,
    TenVoucher NVARCHAR(100) NOT NULL,
    LoaiGiam NVARCHAR(30) NOT NULL,
    GiaTriGiam DECIMAL(18, 2) NOT NULL,
    NgayBatDau DATETIME2(0) NOT NULL,
    NgayKetThuc DATETIME2(0) NOT NULL,
    TrangThai NVARCHAR(30) NOT NULL
        CONSTRAINT DF_Voucher_TrangThai DEFAULT N'Hoạt động',
    CONSTRAINT PK_Voucher PRIMARY KEY (MaVoucher),
    CONSTRAINT CK_Voucher_LoaiGiam
        CHECK (LoaiGiam IN (N'Tiền mặt', N'Phần trăm')),
    CONSTRAINT CK_Voucher_GiaTriGiam CHECK
    (
        GiaTriGiam >= 0
        AND (LoaiGiam <> N'Phần trăm' OR GiaTriGiam <= 100)
    ),
    CONSTRAINT CK_Voucher_ThoiGian CHECK (NgayKetThuc > NgayBatDau),
    CONSTRAINT CK_Voucher_TrangThai CHECK
        (TrangThai IN (N'Hoạt động', N'Tạm khóa', N'Hết hạn'))
);
GO

/* =========================================================
   2. TABLES WITH FOREIGN KEYS
   ========================================================= */

CREATE TABLE dbo.NhanVien
(
    MaNV VARCHAR(10) NOT NULL,
    HoTen NVARCHAR(100) NOT NULL,
    SoDienThoai VARCHAR(15) NOT NULL,
    Email VARCHAR(100) NULL,
    MatKhau VARCHAR(255) NOT NULL,
    TrangThai VARCHAR(20) NOT NULL
        CONSTRAINT DF_NhanVien_TrangThai DEFAULT 'Active',
    MaVaiTro VARCHAR(10) NOT NULL,
    CONSTRAINT PK_NhanVien PRIMARY KEY (MaNV),
    CONSTRAINT UQ_NhanVien_SoDienThoai UNIQUE (SoDienThoai),
    CONSTRAINT FK_NhanVien_VaiTro
        FOREIGN KEY (MaVaiTro) REFERENCES dbo.VaiTro (MaVaiTro),
    CONSTRAINT CK_NhanVien_TrangThai
        CHECK (TrangThai IN ('Active', 'Inactive'))
);
GO

CREATE UNIQUE INDEX UX_NhanVien_Email
ON dbo.NhanVien (Email)
WHERE Email IS NOT NULL;
GO

CREATE TABLE dbo.DangKyCa
(
    MaDangKy VARCHAR(15) NOT NULL,
    MaNV VARCHAR(10) NOT NULL,
    MaCa VARCHAR(10) NOT NULL,
    NgayLam DATE NOT NULL,
    TrangThai VARCHAR(20) NOT NULL
        CONSTRAINT DF_DangKyCa_TrangThai DEFAULT 'ChoDuyet',
    MaNVDuyet VARCHAR(10) NULL,
    ThoiGianDangKy DATETIME2(0) NOT NULL
        CONSTRAINT DF_DangKyCa_ThoiGianDangKy DEFAULT SYSDATETIME(),
    ThoiGianDuyet DATETIME2(0) NULL,
    GhiChu NVARCHAR(255) NULL,
    CONSTRAINT PK_DangKyCa PRIMARY KEY (MaDangKy),
    CONSTRAINT FK_DangKyCa_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT FK_DangKyCa_CaLamViec
        FOREIGN KEY (MaCa) REFERENCES dbo.CaLamViec (MaCa),
    CONSTRAINT FK_DangKyCa_NhanVienDuyet
        FOREIGN KEY (MaNVDuyet) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT CK_DangKyCa_TrangThai
        CHECK (TrangThai IN ('ChoDuyet', 'DaDuyet', 'TuChoi', 'DaHuy')),
    CONSTRAINT CK_DangKyCa_NguoiDuyet
        CHECK (MaNVDuyet IS NULL OR MaNVDuyet <> MaNV),
    CONSTRAINT CK_DangKyCa_Duyet
        CHECK
        (
            (TrangThai IN ('ChoDuyet', 'DaHuy')
                AND MaNVDuyet IS NULL AND ThoiGianDuyet IS NULL)
            OR
            (TrangThai IN ('DaDuyet', 'TuChoi')
                AND MaNVDuyet IS NOT NULL AND ThoiGianDuyet IS NOT NULL)
        )
);
GO

CREATE TABLE dbo.Ban
(
    MaBan VARCHAR(10) NOT NULL,
    TenBan NVARCHAR(50) NOT NULL,
    LoaiViTri NVARCHAR(50) NOT NULL,
    SucChua INT NOT NULL,
    TrangThai NVARCHAR(50) NOT NULL
        CONSTRAINT DF_Ban_TrangThai DEFAULT N'Trống',
    MaKhuVuc VARCHAR(10) NOT NULL,
    CONSTRAINT PK_Ban PRIMARY KEY (MaBan),
    CONSTRAINT UQ_Ban_TenBan UNIQUE (TenBan),
    CONSTRAINT FK_Ban_KhuVuc
        FOREIGN KEY (MaKhuVuc) REFERENCES dbo.KhuVuc (MaKhuVuc),
    CONSTRAINT CK_Ban_SucChua CHECK (SucChua > 0),
    CONSTRAINT CK_Ban_TrangThai CHECK
    (
        TrangThai IN (N'Trống', N'Đang phục vụ', N'Đã đặt', N'Cần dọn')
    )
);
GO

CREATE TABLE dbo.Mon
(
    MaMon VARCHAR(10) NOT NULL,
    TenMon NVARCHAR(100) NOT NULL,
    DonGia DECIMAL(18, 2) NOT NULL,
    HinhAnh VARCHAR(255) NULL,
    TrangThai NVARCHAR(50) NOT NULL
        CONSTRAINT DF_Mon_TrangThai DEFAULT N'Đang bán',
    MaDanhMuc VARCHAR(10) NOT NULL,
    LoaiMon NVARCHAR(50) NOT NULL,
    SoGioQuyDoi DECIMAL(10, 2) NULL,
    HanSuDung INT NULL,
    CONSTRAINT PK_Mon PRIMARY KEY (MaMon),
    CONSTRAINT FK_Mon_DanhMuc
        FOREIGN KEY (MaDanhMuc) REFERENCES dbo.DanhMuc (MaDanhMuc),
    CONSTRAINT CK_Mon_DonGia CHECK (DonGia >= 0),
    CONSTRAINT CK_Mon_TrangThai
        CHECK (TrangThai IN (N'Đang bán', N'Ngừng bán')),
    CONSTRAINT CK_Mon_LoaiMon
        CHECK (LoaiMon IN (N'Đồ ăn', N'Đồ uống', N'Gói giờ')),
    CONSTRAINT CK_Mon_GoiGio CHECK
    (
        (LoaiMon = N'Gói giờ' AND SoGioQuyDoi > 0 AND HanSuDung > 0)
        OR
        (LoaiMon <> N'Gói giờ' AND SoGioQuyDoi IS NULL AND HanSuDung IS NULL)
    )
);
GO

CREATE TABLE dbo.DinhMuc
(
    MaMon VARCHAR(10) NOT NULL,
    MaNL VARCHAR(10) NOT NULL,
    SoLuongTieuHao DECIMAL(18, 4) NOT NULL,
    CONSTRAINT PK_DinhMuc PRIMARY KEY (MaMon, MaNL),
    CONSTRAINT FK_DinhMuc_Mon
        FOREIGN KEY (MaMon) REFERENCES dbo.Mon (MaMon),
    CONSTRAINT FK_DinhMuc_NguyenLieu
        FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu (MaNL),
    CONSTRAINT CK_DinhMuc_SoLuongTieuHao CHECK (SoLuongTieuHao > 0)
);
GO

CREATE TABLE dbo.DatPhong
(
    MaDatPhong VARCHAR(15) NOT NULL,
    MaKH VARCHAR(10) NOT NULL,
    MaBan VARCHAR(10) NOT NULL,
    ThoiGianBatDau DATETIME2(0) NOT NULL,
    ThoiGianKetThuc DATETIME2(0) NOT NULL,
    TrangThai NVARCHAR(50) NOT NULL
        CONSTRAINT DF_DatPhong_TrangThai DEFAULT N'Đã đặt',
    CONSTRAINT PK_DatPhong PRIMARY KEY (MaDatPhong),
    CONSTRAINT FK_DatPhong_KhachHang
        FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang (MaKH),
    CONSTRAINT FK_DatPhong_Ban
        FOREIGN KEY (MaBan) REFERENCES dbo.Ban (MaBan),
    CONSTRAINT CK_DatPhong_ThoiGian
        CHECK (ThoiGianKetThuc > ThoiGianBatDau),
    CONSTRAINT CK_DatPhong_TrangThai
        CHECK (TrangThai IN (N'Đã đặt', N'Đã nhận', N'Đã hủy'))
);
GO

CREATE INDEX IX_DatPhong_Ban_ThoiGian
ON dbo.DatPhong (MaBan, ThoiGianBatDau, ThoiGianKetThuc, TrangThai);
GO

CREATE TABLE dbo.PhienSuDung
(
    MaPhien VARCHAR(15) NOT NULL,
    ThoiGianVao DATETIME2(0) NOT NULL,
    ThoiGianRa DATETIME2(0) NULL,
    TongThoiGian DECIMAL(10, 2) NULL,
    TrangThai NVARCHAR(30) NOT NULL
        CONSTRAINT DF_PhienSuDung_TrangThai DEFAULT N'Đang hoạt động',
    MaBan VARCHAR(10) NOT NULL,
    MaKH VARCHAR(10) NULL,
    MaNV VARCHAR(10) NOT NULL,
    MaDatPhong VARCHAR(15) NULL,
    CONSTRAINT PK_PhienSuDung PRIMARY KEY (MaPhien),
    CONSTRAINT FK_PhienSuDung_Ban
        FOREIGN KEY (MaBan) REFERENCES dbo.Ban (MaBan),
    CONSTRAINT FK_PhienSuDung_KhachHang
        FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang (MaKH),
    CONSTRAINT FK_PhienSuDung_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT FK_PhienSuDung_DatPhong
        FOREIGN KEY (MaDatPhong) REFERENCES dbo.DatPhong (MaDatPhong),
    CONSTRAINT CK_PhienSuDung_ThoiGian CHECK
        (ThoiGianRa IS NULL OR ThoiGianRa >= ThoiGianVao),
    CONSTRAINT CK_PhienSuDung_TongThoiGian CHECK
        (TongThoiGian IS NULL OR TongThoiGian >= 0),
    CONSTRAINT CK_PhienSuDung_TrangThai
        CHECK (TrangThai IN (N'Đang hoạt động', N'Đã kết thúc'))
);
GO

CREATE UNIQUE INDEX UX_PhienSuDung_MotPhienDangMoMoiBan
ON dbo.PhienSuDung (MaBan)
WHERE TrangThai = N'Đang hoạt động';
GO

CREATE TABLE dbo.ChotCa
(
    MaChotCa VARCHAR(15) NOT NULL,
    ThoiGianMo DATETIME2(0) NOT NULL
        CONSTRAINT DF_ChotCa_ThoiGianMo DEFAULT SYSDATETIME(),
    TienDauCa DECIMAL(18, 2) NOT NULL
        CONSTRAINT DF_ChotCa_TienDauCa DEFAULT 0,
    TienThucTe DECIMAL(18, 2) NULL,
    TienHeThong DECIMAL(18, 2) NOT NULL
        CONSTRAINT DF_ChotCa_TienHeThong DEFAULT 0,
    ChenhLech AS
    (
        CASE
            WHEN TienThucTe IS NULL THEN NULL
            ELSE TienThucTe - TienHeThong
        END
    ) PERSISTED,
    LyDoChenhLech NVARCHAR(255) NULL,
    ThoiGianChot DATETIME2(0) NULL,
    TrangThaiChot NVARCHAR(30) NOT NULL
        CONSTRAINT DF_ChotCa_TrangThaiChot DEFAULT N'Đang mở',
    MaCa VARCHAR(10) NOT NULL,
    MaNV VARCHAR(10) NOT NULL,
    CONSTRAINT PK_ChotCa PRIMARY KEY (MaChotCa),
    CONSTRAINT FK_ChotCa_CaLamViec
        FOREIGN KEY (MaCa) REFERENCES dbo.CaLamViec (MaCa),
    CONSTRAINT FK_ChotCa_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT CK_ChotCa_TienDauCa CHECK (TienDauCa >= 0),
    CONSTRAINT CK_ChotCa_TienThucTe
        CHECK (TienThucTe IS NULL OR TienThucTe >= 0),
    CONSTRAINT CK_ChotCa_TienHeThong CHECK (TienHeThong >= 0),
    CONSTRAINT CK_ChotCa_TrangThai
        CHECK (TrangThaiChot IN (N'Đang mở', N'Đã chốt')),
    CONSTRAINT CK_ChotCa_LyDo CHECK
    (
        TienThucTe IS NULL
        OR TienThucTe = TienHeThong
        OR NULLIF(LTRIM(RTRIM(LyDoChenhLech)), N'') IS NOT NULL
    )
);
GO

CREATE UNIQUE INDEX UX_ChotCa_MotCaMoMoiNhanVien
ON dbo.ChotCa (MaNV)
WHERE TrangThaiChot = N'Đang mở';
GO

CREATE TABLE dbo.HoaDon
(
    MaHD VARCHAR(15) NOT NULL,
    NgayLap DATETIME2(0) NULL,
    TienGio DECIMAL(18, 2) NOT NULL
        CONSTRAINT DF_HoaDon_TienGio DEFAULT 0,
    TienMon DECIMAL(18, 2) NOT NULL
        CONSTRAINT DF_HoaDon_TienMon DEFAULT 0,
    TienGiam DECIMAL(18, 2) NOT NULL
        CONSTRAINT DF_HoaDon_TienGiam DEFAULT 0,
    TongTien AS
    (
        CONVERT
        (
            DECIMAL(18, 2),
            CASE
                WHEN TienGio + TienMon - TienGiam < 0 THEN 0
                ELSE TienGio + TienMon - TienGiam
            END
        )
    ) PERSISTED,
    TrangThai NVARCHAR(30) NOT NULL
        CONSTRAINT DF_HoaDon_TrangThai DEFAULT N'Chưa thanh toán',
    LoaiHoaDon VARCHAR(30) NOT NULL,
    MaPhien VARCHAR(15) NULL,
    MaNV VARCHAR(10) NOT NULL,
    MaVoucher VARCHAR(15) NULL,
    MaChotCa VARCHAR(15) NULL,
    PhuongThucThanhToan NVARCHAR(50) NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY (MaHD),
    CONSTRAINT FK_HoaDon_PhienSuDung
        FOREIGN KEY (MaPhien) REFERENCES dbo.PhienSuDung (MaPhien),
    CONSTRAINT FK_HoaDon_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT FK_HoaDon_Voucher
        FOREIGN KEY (MaVoucher) REFERENCES dbo.Voucher (MaVoucher),
    CONSTRAINT FK_HoaDon_ChotCa
        FOREIGN KEY (MaChotCa) REFERENCES dbo.ChotCa (MaChotCa),
    CONSTRAINT CK_HoaDon_TienGio CHECK (TienGio >= 0),
    CONSTRAINT CK_HoaDon_TienMon CHECK (TienMon >= 0),
    CONSTRAINT CK_HoaDon_TienGiam CHECK (TienGiam >= 0),
    CONSTRAINT CK_HoaDon_TrangThai
        CHECK (TrangThai IN (N'Chưa thanh toán', N'Đã thanh toán', N'Đã hủy')),
    CONSTRAINT CK_HoaDon_Loai
        CHECK (LoaiHoaDon IN ('SuDungBan', 'NapGio', 'BanHangLe'))
);
GO

CREATE UNIQUE INDEX UX_HoaDon_MotHoaDonMoiPhien
ON dbo.HoaDon (MaPhien)
WHERE MaPhien IS NOT NULL;
GO

CREATE INDEX IX_HoaDon_NgayLap_TrangThai
ON dbo.HoaDon (NgayLap, TrangThai)
INCLUDE (TienGio, TienMon, TienGiam, TongTien);
GO


CREATE TABLE dbo.ChiTietHoaDon
(
    MaCTHD INT IDENTITY(1, 1) NOT NULL,
    MaHD VARCHAR(15) NOT NULL,
    MaMon VARCHAR(10) NOT NULL,
    SoLuong INT NOT NULL,
    DonGia DECIMAL(18, 2) NOT NULL,
    GhiChu NVARCHAR(255) NULL,
    ThoiGianTao DATETIME2(0) NOT NULL
        CONSTRAINT DF_ChiTietHoaDon_ThoiGianTao DEFAULT SYSDATETIME(),
    TrangThaiMon VARCHAR(50) NOT NULL
        CONSTRAINT DF_ChiTietHoaDon_TrangThai DEFAULT 'ChoPhaChe',
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (MaCTHD),
    CONSTRAINT FK_ChiTietHoaDon_HoaDon
        FOREIGN KEY (MaHD) REFERENCES dbo.HoaDon (MaHD),
    CONSTRAINT FK_ChiTietHoaDon_Mon
        FOREIGN KEY (MaMon) REFERENCES dbo.Mon (MaMon),
    CONSTRAINT CK_ChiTietHoaDon_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietHoaDon_DonGia CHECK (DonGia >= 0),
    CONSTRAINT CK_ChiTietHoaDon_TrangThai CHECK
    (
        TrangThaiMon IN ('ChoPhaChe', 'DangPha', 'DaPha', 'DaGiao', 'DaHuy')
    )
);
GO

CREATE INDEX IX_ChiTietHoaDon_TrangThai
ON dbo.ChiTietHoaDon (TrangThaiMon, MaHD);
GO

CREATE INDEX IX_ChiTietHoaDon_MaHD_TrangThai
ON dbo.ChiTietHoaDon (MaHD, TrangThaiMon)
INCLUDE (SoLuong, DonGia);
GO

CREATE TABLE dbo.TieuHaoNguyenLieu
(
    MaCTHD INT NOT NULL,
    MaNL VARCHAR(10) NOT NULL,
    SoLuongTieuHao DECIMAL(18, 4) NOT NULL,
    CONSTRAINT PK_TieuHaoNguyenLieu PRIMARY KEY (MaCTHD, MaNL),
    CONSTRAINT FK_TieuHaoNguyenLieu_ChiTietHoaDon
        FOREIGN KEY (MaCTHD) REFERENCES dbo.ChiTietHoaDon (MaCTHD)
        ON DELETE CASCADE,
    CONSTRAINT FK_TieuHaoNguyenLieu_NguyenLieu
        FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu (MaNL),
    CONSTRAINT CK_TieuHaoNguyenLieu_SoLuong CHECK (SoLuongTieuHao > 0)
);
GO

CREATE TABLE dbo.PhieuNhapKho
(
    MaPhieuNK VARCHAR(15) NOT NULL,
    NgayNhap DATETIME2(0) NOT NULL
        CONSTRAINT DF_PhieuNhapKho_NgayNhap DEFAULT SYSDATETIME(),
    NhaCungCap NVARCHAR(150) NOT NULL,
    TrangThai NVARCHAR(50) NOT NULL
        CONSTRAINT DF_PhieuNhapKho_TrangThai DEFAULT N'Hoạt động',
    LyDoHuy NVARCHAR(255) NULL,
    GhiChu NVARCHAR(255) NULL,
    MaNV VARCHAR(10) NOT NULL,
    CONSTRAINT PK_PhieuNhapKho PRIMARY KEY (MaPhieuNK),
    CONSTRAINT FK_PhieuNhapKho_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT CK_PhieuNhapKho_TrangThai
        CHECK (TrangThai IN (N'Hoạt động', N'Đã hủy')),
    CONSTRAINT CK_PhieuNhapKho_LyDoHuy CHECK
    (
        TrangThai <> N'Đã hủy'
        OR NULLIF(LTRIM(RTRIM(LyDoHuy)), N'') IS NOT NULL
    )
);
GO

CREATE TABLE dbo.ChiTietPhieuNhapKho
(
    MaPhieuNK VARCHAR(15) NOT NULL,
    MaNL VARCHAR(10) NOT NULL,
    SoLuongNhap DECIMAL(18, 4) NOT NULL,
    DonGiaNhap DECIMAL(18, 2) NOT NULL,
    CONSTRAINT PK_ChiTietPhieuNhapKho PRIMARY KEY (MaPhieuNK, MaNL),
    CONSTRAINT FK_ChiTietPhieuNhapKho_PhieuNhapKho
        FOREIGN KEY (MaPhieuNK) REFERENCES dbo.PhieuNhapKho (MaPhieuNK),
    CONSTRAINT FK_ChiTietPhieuNhapKho_NguyenLieu
        FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu (MaNL),
    CONSTRAINT CK_ChiTietPhieuNhapKho_SoLuong CHECK (SoLuongNhap > 0),
    CONSTRAINT CK_ChiTietPhieuNhapKho_DonGia CHECK (DonGiaNhap >= 0)
);
GO

CREATE TABLE dbo.PhieuKiemKe
(
    MaPhieuKK VARCHAR(15) NOT NULL,
    NgayKiemKe DATETIME2(0) NOT NULL
        CONSTRAINT DF_PhieuKiemKe_NgayKiemKe DEFAULT SYSDATETIME(),
    GhiChu NVARCHAR(255) NULL,
    TrangThai VARCHAR(20) NOT NULL
        CONSTRAINT DF_PhieuKiemKe_TrangThai DEFAULT 'Nhap',
    ThoiGianGuiDuyet DATETIME2(0) NULL,
    LyDoTuChoi NVARCHAR(255) NULL,
    MaNV VARCHAR(10) NOT NULL,
    MaNVDuyet VARCHAR(10) NULL,
    ThoiGianDuyet DATETIME2(0) NULL,
    CONSTRAINT PK_PhieuKiemKe PRIMARY KEY (MaPhieuKK),
    CONSTRAINT FK_PhieuKiemKe_NhanVien
        FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT FK_PhieuKiemKe_NhanVienDuyet
        FOREIGN KEY (MaNVDuyet) REFERENCES dbo.NhanVien (MaNV),
    CONSTRAINT CK_PhieuKiemKe_NguoiDuyet
        CHECK (MaNVDuyet IS NULL OR MaNVDuyet <> MaNV),
    CONSTRAINT CK_PhieuKiemKe_TrangThai
        CHECK (TrangThai IN ('Nhap', 'ChoDuyet', 'DaDuyet', 'TuChoi', 'DaHuy')),
    CONSTRAINT CK_PhieuKiemKe_Duyet
        CHECK
        (
            (TrangThai IN ('Nhap', 'ChoDuyet', 'DaHuy')
                AND MaNVDuyet IS NULL AND ThoiGianDuyet IS NULL)
            OR
            (TrangThai IN ('DaDuyet', 'TuChoi')
                AND MaNVDuyet IS NOT NULL AND ThoiGianDuyet IS NOT NULL)
        ),
    CONSTRAINT CK_PhieuKiemKe_LyDoTuChoi CHECK
    (
        TrangThai <> 'TuChoi'
        OR NULLIF(LTRIM(RTRIM(LyDoTuChoi)), N'') IS NOT NULL
    )
);
GO

CREATE TABLE dbo.ChiTietPhieuKiemKe
(
    MaPhieuKK VARCHAR(15) NOT NULL,
    MaNL VARCHAR(10) NOT NULL,
    SoLuongSoSach DECIMAL(18, 4) NOT NULL,
    SoLuongThucTe DECIMAL(18, 4) NOT NULL,
    ChenhLech AS
        (CONVERT(DECIMAL(18, 4), SoLuongThucTe - SoLuongSoSach)) PERSISTED,
    LyDo NVARCHAR(255) NULL,
    CONSTRAINT PK_ChiTietPhieuKiemKe PRIMARY KEY (MaPhieuKK, MaNL),
    CONSTRAINT FK_ChiTietPhieuKiemKe_PhieuKiemKe
        FOREIGN KEY (MaPhieuKK) REFERENCES dbo.PhieuKiemKe (MaPhieuKK),
    CONSTRAINT FK_ChiTietPhieuKiemKe_NguyenLieu
        FOREIGN KEY (MaNL) REFERENCES dbo.NguyenLieu (MaNL),
    CONSTRAINT CK_ChiTietPhieuKiemKe_SoSach CHECK (SoLuongSoSach >= 0),
    CONSTRAINT CK_ChiTietPhieuKiemKe_ThucTe CHECK (SoLuongThucTe >= 0),
    CONSTRAINT CK_ChiTietPhieuKiemKe_LyDo CHECK
    (
        SoLuongThucTe = SoLuongSoSach
        OR NULLIF(LTRIM(RTRIM(LyDo)), N'') IS NOT NULL
    )
);
GO

CREATE TABLE dbo.LichSuNapGio
(
    MaNapGio VARCHAR(15) NOT NULL,
    SoGioNap DECIMAL(5, 2) NOT NULL,
    SoGioConLai DECIMAL(5, 2) NOT NULL,
    NgayNap DATETIME2(0) NOT NULL
        CONSTRAINT DF_LichSuNapGio_NgayNap DEFAULT SYSDATETIME(),
    NgayHetHan DATETIME2(0) NOT NULL,
    MaKH VARCHAR(10) NOT NULL,
    MaMon VARCHAR(10) NOT NULL,
    MaHD VARCHAR(15) NOT NULL,
    CONSTRAINT PK_LichSuNapGio PRIMARY KEY (MaNapGio),
    CONSTRAINT FK_LichSuNapGio_KhachHang
        FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang (MaKH),
    CONSTRAINT FK_LichSuNapGio_Mon
        FOREIGN KEY (MaMon) REFERENCES dbo.Mon (MaMon),
    CONSTRAINT FK_LichSuNapGio_HoaDon
        FOREIGN KEY (MaHD) REFERENCES dbo.HoaDon (MaHD),
    CONSTRAINT UQ_LichSuNapGio_MaHD UNIQUE (MaHD),
    CONSTRAINT CK_LichSuNapGio_SoGioNap CHECK (SoGioNap > 0),
    CONSTRAINT CK_LichSuNapGio_SoGioConLai
        CHECK (SoGioConLai >= 0 AND SoGioConLai <= SoGioNap),
    CONSTRAINT CK_LichSuNapGio_ThoiHan CHECK (NgayHetHan > NgayNap)
);
GO

CREATE INDEX IX_LichSuNapGio_TruGioFIFO
ON dbo.LichSuNapGio (MaKH, NgayHetHan, NgayNap)
INCLUDE (SoGioConLai);
GO

/* =========================================================
   3. BUSINESS RULE TRIGGERS
   ========================================================= */

CREATE TRIGGER dbo.trg_DatPhong_KhongTrungLich
ON dbo.DatPhong
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS
    (
        SELECT 1
        FROM inserted AS i
        INNER JOIN dbo.DatPhong AS d
            ON d.MaBan = i.MaBan
           AND d.MaDatPhong <> i.MaDatPhong
           AND d.TrangThai <> N'Đã hủy'
           AND i.TrangThai <> N'Đã hủy'
           AND i.ThoiGianBatDau < d.ThoiGianKetThuc
           AND i.ThoiGianKetThuc > d.ThoiGianBatDau
    )
    BEGIN
        RAISERROR(N'Bàn đã có booking trùng khoảng thời gian.', 16, 1);
        ROLLBACK TRANSACTION;
    END;
END;
GO

CREATE TRIGGER dbo.trg_DangKyCa_KhongTrungNgay
ON dbo.DangKyCa
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.DangKyCa AS dk
        INNER JOIN
        (
            SELECT DISTINCT MaNV, NgayLam
            FROM inserted
        ) AS i
            ON i.MaNV = dk.MaNV
           AND i.NgayLam = dk.NgayLam
        WHERE dk.TrangThai IN ('ChoDuyet', 'DaDuyet')
        GROUP BY dk.MaNV, dk.NgayLam
        HAVING COUNT(*) > 1
    )
    BEGIN
        RAISERROR(N'Mỗi nhân viên chỉ được có một đăng ký ca hiệu lực trong ngày.', 16, 1);
        ROLLBACK TRANSACTION;
    END;
END;
GO

CREATE TRIGGER dbo.trg_ChiTietHoaDon_CapNhatTienMonVaKho
ON dbo.ChiTietHoaDon
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS
    (
        SELECT 1
        FROM
        (
            SELECT MaHD FROM inserted
            UNION
            SELECT MaHD FROM deleted
        ) AS x
        INNER JOIN dbo.HoaDon AS hd ON hd.MaHD = x.MaHD
        WHERE hd.TrangThai <> N'Chưa thanh toán'
    )
    BEGIN
        RAISERROR(N'Không được thay đổi chi tiết của hóa đơn đã khóa hoặc đã thanh toán.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM deleted AS d
        LEFT JOIN inserted AS i ON i.MaCTHD = d.MaCTHD
        WHERE d.TrangThaiMon IN ('DaPha', 'DaGiao')
          AND
          (
              i.MaCTHD IS NULL
              OR i.MaMon <> d.MaMon
              OR i.SoLuong <> d.SoLuong
          )
    )
    BEGIN
        RAISERROR(N'Không được xóa hoặc đổi món/số lượng sau khi món đã pha chế hoặc đã giao.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    DELETE th
    FROM dbo.TieuHaoNguyenLieu AS th
    INNER JOIN inserted AS i ON i.MaCTHD = th.MaCTHD
    INNER JOIN deleted AS d ON d.MaCTHD = i.MaCTHD
    WHERE d.TrangThaiMon NOT IN ('DaPha', 'DaGiao')
      AND (i.MaMon <> d.MaMon OR i.SoLuong <> d.SoLuong);

    INSERT INTO dbo.TieuHaoNguyenLieu (MaCTHD, MaNL, SoLuongTieuHao)
    SELECT
        i.MaCTHD,
        dm.MaNL,
        CONVERT(DECIMAL(18, 4), i.SoLuong) * dm.SoLuongTieuHao
    FROM inserted AS i
    INNER JOIN dbo.DinhMuc AS dm ON dm.MaMon = i.MaMon
    WHERE NOT EXISTS
    (
        SELECT 1
        FROM dbo.TieuHaoNguyenLieu AS th
        WHERE th.MaCTHD = i.MaCTHD
          AND th.MaNL = dm.MaNL
    );

    DECLARE @BienDongKho TABLE
    (
        MaNL VARCHAR(10) PRIMARY KEY,
        SoLuongCanTru DECIMAL(18, 4) NOT NULL
    );

    ;WITH Moi AS
    (
        SELECT
            th.MaNL,
            SUM(th.SoLuongTieuHao) AS SL
        FROM inserted AS i
        INNER JOIN dbo.TieuHaoNguyenLieu AS th ON th.MaCTHD = i.MaCTHD
        WHERE i.TrangThaiMon IN ('DaPha', 'DaGiao')
        GROUP BY th.MaNL
    ),
    Cu AS
    (
        SELECT
            th.MaNL,
            SUM(th.SoLuongTieuHao) AS SL
        FROM deleted AS d
        INNER JOIN dbo.TieuHaoNguyenLieu AS th ON th.MaCTHD = d.MaCTHD
        WHERE d.TrangThaiMon IN ('DaPha', 'DaGiao')
        GROUP BY th.MaNL
    )
    INSERT INTO @BienDongKho (MaNL, SoLuongCanTru)
    SELECT
        COALESCE(m.MaNL, c.MaNL),
        COALESCE(m.SL, 0) - COALESCE(c.SL, 0)
    FROM Moi AS m
    FULL JOIN Cu AS c ON c.MaNL = m.MaNL
    WHERE COALESCE(m.SL, 0) <> COALESCE(c.SL, 0);

    IF EXISTS
    (
        SELECT 1
        FROM @BienDongKho AS b
        INNER JOIN dbo.NguyenLieu AS nl WITH (UPDLOCK, HOLDLOCK)
            ON nl.MaNL = b.MaNL
        WHERE b.SoLuongCanTru > 0
          AND nl.SoLuongTon < b.SoLuongCanTru
    )
    BEGIN
        RAISERROR(N'Không đủ nguyên liệu tồn kho để pha chế món.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE nl
    SET nl.SoLuongTon = nl.SoLuongTon - b.SoLuongCanTru
    FROM dbo.NguyenLieu AS nl
    INNER JOIN @BienDongKho AS b ON b.MaNL = nl.MaNL;

    DECLARE @HoaDonBiAnhHuong TABLE (MaHD VARCHAR(15) PRIMARY KEY);

    INSERT INTO @HoaDonBiAnhHuong (MaHD)
    SELECT MaHD FROM inserted
    UNION
    SELECT MaHD FROM deleted;

    UPDATE hd
    SET TienMon = COALESCE(t.TienMon, 0)
    FROM dbo.HoaDon AS hd
    INNER JOIN @HoaDonBiAnhHuong AS a ON a.MaHD = hd.MaHD
    OUTER APPLY
    (
        SELECT SUM(CONVERT(DECIMAL(18, 2), ct.SoLuong * ct.DonGia)) AS TienMon
        FROM dbo.ChiTietHoaDon AS ct
        WHERE ct.MaHD = hd.MaHD
          AND ct.TrangThaiMon <> 'DaHuy'
    ) AS t;
END;
GO

CREATE TRIGGER dbo.trg_ChiTietPhieuNhapKho_CapNhatTon
ON dbo.ChiTietPhieuNhapKho
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @BienDong TABLE
    (
        MaNL VARCHAR(10) PRIMARY KEY,
        SoLuongTang DECIMAL(18, 3) NOT NULL
    );

    ;WITH Moi AS
    (
        SELECT
            i.MaNL,
            SUM
            (
                CASE WHEN p.TrangThai = N'Hoạt động' THEN i.SoLuongNhap ELSE 0 END
            ) AS SL
        FROM inserted AS i
        INNER JOIN dbo.PhieuNhapKho AS p ON p.MaPhieuNK = i.MaPhieuNK
        GROUP BY i.MaNL
    ),
    Cu AS
    (
        SELECT
            d.MaNL,
            SUM
            (
                CASE WHEN p.TrangThai = N'Hoạt động' THEN d.SoLuongNhap ELSE 0 END
            ) AS SL
        FROM deleted AS d
        INNER JOIN dbo.PhieuNhapKho AS p ON p.MaPhieuNK = d.MaPhieuNK
        GROUP BY d.MaNL
    )
    INSERT INTO @BienDong (MaNL, SoLuongTang)
    SELECT
        COALESCE(m.MaNL, c.MaNL),
        COALESCE(m.SL, 0) - COALESCE(c.SL, 0)
    FROM Moi AS m
    FULL JOIN Cu AS c ON c.MaNL = m.MaNL
    WHERE COALESCE(m.SL, 0) <> COALESCE(c.SL, 0);

    IF EXISTS
    (
        SELECT 1
        FROM @BienDong AS b
        INNER JOIN dbo.NguyenLieu AS nl WITH (UPDLOCK, HOLDLOCK)
            ON nl.MaNL = b.MaNL
        WHERE b.SoLuongTang < 0
          AND nl.SoLuongTon < -b.SoLuongTang
    )
    BEGIN
        RAISERROR(N'Tồn kho không đủ để hoàn tác chi tiết phiếu nhập.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE nl
    SET nl.SoLuongTon = nl.SoLuongTon + b.SoLuongTang
    FROM dbo.NguyenLieu AS nl
    INNER JOIN @BienDong AS b ON b.MaNL = nl.MaNL;
END;
GO

CREATE TRIGGER dbo.trg_PhieuNhapKho_HuyVaKhoiPhuc
ON dbo.PhieuNhapKho
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @BienDong TABLE
    (
        MaNL VARCHAR(10) PRIMARY KEY,
        SoLuongTang DECIMAL(18, 3) NOT NULL
    );

    INSERT INTO @BienDong (MaNL, SoLuongTang)
    SELECT
        ct.MaNL,
        SUM
        (
            CASE
                WHEN d.TrangThai = N'Hoạt động' AND i.TrangThai = N'Đã hủy'
                    THEN -ct.SoLuongNhap
                WHEN d.TrangThai = N'Đã hủy' AND i.TrangThai = N'Hoạt động'
                    THEN ct.SoLuongNhap
                ELSE 0
            END
        )
    FROM inserted AS i
    INNER JOIN deleted AS d ON d.MaPhieuNK = i.MaPhieuNK
    INNER JOIN dbo.ChiTietPhieuNhapKho AS ct ON ct.MaPhieuNK = i.MaPhieuNK
    WHERE i.TrangThai <> d.TrangThai
    GROUP BY ct.MaNL
    HAVING SUM
    (
        CASE
            WHEN d.TrangThai = N'Hoạt động' AND i.TrangThai = N'Đã hủy'
                THEN -ct.SoLuongNhap
            WHEN d.TrangThai = N'Đã hủy' AND i.TrangThai = N'Hoạt động'
                THEN ct.SoLuongNhap
            ELSE 0
        END
    ) <> 0;

    IF EXISTS
    (
        SELECT 1
        FROM @BienDong AS b
        INNER JOIN dbo.NguyenLieu AS nl WITH (UPDLOCK, HOLDLOCK)
            ON nl.MaNL = b.MaNL
        WHERE b.SoLuongTang < 0
          AND nl.SoLuongTon < -b.SoLuongTang
    )
    BEGIN
        RAISERROR(N'Tồn kho hiện tại không đủ để hủy phiếu nhập.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE nl
    SET nl.SoLuongTon = nl.SoLuongTon + b.SoLuongTang
    FROM dbo.NguyenLieu AS nl
    INNER JOIN @BienDong AS b ON b.MaNL = nl.MaNL;
END;
GO

CREATE TRIGGER dbo.trg_PhieuKiemKe_DuyetDieuChinhTon
ON dbo.PhieuKiemKe
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS
    (
        SELECT 1
        FROM inserted AS i
        INNER JOIN deleted AS d ON d.MaPhieuKK = i.MaPhieuKK
        WHERE i.TrangThai <> d.TrangThai
          AND NOT
          (
              (d.TrangThai = 'Nhap' AND i.TrangThai IN ('ChoDuyet', 'DaHuy'))
              OR
              (d.TrangThai = 'ChoDuyet' AND i.TrangThai IN ('DaDuyet', 'TuChoi', 'DaHuy'))
              OR
              (d.TrangThai = 'TuChoi' AND i.TrangThai IN ('Nhap', 'ChoDuyet', 'DaHuy'))
          )
    )
    BEGIN
        RAISERROR(N'Chuyển trạng thái phiếu kiểm kê không hợp lệ.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM inserted AS i
        INNER JOIN deleted AS d ON d.MaPhieuKK = i.MaPhieuKK
        WHERE d.TrangThai <> 'DaDuyet'
          AND i.TrangThai = 'DaDuyet'
          AND
          (
              NOT EXISTS
              (
                  SELECT 1
                  FROM dbo.ChiTietPhieuKiemKe AS ct
                  WHERE ct.MaPhieuKK = i.MaPhieuKK
              )
              OR i.MaNVDuyet IS NULL
              OR i.ThoiGianDuyet IS NULL
          )
    )
    BEGIN
        RAISERROR(N'Không thể duyệt phiếu kiểm kê thiếu chi tiết hoặc thông tin phê duyệt.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    DECLARE @DieuChinh TABLE
    (
        MaNL VARCHAR(10) PRIMARY KEY,
        ChenhLech DECIMAL(18, 4) NOT NULL
    );

    INSERT INTO @DieuChinh (MaNL, ChenhLech)
    SELECT ct.MaNL, SUM(ct.ChenhLech)
    FROM inserted AS i
    INNER JOIN deleted AS d ON d.MaPhieuKK = i.MaPhieuKK
    INNER JOIN dbo.ChiTietPhieuKiemKe AS ct ON ct.MaPhieuKK = i.MaPhieuKK
    WHERE d.TrangThai <> 'DaDuyet'
      AND i.TrangThai = 'DaDuyet'
    GROUP BY ct.MaNL;

    IF EXISTS
    (
        SELECT 1
        FROM @DieuChinh AS dc
        INNER JOIN dbo.NguyenLieu AS nl WITH (UPDLOCK, HOLDLOCK)
            ON nl.MaNL = dc.MaNL
        WHERE nl.SoLuongTon + dc.ChenhLech < 0
    )
    BEGIN
        RAISERROR(N'Không thể duyệt vì chênh lệch làm tồn kho hiện tại bị âm.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE nl
    SET nl.SoLuongTon = nl.SoLuongTon + dc.ChenhLech
    FROM dbo.NguyenLieu AS nl
    INNER JOIN @DieuChinh AS dc ON dc.MaNL = nl.MaNL;
END;
GO

CREATE TRIGGER dbo.trg_ChiTietPhieuKiemKe_KhoaSauGui
ON dbo.ChiTietPhieuKiemKe
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS
    (
        SELECT 1
        FROM
        (
            SELECT MaPhieuKK FROM inserted
            UNION
            SELECT MaPhieuKK FROM deleted
        ) AS x
        INNER JOIN dbo.PhieuKiemKe AS p ON p.MaPhieuKK = x.MaPhieuKK
        WHERE p.TrangThai <> 'Nhap'
    )
    BEGIN
        RAISERROR(N'Chỉ được sửa chi tiết khi phiếu kiểm kê ở trạng thái Nháp.', 16, 1);
        ROLLBACK TRANSACTION;
    END;
END;
GO

/* =========================================================
   4. CORE WORKFLOW PROCEDURES
   ========================================================= */

CREATE PROCEDURE dbo.usp_TaoNhanVienDauTien
    @MaNV VARCHAR(10),
    @HoTen NVARCHAR(100),
    @SoDienThoai VARCHAR(15),
    @Email VARCHAR(100) = NULL,
    @MatKhauHash VARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK))
    BEGIN
        RAISERROR(N'Hệ thống đã có nhân viên. Hãy đăng nhập để quản lý nhân viên.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    INSERT INTO dbo.NhanVien
        (MaNV, HoTen, SoDienThoai, Email, MatKhau, TrangThai, MaVaiTro)
    VALUES
        (@MaNV, @HoTen, @SoDienThoai, @Email, @MatKhauHash, 'Active', 'VT01');

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_CheckInBan
    @MaBan VARCHAR(10),
    @MaKH VARCHAR(10) = NULL,
    @MaNV VARCHAR(10),
    @MaDatPhong VARCHAR(15) = NULL,
    @MaPhien VARCHAR(15) OUTPUT,
    @MaHD VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @TrangThaiBan NVARCHAR(50);
    DECLARE @MaChotCa VARCHAR(15);

    SELECT @TrangThaiBan = TrangThai
    FROM dbo.Ban WITH (UPDLOCK, HOLDLOCK)
    WHERE MaBan = @MaBan;

    IF @TrangThaiBan IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy bàn.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @TrangThaiBan NOT IN (N'Trống', N'Đã đặt')
    BEGIN
        RAISERROR(N'Bàn không ở trạng thái có thể check-in.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @TrangThaiBan = N'Đã đặt' AND @MaDatPhong IS NULL
    BEGIN
        RAISERROR(N'Bàn đã đặt cần nhận khách từ booking trên bàn.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @MaDatPhong IS NULL
       AND EXISTS
       (
           SELECT 1
           FROM dbo.DatPhong WITH (UPDLOCK, HOLDLOCK)
           WHERE MaBan = @MaBan
             AND TrangThai = N'Đã đặt'
             AND ThoiGianBatDau <= DATEADD(MINUTE, 30, SYSDATETIME())
             AND ThoiGianKetThuc >= SYSDATETIME()
       )
    BEGIN
        RAISERROR(N'Bàn có booking sắp đến giờ. Hãy nhận khách từ booking trên bàn.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @MaDatPhong IS NOT NULL
    BEGIN
        DECLARE @BookingMaKH VARCHAR(10);
        DECLARE @BookingBatDau DATETIME2(0);
        DECLARE @BookingKetThuc DATETIME2(0);

        SELECT
            @BookingMaKH = MaKH,
            @BookingBatDau = ThoiGianBatDau,
            @BookingKetThuc = ThoiGianKetThuc
        FROM dbo.DatPhong WITH (UPDLOCK, HOLDLOCK)
        WHERE MaDatPhong = @MaDatPhong
          AND MaBan = @MaBan
          AND TrangThai = N'Đã đặt';

        IF @BookingMaKH IS NULL
        BEGIN
            RAISERROR(N'Booking không hợp lệ hoặc không thuộc bàn đã chọn.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        IF SYSDATETIME() < DATEADD(MINUTE, -30, @BookingBatDau)
           OR SYSDATETIME() > @BookingKetThuc
        BEGIN
            RAISERROR(N'Booking chưa đến giờ nhận hoặc đã quá thời gian sử dụng.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        IF @MaKH IS NOT NULL AND @MaKH <> @BookingMaKH
        BEGIN
            RAISERROR(N'Khách hàng không khớp với booking đã chọn.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        SET @MaKH = @BookingMaKH;

        UPDATE dbo.DatPhong
        SET TrangThai = N'Đã nhận'
        WHERE MaDatPhong = @MaDatPhong;
    END;

    SELECT @MaChotCa = MaChotCa
    FROM dbo.ChotCa WITH (UPDLOCK, HOLDLOCK)
    WHERE MaNV = @MaNV
      AND TrangThaiChot = N'Đang mở';

    IF @MaChotCa IS NULL
    BEGIN
        RAISERROR(N'Nhân viên phải mở ca trước khi check-in bàn.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SET @MaPhien = 'P' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);
    SET @MaHD = 'H' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);

    INSERT INTO dbo.PhienSuDung
        (MaPhien, ThoiGianVao, TrangThai, MaBan, MaKH, MaNV, MaDatPhong)
    VALUES
        (@MaPhien, SYSDATETIME(), N'Đang hoạt động', @MaBan, @MaKH, @MaNV, @MaDatPhong);

    INSERT INTO dbo.HoaDon
        (MaHD, TrangThai, LoaiHoaDon, MaPhien, MaNV, MaChotCa)
    VALUES
        (@MaHD, N'Chưa thanh toán', 'SuDungBan', @MaPhien, @MaNV, @MaChotCa);

    UPDATE dbo.Ban
    SET TrangThai = N'Đang phục vụ'
    WHERE MaBan = @MaBan;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_ThemMonVaoHoaDon
    @MaHD VARCHAR(15),
    @MaMon VARCHAR(10),
    @SoLuong INT,
    @GhiChu NVARCHAR(255) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @SoLuong <= 0
    BEGIN
        RAISERROR(N'Số lượng món phải lớn hơn 0.', 16, 1);
        RETURN;
    END;

    BEGIN TRANSACTION;

    IF NOT EXISTS
    (
        SELECT 1
        FROM dbo.HoaDon WITH (UPDLOCK, HOLDLOCK)
        WHERE MaHD = @MaHD
          AND TrangThai = N'Chưa thanh toán'
    )
    BEGIN
        RAISERROR(N'Hóa đơn không tồn tại hoặc đã khóa.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    DECLARE @DonGia DECIMAL(18, 2);
    DECLARE @LoaiMon NVARCHAR(50);

    SELECT
        @DonGia = DonGia,
        @LoaiMon = LoaiMon
    FROM dbo.Mon
    WHERE MaMon = @MaMon
      AND TrangThai = N'Đang bán';

    IF @DonGia IS NULL
    BEGIN
        RAISERROR(N'Món không tồn tại hoặc đang ngừng bán.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @LoaiMon = N'Gói giờ'
    BEGIN
        RAISERROR(N'Gói giờ phải được bán qua chức năng nạp giờ.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.DinhMuc AS dm
        INNER JOIN dbo.NguyenLieu AS nl ON nl.MaNL = dm.MaNL
        WHERE dm.MaMon = @MaMon
          AND nl.SoLuongTon < dm.SoLuongTieuHao * @SoLuong
    )
    BEGIN
        RAISERROR(N'Không đủ nguyên liệu để nhận order món này.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    INSERT INTO dbo.ChiTietHoaDon
        (MaHD, MaMon, SoLuong, DonGia, GhiChu, TrangThaiMon)
    VALUES
        (@MaHD, @MaMon, @SoLuong, @DonGia, @GhiChu, 'ChoPhaChe');

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_CapNhatTrangThaiMon
    @MaCTHD INT,
    @TrangThaiMoi VARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @TrangThaiCu VARCHAR(50);

    BEGIN TRANSACTION;

    SELECT @TrangThaiCu = ct.TrangThaiMon
    FROM dbo.ChiTietHoaDon AS ct WITH (UPDLOCK, HOLDLOCK)
    INNER JOIN dbo.HoaDon AS hd WITH (UPDLOCK, HOLDLOCK)
        ON hd.MaHD = ct.MaHD
    WHERE ct.MaCTHD = @MaCTHD
      AND hd.TrangThai = N'Chưa thanh toán';

    IF @TrangThaiCu IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy chi tiết order hoặc hóa đơn đã khóa.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF NOT
    (
        (@TrangThaiCu = 'ChoPhaChe' AND @TrangThaiMoi IN ('DangPha', 'DaHuy'))
        OR (@TrangThaiCu = 'DangPha' AND @TrangThaiMoi IN ('DaPha', 'DaHuy'))
        OR (@TrangThaiCu = 'DaPha' AND @TrangThaiMoi IN ('DaGiao', 'DangPha'))
        OR (@TrangThaiCu = 'DaGiao' AND @TrangThaiMoi = 'DangPha')
        OR (@TrangThaiCu = @TrangThaiMoi)
    )
    BEGIN
        RAISERROR(N'Chuyển trạng thái món không hợp lệ.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE dbo.ChiTietHoaDon
    SET TrangThaiMon = @TrangThaiMoi
    WHERE MaCTHD = @MaCTHD;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_TaoBooking
    @MaKH VARCHAR(10),
    @MaBan VARCHAR(10),
    @ThoiGianBatDau DATETIME2(0),
    @ThoiGianKetThuc DATETIME2(0),
    @MaDatPhong VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @ThoiGianKetThuc <= @ThoiGianBatDau
    BEGIN
        RAISERROR(N'Thời gian kết thúc phải sau thời gian bắt đầu.', 16, 1);
        RETURN;
    END;

    IF @ThoiGianBatDau <= SYSDATETIME()
    BEGIN
        RAISERROR(N'Thời gian bắt đầu booking phải ở tương lai.', 16, 1);
        RETURN;
    END;

    BEGIN TRANSACTION;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.DatPhong WITH (UPDLOCK, HOLDLOCK)
        WHERE MaBan = @MaBan
          AND TrangThai <> N'Đã hủy'
          AND @ThoiGianBatDau < ThoiGianKetThuc
          AND @ThoiGianKetThuc > ThoiGianBatDau
    )
    BEGIN
        RAISERROR(N'Bàn đã có booking trùng thời gian.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SET @MaDatPhong = 'D' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);

    INSERT INTO dbo.DatPhong
        (MaDatPhong, MaKH, MaBan, ThoiGianBatDau, ThoiGianKetThuc, TrangThai)
    VALUES
        (@MaDatPhong, @MaKH, @MaBan, @ThoiGianBatDau, @ThoiGianKetThuc, N'Đã đặt');

    IF NOT EXISTS
       (
           SELECT 1
           FROM dbo.PhienSuDung
           WHERE MaBan = @MaBan
             AND TrangThai = N'Đang hoạt động'
       )
    BEGIN
        UPDATE dbo.Ban
        SET TrangThai = N'Đã đặt'
        WHERE MaBan = @MaBan;
    END;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_HuyBooking
    @MaDatPhong VARCHAR(15)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @MaBan VARCHAR(10);

    SELECT @MaBan = MaBan
    FROM dbo.DatPhong WITH (UPDLOCK, HOLDLOCK)
    WHERE MaDatPhong = @MaDatPhong
      AND TrangThai = N'Đã đặt';

    IF @MaBan IS NULL
    BEGIN
        RAISERROR(N'Booking không tồn tại hoặc không thể hủy.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE dbo.DatPhong
    SET TrangThai = N'Đã hủy'
    WHERE MaDatPhong = @MaDatPhong;

    IF NOT EXISTS
    (
        SELECT 1
        FROM dbo.PhienSuDung
        WHERE MaBan = @MaBan
          AND TrangThai = N'Đang hoạt động'
    )
    AND NOT EXISTS
    (
        SELECT 1
        FROM dbo.DatPhong
        WHERE MaBan = @MaBan
          AND TrangThai = N'Đã đặt'
          AND ThoiGianKetThuc >= SYSDATETIME()
    )
    BEGIN
        UPDATE dbo.Ban
        SET TrangThai = N'Trống'
        WHERE MaBan = @MaBan
          AND TrangThai <> N'Cần dọn';
    END;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_NapGio
    @MaKH VARCHAR(10),
    @MaMon VARCHAR(10),
    @MaNV VARCHAR(10),
    @TenPT NVARCHAR(50),
    @MaHD VARCHAR(15) OUTPUT,
    @MaNapGio VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @DonGia DECIMAL(18, 2);
    DECLARE @SoGio DECIMAL(5, 2);
    DECLARE @HanSuDung INT;
    DECLARE @MaChotCa VARCHAR(15);

    SELECT
        @DonGia = DonGia,
        @SoGio = CONVERT(DECIMAL(5, 2), SoGioQuyDoi),
        @HanSuDung = HanSuDung
    FROM dbo.Mon
    WHERE MaMon = @MaMon
      AND LoaiMon = N'Gói giờ'
      AND TrangThai = N'Đang bán';

    IF @DonGia IS NULL
    BEGIN
        RAISERROR(N'Gói giờ không tồn tại hoặc đã ngừng bán.', 16, 1);
        RETURN;
    END;

    BEGIN TRANSACTION;

    SET @MaHD = 'H' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);
    SET @MaNapGio = 'N' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);

    SELECT @MaChotCa = MaChotCa
    FROM dbo.ChotCa WITH (UPDLOCK, HOLDLOCK)
    WHERE MaNV = @MaNV
      AND TrangThaiChot = N'Đang mở';

    IF @MaChotCa IS NULL
    BEGIN
        RAISERROR(N'Nhân viên phải mở ca trước khi thực hiện thanh toán.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    INSERT INTO dbo.HoaDon
    (
        MaHD, NgayLap, TienGio, TienMon, TienGiam,
        TrangThai, LoaiHoaDon, MaNV, MaChotCa
    )
    VALUES
    (
        @MaHD, SYSDATETIME(), 0, @DonGia, 0,
        N'Chưa thanh toán', 'NapGio', @MaNV, @MaChotCa
    );

    INSERT INTO dbo.ChiTietHoaDon
        (MaHD, MaMon, SoLuong, DonGia, TrangThaiMon)
    VALUES
        (@MaHD, @MaMon, 1, @DonGia, 'DaGiao');

    INSERT INTO dbo.LichSuNapGio
    (
        MaNapGio, SoGioNap, SoGioConLai, NgayNap, NgayHetHan,
        MaKH, MaMon, MaHD
    )
    VALUES
    (
        @MaNapGio, @SoGio, @SoGio, SYSDATETIME(),
        DATEADD(DAY, @HanSuDung, SYSDATETIME()), @MaKH, @MaMon, @MaHD
    );

    UPDATE dbo.HoaDon
    SET TrangThai = N'Đã thanh toán',
        PhuongThucThanhToan = @TenPT
    WHERE MaHD = @MaHD;

    UPDATE dbo.KhachHang
    SET SoDuGio =
    (
        SELECT COALESCE(SUM(SoGioConLai), 0)
        FROM dbo.LichSuNapGio
        WHERE MaKH = @MaKH
          AND NgayHetHan >= SYSDATETIME()
    )
    WHERE MaKH = @MaKH;

    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR(N'Không tìm thấy khách hàng.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_XemTruocCheckout
    @MaBan VARCHAR(10),
    @MaVoucher VARCHAR(15) = NULL,
    @DonGiaGio DECIMAL(18, 2) = 15000,
    @ThoiDiem DATETIME2(0) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @DonGiaGio <= 0
    BEGIN
        RAISERROR(N'Đơn giá giờ không hợp lệ.', 16, 1);
        RETURN;
    END;

    SET @ThoiDiem = COALESCE(@ThoiDiem, SYSDATETIME());

    DECLARE @MaPhien VARCHAR(15);
    DECLARE @MaHD VARCHAR(15);
    DECLARE @MaKH VARCHAR(10);
    DECLARE @ThoiGianVao DATETIME2(0);
    DECLARE @TongGio DECIMAL(10, 2);
    DECLARE @TongGioGoiConLai DECIMAL(10, 2) = 0;
    DECLARE @GioDaTru DECIMAL(10, 2) = 0;
    DECLARE @GioTinhTien DECIMAL(10, 2);
    DECLARE @TienMon DECIMAL(18, 2);
    DECLARE @TienGio DECIMAL(18, 2);
    DECLARE @TienGiam DECIMAL(18, 2) = 0;
    DECLARE @LoaiGiam NVARCHAR(30);
    DECLARE @GiaTriGiam DECIMAL(18, 2);

    SELECT TOP (1)
        @MaPhien = p.MaPhien,
        @MaKH = p.MaKH,
        @ThoiGianVao = p.ThoiGianVao
    FROM dbo.PhienSuDung AS p
    WHERE p.MaBan = @MaBan
      AND p.TrangThai = N'Đang hoạt động';

    IF @MaPhien IS NULL
    BEGIN
        RAISERROR(N'Bàn không có phiên sử dụng đang hoạt động.', 16, 1);
        RETURN;
    END;

    SELECT
        @MaHD = hd.MaHD,
        @TienMon = hd.TienMon
    FROM dbo.HoaDon AS hd
    WHERE hd.MaPhien = @MaPhien
      AND hd.TrangThai = N'Chưa thanh toán';

    IF @MaHD IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy hóa đơn chưa thanh toán của phiên.', 16, 1);
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.ChiTietHoaDon
        WHERE MaHD = @MaHD
          AND TrangThaiMon IN ('ChoPhaChe', 'DangPha', 'DaPha')
    )
    BEGIN
        RAISERROR(N'Hóa đơn còn món chưa hoàn tất hoặc chưa giao.', 16, 1);
        RETURN;
    END;

    SET @TongGio =
        CONVERT(DECIMAL(10, 2), DATEDIFF(SECOND, @ThoiGianVao, @ThoiDiem) / 3600.0);

    IF @TongGio < 0
    BEGIN
        RAISERROR(N'Thời điểm xem trước không được trước thời điểm check-in.', 16, 1);
        RETURN;
    END;

    IF @MaKH IS NOT NULL
    BEGIN
        SELECT @TongGioGoiConLai = COALESCE(SUM(CONVERT(DECIMAL(10, 2), SoGioConLai)), 0)
        FROM dbo.LichSuNapGio
        WHERE MaKH = @MaKH
          AND SoGioConLai > 0
          AND NgayHetHan >= @ThoiDiem;
    END;

    SET @GioDaTru =
        CASE
            WHEN @TongGioGoiConLai >= @TongGio THEN @TongGio
            ELSE @TongGioGoiConLai
        END;
    SET @GioTinhTien = @TongGio - @GioDaTru;
    SET @TienGio = ROUND(@GioTinhTien * @DonGiaGio, 0);

    IF @MaVoucher IS NOT NULL
    BEGIN
        SELECT
            @LoaiGiam = LoaiGiam,
            @GiaTriGiam = GiaTriGiam
        FROM dbo.Voucher
        WHERE MaVoucher = @MaVoucher
          AND TrangThai = N'Hoạt động'
          AND @ThoiDiem BETWEEN NgayBatDau AND NgayKetThuc;

        IF @LoaiGiam IS NULL
        BEGIN
            RAISERROR(N'Voucher không tồn tại, bị khóa hoặc hết hạn.', 16, 1);
            RETURN;
        END;

        SET @TienGiam =
            CASE
                WHEN @LoaiGiam = N'Phần trăm'
                    THEN ROUND((@TienGio + @TienMon) * @GiaTriGiam / 100, 0)
                ELSE @GiaTriGiam
            END;

        IF @TienGiam > @TienGio + @TienMon
            SET @TienGiam = @TienGio + @TienMon;
    END;

    SELECT
        @MaPhien AS MaPhien,
        @MaHD AS MaHD,
        @ThoiGianVao AS ThoiGianVao,
        @ThoiDiem AS ThoiGianDuKienRa,
        @ThoiDiem AS ThoiGianRa,
        DATEDIFF(MINUTE, @ThoiGianVao, @ThoiDiem) AS TongPhut,
        @TongGio AS TongGio,
        @GioDaTru AS GioDaTru,
        @GioTinhTien AS GioTinhTien,
        @DonGiaGio AS DonGiaGio,
        @TienGio AS TienGio,
        @TienMon AS TienMon,
        @TienGiam AS TienGiam,
        CONVERT(DECIMAL(18, 2), @TienGio + @TienMon - @TienGiam) AS TongThanhToan,
        CONVERT(DECIMAL(18, 2), @TienGio + @TienMon - @TienGiam) AS TongTien;
END;
GO

CREATE PROCEDURE dbo.usp_ThanhToanCheckout
    @MaBan VARCHAR(10),
    @MaNV VARCHAR(10),
    @TenPT NVARCHAR(50),
    @MaHD VARCHAR(15) OUTPUT,
    @MaVoucher VARCHAR(15) = NULL,
    @DonGiaGio DECIMAL(18, 2) = 15000
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @DonGiaGio <= 0
    BEGIN
        RAISERROR(N'Đơn giá giờ không hợp lệ.', 16, 1);
        RETURN;
    END;

    BEGIN TRANSACTION;

    DECLARE @MaPhien VARCHAR(15);
    DECLARE @MaKH VARCHAR(10);
    DECLARE @ThoiGianVao DATETIME2(0);
    DECLARE @ThoiGianRa DATETIME2(0) = SYSDATETIME();
    DECLARE @TongGio DECIMAL(10, 2);
    DECLARE @GioConThieu DECIMAL(10, 2);
    DECLARE @GioTru DECIMAL(5, 2);
    DECLARE @MaNap VARCHAR(15);
    DECLARE @SoDuGoi DECIMAL(5, 2);
    DECLARE @TienMon DECIMAL(18, 2);
    DECLARE @TienGio DECIMAL(18, 2);
    DECLARE @TienGiam DECIMAL(18, 2) = 0;
    DECLARE @LoaiGiam NVARCHAR(30);
    DECLARE @GiaTriGiam DECIMAL(18, 2);
    DECLARE @MaChotCa VARCHAR(15);

    SELECT TOP (1)
        @MaPhien = p.MaPhien,
        @MaKH = p.MaKH,
        @ThoiGianVao = p.ThoiGianVao
    FROM dbo.PhienSuDung AS p WITH (UPDLOCK, HOLDLOCK)
    WHERE p.MaBan = @MaBan
      AND p.TrangThai = N'Đang hoạt động';

    IF @MaPhien IS NULL
    BEGIN
        RAISERROR(N'Bàn không có phiên sử dụng đang hoạt động.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SELECT
        @MaHD = hd.MaHD,
        @TienMon = hd.TienMon
    FROM dbo.HoaDon AS hd WITH (UPDLOCK, HOLDLOCK)
    WHERE hd.MaPhien = @MaPhien
      AND hd.TrangThai = N'Chưa thanh toán';

    IF @MaHD IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy hóa đơn chưa thanh toán của phiên.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.ChiTietHoaDon
        WHERE MaHD = @MaHD
          AND TrangThaiMon IN ('ChoPhaChe', 'DangPha', 'DaPha')
    )
    BEGIN
        RAISERROR(N'Hóa đơn còn món chưa hoàn tất hoặc chưa giao.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SET @TongGio =
        CONVERT(DECIMAL(10, 2), DATEDIFF(SECOND, @ThoiGianVao, @ThoiGianRa) / 3600.0);
    SET @GioConThieu = @TongGio;

    IF @MaKH IS NOT NULL
    BEGIN
        WHILE @GioConThieu >= 0.01
        BEGIN
            SET @MaNap = NULL;
            SET @SoDuGoi = NULL;

            SELECT TOP (1)
                @MaNap = MaNapGio,
                @SoDuGoi = SoGioConLai
            FROM dbo.LichSuNapGio WITH (UPDLOCK, HOLDLOCK)
            WHERE MaKH = @MaKH
              AND SoGioConLai > 0
              AND NgayHetHan >= @ThoiGianRa
            ORDER BY NgayHetHan, NgayNap;

            IF @MaNap IS NULL BREAK;

            SET @GioTru =
                CONVERT
                (
                    DECIMAL(5, 2),
                    CASE WHEN @SoDuGoi >= @GioConThieu THEN @GioConThieu ELSE @SoDuGoi END
                );

            IF @GioTru <= 0 BREAK;

            UPDATE dbo.LichSuNapGio
            SET SoGioConLai = SoGioConLai - @GioTru
            WHERE MaNapGio = @MaNap;

            SET @GioConThieu = @GioConThieu - @GioTru;
        END;

        UPDATE dbo.KhachHang
        SET SoDuGio =
        (
            SELECT COALESCE(SUM(SoGioConLai), 0)
            FROM dbo.LichSuNapGio
            WHERE MaKH = @MaKH
              AND NgayHetHan >= @ThoiGianRa
        )
        WHERE MaKH = @MaKH;
    END;

    SET @TienGio = ROUND(@GioConThieu * @DonGiaGio, 0);

    SELECT @MaChotCa = MaChotCa
    FROM dbo.ChotCa WITH (UPDLOCK, HOLDLOCK)
    WHERE MaNV = @MaNV
      AND TrangThaiChot = N'Đang mở';

    IF @MaChotCa IS NULL
    BEGIN
        RAISERROR(N'Nhân viên phải mở ca trước khi thực hiện thanh toán.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @MaVoucher IS NOT NULL
    BEGIN
        SELECT
            @LoaiGiam = LoaiGiam,
            @GiaTriGiam = GiaTriGiam
        FROM dbo.Voucher WITH (UPDLOCK, HOLDLOCK)
        WHERE MaVoucher = @MaVoucher
          AND TrangThai = N'Hoạt động'
          AND @ThoiGianRa BETWEEN NgayBatDau AND NgayKetThuc;

        IF @LoaiGiam IS NULL
        BEGIN
            RAISERROR(N'Voucher không tồn tại, bị khóa hoặc hết hạn.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;

        SET @TienGiam =
            CASE
                WHEN @LoaiGiam = N'Phần trăm'
                    THEN ROUND((@TienGio + @TienMon) * @GiaTriGiam / 100, 0)
                ELSE @GiaTriGiam
            END;

        IF @TienGiam > @TienGio + @TienMon
            SET @TienGiam = @TienGio + @TienMon;
    END;

    UPDATE dbo.PhienSuDung
    SET
        ThoiGianRa = @ThoiGianRa,
        TongThoiGian = @TongGio,
        TrangThai = N'Đã kết thúc'
    WHERE MaPhien = @MaPhien;

    UPDATE dbo.HoaDon
    SET
        NgayLap = @ThoiGianRa,
        TienGio = @TienGio,
        TienGiam = @TienGiam,
        TrangThai = N'Đã thanh toán',
        PhuongThucThanhToan = @TenPT,
        MaVoucher = @MaVoucher,
        MaNV = @MaNV,
        MaChotCa = @MaChotCa
    WHERE MaHD = @MaHD;

    IF @MaKH IS NOT NULL
    BEGIN
        UPDATE dbo.KhachHang
        SET DiemTichLuy = DiemTichLuy +
            CONVERT(INT, FLOOR(@TienMon / 10000)),
            HangThanhVien = CASE
                WHEN DiemTichLuy + CONVERT(INT, FLOOR(@TienMon / 10000)) >= 500 THEN N'Vàng'
                WHEN DiemTichLuy + CONVERT(INT, FLOOR(@TienMon / 10000)) >= 200 THEN N'Bạc'
                ELSE HangThanhVien
            END
        WHERE MaKH = @MaKH
          AND LaThanhVien = 1;
    END;

    UPDATE dbo.Ban
    SET TrangThai = N'Cần dọn'
    WHERE MaBan = @MaBan;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_HuyPhieuNhapKho
    @MaPhieuNK VARCHAR(15),
    @LyDoHuy NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

    IF NULLIF(LTRIM(RTRIM(@LyDoHuy)), N'') IS NULL
    BEGIN
        RAISERROR(N'Phải nhập lý do hủy phiếu nhập.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.PhieuNhapKho
    SET
        TrangThai = N'Đã hủy',
        LyDoHuy = @LyDoHuy
    WHERE MaPhieuNK = @MaPhieuNK
      AND TrangThai = N'Hoạt động';

    IF @@ROWCOUNT = 0
        RAISERROR(N'Phiếu nhập không tồn tại hoặc đã hủy.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_DuyetPhieuKiemKe
    @MaPhieuKK VARCHAR(15),
    @MaNVDuyet VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.PhieuKiemKe
    SET
        MaNVDuyet = @MaNVDuyet,
        ThoiGianDuyet = SYSDATETIME(),
        TrangThai = 'DaDuyet',
        LyDoTuChoi = NULL
    WHERE MaPhieuKK = @MaPhieuKK
      AND TrangThai = 'ChoDuyet'
      AND MaNVDuyet IS NULL
      AND MaNV <> @MaNVDuyet;

    IF @@ROWCOUNT = 0
        RAISERROR(N'Phiếu kiểm kê không ở trạng thái chờ duyệt hoặc người duyệt trùng người kiểm.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_GuiDuyetPhieuKiemKe
    @MaPhieuKK VARCHAR(15),
    @MaNV VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.PhieuKiemKe
    SET
        TrangThai = 'ChoDuyet',
        ThoiGianGuiDuyet = SYSDATETIME(),
        LyDoTuChoi = NULL,
        MaNVDuyet = NULL,
        ThoiGianDuyet = NULL
    WHERE MaPhieuKK = @MaPhieuKK
      AND MaNV = @MaNV
      AND TrangThai IN ('Nhap', 'TuChoi')
      AND EXISTS
      (
          SELECT 1
          FROM dbo.ChiTietPhieuKiemKe
          WHERE MaPhieuKK = @MaPhieuKK
      );

    IF @@ROWCOUNT = 0
        RAISERROR(N'Phiếu không thể gửi duyệt hoặc chưa có chi tiết kiểm kê.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_TuChoiPhieuKiemKe
    @MaPhieuKK VARCHAR(15),
    @MaNVDuyet VARCHAR(10),
    @LyDoTuChoi NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

    IF NULLIF(LTRIM(RTRIM(@LyDoTuChoi)), N'') IS NULL
    BEGIN
        RAISERROR(N'Phải nhập lý do từ chối phiếu kiểm kê.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.PhieuKiemKe
    SET
        TrangThai = 'TuChoi',
        MaNVDuyet = @MaNVDuyet,
        ThoiGianDuyet = SYSDATETIME(),
        LyDoTuChoi = @LyDoTuChoi
    WHERE MaPhieuKK = @MaPhieuKK
      AND TrangThai = 'ChoDuyet'
      AND MaNV <> @MaNVDuyet;

    IF @@ROWCOUNT = 0
        RAISERROR(N'Phiếu kiểm kê không ở trạng thái chờ duyệt.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_DangKyCa
    @MaNV VARCHAR(10),
    @MaCa VARCHAR(10),
    @NgayLam DATE,
    @GhiChu NVARCHAR(255) = NULL,
    @MaDangKy VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @NgayLam < CONVERT(DATE, SYSDATETIME())
    BEGIN
        RAISERROR(N'Không thể đăng ký ca cho ngày đã qua.', 16, 1);
        RETURN;
    END;

    BEGIN TRANSACTION;

    DECLARE @SucChua INT;
    SELECT @SucChua = SucChua
    FROM dbo.CaLamViec WITH (UPDLOCK, HOLDLOCK)
    WHERE MaCa = @MaCa;

    IF @SucChua IS NULL
    BEGIN
        RAISERROR(N'Khung ca không tồn tại.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.DangKyCa
        WHERE MaNV = @MaNV
          AND NgayLam = @NgayLam
          AND TrangThai IN ('ChoDuyet', 'DaDuyet')
    )
    BEGIN
        RAISERROR(N'Nhân viên đã có đăng ký hiệu lực trong ngày này.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF
    (
        SELECT COUNT(*)
        FROM dbo.DangKyCa
        WHERE MaCa = @MaCa
          AND NgayLam = @NgayLam
          AND TrangThai IN ('ChoDuyet', 'DaDuyet')
    ) >= @SucChua
    BEGIN
        RAISERROR(N'Ca làm đã đủ số lượng đăng ký.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SET @MaDangKy = 'DK' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 13);

    INSERT INTO dbo.DangKyCa
        (MaDangKy, MaNV, MaCa, NgayLam, TrangThai, GhiChu)
    VALUES
        (@MaDangKy, @MaNV, @MaCa, @NgayLam, 'ChoDuyet', @GhiChu);

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_DuyetDangKyCa
    @MaDangKy VARCHAR(15),
    @MaNVDuyet VARCHAR(10),
    @MaCaMoi VARCHAR(10) = NULL,
    @NgayLamMoi DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @MaNV VARCHAR(10);
    DECLARE @MaCa VARCHAR(10);
    DECLARE @NgayLam DATE;
    DECLARE @SucChua INT;

    SELECT
        @MaNV = MaNV,
        @MaCa = COALESCE(@MaCaMoi, MaCa),
        @NgayLam = COALESCE(@NgayLamMoi, NgayLam)
    FROM dbo.DangKyCa WITH (UPDLOCK, HOLDLOCK)
    WHERE MaDangKy = @MaDangKy
      AND TrangThai = 'ChoDuyet';

    IF @MaNV IS NULL OR @MaNV = @MaNVDuyet
    BEGIN
        RAISERROR(N'Đăng ký không ở trạng thái chờ duyệt hoặc người duyệt không hợp lệ.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SELECT @SucChua = SucChua
    FROM dbo.CaLamViec WITH (UPDLOCK, HOLDLOCK)
    WHERE MaCa = @MaCa;

    IF @SucChua IS NULL
    BEGIN
        RAISERROR(N'Khung ca điều chỉnh không tồn tại.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF
    (
        SELECT COUNT(*)
        FROM dbo.DangKyCa
        WHERE MaCa = @MaCa
          AND NgayLam = @NgayLam
          AND TrangThai = 'DaDuyet'
          AND MaDangKy <> @MaDangKy
    ) >= @SucChua
    BEGIN
        RAISERROR(N'Ca làm đã đủ số nhân viên được duyệt.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE dbo.DangKyCa
    SET
        MaCa = @MaCa,
        NgayLam = @NgayLam,
        TrangThai = 'DaDuyet',
        MaNVDuyet = @MaNVDuyet,
        ThoiGianDuyet = SYSDATETIME()
    WHERE MaDangKy = @MaDangKy;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.usp_TuChoiDangKyCa
    @MaDangKy VARCHAR(15),
    @MaNVDuyet VARCHAR(10),
    @GhiChu NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

    IF NULLIF(LTRIM(RTRIM(@GhiChu)), N'') IS NULL
    BEGIN
        RAISERROR(N'Phải nhập lý do từ chối đăng ký ca.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.DangKyCa
    SET
        TrangThai = 'TuChoi',
        MaNVDuyet = @MaNVDuyet,
        ThoiGianDuyet = SYSDATETIME(),
        GhiChu = @GhiChu
    WHERE MaDangKy = @MaDangKy
      AND TrangThai = 'ChoDuyet'
      AND MaNV <> @MaNVDuyet;

    IF @@ROWCOUNT = 0
        RAISERROR(N'Đăng ký ca không ở trạng thái chờ duyệt.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_HuyDangKyCa
    @MaDangKy VARCHAR(15),
    @MaNV VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.DangKyCa
    SET TrangThai = 'DaHuy'
    WHERE MaDangKy = @MaDangKy
      AND MaNV = @MaNV
      AND TrangThai = 'ChoDuyet';

    IF @@ROWCOUNT = 0
        RAISERROR(N'Chỉ có thể hủy đăng ký ca đang chờ duyệt của chính mình.', 16, 1);
END;
GO

CREATE PROCEDURE dbo.usp_MoCa
    @MaCa VARCHAR(10),
    @MaNV VARCHAR(10),
    @TienDauCa DECIMAL(18, 2),
    @MaChotCa VARCHAR(15) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @TienDauCa < 0
    BEGIN
        RAISERROR(N'Tiền đầu ca không được âm.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS
    (
        SELECT 1
        FROM dbo.NhanVien
        WHERE MaNV = @MaNV
          AND
          (
              MaVaiTro = 'VT01'
              OR EXISTS
              (
                  SELECT 1
                  FROM dbo.DangKyCa
                  WHERE MaNV = @MaNV
                    AND MaCa = @MaCa
                    AND NgayLam = CONVERT(DATE, SYSDATETIME())
                    AND TrangThai = 'DaDuyet'
              )
          )
    )
    BEGIN
        RAISERROR(N'Nhân viên chưa có lịch ca được duyệt cho hôm nay.', 16, 1);
        RETURN;
    END;

    SET @MaChotCa = 'C' + LEFT(REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 14);

    INSERT INTO dbo.ChotCa
        (MaChotCa, TienDauCa, TienHeThong, TrangThaiChot, MaCa, MaNV)
    VALUES
        (@MaChotCa, @TienDauCa, @TienDauCa, N'Đang mở', @MaCa, @MaNV);
END;
GO

CREATE PROCEDURE dbo.usp_ChotCa
    @MaChotCa VARCHAR(15),
    @MaNVThucHien VARCHAR(10),
    @TienThucTe DECIMAL(18, 2),
    @LyDoChenhLech NVARCHAR(255) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @MaNV VARCHAR(10);
    DECLARE @TienDauCa DECIMAL(18, 2);
    DECLARE @DoanhThuTienMat DECIMAL(18, 2);
    DECLARE @TienHeThong DECIMAL(18, 2);

    SELECT
        @MaNV = MaNV,
        @TienDauCa = TienDauCa
    FROM dbo.ChotCa WITH (UPDLOCK, HOLDLOCK)
    WHERE MaChotCa = @MaChotCa
      AND MaNV = @MaNVThucHien
      AND TrangThaiChot = N'Đang mở';

    IF @MaNV IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy ca đang mở thuộc tài khoản hiện tại.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS
    (
        SELECT 1
        FROM dbo.HoaDon
        WHERE MaChotCa = @MaChotCa
          AND TrangThai = N'Chưa thanh toán'
    )
    BEGIN
        RAISERROR(N'Ca còn hóa đơn hoặc table chưa thanh toán.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    SELECT @DoanhThuTienMat = COALESCE(SUM(TongTien), 0)
    FROM dbo.HoaDon
    WHERE MaChotCa = @MaChotCa
      AND PhuongThucThanhToan = N'Tiền mặt'
      AND TrangThai = N'Đã thanh toán';

    SET @TienHeThong = @TienDauCa + COALESCE(@DoanhThuTienMat, 0);

    IF @TienThucTe <> @TienHeThong
       AND NULLIF(LTRIM(RTRIM(@LyDoChenhLech)), N'') IS NULL
    BEGIN
        RAISERROR(N'Có chênh lệch két tiền, cần nhập lý do.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE dbo.ChotCa
    SET
        TienHeThong = @TienHeThong,
        TienThucTe = @TienThucTe,
        LyDoChenhLech = @LyDoChenhLech,
        ThoiGianChot = SYSDATETIME(),
        TrangThaiChot = N'Đã chốt'
    WHERE MaChotCa = @MaChotCa;

    COMMIT TRANSACTION;
END;
GO

/* =========================================================
   5. UI AND REPORTING VIEWS
   ========================================================= */

CREATE VIEW dbo.vw_SoDoBan
AS
SELECT
    b.MaBan,
    b.TenBan,
    b.LoaiViTri,
    b.SucChua,
    CASE
        WHEN b.TrangThai = N'Đã đặt'
             AND NOT EXISTS
             (
                 SELECT 1
                 FROM dbo.DatPhong AS dp
                 WHERE dp.MaBan = b.MaBan
                   AND dp.TrangThai = N'Đã đặt'
                   AND dp.ThoiGianKetThuc >= SYSDATETIME()
             )
        THEN N'Trống'
        ELSE b.TrangThai
    END AS TrangThai,
    b.MaKhuVuc,
    kv.TenKhuVuc,
    p.MaPhien,
    p.ThoiGianVao,
    p.MaKH,
    kh.HoTen AS TenKhachHang,
    kh.SoDienThoai,
    hd.MaHD,
    hd.TienMon,
    DATEDIFF(MINUTE, p.ThoiGianVao, SYSDATETIME()) AS SoPhutDaDung
FROM dbo.Ban AS b
INNER JOIN dbo.KhuVuc AS kv ON kv.MaKhuVuc = b.MaKhuVuc
LEFT JOIN dbo.PhienSuDung AS p
    ON p.MaBan = b.MaBan
   AND p.TrangThai = N'Đang hoạt động'
LEFT JOIN dbo.KhachHang AS kh ON kh.MaKH = p.MaKH
LEFT JOIN dbo.HoaDon AS hd
    ON hd.MaPhien = p.MaPhien
   AND hd.TrangThai = N'Chưa thanh toán';
GO

CREATE VIEW dbo.vw_OrderPhaChe
AS
SELECT
    ct.MaCTHD,
    ct.MaHD,
    p.MaBan,
    b.TenBan,
    ct.MaMon,
    m.TenMon,
    ct.SoLuong,
    ct.GhiChu,
    ct.TrangThaiMon,
    ct.ThoiGianTao
FROM dbo.ChiTietHoaDon AS ct
INNER JOIN dbo.Mon AS m ON m.MaMon = ct.MaMon
INNER JOIN dbo.HoaDon AS hd ON hd.MaHD = ct.MaHD
LEFT JOIN dbo.PhienSuDung AS p ON p.MaPhien = hd.MaPhien
LEFT JOIN dbo.Ban AS b ON b.MaBan = p.MaBan
WHERE ct.TrangThaiMon IN ('ChoPhaChe', 'DangPha', 'DaPha');
GO

CREATE VIEW dbo.vw_CanhBaoTonKho
AS
SELECT
    MaNL,
    TenNL,
    DonViTinh,
    SoLuongTon,
    MucCanhBao,
    CASE
        WHEN SoLuongTon = 0 THEN N'Hết hàng'
        WHEN SoLuongTon <= MucCanhBao THEN N'Sắp hết'
        ELSE N'Đủ hàng'
    END AS TrangThaiTon
FROM dbo.NguyenLieu;
GO

CREATE VIEW dbo.vw_DoanhThuNgay
AS
SELECT
    CONVERT(DATE, NgayLap) AS Ngay,
    COUNT_BIG(*) AS SoHoaDon,
    SUM(TienGio) AS DoanhThuGio,
    SUM(TienMon) AS DoanhThuMon,
    SUM(TienGiam) AS TongGiamGia,
    SUM(TongTien) AS TongDoanhThu
FROM dbo.HoaDon
WHERE TrangThai = N'Đã thanh toán'
  AND NgayLap IS NOT NULL
GROUP BY CONVERT(DATE, NgayLap);
GO

CREATE VIEW dbo.vw_MonBanChay
AS
SELECT
    m.MaMon,
    m.TenMon,
    m.LoaiMon,
    SUM(CONVERT(BIGINT, ct.SoLuong)) AS SoLuongBan,
    SUM(CONVERT(DECIMAL(18, 2), ct.SoLuong * ct.DonGia)) AS DoanhThu
FROM dbo.ChiTietHoaDon AS ct
INNER JOIN dbo.HoaDon AS hd ON hd.MaHD = ct.MaHD
INNER JOIN dbo.Mon AS m ON m.MaMon = ct.MaMon
WHERE hd.TrangThai = N'Đã thanh toán'
  AND ct.TrangThaiMon <> 'DaHuy'
GROUP BY m.MaMon, m.TenMon, m.LoaiMon;
GO

/* =========================================================
   6. REQUIRED CONFIGURATION DATA
   NhanVien and all business data remain empty.
   ========================================================= */

INSERT INTO dbo.VaiTro (MaVaiTro, TenVaiTro, MoTa)
VALUES
    ('VT01', N'Chủ quán', N'Toàn quyền hệ thống'),
    ('VT02', N'Quản lý vận hành', N'Quản lý nhân sự, ca, báo cáo và vận hành'),
    ('VT03', N'Thu ngân/Lễ tân', N'Check-in, order, booking và thanh toán'),
    ('VT04', N'Pha chế/Kho', N'Pha chế, nhập kho và kiểm kê');
GO

INSERT INTO dbo.CaLamViec (MaCa, TenCa, GioBatDau, GioKetThuc, SucChua)
VALUES
    ('CA01', N'Ca sáng', '06:00:00', '14:00:00', 5),
    ('CA02', N'Ca chiều', '14:00:00', '22:00:00', 5),
    ('CA03', N'Ca đêm', '22:00:00', '06:00:00', 4);
GO


/* =========================================================
   7. POST-CREATION CHECKS
   ========================================================= */

SELECT
    DB_NAME() AS TenDatabase,
    (SELECT COUNT(*) FROM sys.tables) AS SoBang,
    (SELECT COUNT(*) FROM dbo.NhanVien) AS SoNhanVienBanDau,
    (SELECT COUNT(*) FROM dbo.VaiTro) AS SoVaiTro,
    (SELECT COUNT(*) FROM dbo.CaLamViec) AS SoKhungCa;
GO

SET NOEXEC OFF;
GO
