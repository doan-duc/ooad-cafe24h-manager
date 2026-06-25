package dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import model.LookupItem;
import model.ShiftRecord;
import model.ShiftRegistration;

/** Defines data access operations for work shifts. */
public interface IShiftDao {

    List<LookupItem> shiftTypes();

    List<LookupItem> activeEmployees();

    String register(String maNV, String maCa, LocalDate ngayLam, String ghiChu);

    void approve(String maDangKy, String maNVDuyet, String maCaMoi, LocalDate ngayLamMoi);

    void reject(String maDangKy, String maNVDuyet, String ghiChu);

    void cancel(String maDangKy, String maNV);

    String assign(String maNV, String maCa, LocalDate ngayLam, String ghiChu, String maNVDuyet);

    List<ShiftRegistration> registrationsForEmployee(String maNV);

    List<ShiftRegistration> registrations();

    String open(String maCa, String maNV, BigDecimal openingCash);

    void close(String maChotCa, String employeeId, BigDecimal actualCash, String reason);

    ShiftRecord findOpen(String maNV);

    List<ShiftRecord> history();
}
