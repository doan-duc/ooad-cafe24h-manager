package dao;

import java.time.LocalDate;
import java.util.List;

import model.BestSeller;
import model.RevenueSummary;
import model.TableUsage;

/** Defines data access operations for revenue reports. */
public interface IReportDao {

    RevenueSummary revenue(LocalDate from, LocalDate to);

    List<BestSeller> bestSellers(LocalDate from, LocalDate to);

    List<TableUsage> tableUsage(LocalDate from, LocalDate to);
}
