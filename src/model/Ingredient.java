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
        return amountPerServing + " " + unit + " " + name;
    }
}