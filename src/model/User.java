package model;

/**
 * Stores the account fields shared by administrators and customers.
 */
public abstract class User {
    /** Stable account identifier stored with orders and user records. */
    protected String userId;
    /** Name used to sign in. */
    protected String userName;
    /** Password value, encoded in persisted records. */
    protected String userPassword;
    /** Local registration timestamp in the record format. */
    protected String userRegisterTime;
    /** Account role used to select the appropriate menu. */
    protected String userRole;

    /**
     * Constructs an account model using the supplied fields.
     *
     * @param userId a unique user ID in the form {@code u_1234567890}
     * @param userName the username
     * @param userPassword the password, encoded before persistence
     * @param userRegisterTime the registration time in day-month-year and hour-minute-second format
     * @param userRole the account role
     */
    public User(String userId, String userName, String userPassword,
                String userRegisterTime, String userRole) {
        this.userId = userId;
        this.userName = userName;
        this.userPassword = userPassword;
        this.userRegisterTime = userRegisterTime;
        this.userRole = userRole;
    }

    /**
     * Constructs a user model with the default customer field values.
     */
    public User() {
        this("u_0000000000", "default_user", "default1", "23-11-2026_00:00:00", "customer");
    }

    /**
     * Returns the common user fields as a JSON record.
     *
     * @return the serialized account
     */
    @Override
    public String toString() {
        return "{" + userFieldsToString() + "}";
    }

    /**
     * Serializes the shared fields so subclasses can add their own fields.
     *
     * @return the shared fields without enclosing braces
     */
    protected String userFieldsToString() {
        return String.format(
                "\"user_id\":%s, \"user_name\":%s, \"user_password\":%s, "
                        + "\"user_register_time\":%s, \"user_role\":%s",
                JsonStrings.quote(userId), JsonStrings.quote(userName), JsonStrings.quote(userPassword),
                JsonStrings.quote(userRegisterTime), JsonStrings.quote(userRole));
    }

    /**
     * Returns the stored user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the stored username.
     *
     * @return the username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns the model's stored password field.
     *
     * @return the password field
     */
    public String getUserPassword() {
        return userPassword;
    }

    /**
     * Returns when the account was registered.
     *
     * @return the formatted registration time
     */
    public String getUserRegisterTime() {
        return userRegisterTime;
    }

    /**
     * Returns the account role.
     *
     * @return the role
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Updates the model's user ID.
     *
     * @param userId the new ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Updates the model's username.
     *
     * @param userName the new username
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Updates the model's password field.
     *
     * @param userPassword the new password field
     */
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Updates the model's registration time.
     *
     * @param userRegisterTime the new formatted registration time
     */
    public void setUserRegisterTime(String userRegisterTime) {
        this.userRegisterTime = userRegisterTime;
    }

    /**
     * Updates the model's role.
     *
     * @param userRole the new role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}
