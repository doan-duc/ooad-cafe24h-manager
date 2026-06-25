package dao;

import model.Employee;

/** Defines data access operations for authentication and account setup. */
public interface IAuthDao {

    /** Returns the total employee count. */
    int countEmployees();

    /** Returns the number of active accounts with a valid bcrypt hash. */
    int countUsableAccounts();

    /** Returns the matching login account, or {@code null} when none exists. */
    Employee findForLogin(String login);

    /** Returns an active account matching the given phone or email, or {@code null}. */
    Employee findByContact(String contact);

    /** Updates the hashed password for the given employee. */
    void updatePassword(String maNV, String hash);

    /** Creates the initial owner account during first-run setup. */
    void createFirstOwner(
            String maNV,
            String hoTen,
            String soDienThoai,
            String email,
            String passwordHash);
}
