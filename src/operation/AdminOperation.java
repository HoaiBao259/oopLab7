package operation;

import java.util.List;
import model.Admin;

/**
 * Ensures that a local installation has an administrator account.
 */
public class AdminOperation {
    private static final AdminOperation INSTANCE = new AdminOperation();
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin1";
    private final UserOperation users = UserOperation.getInstance();

    private AdminOperation() {}

    /**
     * Returns the shared administrator operations instance.
     *
     * @return the shared instance
     */
    public static AdminOperation getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a default administrator at startup if there are no administrators.
     * An existing account and its saved password are retained.
     *
     * @throws java.io.UncheckedIOException if the user records cannot be read or saved
     */
    public void registerAdmin() {
        synchronized (users) {
            List<String> lines = users.readUserLines();
            for (String line : lines) {
                if (users.toUser(users.parseJson(line)) instanceof Admin) {
                    return;
                }
            }

            String userName = DEFAULT_USERNAME;
            while (users.usernameExists(lines, userName, null)) {
                userName += "_";
            }
            Admin admin = new Admin(users.generateUniqueUserId(lines), userName,
                    users.encryptPassword(DEFAULT_PASSWORD), DateTimes.now(), "admin");
            lines.add(admin.toString());
            users.writeUserLines(lines);
        }
    }
}
