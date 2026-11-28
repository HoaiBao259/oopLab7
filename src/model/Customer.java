package model;

/**
 * Represents a customer account with email and mobile contact information.
 */
public class Customer extends User {
    private String userEmail;
    private String userMobile;

    /**
     * Constructs a customer object.
     * @param userId Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName The user's name
     * @param userPassword The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole Default value: "customer"
     * @param userEmail The customer's email address
     * @param userMobile The customer's mobile number
     */
    public Customer(String userId, String userName, String userPassword,
                    String userRegisterTime, String userRole,
                    String userEmail, String userMobile) {
        super(userId, userName, userPassword, userRegisterTime, userRole);
        this.userEmail = userEmail;
        this.userMobile = userMobile;
    }

    /**
     * Constructs a customer model with default account and contact fields.
     */
    public Customer() {
        super();
        userEmail = "default@example.com";
        userMobile = "0400000000";
    }

    /**
     * Returns the customer information as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return String.format(
            "{%s, \"user_email\":%s, \"user_mobile\":%s}",
            userFieldsToString(), JsonStrings.quote(userEmail), JsonStrings.quote(userMobile)
        );
    }

    /**
     * Returns the customer's email address.
     *
     * @return the email address
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Returns the customer's mobile number.
     *
     * @return the mobile number
     */
    public String getUserMobile() {
        return userMobile;
    }

    /**
     * Updates the model's email address.
     *
     * @param userEmail the new email address
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     * Updates the model's mobile number.
     *
     * @param userMobile the new mobile number
     */
    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }
}
