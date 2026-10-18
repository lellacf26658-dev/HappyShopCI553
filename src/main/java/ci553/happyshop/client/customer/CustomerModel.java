package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.OrganisedTrolley;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.Trolley;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class includes TODO comments in addToTrolley() and checkout()
// indicating possible extension ideas to explore.
public class CustomerModel {
    public CustomerView cusView; //part of MVC
    private DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private ArrayList<Product> searchResults = new ArrayList<>(); // in-stock products found by the last search
    private Product selectedProduct = null; // the search result the customer has chosen to add to the trolley
    private Trolley trolley =  new OrganisedTrolley(); // holds the products currently in the customer's trolley
                                                      // (merged by product ID and sorted, see OrganisedTrolley)

    // Data passed to CustomerView for UI updates (including search, trolley and receipt page)
    private String searchResultImageName = "imageHolder.jpg";
                    // Image file name representing the current search result.
                    // - Product image if a product is found
                   // - Default placeholder image otherwise
    private String searchResultProductText = "No product has been searched yet";
                  // Textual representation of the product from the search result.
                  // - Shows product details (e.g. "0001, 40-inch TV, ...") if found
                 // - Otherwise shows a status message
    private String trolleyContentsText = "";
                 // Textual representation of current trolley contents,
                 // used for display in the Trolley page
    private String receiptContentsText = "";
                 // Textual representation of the generated receipt after checkout,
                // used for display in the Receipt page

    // Dependency Injection via constructor
    public CustomerModel(DatabaseRW databaseRW){
        this.databaseRW = databaseRW;
    }

    /**
     * Searches for products by product ID or keyword, using one combined query.
     *
     * The query is passed in by the controller, so the model does not read UI controls
     * directly (keeping MVC layers separate, and making search testable without JavaFX).
     *
     * DatabaseRW.searchProducts() first tries an exact ID match, then a keyword search
     * on the description. Products that are out of stock are left out of the results.
     * If exactly one product is found it is selected automatically; otherwise the
     * customer chooses one from the results list.
     *
     * @param query the product ID or keyword typed by the customer
     */
    void search(String query){
        searchResults.clear();
        selectedProduct = null;

        String trimmedQuery = query == null ? "" : query.trim();
        if(trimmedQuery.isEmpty()){
            searchResultProductText = "Please type a product ID or keyword";
            System.out.println("Please type a product ID or keyword.");
            updateView();
            return;
        }

        for(Product p : databaseRW.searchProducts(trimmedQuery)){
            if(p.getStockQuantity() > 0){
                searchResults.add(p);
            }
        }

        if(searchResults.isEmpty()){
            searchResultProductText = "No products found for \"" + trimmedQuery + "\"";
        }
        else if(searchResults.size() == 1){
            selectedProduct = searchResults.get(0);
            searchResultProductText = describe(selectedProduct);
        }
        else{
            searchResultProductText = searchResults.size() + " products found.\nSelect one to add to your trolley.";
        }
        System.out.println(searchResultProductText);
        updateView();
    }

    /**
     * Selects one of the current search results, ready to be added to the trolley.
     * Products that are not in the current results are ignored.
     *
     * @param product the product the customer picked from the results list
     */
    void selectProduct(Product product){
        if(product != null && searchResults.contains(product)){
            selectedProduct = product;
            searchResultProductText = describe(product);
            updateView();
        }
    }

    // Builds the text shown for a selected product, e.g. ID, description, price and low-stock warning.
    private String describe(Product product){
        String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f",
                product.getProductId(), product.getProductDescription(), product.getUnitPrice());
        int stock = product.getStockQuantity();
        String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
        return baseInfo + quantityInfo;
    }

    void addToTrolley(){
        if(selectedProduct != null){
            // Add the selected product to the trolley.
            // The OrganisedTrolley merges it with any line that has the same product ID
            // and keeps the trolley sorted by product ID.
            trolley.addProduct(selectedProduct);
            // Build a formatted string so the trolley contents can be displayed in the UI
            trolleyContentsText = ProductListFormatter.buildString(trolley.getProducts());
        }
        else{
            searchResultProductText = searchResults.isEmpty()
                    ? "Please search for an available product before adding to trolley"
                    : "Please select a product from the results before adding to trolley";
            System.out.println("must search and select an available product before add to trolley");
        }
        receiptContentsText =""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    public void checkOut()  {
        if(!trolley.getProducts().isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedProducts= groupProductsById(trolley.getProducts());
            ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(groupedProducts);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley.getProducts());
                trolley.clear();
                trolleyContentsText ="";
                clearSearch(); // stock levels have changed, so old results may be out of date
                searchResultProductText ="Thank you for shopping with us.";
                receiptContentsText = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(receiptContentsText);
            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                clearSearch();

                //TODO
                // Add the following logic here:
                // 1. Remove products with insufficient stock from the trolley.
                // 2. Trigger a message window to notify the customer about the insufficient stock, rather than directly changing displayLaSearchResult.
                //You can use the provided RemoveProductNotifier class and its showRemovalMsg method for this purpose.
                //remember close the message window where appropriate (using method closeNotifierWindow() of RemoveProductNotifier class)
                searchResultProductText = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg.toString();
                System.out.println("stock is not enough");
            }
        }
        else{
            trolleyContentsText = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                grouped.put(id,new Product(p.getProductId(),p.getProductDescription(),
                        p.getProductImageName(),p.getUnitPrice(),p.getOrderedQuantity(),p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        trolleyContentsText ="";
        updateView();
    }
    void closeReceipt(){
        receiptContentsText ="";
    }

    // Clears the search results and the selected product.
    private void clearSearch(){
        searchResults.clear();
        selectedProduct = null;
    }

    void updateView() {
        if(selectedProduct != null){
            searchResultImageName = selectedProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder + searchResultImageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            searchResultImageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            searchResultImageName = "imageHolder.jpg";
        }
        cusView.update(searchResultImageName, searchResultProductText,
                List.copyOf(searchResults), selectedProduct, trolleyContentsText, receiptContentsText);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test
    public Trolley getTrolley() {
        return trolley;
    }

    //for test
    List<Product> getSearchResults() {
        return List.copyOf(searchResults);
    }

    //for test
    Product getSelectedProduct() {
        return selectedProduct;
    }

    //for test
    String getSearchResultProductText() {
        return searchResultProductText;
    }
}
