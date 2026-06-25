package dao;

import java.util.List;

import model.Employee;
import model.LookupItem;

/** Defines employee management data access operations. */
public interface IEmployeeDao {

    /** Searches employees by identifier, name, phone number, or role. */
    List<Employee> listEmployees(String keyword);

    /** Returns all configured roles. */
    List<LookupItem> listRoles();

    /** Inserts an employee with an already hashed password. */
    void insertEmployee(Employee employee, String passwordHash);

    /** Updates employee details while preserving at least one active owner. */
    void updateEmployee(Employee employee);

    /** Replaces an employee password hash. */
    void updatePassword(String maNV, String passwordHash);
}
