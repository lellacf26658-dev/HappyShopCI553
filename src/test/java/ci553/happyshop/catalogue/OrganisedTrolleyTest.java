package ci553.happyshop.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State-based tests for {@link OrganisedTrolley}.
 *
 * Covers the two requirements of the "organised trolley" feature:
 * merging products with the same ID, and keeping the trolley sorted by ID.
 * Also covers the defensive copy, which prevents a quantity-doubling bug
 * when the same Product object is added more than once.
 */
class OrganisedTrolleyTest {

    private OrganisedTrolley trolley;

    @BeforeEach
    void setUp() {
        trolley = new OrganisedTrolley();
    }

    private static Product product(String id) {
        return new Product(id, "item " + id, id + ".jpg", 10.00, 100);
    }

    private List<String> idsInTrolley() {
        List<String> ids = new ArrayList<>();
        for (Product p : trolley.getProducts()) {
            ids.add(p.getProductId());
        }
        return ids;
    }

    @Test
    @DisplayName("A new trolley is empty")
    void newTrolleyIsEmpty() {
        assertTrue(trolley.getProducts().isEmpty());
    }

    @Test
    @DisplayName("Adding one product creates one line with quantity 1")
    void addSingleProduct() {
        trolley.addProduct(product("0001"));

        assertEquals(1, trolley.getProducts().size());
        assertEquals(1, trolley.getProducts().get(0).getOrderedQuantity());
    }

    @Test
    @DisplayName("Products with the same ID are merged into one line")
    void mergesSameProductId() {
        trolley.addProduct(product("0002"));
        trolley.addProduct(product("0002"));

        assertEquals(1, trolley.getProducts().size());
        assertEquals(2, trolley.getProducts().get(0).getOrderedQuantity());
    }

    @Test
    @DisplayName("Merging adds the ordered quantities together")
    void mergeAddsQuantities() {
        Product three = product("0003");
        three.setOrderedQuantity(3);
        Product four = product("0003");
        four.setOrderedQuantity(4);

        trolley.addProduct(three);
        trolley.addProduct(four);

        assertEquals(7, trolley.getProducts().get(0).getOrderedQuantity());
    }

    @Test
    @DisplayName("Products are kept sorted by product ID")
    void sortsByProductId() {
        trolley.addProduct(product("0005"));
        trolley.addProduct(product("0001"));
        trolley.addProduct(product("0003"));

        assertEquals(List.of("0001", "0003", "0005"), idsInTrolley());
    }

    @Test
    @DisplayName("Merging and sorting work together on a mixed sequence")
    void mergesAndSortsMixedSequence() {
        for (String id : List.of("0004", "0002", "0004", "0001", "0002", "0004")) {
            trolley.addProduct(product(id));
        }

        assertEquals(List.of("0001", "0002", "0004"), idsInTrolley());
        assertEquals(1, trolley.getProducts().get(0).getOrderedQuantity());
        assertEquals(2, trolley.getProducts().get(1).getOrderedQuantity());
        assertEquals(3, trolley.getProducts().get(2).getOrderedQuantity());
    }

    @Test
    @DisplayName("Adding the same object twice gives quantity 2, not 4 (defensive copy)")
    void sameObjectAddedTwiceIsCountedCorrectly() {
        // CustomerModel passes the same searchResult object on every "Add to Trolley" click
        Product searchResult = product("0001");

        trolley.addProduct(searchResult);
        trolley.addProduct(searchResult);
        trolley.addProduct(searchResult);

        assertEquals(3, trolley.getProducts().get(0).getOrderedQuantity());
        assertEquals(1, searchResult.getOrderedQuantity(), "the caller's product must not be changed");
        assertNotSame(searchResult, trolley.getProducts().get(0));
    }

    @ParameterizedTest(name = "adding the same product {0} times gives quantity {0}")
    @ValueSource(ints = {1, 2, 5, 10})
    void quantityMatchesNumberOfAdds(int times) {
        for (int i = 0; i < times; i++) {
            trolley.addProduct(product("0007"));
        }

        assertEquals(1, trolley.getProducts().size());
        assertEquals(times, trolley.getProducts().get(0).getOrderedQuantity());
    }

    @Test
    @DisplayName("clear() still empties an organised trolley")
    void clearEmptiesTrolley() {
        trolley.addProduct(product("0001"));
        trolley.addProduct(product("0002"));

        trolley.clear();

        assertTrue(trolley.getProducts().isEmpty());
    }

    @Test
    @DisplayName("An OrganisedTrolley can be used wherever a Trolley is expected")
    void substitutableForTrolley() {
        Trolley asTrolley = trolley; // Liskov Substitution: used through the parent type
        asTrolley.addProduct(product("0002"));
        asTrolley.addProduct(product("0001"));
        asTrolley.addProduct(product("0002"));

        assertEquals(List.of("0001", "0002"), idsInTrolley());
    }
}
