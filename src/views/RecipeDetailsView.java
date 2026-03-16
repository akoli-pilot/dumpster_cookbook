package views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Recipe;

public class RecipeDetailsView {

    private Stage stage;
    private Parent previousRoot;
    private Recipe recipe;


    public RecipeDetailsView(Stage stage, Scene previousScene, Recipe recipe) {
        this.stage = stage;
        this.previousRoot = previousScene.getRoot();
        this.recipe = recipe;
    }

    public void show() {

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("screen");
        root.setMaxWidth(900);
        root.setAlignment(Pos.TOP_CENTER);

        StackPane appShell = new StackPane(root);
        appShell.getStyleClass().add("app-shell");

        ScrollPane scrollPane = new ScrollPane(appShell);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");

        Button backButton = new Button("Back");
        backButton.getStyleClass().addAll("button", "text-button");
        backButton.setOnAction(e -> {
            stage.getScene().setRoot(previousRoot);
        });

        HBox topBar = new HBox(backButton);
        topBar.getStyleClass().add("top-bar");

        Label title = new Label(recipe.getName());
        title.getStyleClass().add("headline");

        Label ingredientsLabel = new Label("Ingredients");
        ingredientsLabel.getStyleClass().add("field-label");

        ListView<String> ingredientsList = new ListView<>();
        ingredientsList.getStyleClass().add("md-list");

        recipe.getIngredients().forEach(i ->
                ingredientsList.getItems().add(i.toString())
        );

        VBox ingredientsCard = new VBox(10, ingredientsLabel, ingredientsList);
        ingredientsCard.getStyleClass().add("card");
        ingredientsCard.setMaxWidth(700);

        Label directionsLabel = new Label("Directions");
        directionsLabel.getStyleClass().add("field-label");

        TextArea directionsArea = new TextArea(recipe.getDirections());
        directionsArea.setWrapText(true);
        directionsArea.setEditable(true);
        directionsArea.getStyleClass().add("md-input");
        directionsArea.setStyle("-fx-control-inner-background: #211f26;");

        Button saveButton = new Button("Save Directions");
        saveButton.getStyleClass().add("filled-button");

        saveButton.setOnAction(e -> {
            recipe.setDirections(directionsArea.getText());
        });

        VBox directionsCard = new VBox(10, directionsLabel, directionsArea);
        directionsCard.getStyleClass().add("card");
        directionsCard.setMaxWidth(700);

        root.getChildren().addAll(
                topBar,
                title,
                ingredientsCard,
                directionsCard,
                saveButton

        );
        Scene scene = stage.getScene();
        scene.setRoot(scrollPane);

    }
}
