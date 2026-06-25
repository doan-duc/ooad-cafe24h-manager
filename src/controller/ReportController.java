package controller;

import java.time.LocalDate;
import java.util.List;

import dao.IReportDao;
import dao.ReportDao;
import model.BestSeller;
import model.RevenueSummary;
import model.TableUsage;
import security.Authorization;
import security.Permission;
import security.Session;

public final class ReportController {
    private final IReportDao dao;

    public ReportController() {
        this(new ReportDao());
    }

    public ReportController(IReportDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy tóm tắt doanh thu trong khoảng thời gian (tổng tiền, số hóa đơn, ...)
    public RevenueSummary revenue(LocalDate from, LocalDate to) {
        Authorization.require(Session.currentUser(), Permission.REPORT_VIEW);
        validateRange(from, to);
        return dao.revenue(from, to);
    }

    // Tóm tắt: Lấy danh sách sản phẩm bán chạy nhất trong khoảng thời gian (tên, số lượng, doanh thu)
    public List<BestSeller> bestSellers(LocalDate from, LocalDate to) {
        Authorization.require(Session.currentUser(), Permission.REPORT_VIEW);
        validateRange(from, to);
        return dao.bestSellers(from, to);
    }

    // Tóm tắt: Lấy thống kê sử dụng bàn trong khoảng thời gian (tên bàn, số lần sử dụng, doanh thu)
    public List<TableUsage> tableUsage(LocalDate from, LocalDate to) {
        Authorization.require(Session.currentUser(), Permission.REPORT_VIEW);
        validateRange(from, to);
        return dao.tableUsage(from, to);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian báo cáo không hợp lệ.");
        }
    }
}
