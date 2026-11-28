package operation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.Product;

/** Reads and updates the product catalog and produces product reports. */
public final class ProductOperation {
    private static final ProductOperation INSTANCE = new ProductOperation();
    private static final String PRODUCT_FILE = "data/products.txt";
    private static final String SOURCE_DIRECTORY = "data/product_data";
    private static final List<String> PRODUCT_FIELDS = List.of("pro_id", "pro_model", "pro_category",
            "pro_name", "pro_current_price", "pro_raw_price", "pro_discount", "pro_likes_count");

    private ProductOperation() {
    }

    /** @return the single product operation instance */
    public static ProductOperation getInstance() {
        return INSTANCE;
    }

    /** Imports the source catalog when the saved catalog is missing. */
    public synchronized void initializeProducts() {
        if (!DataFiles.exists(PRODUCT_FILE)) {
            extractProductsFromFiles();
        }
    }

    /** Imports JSON line and CSV product records, updating existing products by ID. */
    public synchronized void extractProductsFromFiles() {
        File sourceDirectory = new File(SOURCE_DIRECTORY);
        File[] files = sourceDirectory.listFiles(file -> file.isFile() && isSourceFile(file.getName()));
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        Map<String, Product> imported = new LinkedHashMap<>();
        for (File file : files) {
            List<String> lines = DataFiles.read(file.getPath());
            boolean csv = file.getName().toLowerCase(Locale.ROOT).endsWith(".csv");
            List<String> columns = PRODUCT_FIELDS;
            for (String line : lines) {
                String record = line.strip();
                if (record.startsWith("\uFEFF")) {
                    record = record.substring(1).stripLeading();
                }
                if (record.isEmpty()) {
                    continue;
                }
                Map<String, String> fields;
                if (csv) {
                    List<String> values = splitCsvRow(record);
                    if (values.size() != PRODUCT_FIELDS.size()) {
                        continue;
                    }
                    List<String> header = productHeader(values);
                    if (header != null) {
                        columns = header;
                        continue;
                    }
                    fields = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        fields.put(columns.get(i), values.get(i));
                    }
                } else {
                    fields = FlatJson.parse(record);
                }
                Product product = toProduct(fields);
                if (product != null) {
                    imported.put(product.getProId(), product);
                }
            }
        }
        if (!imported.isEmpty()) {
            Map<String, Product> catalog = new LinkedHashMap<>();
            for (Product product : getAllProducts()) {
                catalog.put(product.getProId(), product);
            }
            catalog.putAll(imported);
            DataFiles.write(PRODUCT_FILE, catalog.values().stream().map(Product::toString).toList());
        }
    }

    /**
     * Retrieves up to ten products at the requested position.
     * @param pageNumber the one-based page number
     * @return the products and pagination details
     */
    public ProductListResult getProductList(int pageNumber) {
        return new ProductListResult(getAllProducts(), pageNumber);
    }

    /**
     * Removes a product entry from the catalog.
     * @param productId the ID of the product
     * @return whether the product was found and removed
     */
    public synchronized boolean deleteProduct(String productId) {
        if (productId == null) {
            return false;
        }
        List<String> lines = DataFiles.read(PRODUCT_FILE);
        boolean removed = lines.removeIf(line -> productId.equals(FlatJson.parse(line).get("pro_id")));
        if (removed) {
            DataFiles.write(PRODUCT_FILE, lines);
        }
        return removed;
    }

    /**
     * Finds product names that contain a keyword, ignoring case.
     * @param keyword the text to find in a product name
     * @return all matching products in catalog order
     */
    public List<Product> getProductListByKeyword(String keyword) {
        if (keyword == null) {
            return List.of();
        }
        String query = keyword.toLowerCase(Locale.ROOT);
        return getAllProducts().stream()
                .filter(product -> product.getProName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    /**
     * Finds one page of products whose name contains a keyword.
     * @param keyword the text to find in a product name
     * @param pageNumber the one-based page number
     * @return the matching products and pagination details
     */
    public ProductListResult getProductListByKeyword(String keyword, int pageNumber) {
        return new ProductListResult(getProductListByKeyword(keyword), pageNumber);
    }

    /**
     * Looks up a product by ID.
     * @param productId the product ID
     * @return the product if found, otherwise null
     */
    public Product getProductById(String productId) {
        return getAllProducts().stream().filter(product -> product.getProId().equals(productId))
                .findFirst().orElse(null);
    }

    /** Saves the number of products per category, sorted in descending order. */
    public void generateCategoryFigure() {
        Map<String, Long> counts = new HashMap<>();
        for (Product product : getAllProducts()) {
            counts.merge(product.getProCategory(), 1L, Long::sum);
        }
        ChartFiles.bar("product_categories.svg", "Products by category", "Category", "Products",
                sortedCategories(counts, Comparator.reverseOrder()));
    }

    /** Saves a pie chart of the three discount ranges. */
    public void generateDiscountFigure() {
        int underThirty = 0;
        int thirtyToSixty = 0;
        int overSixty = 0;
        for (Product product : getAllProducts()) {
            double discount = product.getProDiscount();
            if (discount < 30) {
                underThirty++;
            } else if (discount <= 60) {
                thirtyToSixty++;
            } else {
                overSixty++;
            }
        }
        Map<String, Integer> ranges = new LinkedHashMap<>();
        ranges.put("Under 30%", underThirty);
        ranges.put("30% to 60%", thirtyToSixty);
        ranges.put("Over 60%", overSixty);
        ChartFiles.pie("product_discounts.svg", "Product discounts", ranges);
    }

    /** Saves the total likes per category, sorted in ascending order. */
    public void generateLikesCountFigure() {
        Map<String, Long> counts = new HashMap<>();
        for (Product product : getAllProducts()) {
            counts.merge(product.getProCategory(), (long) product.getProLikesCount(), Long::sum);
        }
        ChartFiles.bar("product_likes_by_category.svg", "Likes by category", "Category", "Likes",
                sortedCategories(counts, Comparator.naturalOrder()));
    }

    /** Saves a scatter plot comparing product discount percentage and likes. */
    public void generateDiscountLikesCountFigure() {
        List<Double> discounts = new ArrayList<>();
        List<Double> likes = new ArrayList<>();
        for (Product product : getAllProducts()) {
            discounts.add(product.getProDiscount());
            likes.add((double) product.getProLikesCount());
        }
        ChartFiles.scatter("product_discount_vs_likes.svg", "Product discounts and likes",
                "Discount (%)", "Likes", discounts, likes);
    }

    /** Clears the saved catalog. */
    public synchronized void deleteAllProducts() {
        DataFiles.write(PRODUCT_FILE, List.of());
    }

    /** @return the products in catalog order */
    List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        for (String line : DataFiles.read(PRODUCT_FILE)) {
            Product product = toProduct(FlatJson.parse(line));
            if (product != null) {
                products.add(product);
            }
        }
        return products;
    }

    private static Map<String, Long> sortedCategories(Map<String, Long> totals, Comparator<Long> order) {
        Map<String, Long> sorted = new LinkedHashMap<>();
        totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(order).thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    private static Product toProduct(Map<String, String> fields) {
        for (String field : PRODUCT_FIELDS) {
            String value = fields.get(field);
            if (value == null || value.isBlank()) {
                return null;
            }
        }
        try {
            double currentPrice = Double.parseDouble(fields.get("pro_current_price"));
            double rawPrice = Double.parseDouble(fields.get("pro_raw_price"));
            double discount = Double.parseDouble(fields.get("pro_discount"));
            int likes = Integer.parseInt(fields.get("pro_likes_count"));
            if (!Double.isFinite(currentPrice) || !Double.isFinite(rawPrice) || !Double.isFinite(discount)
                    || currentPrice < 0 || rawPrice < 0 || discount < 0 || likes < 0) {
                return null;
            }
            return new Product(fields.get("pro_id"), fields.get("pro_model"), fields.get("pro_category"),
                    fields.get("pro_name"), currentPrice, rawPrice, discount, likes);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isSourceFile(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".jsonl") || name.endsWith(".txt") || name.endsWith(".csv");
    }

    private static List<String> productHeader(List<String> values) {
        List<String> names = values.stream().map(value -> {
            String name = value.toLowerCase(Locale.ROOT);
            if (name.startsWith("product_")) {
                return "pro_" + name.substring("product_".length());
            }
            return name.startsWith("pro_") ? name : "pro_" + name;
        }).toList();
        return names.containsAll(PRODUCT_FIELDS) ? names : null;
    }

    private static List<String> splitCsvRow(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char character = row.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(field.toString().strip());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        if (quoted) {
            return List.of();
        }
        fields.add(field.toString().strip());
        return fields;
    }
}
