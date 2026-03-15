package model;

import java.io.Serializable;

/**
 * represents a single ingredient requirement for one serving of a recipe
 */
public class Ingredient implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private double amountPerServing;
    private String unit;

    /**
     * @param name ingredient display name
     * @param amountPerServing amount consumed to produce one serving
     * @param unit measurment unit (cups, tbsp, tsp, etc)
     */
    public Ingredient(String name, double amountPerServing, String unit) {
        this.name = name;
        this.amountPerServing = amountPerServing;
        this.unit = unit;
    }

    /** returns the ingredient name */
    public String getName() {
        return name;
    }

    /** updates the ingredient name */
    public void setName(String name) {
        this.name = name;
    }

    /** returns amount needed per serving */
    public double getAmountPerServing() {
        return amountPerServing;
    }

    /** updates amount needed per serving */
    public void setAmountPerServing(double amountPerServing) {
        this.amountPerServing = amountPerServing;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /** returns a concise, user facing description string */
    @Override
    public String toString() {

        String displayUnit = unit.toLowerCase();

        if (amountPerServing != 1) {
            switch (unit) {
                case "Cup": displayUnit = "cups"; break;
                case "Tablespoon": displayUnit = "tbsp"; break;
                case "Teaspoon": displayUnit = "tsp"; break;
                case "Milliliter": displayUnit = "ml"; break;
                case "Liter": displayUnit = "L"; break;
                case "Pint": displayUnit = "pt"; break;
                case "Quart": displayUnit = "qt"; break;
                case "Gallon": displayUnit = "gal"; break;
                case "Gram": displayUnit = "g"; break;
                case "Kilogram": displayUnit = "kg"; break;
                case "Ounce": displayUnit = "oz"; break;
                case "Pound": displayUnit = "lb"; break;
                case "Count": displayUnit = "counts"; break;
                case "Piece": displayUnit = "pieces"; break;
                case "Slice": displayUnit = "slices"; break;
                case "Clove": displayUnit = "cloves"; break;
                case "Can": displayUnit = "cans"; break;
                case "Package": displayUnit = "packages"; break;
                case "Stick": displayUnit = "sticks"; break;
            }
        }
        return amountPerServing + " " + displayUnit + " " + name;
    }
}