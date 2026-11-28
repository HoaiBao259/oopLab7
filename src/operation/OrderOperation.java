package operation;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import model.Customer;
import model.Order;
import model.Product;
import model.User;

/** Stores customer orders and produces order and consumption reports. */
public final class OrderOperation {
    private static final OrderOperation INSTANCE = new OrderOperation();
    private static final String ORDER_FILE = "data/orders.txt";
    private static final int ID_COUNT = 100_000;
    private static final Random RANDOM = new Random();

    private OrderOperation() {
    }

    /** @return the single order operation instance */
    public static OrderOperation getInstance() {
        return INSTANCE;
    }

    /**
     * Finds an unused order ID consisting of the prefix o_ and five digits.
     * @return an ID absent from the stored orders
     * @throws IllegalStateException if all order IDs are in use
     */
    public synchronized String generateUniqueOrderId() {
        return takeOrderId(allocatedIds(DataFiles.read(ORDER_FILE)));
    }

    /**
     * Saves one order for an existing customer and product.
     * @param customerId the ID of the customer making the order
     * @param productId the ID of the product being ordered
     * @param createTime the formatted creation time, or null for the current time
     * @return whether the order was saved
     */
    public synchronized boolean createAnOrder(String customerId, String productId, String createTime) {
        String orderTime = createTime == null ? DateTimes.now() : createTime;
        if (customerId == null || productId == null || DateTimes.parse(orderTime) == null
                || CustomerOperation.getInstance().getCustomerById(customerId) == null
                || ProductOperation.getInstance().getProductById(productId) == null) {
            return false;
        }
        List<String> lines = DataFiles.read(ORDER_FILE);
        BitSet usedIds = allocatedIds(lines);
        if (usedIds.cardinality() == ID_COUNT) {
            return false;
        }
        lines.add(new Order(takeOrderId(usedIds), customerId, productId, orderTime).toString());
        DataFiles.write(ORDER_FILE, lines);
        return true;
    }

    /**
     * Removes an order from storage.
     * @param orderId the ID of the order to remove
     * @return whether the order was found and removed
     */
    public synchronized boolean deleteOrder(String orderId) {
        if (orderId == null) {
            return false;
        }
        List<String> lines = DataFiles.read(ORDER_FILE);
        boolean removed = lines.removeIf(line -> orderId.equals(FlatJson.parse(line).get("order_id")));
        if (removed) {
            DataFiles.write(ORDER_FILE, lines);
        }
        return removed;
    }

    /**
     * Retrieves one page of a customer's orders.
     * @param customerId the customer ID, or null to view all orders
     * @param pageNumber the one-based page number
     * @return up to ten orders and pagination details
     */
    public OrderListResult getOrderList(String customerId, int pageNumber) {
        return new OrderListResult(readOrders(customerId), pageNumber);
    }

    /**
     * Retrieves one page of orders across all customers.
     * @param pageNumber the one-based page number
     * @return up to ten orders and pagination details
     */
    public OrderListResult getOrderList(int pageNumber) {
        return getOrderList(null, pageNumber);
    }

    /**
     * Adds ten sample customers and 50 to 200 orders per customer.
     * Each customer's orders cover all months of the previous calendar year.
     * @throws IllegalStateException if there are no products or the sample data cannot be created
     */
    public synchronized void generateTestOrderData() {
        List<Product> products = ProductOperation.getInstance().getAllProducts();
        if (products.isEmpty()) {
            throw new IllegalStateException("Import products before generating test orders.");
        }
        List<String> orderLines = DataFiles.read(ORDER_FILE);
        BitSet usedIds = allocatedIds(orderLines);
        int[] orderCounts = RANDOM.ints(10, 50, 201).toArray();
        int totalOrders = 0;
        for (int count : orderCounts) {
            totalOrders += count;
        }
        if (usedIds.cardinality() + totalOrders > ID_COUNT) {
            throw new IllegalStateException("There are not enough unused order IDs to generate test data.");
        }

        List<String> createdCustomers = new ArrayList<>();
        int previousYear = Year.now().getValue() - 1;
        try {
            for (int count : orderCounts) {
                String customerId = registerTestCustomer();
                createdCustomers.add(customerId);
                for (int i = 0; i < count; i++) {
                    int month = i < 12 ? i + 1 : RANDOM.nextInt(1, 13);
                    LocalDateTime time = LocalDateTime.of(previousYear, month, RANDOM.nextInt(1, 29),
                            RANDOM.nextInt(24), RANDOM.nextInt(60), RANDOM.nextInt(60));
                    String productId = products.get(RANDOM.nextInt(products.size())).getProId();
                    Order order = new Order(takeOrderId(usedIds), customerId, productId, DateTimes.format(time));
                    orderLines.add(order.toString());
                }
            }
            DataFiles.write(ORDER_FILE, orderLines);
        } catch (RuntimeException exception) {
            for (String customerId : createdCustomers) {
                try {
                    CustomerOperation.getInstance().deleteCustomer(customerId);
                } catch (RuntimeException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
            }
            throw exception;
        }
    }

    /**
     * Saves a twelve-month spending chart for a customer.
     * @param customerId the customer ID
     */
    public void generateSingleCustomerConsumptionFigure(String customerId) {
        if (customerId == null || !customerId.matches("u_[0-9]{10}")) {
            throw new IllegalArgumentException("A valid customer ID is required.");
        }
        generateConsumptionFigure(customerId, "order_consumption_" + customerId + ".svg",
                "Monthly spending for " + customerId);
    }

    /** Saves a twelve-month spending chart for all customers. */
    public void generateAllCustomersConsumptionFigure() {
        generateConsumptionFigure(null, "order_consumption_all.svg", "Monthly spending for all customers");
    }

    /** Saves the ten products with the highest order counts. */
    public void generateAllTop10BestSellersFigure() {
        Map<String, Integer> counts = new HashMap<>();
        for (Order order : readOrders(null)) {
            counts.merge(order.getProId(), 1, Integer::sum);
        }
        Map<String, Product> products = productsById();
        Map<String, Integer> ranked = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .forEach(entry -> {
                    String id = entry.getKey();
                    Product product = products.get(id);
                    String label = product == null ? id : product.getProName() + " (" + id + ")";
                    ranked.put(label, entry.getValue());
                });
        ChartFiles.bar("order_top_10_products.svg", "Top 10 best-selling products", "Product", "Orders", ranked);
    }

    /** Clears all stored orders. */
    public synchronized void deleteAllOrders() {
        DataFiles.write(ORDER_FILE, List.of());
    }

    private static String registerTestCustomer() {
        UserOperation users = UserOperation.getInstance();
        String username;
        do {
            StringBuilder candidate = new StringBuilder("test_customer_");
            for (int i = 0; i < 12; i++) {
                candidate.append((char) ('a' + RANDOM.nextInt(26)));
            }
            username = candidate.toString();
        } while (users.checkUsernameExist(username));
        String password = "LabTest1" + RANDOM.nextInt(100_000_000);
        String mobile = String.format(Locale.ROOT, "04%08d", RANDOM.nextInt(100_000_000));
        if (!CustomerOperation.getInstance().registerCustomer(username, password, username + "@example.com", mobile)) {
            throw new IllegalStateException("Could not create a test customer.");
        }
        User created = users.login(username, password);
        if (!(created instanceof Customer customer)) {
            throw new IllegalStateException("Could not read the new test customer.");
        }
        return customer.getUserId();
    }

    private static List<Order> readOrders(String customerId) {
        List<Order> orders = new ArrayList<>();
        for (String line : DataFiles.read(ORDER_FILE)) {
            Map<String, String> fields = FlatJson.parse(line);
            String id = fields.get("order_id");
            String userId = fields.get("user_id");
            String productId = fields.get("pro_id");
            String orderTime = fields.get("order_time");
            if (id != null && userId != null && productId != null && DateTimes.parse(orderTime) != null
                    && (customerId == null || customerId.equals(userId))) {
                orders.add(new Order(id, userId, productId, orderTime));
            }
        }
        return orders;
    }

    private static BitSet allocatedIds(List<String> lines) {
        BitSet usedIds = new BitSet(ID_COUNT);
        for (String line : lines) {
            String id = FlatJson.parse(line).get("order_id");
            if (id != null && id.matches("o_[0-9]{5}")) {
                usedIds.set(Integer.parseInt(id.substring(2)));
            }
        }
        return usedIds;
    }

    private static String takeOrderId(BitSet usedIds) {
        int candidate = usedIds.nextClearBit(RANDOM.nextInt(ID_COUNT));
        if (candidate >= ID_COUNT) {
            candidate = usedIds.nextClearBit(0);
        }
        if (candidate >= ID_COUNT) {
            throw new IllegalStateException("No unused order IDs remain.");
        }
        usedIds.set(candidate);
        return String.format(Locale.ROOT, "o_%05d", candidate);
    }

    private static Map<String, Product> productsById() {
        Map<String, Product> products = new HashMap<>();
        for (Product product : ProductOperation.getInstance().getAllProducts()) {
            products.put(product.getProId(), product);
        }
        return products;
    }

    private static void generateConsumptionFigure(String customerId, String filename, String title) {
        Map<String, Product> products = productsById();
        double[] spending = new double[12];
        for (Order order : readOrders(customerId)) {
            Product product = products.get(order.getProId());
            if (product != null) {
                int month = DateTimes.parse(order.getOrderTime()).getMonthValue();
                spending[month - 1] += product.getProCurrentPrice();
            }
        }
        Map<String, Double> months = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            months.put(month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), spending[month.getValue() - 1]);
        }
        ChartFiles.bar(filename, title, "Month", "Spending", months);
    }
}
