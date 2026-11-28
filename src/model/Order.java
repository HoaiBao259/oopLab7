package model;

/** A customer's order for one product. */
public class Order {
    private String orderId;
    private String userId;
    private String proId;
    private String orderTime;

    /**
     * Constructs an order.
     * @param orderId the unique order ID with prefix o_ and five digits
     * @param userId the ID of the customer who ordered
     * @param proId the ID of the product ordered
     * @param orderTime the creation time in DD-MM-YYYY_HH:MM:SS format
     */
    public Order(String orderId, String userId, String proId, String orderTime) {
        this.orderId = orderId;
        this.userId = userId;
        this.proId = proId;
        this.orderTime = orderTime;
    }

    /** Constructs a default order. */
    public Order() {
        this("o_00000", "u_0000000000", "p_000", "23-11-2026_00:00:00");
    }

    /**
     * Formats the order for storage.
     * @return the order as one JSON record
     */
    @Override
    public String toString() {
        return "{\"order_id\":" + JsonStrings.quote(orderId) + ", \"user_id\":" + JsonStrings.quote(userId)
                + ", \"pro_id\":" + JsonStrings.quote(proId) + ", \"order_time\":" + JsonStrings.quote(orderTime) + "}";
    }

    /** @return the order ID */
    public String getOrderId() {
        return orderId;
    }

    /** @param orderId the new order ID */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** @return the customer's ID */
    public String getUserId() {
        return userId;
    }

    /** @param userId the new customer ID */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** @return the ordered product ID */
    public String getProId() {
        return proId;
    }

    /** @param proId the new product ID */
    public void setProId(String proId) {
        this.proId = proId;
    }

    /** @return the formatted time the order was placed */
    public String getOrderTime() {
        return orderTime;
    }

    /** @param orderTime the new formatted order time */
    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }
}
