package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the flexible search feature in {@link CustomerModel}.
 *
 * The database is replaced with a Mockito mock, so each test controls exactly which
 * products a search returns. The view is also mocked, because the model calls
 * cusView.update() after every change. No JavaFX window or real database is needed,
 * which is possible because the controller passes the query into the model.
 */
class CustomerSearchTest {

    private DatabaseRW mockDatabase;
    private CustomerView mockView;
    private CustomerModel model;

    private final Product tv = new Product("0001", "40 inch TV", "0001.jpg", 269.00, 50);
    private final Product radio = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 20);
    private final Product tvStand = new Product("0003", "TV stand", "0003.jpg", 49.99, 8);
    private final Product soldOutTv = new Product("0004", "Smart TV", "0004.jpg", 399.00, 0);

    @BeforeEach
    void setUp() {
        mockDatabase = mock(DatabaseRW.class);
        mockView = mock(CustomerView.class);
        model = new CustomerModel(mockDatabase);
        model.cusView = mockView;
    }

    // Stubs the database so searching for the given query returns these products
    private void databaseReturns(String query, Product... products) {
        when(mockDatabase.searchProducts(query)).thenReturn(new ArrayList<>(List.of(products)));
    }

    @Test
    @DisplayName("Searching by ID returns that product and selects it")
    void searchById() {
        databaseReturns("0001", tv);

        model.search("0001");

        assertEquals(List.of(tv), model.getSearchResults());
        assertSame(tv, model.getSelectedProduct());
    }

    @Test
    @DisplayName("Searching by keyword can return several products, none selected yet")
    void searchByKeywordReturnsSeveral() {
        databaseReturns("tv", tv, tvStand);

        model.search("tv");

        assertEquals(List.of(tv, tvStand), model.getSearchResults());
        assertNull(model.getSelectedProduct(), "the customer must choose one of several results");
        assertTrue(model.getSearchResultProductText().startsWith("2 products found"));
    }

    @Test
    @DisplayName("Out-of-stock products are left out of the results")
    void outOfStockProductsAreFilteredOut() {
        databaseReturns("tv", tv, soldOutTv, tvStand);

        model.search("tv");

        assertEquals(List.of(tv, tvStand), model.getSearchResults());
    }

    @Test
    @DisplayName("If every match is out of stock, the customer is told nothing was found")
    void allMatchesOutOfStock() {
        databaseReturns("smart", soldOutTv);

        model.search("smart");

        assertTrue(model.getSearchResults().isEmpty());
        assertNull(model.getSelectedProduct());
        assertEquals("No products found for \"smart\"", model.getSearchResultProductText());
    }

    @Test
    @DisplayName("A search with no matches gives an empty result and a message")
    void noMatches() {
        databaseReturns("piano");

        model.search("piano");

        assertTrue(model.getSearchResults().isEmpty());
        assertEquals("No products found for \"piano\"", model.getSearchResultProductText());
    }

    @ParameterizedTest(name = "blank query \"{0}\" does not search the database")
    @ValueSource(strings = {"", " ", "   ", "\t"})
    void blankQueryDoesNotSearch(String blank) {
        model.search(blank);

        verify(mockDatabase, never()).searchProducts(anyString());
        assertTrue(model.getSearchResults().isEmpty());
        assertEquals("Please type a product ID or keyword", model.getSearchResultProductText());
    }

    @Test
    @DisplayName("A null query is treated like a blank one")
    void nullQueryDoesNotSearch() {
        model.search(null);

        verify(mockDatabase, never()).searchProducts(anyString());
    }

    @ParameterizedTest(name = "query \"{0}\" is trimmed before searching")
    @ValueSource(strings = {"0001", " 0001", "0001 ", "  0001  "})
    void queryIsTrimmed(String query) {
        databaseReturns("0001", tv);

        model.search(query);

        verify(mockDatabase).searchProducts("0001");
        assertSame(tv, model.getSelectedProduct());
    }

    @Test
    @DisplayName("Selecting one of several results makes it the selected product")
    void selectProductFromResults() {
        databaseReturns("tv", tv, tvStand);
        model.search("tv");

        model.selectProduct(tvStand);

        assertSame(tvStand, model.getSelectedProduct());
        assertTrue(model.getSearchResultProductText().contains("TV stand"));
    }

    @Test
    @DisplayName("A product that is not in the current results cannot be selected")
    void cannotSelectProductOutsideResults() {
        databaseReturns("tv", tv, tvStand);
        model.search("tv");

        model.selectProduct(radio);

        assertNull(model.getSelectedProduct());
    }

    @Test
    @DisplayName("A new search clears the previous selection")
    void newSearchClearsSelection() {
        databaseReturns("0002", radio);
        databaseReturns("tv", tv, tvStand);
        model.search("0002");
        assertSame(radio, model.getSelectedProduct());

        model.search("tv");

        assertNull(model.getSelectedProduct());
    }

    @Test
    @DisplayName("Add to Trolley adds the selected search result")
    void addSelectedProductToTrolley() {
        databaseReturns("tv", tv, tvStand);
        model.search("tv");
        model.selectProduct(tvStand);

        model.addToTrolley();

        List<Product> trolley = model.getTrolley().getProducts();
        assertEquals(1, trolley.size());
        assertEquals("0003", trolley.get(0).getProductId());
    }

    @Test
    @DisplayName("Add to Trolley with several results but none selected adds nothing")
    void addWithNothingSelected() {
        databaseReturns("tv", tv, tvStand);
        model.search("tv");

        model.addToTrolley();

        assertTrue(model.getTrolley().getProducts().isEmpty());
        assertEquals("Please select a product from the results before adding to trolley",
                model.getSearchResultProductText());
    }

    @Test
    @DisplayName("Add to Trolley before any search adds nothing")
    void addBeforeSearching() {
        model.addToTrolley();

        assertTrue(model.getTrolley().getProducts().isEmpty());
        assertEquals("Please search for an available product before adding to trolley",
                model.getSearchResultProductText());
    }

    @Test
    @DisplayName("The view is given the search results and the selected product")
    void viewReceivesResults() {
        databaseReturns("0001", tv);

        model.search("0001");

        verify(mockView).update(anyString(), anyString(), eq(List.of(tv)), eq(tv), anyString(), anyString());
    }
}
