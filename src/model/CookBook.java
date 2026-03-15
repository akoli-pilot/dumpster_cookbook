package model;

import java.io.Serializable;
import java.util.*;

/**
 * aggregate root for recipe management
 */
public class CookBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Recipe> recipes;

    public CookBook() {
        recipes = new ArrayList<>();
    }

    /**
     * returns a readonly view of all recipes
     */
    public List<Recipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    /**
     * finds a recipe by name using case insensitive matching
     *
     * @param name recipe name entered by the user
     * @return matching recipe when present
     */
    public Optional<Recipe> findRecipe(String name) {
        if (name == null) {
            return Optional.empty();
        }

        return recipes.stream()
                .filter(recipe -> recipe.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    /**
     * adds a new recipe when the provided name is non empty and unique
     */
    public boolean addRecipe(String name) {
        if (name == null || name.isBlank() || findRecipe(name).isPresent()) {
            return false;
        }

        recipes.add(new Recipe(name.trim()));
        return true;
    }

    public Map<String, Double> calculateIngredientsForRecipe(String recipeName, double servings) {
        Optional<Recipe> recipe = findRecipe(recipeName);

        if (recipe.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return recipe.get().calculateIngredientAmountsForServings(servings);
    }
    /**
     * renames an existing recipe while guarding against duplicate names
     */
    public boolean updateRecipeName(String existingName, String newName) {
        if (newName == null || newName.isBlank()) {
            return false;
        }

        Optional<Recipe> recipeToUpdate = findRecipe(existingName);
        if (recipeToUpdate.isEmpty()) {
            return false;
        }

        Optional<Recipe> conflict = findRecipe(newName);
        if (conflict.isPresent() && conflict.get() != recipeToUpdate.get()) {
            return false;
        }

        return recipeToUpdate.get().setName(newName);
    }

    /**
     * removes a recipe by name
     */
    public boolean removeRecipe(String name) {
        Optional<Recipe> recipe = findRecipe(name);
        if (recipe.isEmpty()) {
            return false;
        }

        recipes.remove(recipe.get());
        return true;
    }
}
