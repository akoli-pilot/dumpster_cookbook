package model;

public class Unit {
    private final String name;
    private final String singular;
    private final String plural;
    private final String abbreviation;

    public Unit(String name, String singular, String plural, String abbreviation) {
        this.name = name;
        this.singular = singular;
        this.plural = plural;
        this.abbreviation = abbreviation;
    }

    public String getSingular() {
        return singular;
    }

    public String getPlural() {
        return plural;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return name; // what appears in the dropdown
    }
}
