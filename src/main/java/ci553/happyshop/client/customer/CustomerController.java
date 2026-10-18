package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;

public class CustomerController {
    public CustomerModel cusModel;

    public void doAction(String action) {
        switch (action) {
            case "Add to Trolley":
                cusModel.addToTrolley();
                break;
            case "Cancel":
                cusModel.cancel();
                break;
            case "Check Out":
                cusModel.checkOut();
                break;
            case "OK & Close":
                cusModel.closeReceipt();
                break;
        }
    }

    /**
     * Passes the customer's search text (a product ID or keyword) to the model.
     * The view supplies the text, so the model never reads UI controls directly.
     *
     * @param query the text typed in the search box
     */
    public void doSearch(String query) {
        cusModel.search(query);
    }

    /**
     * Tells the model which search result the customer has selected.
     *
     * @param product the product selected in the results list
     */
    public void selectProduct(Product product) {
        cusModel.selectProduct(product);
    }
}
