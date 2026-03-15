package views;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Recipe;

public class RecipeDetailsView {

    private Stage stage;
    private Scene previousScene;
    private Recipe recipe;


    public RecipeDetailsView(Stage stage, Scene previousScene, Recipe recipe) {
        this.stage = stage;
        this.previousScene = previousScene;
        this.recipe = recipe;
    }

    public void show() {

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> stage.setScene(previousScene));

        Label title = new Label(recipe.getName());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label ingredientsLabel = new Label("Ingredients");

        ListView<String> ingredientsList = new ListView<>();

        recipe.getIngredients().forEach(ingredient ->
                ingredientsList.getItems().add(ingredient.toString())
        );

        root.getChildren().addAll(
                backButton,
                title,
                ingredientsLabel,
                ingredientsList
        );

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
    }
}
