package operation;

import java.util.List;
import java.util.Map;
import model.Admin;
import model.Customer;
import model.User;

/**
 * Shares user-record access for customer and admin operations without changing
 * the existing section 2.6 UserOperation API.
 */
final class UserRecords {
    private static final String USER_FILE = "data/users.txt";

    private UserRecords() {}

    /** Checks for a duplicate username, optionally ignoring the edited account. */
    static boolean usernameExists(List<String> lines, String userName, String ignoredUserId) {
        for (String line : lines) {
            Map<String, String> record = FlatJson.parse(line);
            if (userName.equals(record.get("user_name"))
                    && (ignoredUserId == null || !ignoredUserId.equals(record.get("user_id")))) {
                return true;
            }
        }
        return false;
    }

    /** Reconstructs a valid account from its stored fields. */
    static User toUser(Map<String, String> record) {
        String id = record.get("user_id");
        String name = record.get("user_name");
        String password = record.get("user_password");
        String registerTime = record.get("user_register_time");
        String role = record.get("user_role");
        String decodedPassword = password == null ? null
                : UserOperation.getInstance().decryptPassword(password);
        if (id == null || !id.matches("u_[0-9]{10}") || name == null || name.isBlank()
                || decodedPassword == null || decodedPassword.isEmpty() || DateTimes.parse(registerTime) == null) {
            return null;
        }
        if ("admin".equals(role)) {
            return new Admin(id, name, password, registerTime, role);
        }

        String email = record.get("user_email");
        String mobile = record.get("user_mobile");
        if ("customer".equals(role) && email != null && !email.isBlank()
                && mobile != null && !mobile.isBlank()) {
            return new Customer(id, name, password, registerTime, role, email, mobile);
        }
        return null;
    }

    static List<String> readUserLines() {
        return DataFiles.read(USER_FILE);
    }

    static void writeUserLines(List<String> lines) {
        DataFiles.write(USER_FILE, lines);
    }

}
