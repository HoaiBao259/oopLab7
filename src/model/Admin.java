package model;

/**
 * Represents an administrator account.
 */
public class Admin extends User {

    /**
     * Constructs an admin object.
     * @param userId Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName The user's name
     * @param userPassword The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole Default value: "admin"
     */

    public Admin(String userId, String userName, String userPassword,
                 String userRegisterTime, String userRole) {
        super(userId, userName, userPassword, userRegisterTime, userRole);
    }

    /**
     * Constructs an administrator model with default account fields.
     */
    public Admin() {
        super("u_0000000000", "admin", "admin1", "23-11-2026_00:00:00", "admin");
    }

    /**
     * Returns the admin's attributes as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
