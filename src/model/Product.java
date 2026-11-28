package model;

/** A catalog product with pricing and popularity information. */
public class Product {
    private String proId;
    private String proModel;
    private String proCategory;
    private String proName;
    private double proCurrentPrice;
    private double proRawPrice;
    private double proDiscount;
    private int proLikesCount;

    /**
     * Constructs a product.
     * @param proId the product's unique ID
     * @param proModel the model
     * @param proCategory the category
     * @param proName the name
     * @param proCurrentPrice the current price
     * @param proRawPrice the original price
     * @param proDiscount the discount percentage
     * @param proLikesCount the number of likes
     */
    public Product(String proId, String proModel, String proCategory,
                   String proName, double proCurrentPrice, double proRawPrice,
                   double proDiscount, int proLikesCount) {
        this.proId = proId;
        this.proModel = proModel;
        this.proCategory = proCategory;
        this.proName = proName;
        this.proCurrentPrice = proCurrentPrice;
        this.proRawPrice = proRawPrice;
        this.proDiscount = proDiscount;
        this.proLikesCount = proLikesCount;
    }

    /** Constructs a default product. */
    public Product() {
        this("default_id", "default_model", "default_category", "default_name", 0.0, 0.0, 0.0, 0);
    }

    /**
     * Formats the product for storage.
     * @return the product as one JSON record with quoted field values
     */
    @Override
    public String toString() {
        return "{\"pro_id\":" + JsonStrings.quote(proId) + ", \"pro_model\":" + JsonStrings.quote(proModel)
                + ", \"pro_category\":" + JsonStrings.quote(proCategory) + ", \"pro_name\":" + JsonStrings.quote(proName)
                + ", \"pro_current_price\":\"" + proCurrentPrice + "\", \"pro_raw_price\":\"" + proRawPrice
                + "\", \"pro_discount\":\"" + proDiscount + "\", \"pro_likes_count\":\"" + proLikesCount + "\"}";
    }

    /** @return the product ID */
    public String getProId() {
        return proId;
    }

    /** @param proId the new product ID */
    public void setProId(String proId) {
        this.proId = proId;
    }

    /** @return the product model */
    public String getProModel() {
        return proModel;
    }

    /** @param proModel the new product model */
    public void setProModel(String proModel) {
        this.proModel = proModel;
    }

    /** @return the product category */
    public String getProCategory() {
        return proCategory;
    }

    /** @param proCategory the new product category */
    public void setProCategory(String proCategory) {
        this.proCategory = proCategory;
    }

    /** @return the product name */
    public String getProName() {
        return proName;
    }

    /** @param proName the new product name */
    public void setProName(String proName) {
        this.proName = proName;
    }

    /** @return the current price */
    public double getProCurrentPrice() {
        return proCurrentPrice;
    }

    /** @param proCurrentPrice the new current price */
    public void setProCurrentPrice(double proCurrentPrice) {
        this.proCurrentPrice = proCurrentPrice;
    }

    /** @return the original price */
    public double getProRawPrice() {
        return proRawPrice;
    }

    /** @param proRawPrice the new original price */
    public void setProRawPrice(double proRawPrice) {
        this.proRawPrice = proRawPrice;
    }

    /** @return the discount as a percentage */
    public double getProDiscount() {
        return proDiscount;
    }

    /** @param proDiscount the new discount percentage */
    public void setProDiscount(double proDiscount) {
        this.proDiscount = proDiscount;
    }

    /** @return the number of likes */
    public int getProLikesCount() {
        return proLikesCount;
    }

    /** @param proLikesCount the new number of likes */
    public void setProLikesCount(int proLikesCount) {
        this.proLikesCount = proLikesCount;
    }
}
