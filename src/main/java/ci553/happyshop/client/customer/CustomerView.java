package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.util.List;

/**
 * The CustomerView is separated into two sections by a line :
 *
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    private TextField tfSearch; // one combined search box: accepts a product ID or a keyword

    //UI controls that need updating when the program is running
    private ObservableList<Product> searchResultsCopy; // copy of the model's search results, observed by the ListView
    private ListView<Product> lvSearchResults; // shows every product found by the last search
    private boolean updatingFromModel = false; // true while update() refreshes the list, so selection
                                               // changes made by the program are not sent back to the model
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductText;//product text info in searchPage
    private TextArea taTrolley; //in trolley Page
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyleGreen);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Standard Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        Label laPageTitle = new Label("🔍 Browse & Search Products 🔍");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        tfSearch = new TextField();
        tfSearch.setPromptText("ID or keyword, eg. 0001 or TV");
        tfSearch.setStyle(UIStyle.customerTextFieldStyle);
        tfSearch.setOnAction(e -> cusController.doSearch(tfSearch.getText())); // pressing Enter also searches
        Button btnSearch = new Button("Search");
        btnSearch.setStyle(UIStyle.buttonStyle);
        btnSearch.setOnAction(this::buttonClicked);
        HBox hbSearch = new HBox(10, tfSearch, btnSearch);
        hbSearch.setAlignment(Pos.CENTER);

        // Search results list, using the same approach as the Warehouse client's search page
        searchResultsCopy = FXCollections.observableArrayList();
        lvSearchResults = new ListView<>(searchResultsCopy);
        lvSearchResults.setPrefHeight(130);
        lvSearchResults.setFixedCellSize(45);
        lvSearchResults.setStyle(UIStyle.listViewStyle);
        lvSearchResults.setPlaceholder(new Label("Search results appear here"));
        lvSearchResults.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                } else {
                    String imageUri = Paths.get(StorageLocation.imageFolder + product.getProductImageName())
                            .toAbsolutePath().toUri().toString();
                    ImageView ivPro;
                    try {
                        ivPro = new ImageView(new Image(imageUri, 40, 35, true, true));
                    } catch (Exception e) {
                        ivPro = new ImageView(new Image("imageHolder.jpg", 40, 35, true, true));
                    }
                    Label laPro = new Label(String.format("%s  %s\n£%.2f",
                            product.getProductId(), product.getProductDescription(), product.getUnitPrice()));
                    setGraphic(new HBox(8, ivPro, laPro));
                }
            }
        });
        // When the customer selects a result, tell the controller (but not when update() changes the list)
        lvSearchResults.getSelectionModel().selectedItemProperty().addListener((obs, oldPro, newPro) -> {
            if (!updatingFromModel && newPro != null) {
                cusController.selectProduct(newPro);
            }
        });

        Button btnAddToTrolley = new Button("Add to Trolley");
        btnAddToTrolley.setStyle(UIStyle.buttonStyle);
        btnAddToTrolley.setOnAction(this::buttonClicked);
        HBox hbBtns = new HBox(10, btnAddToTrolley);
        hbBtns.setAlignment(Pos.CENTER);

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); // Image keeps its original shape and fits inside 60×60
        ivProduct.setSmooth(true); //make it smooth and nice-looking

        lbProductText = new Label("Thank you for shopping with us.");
        lbProductText.setWrapText(true);
        lbProductText.setMinHeight(Label.USE_PREF_SIZE);  // Allow auto-resize
        lbProductText.setStyle(UIStyle.labelMulLineStyle);
        HBox hbSearchResult = new HBox(5, ivProduct, lbProductText);
        hbSearchResult.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchPage = new VBox(10, laPageTitle, hbSearch, lvSearchResults, hbSearchResult, hbBtns);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }

    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taTrolley = new TextArea();
        taTrolley.setEditable(false);
        taTrolley.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel,btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15, laPageTitle, taTrolley, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");
        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);

        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }


    private void buttonClicked(ActionEvent event) {
        Button btn = (Button)event.getSource();
        String action = btn.getText();
        if(action.equals("Add to Trolley")){
            showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
        }
        if(action.equals("OK & Close")){
            showTrolleyOrReceiptPage(vbTrolleyPage);
        }
        if(action.equals("Search")){
            cusController.doSearch(tfSearch.getText()); // the view passes the query; the model never reads the TextField
            return;
        }
        cusController.doAction(action);
    }


    /**
     * Refreshes the customer window with the model's current state.
     *
     * @param productImageName image URI for the selected product, or the placeholder image
     * @param productTextInfo  details of the selected product, or a status message
     * @param searchResults    products found by the last search
     * @param selectedProduct  the selected search result, or null if none is selected
     * @param trolley          formatted trolley contents
     * @param receipt          formatted receipt; the receipt page is shown when this is not empty
     */
    public void update(String productImageName, String productTextInfo, List<Product> searchResults,
                       Product selectedProduct, String trolley, String receipt) {
        ivProduct.setImage(new Image(productImageName));
        lbProductText.setText(productTextInfo);

        updatingFromModel = true;
        if (!searchResultsCopy.equals(searchResults)) {
            searchResultsCopy.setAll(searchResults); // only replace the list when the results have changed
        }
        if (selectedProduct == null) {
            lvSearchResults.getSelectionModel().clearSelection();
        } else {
            lvSearchResults.getSelectionModel().select(selectedProduct);
        }
        updatingFromModel = false;

        taTrolley.setText(trolley);
        if (!receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }
}
