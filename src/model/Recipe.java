package model;

import java.io.Serializable;
import java.util.*;

/**
 *  model for a recipe and its required ingredients.
 */
public class Recipe implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private final List<Ingredient> ingredients;
    private String directions = "";

    /**
     * @param name user-visible recipe name
     */
    public Recipe(String name) {
        this.name = name.trim();
        ingredients = new ArrayList<>();
    }

    /**
     * adds an ingredient if name is valid, amount is positive, and no duplicate exists
     */
    public boolean addIngredient(String name, double amount, String unit) {
        if (name == null || name.isBlank() || amount <= 0 || unit == null || unit.isBlank()) {
            return false;
        }

        if (findIngredient(name).isPresent()) {
            return false;
        }

        ingredients.add(new Ingredient(name.trim(), amount, unit.trim()));
        return true;
    }

    /** returns a read-only ingredient list */
    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    /** returns recipe name */
    public String getName() {
        return name;
    }

    /** Get Directions for Recipe Display */
    public String getDirections() {
        return directions;
    }

    public void setDirections(String directions) {
        this.directions = (directions == null) ? "" : directions.trim();
    }
    /**
     * updates recipe name when non empty
     */
    public boolean setName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        this.name = name.trim();
        return true;
    }

    /**
     * finds an ingredient by name with case insensitive matching
     */
    public Optional<Ingredient> findIngredient(String ingredientName) {
        if (ingredientName == null) {
            return Optional.empty();
        }

        return ingredients.stream()
                .filter(ing -> ing.getName().equalsIgnoreCase(ingredientName.trim()))
                .findFirst();
    }

    /**
     * updates an ingredient and prevents duplicate names in the same recipe
     */
    public boolean updateIngredient(String existingName, String newName, double newAmount, String newUnit) {
        if (newName == null || newName.isBlank() || newAmount <= 0 || newUnit == null || newUnit.isBlank()) {
            return false;
        }

        Optional<Ingredient> existingIngredient = findIngredient(existingName);
        if (existingIngredient.isEmpty()) {
            return false;
        }

        Optional<Ingredient> conflictingIngredient = findIngredient(newName);
        if (conflictingIngredient.isPresent() && conflictingIngredient.get() != existingIngredient.get()) {
            return false;
        }

        Ingredient ingredient = existingIngredient.get();
        ingredient.setName(newName.trim());
        ingredient.setAmountPerServing(newAmount);
        ingredient.setUnit(newUnit.trim());
        return true;
    }

    /** removes an ingredient by name */
    public boolean removeIngredient(String ingredientName) {
        Optional<Ingredient> ingredient = findIngredient(ingredientName);
        if (ingredient.isEmpty()) {
            return false;
        }

        ingredients.remove(ingredient.get());
        return true;
    }

    /**
     * Computes the maximum number of servings possible with the supplied inventory map
     */
    public double calculateMaxServings(Map<String, Double> available) {

        if (ingredients.isEmpty()) {
            return 0;
        }

        double maxServings = Double.MAX_VALUE;

        for (Ingredient ing : ingredients) {

            if (ing.getAmountPerServing() <= 0) {
                return 0;
            }

            double availableAmount =
                    available.getOrDefault(ing.getName(), 0.0);

            double servings =
                    availableAmount / ing.getAmountPerServing();

            maxServings = Math.min(maxServings, servings);
        }

        return maxServings;
    }

    public Map<String, Double> calculateIngredientAmountsForServings(double servings)
    {
        Map<String, Double> requiredAmounts = new LinkedHashMap<>();

        if (servings <= 0)
        {
            return requiredAmounts;
        }

        for (Ingredient ingredient : ingredients)
        {
            double totalAmount = ingredient.getAmountPerServing() * servings;
            requiredAmounts.put(ingredient.getName(), totalAmount);
        }

        return requiredAmounts;
    }

    public List<String> getScaledIngredientDescriptions(double servings) {
        List<String> results = new ArrayList<>();

        if (servings <= 0) {
            return results;
        }

        for (Ingredient ingredient : ingredients) {
            double totalAmount = ingredient.getAmountPerServing() * servings;
            results.add(String.format("%.2f %s %s",
                    totalAmount,
                    ingredient.getUnit(),
                    ingredient.getName()));
        }

        return results;
    }

    /** returns recipe name for list controls. */
    @Override
    public String toString() {
        return name;
    }
}