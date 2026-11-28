package operation;

import java.util.List;
import model.Order;

/** The orders shown on a page and that page's position. */
public final class OrderListResult {
    private final Page<Order> page;

    OrderListResult(List<Order> orders, int pageNumber) {
        this.page = Page.slice(orders, pageNumber);
    }

    /** @return the orders on the page */
    public List<Order> getOrders() {
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
