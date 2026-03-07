package model;

import java.util.*;

public class Recipe {

    private String name;
    private List<Ingredient> ingredients;

    public Recipe(String name) {
        this.name = name;
        ingredients = new ArrayList<>();
    }

    public void addIngredient(String name, double amount) {
        ingredients.add(new Ingredient(name, amount));
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public String getName() {
        return name;
    }

    public double calculateMaxServings(Map<String, Double> available) {

        double maxServings = Double.MAX_VALUE;

        for (Ingredient ing : ingredients) {

            double availableAmount =
                    available.getOrDefault(ing.getName(), 0.0);

            double servings =
                    availableAmount / ing.getAmountPerServing();

            maxServings = Math.min(maxServings, servings);
        }

        return maxServings;
    }
}