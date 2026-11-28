package operation;

import java.util.List;
import model.Customer;

/**
 * Holds a page of customers and its position within the full list.
 */
public final class CustomerListResult {
    private final List<Customer> customers;
    private final int currentPage;
    private final int totalPages;

    /**
     * Creates a page of customer records.
     *
     * @param customers the records on the page
     * @param currentPage the one-based page number
     * @param totalPages the number of available pages
     */
    public CustomerListResult(List<Customer> customers, int currentPage, int totalPages) {
        this.customers = List.copyOf(customers);
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    /**
     * Returns the customers on the page.
     *
     * @return an unmodifiable list of customers
     */
    public List<Customer> getCustomers() {
        return customers;
    }

    /**
     * Returns the position of the displayed page.
     *
     * @return the one-based page number
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns the total number of available customer pages.
     *
     * @return the number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }
}
