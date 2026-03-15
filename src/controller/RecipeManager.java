package controller;

import model.Recipe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

public class RecipeManager implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final List<Recipe> recipes;

    public RecipeManager()
    {
        this.recipes = new ArrayList<>();
    }

    public boolean addRecipe(String name)
    {
        if (name == null || name.isBlank())
        {
            return false;
        }

        String trimmedName = name.trim();

        if (findRecipe(trimmedName).isPresent())
        {
            return false;
        }

        recipes.add(new Recipe(trimmedName));
        return true;
    }

    public Optional<Recipe> findRecipe(String name)
    {
        if (name == null)
        {
            return Optional.empty();
        }

        String trimmedName = name.trim();

        return recipes.stream()
                .filter(recipe -> recipe.getName().equalsIgnoreCase(trimmedName))
                .findFirst();
    }

    public boolean updateRecipeName(String oldName, String newName)
    {
        if (oldName == null || newName == null || oldName.isBlank() || newName.isBlank())
        {
            return false;
        }

        Optional<Recipe> recipeOptional = findRecipe(oldName);
        if (recipeOptional.isEmpty())
        {
            return false;
        }

        Optional<Recipe> conflictingRecipe = findRecipe(newName);
        if (conflictingRecipe.isPresent() && conflictingRecipe.get() != recipeOptional.get())
        {
            return false;
        }

        return recipeOptional.get().setName(newName.trim());
    }

    public boolean deleteRecipe(String name)
    {
        Optional<Recipe> recipeOptional = findRecipe(name);
        if (recipeOptional.isEmpty())
        {
            return false;
        }

        recipes.remove(recipeOptional.get());
        return true;
    }

    public List<Recipe> getRecipes()
    {
        return Collections.unmodifiableList(recipes);
    }

    public int size()
    {
        return recipes.size();
    }

    public boolean isEmpty()
    {
        return recipes.isEmpty();
    }

    public void clear()
    {
        recipes.clear();
    }
}
