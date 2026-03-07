package model;

public class Ingredient {

    private String name;
    private double amountPerServing;

    public Ingredient(String name, double amountPerServing) {
        this.name = name;
        this.amountPerServing = amountPerServing;
    }

    public String getName() {
        return name;
    }

    public double getAmountPerServing() {
        return amountPerServing;
    }
}