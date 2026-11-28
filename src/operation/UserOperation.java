package operation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import model.Admin;
import model.Customer;
import model.User;

/**
 * Provides shared validation, password encoding, and access to saved user records.
 */
public class UserOperation {
    private static final UserOperation INSTANCE = new UserOperation();
    private static final String USER_FILE = "data/users.txt";
    private static final String PASSWORD_FILLER =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    private UserOperation() {}

    /**
     * Returns the shared user operations instance and user storage lock.
     *
     * @return the shared instance
     */
    public static UserOperation getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a user ID with ten digits that is absent from the stored records.
     *
     * @return an unused user ID
     * @throws java.io.UncheckedIOException if the user records cannot be read
     */
    public synchronized String generateUniqueUserId() {
        return generateUniqueUserId(readUserLines());
    }

    String generateUniqueUserId(List<String> lines) {
        Set<String> storedIds = new HashSet<>();
        for (String line : lines) {
            String storedId = parseJson(line).get("user_id");
            if (storedId != null) {
                storedIds.add(storedId);
            }
        }
        String id;
        do {
            id = "u_" + RANDOM.nextLong(1_000_000_000L, 10_000_000_000L);
        } while (storedIds.contains(id));
        return id;
    }

    /**
     * Encodes a password by adding two random alphanumeric characters before each
     * password character and surrounding the result with the specified delimiters.
     *
     * @param password the original password
     * @return the encoded password
     */
    public String encryptPassword(String password) {
        StringBuilder result = new StringBuilder("^^");
        for (int i = 0; i < password.length(); i++) {
            result.append(PASSWORD_FILLER.charAt(RANDOM.nextInt(PASSWORD_FILLER.length())));
            result.append(PASSWORD_FILLER.charAt(RANDOM.nextInt(PASSWORD_FILLER.length())));
            result.append(password.charAt(i));
        }
        result.append("$$");
        return result.toString();
    }

    /**
     * Extracts the original password from the third character of each encoded group.
     *
     * @param encrypted the encoded password
     * @return the original password, or null if the encoding is malformed
     */
    public String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.length() < 4 || !encrypted.startsWith("^^")
                || !encrypted.endsWith("$$") || (encrypted.length() - 4) % 3 != 0) {
            return null;
        }
        StringBuilder password = new StringBuilder();
        for (int i = 2; i < encrypted.length() - 2; i += 3) {
            if (PASSWORD_FILLER.indexOf(encrypted.charAt(i)) < 0
                    || PASSWORD_FILLER.indexOf(encrypted.charAt(i + 1)) < 0) {
                return null;
            }
            password.append(encrypted.charAt(i + 2));
        }
        return password.toString();
    }

    /**
     * Checks whether a username appears in the customer or administrator records.
     *
     * @param userName the username to check
     * @return true when the username already appears
     * @throws java.io.UncheckedIOException if the user records cannot be read
     */
    public synchronized boolean checkUsernameExist(String userName) {
        return userName != null && usernameExists(readUserLines(), userName, null);
    }

    boolean usernameExists(List<String> lines, String userName, String ignoredUserId) {
        for (String line : lines) {
            Map<String, String> record = parseJson(line);
            if (userName.equals(record.get("user_name"))
                    && (ignoredUserId == null || !ignoredUserId.equals(record.get("user_id")))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks that a username consists of at least five letters or underscores.
     *
     * @param name the username to validate
     * @return true when the username meets the rule
     */
    public boolean validateUsername(String name) {
        return name != null && name.matches("[A-Za-z_]{5,}");
    }

    /**
     * Checks that a password has at least five characters, a letter, and a digit.
     *
     * @param pass the password to validate
     * @return true when the password meets the rule
     */
    public boolean validatePassword(String pass) {
        return pass != null && pass.matches("(?s)(?=.*[A-Za-z])(?=.*\\d).{5,}");
    }

    /**
     * Verifies a username and password against a saved user record.
     *
     * @param userName the supplied username
     * @param userPassword the supplied password
     * @return the matching administrator or customer, or null if login fails
     * @throws java.io.UncheckedIOException if the user records cannot be read
     */
    public synchronized User login(String userName, String userPassword) {
        if (userName == null || userPassword == null) {
            return null;
        }
        for (String line : readUserLines()) {
            Map<String, String> record = parseJson(line);
            if (userName.equals(record.get("user_name"))
                    && userPassword.equals(decryptPassword(record.get("user_password")))) {
                User user = toUser(record);
                if (user != null) {
                    return user;
                }
            }
        }
        return null;
    }

    User toUser(Map<String, String> record) {
        String id = record.get("user_id");
        String name = record.get("user_name");
        String password = record.get("user_password");
        String registerTime = record.get("user_register_time");
        String role = record.get("user_role");
        String decodedPassword = decryptPassword(password);
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

    List<String> readUserLines() {
        return DataFiles.read(USER_FILE);
    }

    void writeUserLines(List<String> lines) {
        DataFiles.write(USER_FILE, lines);
    }

    /**
     * Parses a single stored record into its field names and values.
     *
     * @param line the JSON record
     * @return the parsed fields, or an empty map for a malformed record
     */
    public Map<String, String> parseJson(String line) {
        return FlatJson.parse(line);
    }
}
