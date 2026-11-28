package operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import model.Customer;

/**
 * Registers customers and manages their stored profiles.
 */
public class CustomerOperation {
    private static final CustomerOperation INSTANCE = new CustomerOperation();
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@"
                    + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]+");
    private static final Pattern MOBILE = Pattern.compile("0[34][0-9]{8}");
    private final UserOperation users = UserOperation.getInstance();

    private CustomerOperation() {}

    /**
     * Returns the shared customer operations instance.
     *
     * @return the shared instance
     */
    public static CustomerOperation getInstance() {
        return INSTANCE;
    }

    /**
     * Checks for an email address with a local part, a domain, and an extension.
     *
     * @param userEmail the email address to validate
     * @return true when the address matches the supported format
     */
    public boolean validateEmail(String userEmail) {
        return userEmail != null && EMAIL.matcher(userEmail).matches();
    }

    /**
     * Checks for a ten-digit mobile number beginning with 03 or 04.
     *
     * @param userMobile the mobile number to validate
     * @return true when the number matches the required format
     */
    public boolean validateMobile(String userMobile) {
        return userMobile != null && MOBILE.matcher(userMobile).matches();
    }

    /**
     * Registers a customer using a unique username and stores the encoded password.
     *
     * @param userName the desired username
     * @param userPassword the original password
     * @param userEmail the customer's email address
     * @param userMobile the customer's mobile number
     * @return true if the record was saved, or false if validation failed
     * @throws java.io.UncheckedIOException if the user records cannot be read or saved
     */
    public boolean registerCustomer(String userName, String userPassword,
                                    String userEmail, String userMobile) {
        if (!users.validateUsername(userName) || !users.validatePassword(userPassword)
                || !validateEmail(userEmail) || !validateMobile(userMobile)) {
            return false;
        }
        synchronized (users) {
            List<String> lines = users.readUserLines();
            if (users.usernameExists(lines, userName, null)) {
                return false;
            }
            Customer customer = new Customer(users.generateUniqueUserId(lines), userName,
                    users.encryptPassword(userPassword), DateTimes.now(), "customer", userEmail, userMobile);
            lines.add(customer.toString());
            users.writeUserLines(lines);
            return true;
        }
    }

    /**
     * Updates the saved username, password, email, or mobile number for a customer.
     * The supplied customer object is refreshed after the record is saved.
     *
     * @param attributeName the name of the profile field to update
     * @param value the desired field value
     * @param customerObject the customer whose profile should change
     * @return true if the profile was saved, or false if the customer or value is invalid
     * @throws java.io.UncheckedIOException if the user records cannot be read or saved
     */
    public boolean updateProfile(String attributeName, String value, Customer customerObject) {
        if (attributeName == null || customerObject == null || customerObject.getUserId() == null) {
            return false;
        }
        String attribute = attributeName.replace("_", "").toLowerCase(Locale.ROOT);
        synchronized (users) {
            List<String> lines = users.readUserLines();
            for (int index = 0; index < lines.size(); index++) {
                Map<String, String> record = users.parseJson(lines.get(index));
                if (!customerObject.getUserId().equals(record.get("user_id"))
                        || !(users.toUser(record) instanceof Customer stored)) {
                    continue;
                }
                if (!applyProfileChange(attribute, value, stored, lines)) {
                    return false;
                }
                lines.set(index, stored.toString());
                users.writeUserLines(lines);
                customerObject.setUserName(stored.getUserName());
                customerObject.setUserPassword(stored.getUserPassword());
                customerObject.setUserEmail(stored.getUserEmail());
                customerObject.setUserMobile(stored.getUserMobile());
                return true;
            }
        }
        return false;
    }

    private boolean applyProfileChange(String attribute, String value, Customer customer,
                                       List<String> lines) {
        switch (attribute) {
            case "name", "username" -> {
                if (!users.validateUsername(value) || users.usernameExists(lines, value, customer.getUserId())) {
                    return false;
                }
                customer.setUserName(value);
            }
            case "password", "userpassword" -> {
                if (!users.validatePassword(value)) {
                    return false;
                }
                customer.setUserPassword(users.encryptPassword(value));
            }
            case "email", "useremail" -> {
                if (!validateEmail(value)) {
                    return false;
                }
                customer.setUserEmail(value);
            }
            case "mobile", "usermobile" -> {
                if (!validateMobile(value)) {
                    return false;
                }
                customer.setUserMobile(value);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes a customer record by ID. Administrator records are retained.
     *
     * @param customerId the customer's user ID
     * @return true if a customer record was removed
     * @throws java.io.UncheckedIOException if the user records cannot be read or saved
     */
    public boolean deleteCustomer(String customerId) {
        if (customerId == null) {
            return false;
        }
        synchronized (users) {
            List<String> lines = users.readUserLines();
            boolean removed = lines.removeIf(line -> {
                Map<String, String> record = users.parseJson(line);
                return customerId.equals(record.get("user_id")) && "customer".equals(record.get("user_role"));
            });
            if (removed) {
                users.writeUserLines(lines);
            }
            return removed;
        }
    }

    /**
     * Finds a saved customer by user ID.
     *
     * @param customerId the customer's user ID
     * @return the matching customer, or null if the customer does not exist
     * @throws java.io.UncheckedIOException if the user records cannot be read
     */
    public Customer getCustomerById(String customerId) {
        if (customerId == null) {
            return null;
        }
        synchronized (users) {
            for (String line : users.readUserLines()) {
                Map<String, String> record = users.parseJson(line);
                if (customerId.equals(record.get("user_id")) && users.toUser(record) instanceof Customer customer) {
                    return customer;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves one page of up to ten customers in storage order.
     * An out-of-range request is clamped to the nearest page.
     *
     * @param pageNumber the requested page number, starting at one
     * @return the customers on that page and the pagination details
     * @throws java.io.UncheckedIOException if the user records cannot be read
     */
    public CustomerListResult getCustomerList(int pageNumber) {
        List<Customer> customers = new ArrayList<>();
        synchronized (users) {
            for (String line : users.readUserLines()) {
                if (users.toUser(users.parseJson(line)) instanceof Customer customer) {
                    customers.add(customer);
                }
            }
        }
        Page<Customer> page = Page.slice(customers, pageNumber);
        return new CustomerListResult(page.items(), page.currentPage(), page.totalPages());
    }

    /**
     * Removes all customer records from storage while retaining administrator records.
     *
     * @throws java.io.UncheckedIOException if the user records cannot be read or saved
     */
    public void deleteAllCustomers() {
        synchronized (users) {
            List<String> lines = users.readUserLines();
            if (lines.removeIf(line -> "customer".equals(users.parseJson(line).get("user_role")))) {
                users.writeUserLines(lines);
            }
        }
    }
}
