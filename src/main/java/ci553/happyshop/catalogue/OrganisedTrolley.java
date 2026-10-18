package ci553.happyshop.catalogue;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * A trolley that keeps its contents organised.
 *
 * Compared with the basic {@link Trolley}, which simply appends every product:
 * 1. Products with the same product ID are merged into one line,
 *    and their ordered quantities are added together.
 * 2. The trolley is kept sorted by product ID (ascending).
 *
 * Design note: this class extends Trolley and overrides addProduct(),
 * so the trolley behaviour changes without modifying the original Trolley class
 * (Open/Closed Principle). Any code that works with a Trolley also works with
 * an OrganisedTrolley (Liskov Substitution Principle).
 */
public class OrganisedTrolley extends Trolley {

    /** Orders trolley lines by product ID, e.g. 0001 before 0002. */
    private static final Comparator<Product> BY_PRODUCT_ID =
            Comparator.comparing(Product::getProductId);

    /**
     * Adds a product to the trolley, merging it with an existing line
     * that has the same product ID, then keeps the trolley sorted by product ID.
     *
     * A copy of the product is stored rather than the product itself.
     * CustomerModel passes the same search-result object each time the customer
     * clicks "Add to Trolley"; storing that object directly would mean merging
     * changes the caller's product too, and the quantity would double on every click.
     *
     * @param product the product to add; its ordered quantity is added to the trolley
     */
    @Override
    public void addProduct(Product product) {
        ArrayList<Product> products = getProducts();

        for (Product existing : products) {
            if (existing.getProductId().equals(product.getProductId())) {
                existing.setOrderedQuantity(existing.getOrderedQuantity() + product.getOrderedQuantity());
                return; // merged, and the order of lines is unchanged
            }
        }

        products.add(copyOf(product));
        products.sort(BY_PRODUCT_ID);
    }

    /**
     * Creates an independent copy of a product, so later changes to the trolley line
     * do not affect the original object (and vice versa).
     *
     * @param p the product to copy
     * @return a new Product with the same field values
     */
    private Product copyOf(Product p) {
        return new Product(p.getProductId(), p.getProductDescription(), p.getProductImageName(),
                p.getUnitPrice(), p.getOrderedQuantity(), p.getStockQuantity());
    }
}
