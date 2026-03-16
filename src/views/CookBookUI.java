package views;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import model.*;

import java.nio.file.Paths;
import java.util.*;

/**
 * JavaFX frontend
 *
 */
public class CookBookUI extends Application {

    private CookBook cookBook;
    private final ObservableList<Recipe> recipes = FXCollections.observableArrayList();
    private final ObservableList<Ingredient> ingredients = FXCollections.observableArrayList();
    private final ObservableList<String> inventoryRows = FXCollections.observableArrayList();
    private final Map<String, Double> inventoryByIngredient = new HashMap<>();

    private final CookBookStorage storage = new CookBookStorage(
            Paths.get(System.getProperty("user.home"), ".dumpster-cookbook", "cookbook.json")
    );

    private ListView<Recipe> recipeListView;
    private ListView<Ingredient> ingredientListView;
    private ListView<String> inventoryListView;
    private ListView<Recipe> calculatorRecipeListView;
    private ComboBox<Recipe> recipePicker;
    private ComboBox<String> ingredientUnitField;

    private TextField recipeNameField;
    private TextField ingredientNameField;
    private TextField ingredientAmountField;
    private TextField inventoryIngredientField;
    private TextField inventoryAmountField;


    private Label recipeStatusLabel;
    private Label ingredientStatusLabel;
    private Label inventoryStatusLabel;

    private Stage primaryStage;

    /**
     * Initializes the scene graph, loads persisted data, and applies the app theme.
     */
    @Override
    public void start(Stage stage) {
        loadOrSeedData();

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("app-shell");

        Label title = new Label("Le Dumpster CookBook");
        title.getStyleClass().add("headline");

        Label subtitle = new Label("A Ghetto Style CookBook during Tryin' Times.");
        subtitle.getStyleClass().add("body");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("md-tabs");
        tabPane.getTabs().addAll(buildRecipeTab(), buildIngredientTab(), buildInventoryTab(), buildCalculatorTab());
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        root.getChildren().addAll(title, subtitle, tabPane);

        Scene scene = new Scene(root, 900, 620);
        scene.setFill(Color.web("#141218"));

        var stylesheet = getClass().getResource("material3.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        refreshRecipes(null);
        syncInventoryWithRecipeIngredients();
        refreshInventoryView(null);

        stage.setTitle("Dumpster Cookbook");
        stage.setScene(scene);
        stage.show();

        this.primaryStage = stage;
    }

    /**
     * loads persisted state or load default
     */
    private void loadOrSeedData() {
        var loadedData = storage.loadData();
        if (loadedData.isPresent()) {
            cookBook = loadedData.get().getCookBook();
            inventoryByIngredient.clear();
            inventoryByIngredient.putAll(loadedData.get().getInventoryByIngredient());
            return;
        }

        CookBook seeded = new CookBook();
        seeded.addRecipe("Pancakes");
        seeded.addRecipe("Omelette");

        seeded.findRecipe("Pancakes").ifPresent(recipe -> {
            recipe.addIngredient("Flour", 1, "cups");
            recipe.addIngredient("Milk", 2, "cups");
            recipe.addIngredient("Eggs", 1, "count");
        });

        seeded.findRecipe("Omelette").ifPresent(recipe -> {
            recipe.addIngredient("Eggs", 2, "count");
            recipe.addIngredient("Milk", 2, "cups");
            recipe.addIngredient("Cheese", 1, "cups");
        });

        cookBook = seeded;
        storage.save(cookBook, inventoryByIngredient);
    }

    /** builds the tab for creating, renaming, and deleting recipes */
    private Tab buildRecipeTab() {
        Tab tab = new Tab("Recipes");
        tab.setClosable(false);

        recipeListView = new ListView<>(recipes);
        recipeListView.setPrefWidth(320);
        recipeListView.getStyleClass().add("md-list");
        recipeListView.setCellFactory(listView -> new ListCell<>() {
            private final javafx.scene.text.Text textNode = new javafx.scene.text.Text();

            @Override
            protected void updateItem(Recipe item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // strikethrough
                textNode.setText(item.getName());
                textNode.setStrikethrough(isRecipeMissingInventory(item));
                textNode.setFill(Color.web("#e6e1e5"));
                setText(null);
                setGraphic(textNode);
            }
        });
        recipeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                recipeNameField.clear();
                return;
            }

            recipeNameField.setText(selected.getName());
            recipePicker.getSelectionModel().select(selected);
            refreshIngredients(selected);
        });

        recipeListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {

                Recipe selected = recipeListView.getSelectionModel().getSelectedItem();

                if (selected != null) {
                    Scene currentScene = primaryStage.getScene();
                    new RecipeDetailsView(
                            primaryStage,
                            currentScene,
                            selected,
                            cookBook,
                            storage,
                            inventoryByIngredient
                    ).show();
                }
            }
        });

        Label listTitle = new Label("Recipe List");
        listTitle.getStyleClass().add("title");

        VBox leftSection = new VBox(10, listTitle, recipeListView);
        leftSection.getStyleClass().add("card");
        VBox.setVgrow(recipeListView, Priority.ALWAYS);

        recipeNameField = new TextField();
        recipeNameField.setPromptText("Recipe name");
        recipeNameField.getStyleClass().add("md-input");

        Button createButton = new Button("Create");
        createButton.getStyleClass().add("filled-button");
        createButton.setOnAction(e -> createRecipe());

        Button updateButton = new Button("Update");
        updateButton.getStyleClass().add("tonal-button");
        updateButton.setOnAction(e -> updateRecipe());

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("outlined-button", "danger-button");
        deleteButton.setOnAction(e -> deleteRecipe());

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("text-button");
        clearButton.setOnAction(e -> {
            recipeListView.getSelectionModel().clearSelection();
            recipeNameField.clear();
            recipeStatusLabel.setText("");
        });

        HBox buttons = new HBox(10, createButton, updateButton, deleteButton, clearButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.getStyleClass().add("action-row");

        recipeStatusLabel = new Label();
        recipeStatusLabel.getStyleClass().add("status-text");

        Label editorTitle = new Label("Manage Recipe");
        editorTitle.getStyleClass().add("title");

        VBox rightSection = new VBox(
                12,
            editorTitle,
                recipeNameField,
                buttons,
                recipeStatusLabel
        );
        rightSection.getStyleClass().add("card");
        VBox.setVgrow(rightSection, Priority.ALWAYS);

        HBox content = new HBox(16, leftSection, rightSection);
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        content.setPadding(new Insets(12));

        tab.setContent(content);
        return tab;
    }

    /** builds the ingredient management tab scoped to the currently selected recipe */
    private Tab buildIngredientTab() {
        Tab tab = new Tab("Ingredients");
        tab.setClosable(false);

        recipePicker = new ComboBox<>(recipes);
        recipePicker.setPromptText("Select recipe");
        recipePicker.setMaxWidth(Double.MAX_VALUE);
        recipePicker.getStyleClass().add("md-input");
        recipePicker.setOnAction(e -> {
            Recipe selected = recipePicker.getSelectionModel().getSelectedItem();
            refreshIngredients(selected);
            ingredientStatusLabel.setText("");
        });

        ingredientListView = new ListView<>(ingredients);
        ingredientListView.getStyleClass().add("md-list");
        ingredientListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                ingredientNameField.clear();
                ingredientAmountField.clear();
                ingredientUnitField.setValue(null);
                return;
            }

            ingredientNameField.setText(selected.getName());
            ingredientAmountField.setText(String.valueOf(selected.getAmountPerServing()));
            ingredientUnitField.setValue(selected.getUnit());
        });

        Label recipeSelectorTitle = new Label("Recipe");
        recipeSelectorTitle.getStyleClass().add("title");

        Label ingredientListTitle = new Label("Ingredient List");
        ingredientListTitle.getStyleClass().add("title");

        VBox leftSection = new VBox(10, recipeSelectorTitle, recipePicker, ingredientListTitle, ingredientListView);
        leftSection.getStyleClass().add("card");
        VBox.setVgrow(ingredientListView, Priority.ALWAYS);

        ingredientNameField = new TextField();
        ingredientNameField.setPromptText("Ingredient name");
        ingredientNameField.getStyleClass().add("md-input");

        ingredientAmountField = new TextField();
        ingredientAmountField.setPromptText("Amount per serving");
        ingredientAmountField.getStyleClass().add("md-input");

        ingredientUnitField = new ComboBox<>();
        ingredientUnitField.getItems().addAll(
                "Cup",
                "Tablespoon",
                "Teaspoon",
                "Milliliter",
                "Liter",
                "Pint",
                "Quart",
                "Gallon",
                "Gram",
                "Kilogram",
                "Ounce",
                "Pound",
                "Count",
                "Piece",
                "Pinch",
                "Dash",
                "Slice",
                "Clove",
                "Can",
                "Package",
                "Stick"
        );
        ingredientUnitField.setPromptText("Unit of Measurement");
        ingredientUnitField.getStyleClass().add("md-input");

        Button createButton = new Button("Create");
        createButton.getStyleClass().add("filled-button");
        createButton.setOnAction(e -> createIngredient());

        Button updateButton = new Button("Update");
        updateButton.getStyleClass().add("tonal-button");
        updateButton.setOnAction(e -> updateIngredient());

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("outlined-button", "danger-button");
        deleteButton.setOnAction(e -> deleteIngredient());

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("text-button");
        clearButton.setOnAction(e -> {
            ingredientListView.getSelectionModel().clearSelection();
            ingredientNameField.clear();
            ingredientAmountField.clear();
            ingredientUnitField.setValue(null);
            ingredientStatusLabel.setText("");
        });

        HBox buttons = new HBox(10, createButton, updateButton, deleteButton, clearButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.getStyleClass().add("action-row");

        ingredientStatusLabel = new Label();
        ingredientStatusLabel.getStyleClass().add("status-text");

        Label editorTitle = new Label("Manage Ingredient");
        editorTitle.getStyleClass().add("title");

        VBox rightSection = new VBox(
                12,
            editorTitle,
                ingredientNameField,
                ingredientAmountField,
                ingredientUnitField,
                buttons,
                ingredientStatusLabel
        );
        rightSection.getStyleClass().add("card");
        VBox.setVgrow(rightSection, Priority.ALWAYS);

        HBox content = new HBox(16, leftSection, rightSection);
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        content.setPadding(new Insets(12));

        tab.setContent(content);
        return tab;
    }

    /** builds the inventory tab used to edit available ingredient amounts */
    private Tab buildInventoryTab() {
        Tab tab = new Tab("Inventory");
        tab.setClosable(false);

        inventoryListView = new ListView<>(inventoryRows);
        inventoryListView.getStyleClass().add("md-list");
        inventoryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                inventoryIngredientField.clear();
                inventoryAmountField.clear();
                return;
            }

            String ingredientName = getIngredientNameFromInventoryRow(selected);
            inventoryIngredientField.setText(ingredientName);
            inventoryAmountField.setText(String.format("%.2f", inventoryByIngredient.getOrDefault(ingredientName, 0.0)));
        });

        Label listTitle = new Label("Available Ingredients");
        listTitle.getStyleClass().add("title");

        VBox leftSection = new VBox(10, listTitle, inventoryListView);
        leftSection.getStyleClass().add("card");
        VBox.setVgrow(inventoryListView, Priority.ALWAYS);

        inventoryIngredientField = new TextField();
        inventoryIngredientField.setPromptText("Ingredient");
        inventoryIngredientField.setEditable(false);
        inventoryIngredientField.getStyleClass().add("md-input");

        inventoryAmountField = new TextField();
        inventoryAmountField.setPromptText("Available amount");
        inventoryAmountField.getStyleClass().add("md-input");

        Button setAmountButton = new Button("Set Amount");
        setAmountButton.getStyleClass().add("filled-button");
        setAmountButton.setOnAction(e -> setInventoryAmount());

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("text-button");
        clearButton.setOnAction(e -> {
            inventoryListView.getSelectionModel().clearSelection();
            inventoryIngredientField.clear();
            inventoryAmountField.clear();
            inventoryStatusLabel.setText("");
        });

        HBox buttons = new HBox(10, setAmountButton, clearButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.getStyleClass().add("action-row");

        inventoryStatusLabel = new Label();
        inventoryStatusLabel.getStyleClass().add("status-text");

        Label editorTitle = new Label("Manage Availability");
        editorTitle.getStyleClass().add("title");

        VBox rightSection = new VBox(
                12,
                editorTitle,
                inventoryIngredientField,
                inventoryAmountField,
                buttons,
                inventoryStatusLabel
        );
        rightSection.getStyleClass().add("card");
        VBox.setVgrow(rightSection, Priority.ALWAYS);

        HBox content = new HBox(16, leftSection, rightSection);
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        content.setPadding(new Insets(12));

        tab.setContent(content);
        return tab;
    }

    /** Create Calculator Tab */
    private Tab buildCalculatorTab() {
        Tab tab = new Tab("Calculator");
        tab.setClosable(false);

        HBox content = new HBox(16);
        content.setPadding(new Insets(12));

        /* LEFT SIDE (Recipe List Card) */

        calculatorRecipeListView = new ListView<>(recipes);
        calculatorRecipeListView.getStyleClass().add("md-list");

        Label listTitle = new Label("Recipes");
        listTitle.getStyleClass().add("title");

        VBox leftSection = new VBox(10, listTitle, calculatorRecipeListView);
        leftSection.getStyleClass().add("card");
        VBox.setVgrow(calculatorRecipeListView, Priority.ALWAYS);

        leftSection.setPrefWidth(320);
        leftSection.setMinWidth(280);


        /* RIGHT SIDE (Calculator Cards) */

        VBox calculators = new VBox(16,
                buildServingCalculator(calculatorRecipeListView),
                buildInventoryCalculator(calculatorRecipeListView)
        );

        VBox rightSection = new VBox(calculators);
        rightSection.getStyleClass().add("card");
        VBox.setVgrow(rightSection, Priority.ALWAYS);


        /* Layout */

        HBox.setHgrow(leftSection, Priority.NEVER);
        HBox.setHgrow(rightSection, Priority.ALWAYS);

        content.getChildren().addAll(leftSection, rightSection);

        tab.setContent(content);
        return tab;
    }

    /** handles create action for recipes */
    private void createRecipe() {
        String name = recipeNameField.getText();
        if (cookBook.addRecipe(name)) {
            refreshRecipes(name.trim());
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(null);
            persistAndReport(recipeStatusLabel, "Recipe created.");
            return;
        }

        recipeStatusLabel.setText("Unable to create recipe. Use a unique non-empty name.");
    }

    /** handles update action for selected recipe */
    private void updateRecipe() {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            recipeStatusLabel.setText("Select a recipe to update.");
            return;
        }

        String newName = recipeNameField.getText();
        String oldName = selected.getName();

        if (cookBook.updateRecipeName(oldName, newName)) {
            refreshRecipes(newName.trim());
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(null);
            persistAndReport(recipeStatusLabel, "Recipe updated.");
            return;
        }

        recipeStatusLabel.setText("Unable to update recipe. Use a unique non-empty name.");
    }

    /** handles Delete action for selected recipe and clears dependent ingredient form state */
    private void deleteRecipe() {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            recipeStatusLabel.setText("Select a recipe to delete.");
            return;
        }

        /** Delete recipe confirmation pop up */
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Recipe");
        confirm.setHeaderText("Delete Recipe: " + selected.getName() + "?");
        confirm.setContentText("Are you sure you want to delete this recipe? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String deletedName = selected.getName();
        if (cookBook.removeRecipe(deletedName)) {
            refreshRecipes(null);
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(null);
            ingredients.clear();
            ingredientListView.getSelectionModel().clearSelection();
            ingredientNameField.clear();
            ingredientAmountField.clear();
            ingredientUnitField.setValue(null);
            persistAndReport(recipeStatusLabel, "Recipe deleted.");
            return;
        }

        recipeStatusLabel.setText("Unable to delete recipe.");
    }

    /** handles create action for ingredient in selected recipe */
    private void createIngredient() {
        Recipe selectedRecipe = recipePicker.getSelectionModel().getSelectedItem();
        if (selectedRecipe == null) {
            ingredientStatusLabel.setText("Select a recipe first.");
            return;
        }

        Double amount = parseAmount();
        if (amount == null) {
            ingredientStatusLabel.setText("Amount must be a number greater than 0.");
            return;
        }

        if (selectedRecipe.addIngredient(ingredientNameField.getText(), amount, ingredientUnitField.getValue())) {
            refreshIngredients(selectedRecipe);
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(ingredientNameField.getText().trim());
            persistAndReport(ingredientStatusLabel, "Ingredient created.");
            return;
        }

        ingredientStatusLabel.setText("Unable to create ingredient. Use a unique non-empty name.");
    }

    /** handles update action for selected ingredient in selected recipe */
    private void updateIngredient() {
        Recipe selectedRecipe = recipePicker.getSelectionModel().getSelectedItem();
        Ingredient selectedIngredient = ingredientListView.getSelectionModel().getSelectedItem();

        if (selectedRecipe == null) {
            ingredientStatusLabel.setText("Select a recipe first.");
            return;
        }

        if (selectedIngredient == null) {
            ingredientStatusLabel.setText("Select an ingredient to update.");
            return;
        }

        Double amount = parseAmount();
        if (amount == null) {
            ingredientStatusLabel.setText("Amount must be a number greater than 0.");
            return;
        }

        String existingName = selectedIngredient.getName();
        String newName = ingredientNameField.getText();

        if (selectedRecipe.updateIngredient(existingName, newName, amount, ingredientUnitField.getValue())) {
            // Preserve available stock when a user only renames an ingredient.
            transferInventoryAmount(existingName, newName);
            refreshIngredients(selectedRecipe);
            selectedRecipe.findIngredient(newName).ifPresent(found -> ingredientListView.getSelectionModel().select(found));
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(newName.trim());
            persistAndReport(ingredientStatusLabel, "Ingredient updated.");
            return;
        }

        ingredientStatusLabel.setText("Unable to update ingredient. Check name uniqueness and amount.");
    }

    /** handles delete action for selected ingredient */
    private void deleteIngredient() {
        Recipe selectedRecipe = recipePicker.getSelectionModel().getSelectedItem();
        Ingredient selectedIngredient = ingredientListView.getSelectionModel().getSelectedItem();

        if (selectedRecipe == null) {
            ingredientStatusLabel.setText("Select a recipe first.");
            return;
        }

        if (selectedIngredient == null) {
            ingredientStatusLabel.setText("Select an ingredient to delete.");
            return;
        }

        /** Delete Ingredient confirmation pop up */
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Ingredient");
        confirm.setHeaderText("Delete Ingredient: " + selectedIngredient + "?");
        confirm.setContentText("Are you sure you want to delete this ingredient? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        if (selectedRecipe.removeIngredient(selectedIngredient.getName())) {
            refreshIngredients(selectedRecipe);
            syncInventoryWithRecipeIngredients();
            refreshInventoryView(null);
            ingredientNameField.clear();
            ingredientAmountField.clear();
            ingredientUnitField.setValue(null);
            persistAndReport(ingredientStatusLabel, "Ingredient deleted.");
            return;
        }

        ingredientStatusLabel.setText("Unable to delete ingredient.");
    }

    /** parses ingredient amount text box as a positive double */
    private Double parseAmount() {
        try {
            double value = Double.parseDouble(ingredientAmountField.getText().trim());
            if (value <= 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** validates and stores the inventory amount for the selected ingredient row */
    private void setInventoryAmount() {
        String selectedName = inventoryIngredientField.getText().trim();
        if (selectedName.isEmpty()) {
            inventoryStatusLabel.setText("Select an ingredient first.");
            return;
        }

        Double parsedAmount;
        try {
            parsedAmount = Double.parseDouble(inventoryAmountField.getText().trim());
        } catch (NumberFormatException ex) {
            inventoryStatusLabel.setText("Available amount must be a valid number.");
            return;
        }

        if (parsedAmount < 0) {
            inventoryStatusLabel.setText("Available amount cannot be negative.");
            return;
        }

        String canonicalName = findInventoryNameCaseInsensitive(selectedName);
        if (canonicalName == null) {
            inventoryStatusLabel.setText("Selected ingredient is not in inventory.");
            return;
        }

        inventoryByIngredient.put(canonicalName, parsedAmount);
        refreshInventoryView(canonicalName);
        refreshRecipeListInventoryFormatting();
        persistAndReport(inventoryStatusLabel, "Inventory amount updated.");
    }

    /**
     * rebuilds inventory keys from all current recipe ingredients
     */
    private void syncInventoryWithRecipeIngredients() {
        Map<String, Double> nextInventory = new HashMap<>();
        Set<String> seenNames = new HashSet<>();

        for (Recipe recipe : cookBook.getRecipes()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String name = ingredient.getName().trim();
                if (name.isEmpty()) {
                    continue;
                }

                String lowerName = name.toLowerCase(Locale.ROOT);
                if (!seenNames.add(lowerName)) {
                    continue;
                }

                Double existingAmount = getInventoryAmountCaseInsensitive(name);
                nextInventory.put(name, existingAmount == null ? 0.0 : existingAmount);
            }
        }

        inventoryByIngredient.clear();
        inventoryByIngredient.putAll(nextInventory);
        refreshRecipeListInventoryFormatting();
    }

    /**
     * returns true when at least one ingredient requirement exceeds available inventory
     */
    private boolean isRecipeMissingInventory(Recipe recipe) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            Double available = getInventoryAmountCaseInsensitive(ingredient.getName());
            if (available == null || available < ingredient.getAmountPerServing()) {
                return true;
            }
        }
        return false;
    }

    /** forces recipe list cell rerendering after inventory changes */
    private void refreshRecipeListInventoryFormatting() {
        if (recipeListView != null) {
            recipeListView.refresh();
        }
    }

    /** looks up ingredient availability by case insensitive key */
    private Double getInventoryAmountCaseInsensitive(String ingredientName) {
        String canonical = findInventoryNameCaseInsensitive(ingredientName);
        if (canonical == null) {
            return null;
        }
        return inventoryByIngredient.get(canonical);
    }

    /** finds the canonical map key for an ingredient name ignoring case */
    private String findInventoryNameCaseInsensitive(String ingredientName) {
        for (String name : inventoryByIngredient.keySet()) {
            if (name.equalsIgnoreCase(ingredientName)) {
                return name;
            }
        }
        return null;
    }

    /**
     * rebuilds inventory display rows and optionally re-selects a preferred ingredient
     */
    private void refreshInventoryView(String preferredName) {
        List<String> names = new ArrayList<>(inventoryByIngredient.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);

        inventoryRows.clear();
        for (String name : names) {
            inventoryRows.add(String.format("%s - %.2f", name, inventoryByIngredient.get(name)));
        }

        if (inventoryListView == null) {
            return;
        }

        if (preferredName == null || preferredName.isBlank()) {
            return;
        }

        for (String row : inventoryRows) {
            if (getIngredientNameFromInventoryRow(row).equalsIgnoreCase(preferredName.trim())) {
                inventoryListView.getSelectionModel().select(row);
                break;
            }
        }
    }

    /** extracts ingredient name from a formatted inventory row */
    private String getIngredientNameFromInventoryRow(String row) {
        int splitIndex = row.lastIndexOf(" - ");
        if (splitIndex < 0) {
            return row;
        }
        return row.substring(0, splitIndex);
    }

    /**
     * migrates inventory amount from old ingredient key to new key during rename
     */
    private void transferInventoryAmount(String fromName, String toName) {
        if (fromName == null || toName == null || fromName.equalsIgnoreCase(toName)) {
            return;
        }

        String existingKey = findInventoryNameCaseInsensitive(fromName);
        if (existingKey == null) {
            return;
        }

        Double existingAmount = inventoryByIngredient.get(existingKey);
        inventoryByIngredient.remove(existingKey);
        inventoryByIngredient.put(toName.trim(), existingAmount == null ? 0.0 : existingAmount);
    }

    /**
     * refreshes recipe list observable state and updates selected recipe across tabs
     */
    private void refreshRecipes(String preferredRecipeName) {
        recipes.setAll(cookBook.getRecipes());

        if (recipes.isEmpty()) {
            recipeListView.getSelectionModel().clearSelection();
            recipePicker.getSelectionModel().clearSelection();
            ingredients.clear();
            return;
        }

        Recipe recipeToSelect = null;

        if (preferredRecipeName != null) {
            for (Recipe recipe : recipes) {
                if (recipe.getName().equalsIgnoreCase(preferredRecipeName)) {
                    recipeToSelect = recipe;
                    break;
                }
            }
        }

        if (recipeToSelect == null) {
            recipeToSelect = recipes.get(0);
        }

        recipeListView.getSelectionModel().select(recipeToSelect);
        recipePicker.getSelectionModel().select(recipeToSelect);
        refreshIngredients(recipeToSelect);

        if(calculatorRecipeListView!=null){
            calculatorRecipeListView.refresh();
        }
    }

    /** updates ingredient list for selected recipe */
    private void refreshIngredients(Recipe selectedRecipe) {
        if (selectedRecipe == null) {
            ingredients.clear();
            return;
        }

        ingredients.setAll(selectedRecipe.getIngredients());

        if(calculatorRecipeListView!=null){
            calculatorRecipeListView.refresh();
        }
    }

    /**
     * persists state and updates status text with warning when save fails
     */
    private void persistAndReport(Label statusLabel, String successMessage) {
        if (storage.save(cookBook, inventoryByIngredient)) {
            statusLabel.setText(successMessage);
            return;
        }

        statusLabel.setText(successMessage + " Warning: failed to save data to disk.");
    }

    /** Servings Calculator */
    private VBox buildServingCalculator(ListView<Recipe> recipeList) {

        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label("Serving Calculator");
        title.getStyleClass().add("title");

        TextField servingsInput = new TextField();
        servingsInput.setPromptText("Desired servings");
        servingsInput.getStyleClass().add("md-input");

        VBox results = new VBox(5);

        Button calculate = new Button("Calculate");
        calculate.getStyleClass().add("filled-button");

        calculate.setOnAction(e -> {

            Recipe recipe = recipeList.getSelectionModel().getSelectedItem();
            if (recipe == null) return;

            results.getChildren().clear();

            String text = servingsInput.getText();

            /** Empty input check */
            if (text.isBlank()) {
                Label error = new Label("Enter a serving amount");
                error.getStyleClass().add("body");
                results.getChildren().add(error);
                return;
            }

            double servings;

            try {
                servings = Double.parseDouble(text);
                if (servings <= 0) {
                    Label error = new Label("Servings must be greater than 0");
                    error.getStyleClass().add("body");
                    results.getChildren().add(error);
                    return;
                }
            } catch (NumberFormatException ex) {
                Label error = new Label("Servings must be a number");
                error.getStyleClass().add("body");
                results.getChildren().add(error);
                return;
            }

            Map<String, Double> scaledAmounts = recipe.calculateIngredientAmountsForServings(servings);

            for (Ingredient i : recipe.getIngredients()) {
                Double scaled = scaledAmounts.get(i.getName());

                if (scaled == null) {
                    continue;
                }

                Label label = new Label(
                        String.format("%.2f %s %s", scaled, i.getUnit(), i.getName())
                );
                label.getStyleClass().add("body");
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);

                results.getChildren().add(label);
            }

        });

        recipeList.getSelectionModel().selectedItemProperty().addListener((obs,o,n)-> {
                    servingsInput.clear();
                    results.getChildren().clear();
                });
        card.getChildren().addAll(
                title,
                servingsInput,
                calculate,
                results
        );

        return card;
    }

    /** Inventory Calculator */
    private VBox buildInventoryCalculator(ListView<Recipe> recipeList) {

        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label("Inventory Calculator");
        title.getStyleClass().add("title");

        VBox ingredientInputs = new VBox(6);

        Map<Ingredient, TextField> ingredientFields = new HashMap<>();

        Label result = new Label();
        result.getStyleClass().add("body");

        Button calculate = new Button("Calculate Servings");
        calculate.getStyleClass().add("filled-button");

        calculate.setOnAction(e -> {

            Recipe recipe = recipeList.getSelectionModel().getSelectedItem();
            if (recipe == null) return;

            Map<String, Double> available = new HashMap<>();

            for (Ingredient i : recipe.getIngredients()) {
                TextField field = ingredientFields.get(i);

                if (field == null || field.getText().isBlank()) {
                    result.setText("All ingredient amounts must be filled.");
                    return;
                }

                double inventory;

                try {
                    inventory = Double.parseDouble(field.getText());
                } catch (NumberFormatException ex) {
                    result.setText("Ingredients must be a number");
                    return;
                }

                if (inventory < 0) {
                    result.setText("Ingredient amounts cannot be negative.");
                    return;
                }

                available.put(i.getName(), inventory);
            }

            double maxServings = recipe.calculateMaxServings(available);
            int confirmed = (int) Math.floor(maxServings);

            result.setText(
                    "Servings: " + confirmed +
                            " (" + String.format("%.2f", maxServings) + " possible)"
            );
        });

        recipeList.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->{

            ingredientInputs.getChildren().clear();
            ingredientFields.clear();
            result.setText("");

            if (n == null) return;

            for (Ingredient i : n.getIngredients()) {

                Label label = new Label(i.getName()+ " (" + i.getUnit() + ")");
                label.getStyleClass().add("field-label");
                label.setPrefWidth(140);
                label.setMinWidth(140);
                label.setWrapText(true);

                TextField input = new TextField();
                input.getStyleClass().add("md-input");
                ingredientFields.put(i, input);

                HBox row = new HBox(10,label,input);
                row.setAlignment(Pos.CENTER_LEFT);

                ingredientInputs.getChildren().add(row);
            }
        });

        card.getChildren().addAll(
                title,
                ingredientInputs,
                calculate,
                result
        );

        return card;
    }
}
