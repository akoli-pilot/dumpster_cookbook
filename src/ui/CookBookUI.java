package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import model.*;

import java.util.*;

public class CookBookUI extends Application {

    private Recipe recipe;
    private Map<String, TextField> ingredientInputs = new HashMap<>();

    @Override
    public void start(Stage stage) {

        recipe = new Recipe("Pancakes");

        recipe.addIngredient("Flour", 100);
        recipe.addIngredient("Milk", 200);
        recipe.addIngredient("Eggs", 1);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Interactive Cookbook");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label recipeLabel = new Label("Recipe: " + recipe.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        for (Ingredient ing : recipe.getIngredients()) {

            Label nameLabel = new Label(
                    ing.getName() + " (per serving: " +
                            ing.getAmountPerServing() + ")"
            );

            TextField input = new TextField();

            ingredientInputs.put(ing.getName(), input);

            grid.add(nameLabel, 0, row);
            grid.add(input, 1, row);

            row++;
        }

        Button calculateButton = new Button("Calculate Servings");

        Label resultLabel = new Label("Max servings: ");

        calculateButton.setOnAction(e -> {

            Map<String, Double> available = new HashMap<>();

            for (String name : ingredientInputs.keySet()) {

                try {

                    double value = Double.parseDouble(
                            ingredientInputs.get(name).getText()
                    );

                    available.put(name, value);

                } catch (Exception ignored) {}
            }

            double servings =
                    recipe.calculateMaxServings(available);

            resultLabel.setText(
                    "Max servings possible: " +
                            String.format("%.2f", servings)
            );
        });

        root.getChildren().addAll(
                title,
                recipeLabel,
                grid,
                calculateButton,
                resultLabel
        );

        Scene scene = new Scene(root, 400, 350);

        stage.setTitle("CookBook");
        stage.setScene(scene);
        stage.show();
    }
}