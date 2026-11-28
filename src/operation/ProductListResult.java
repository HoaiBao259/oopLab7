package operation;

import java.util.List;
import model.Product;

/** The products shown on a page and that page's position. */
public final class ProductListResult {
    private final Page<Product> page;

    ProductListResult(List<Product> products, int pageNumber) {
        this.page = Page.slice(products, pageNumber);
    }

    /** @return the products on the page */
    public List<Product> getProducts() {
        return page.items();
    }

    /** @return the one-based current page */
    public int getCurrentPage() {
        return page.currentPage();
    }

    /** @return the number of available pages */
    public int getTotalPages() {
        return page.totalPages();
    }
}
