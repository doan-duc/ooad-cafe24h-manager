package dao;

import model.OperationsSnapshot;

/** Defines data access for the dashboard operations snapshot. */
public interface IOperationsDao {

    OperationsSnapshot snapshot(boolean includeRevenue);
}
