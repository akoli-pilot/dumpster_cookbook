package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles loading and saving cookbook data as JSON document
 */
public class CookBookStorage {

    /**
     * container for persisted app state.
     */
    public static class StoredData {
        private final CookBook cookBook;
        private final Map<String, Double> inventoryByIngredient;

        /**
         * @param cookBook cookbook graph loaded from file
         * @param inventoryByIngredient available ingredient amounts
         */
        public StoredData(CookBook cookBook, Map<String, Double> inventoryByIngredient) {
            this.cookBook = cookBook;
            this.inventoryByIngredient = new LinkedHashMap<>(inventoryByIngredient);
        }

        /** returns cookbook snapshot from file */
        public CookBook getCookBook() {
            return cookBook;
        }

        /** returns a defensive copy of inventory to protect internal state */
        public Map<String, Double> getInventoryByIngredient() {
            return new LinkedHashMap<>(inventoryByIngredient);
        }
    }

    private final Path filePath;

    public CookBookStorage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * convenience load method for callers that only care about recipes
     */
    public Optional<CookBook> load() {
        return loadData().map(StoredData::getCookBook);
    }

    /**
     * loads cookbook + inventory from disk when valid
     */
    public Optional<StoredData> loadData() {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            return parseStoredData(json);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    /** saves cookbook with an empty inventory map */
    public boolean save(CookBook cookBook) {
        return save(cookBook, new LinkedHashMap<>());
    }

    /**
     * persists cookbook and inventory to disk
     */
    public boolean save(CookBook cookBook, Map<String, Double> inventoryByIngredient) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String json = toJson(cookBook, inventoryByIngredient);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);

            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * serializes objects into a JSON string
     */
    private String toJson(CookBook cookBook, Map<String, Double> inventoryByIngredient) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"recipes\": [\n");

        List<Recipe> recipes = cookBook.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escapeJson(recipe.getName())).append("\",\n");
            sb.append("      \"ingredients\": [\n");

            List<Ingredient> ingredients = recipe.getIngredients();
            for (int j = 0; j < ingredients.size(); j++) {
                Ingredient ingredient = ingredients.get(j);
                sb.append("        {\"name\": \"")
                        .append(escapeJson(ingredient.getName()))
                        .append("\", \"amountPerServing\": ")
                        .append(ingredient.getAmountPerServing())
                        .append("}");

                if (j < ingredients.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append("      ]\n");
            sb.append("    }");

            if (i < recipes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ],\n");
        sb.append("  \"inventory\": {\n");

        List<String> inventoryNames = new ArrayList<>(inventoryByIngredient.keySet());
        inventoryNames.sort(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < inventoryNames.size(); i++) {
            String name = inventoryNames.get(i);
            double amount = inventoryByIngredient.getOrDefault(name, 0.0);
            sb.append("    \"").append(escapeJson(name)).append("\": ").append(amount);
            if (i < inventoryNames.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  }\n}");
        return sb.toString();
    }

    /** escapes special characters for safe JSON string embedding */
    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * parses root JSON into domain structures
     *
     */
    private Optional<StoredData> parseStoredData(String json) {
        try {
            JsonParser parser = new JsonParser(json);
            Object root = parser.parseValue();

            if (!(root instanceof Map<?, ?> rootMap)) {
                return Optional.empty();
            }

            Object recipesObj = rootMap.get("recipes");
            if (!(recipesObj instanceof List<?> recipeList)) {
                return Optional.empty();
            }

            CookBook cookBook = new CookBook();

            for (Object recipeObj : recipeList) {
                if (!(recipeObj instanceof Map<?, ?> recipeMap)) {
                    continue;
                }

                Object recipeNameObj = recipeMap.get("name");
                if (!(recipeNameObj instanceof String recipeName)) {
                    continue;
                }

                if (!cookBook.addRecipe(recipeName)) {
                    continue;
                }

                Object ingredientsObj = recipeMap.get("ingredients");
                if (!(ingredientsObj instanceof List<?> ingredientList)) {
                    continue;
                }

                cookBook.findRecipe(recipeName).ifPresent(recipe -> {
                    for (Object ingredientObj : ingredientList) {
                        if (!(ingredientObj instanceof Map<?, ?> ingredientMap)) {
                            continue;
                        }

                        Object ingredientNameObj = ingredientMap.get("name");
                        Object amountObj = ingredientMap.get("amountPerServing");

                        if (!(ingredientNameObj instanceof String ingredientName)) {
                            continue;
                        }

                        if (!(amountObj instanceof Number amountNumber)) {
                            continue;
                        }

                        recipe.addIngredient(ingredientName, amountNumber.doubleValue());
                    }
                });
            }

            Map<String, Double> inventoryByIngredient = new LinkedHashMap<>();
            Object inventoryObj = rootMap.get("inventory");
            if (inventoryObj instanceof Map<?, ?> inventoryMap) {
                for (Map.Entry<?, ?> entry : inventoryMap.entrySet()) {
                    if (!(entry.getKey() instanceof String ingredientName)) {
                        continue;
                    }

                    if (!(entry.getValue() instanceof Number amountNumber)) {
                        continue;
                    }

                    double amount = amountNumber.doubleValue();
                    if (amount < 0) {
                        continue;
                    }

                    inventoryByIngredient.put(ingredientName, amount);
                }
            }

            return Optional.of(new StoredData(cookBook, inventoryByIngredient));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * minimal parser for JSON structure
     */
    private static class JsonParser {
        private final String json;
        private int index;

        JsonParser(String json) {
            this.json = json;
            this.index = 0;
        }

        /** parses any JSON value at the current parser cursor */
        Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }

            char c = json.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == '-' || Character.isDigit(c)) {
                return parseNumber();
            }
            if (json.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (json.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            if (json.startsWith("null", index)) {
                index += 4;
                return null;
            }

            throw new IllegalArgumentException("Invalid JSON value");
        }

        /** parses a JSON object into key/value pairs */
        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();

            Map<String, Object> object = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return object;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    expect('}');
                    break;
                }

                expect(',');
            }

            return object;
        }

        /** parses a JSON array preserving source order */
        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();

            List<Object> array = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return array;
            }

            while (true) {
                Object value = parseValue();
                array.add(value);
                skipWhitespace();

                if (peek(']')) {
                    expect(']');
                    break;
                }

                expect(',');
            }

            return array;
        }

        /** parses a JSON string including standard escape forms and unicode escapes */
        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();

            while (index < json.length()) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }

                if (c == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Invalid escape sequence");
                    }

                    char esc = json.charAt(index++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > json.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape");
                            }
                            String hex = json.substring(index, index + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape sequence");
                    }
                } else {
                    sb.append(c);
                }
            }

            throw new IllegalArgumentException("Unterminated string");
        }

        /** parses numeric values as doubles to simplify storage in domain classes */
        private Number parseNumber() {
            int start = index;

            if (peek('-')) {
                index++;
            }

            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }

            if (peek('.')) {
                index++;
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }

            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }

            String numberText = json.substring(start, index);
            return Double.parseDouble(numberText);
        }

        /** advances cursor through whitespace between tokens */
        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        /** consumes a specific token or throws a parse error */
        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "'");
            }
            index++;
        }

        /** checks if the next non whitespace character matches the provided token */
        private boolean peek(char c) {
            skipWhitespace();
            return index < json.length() && json.charAt(index) == c;
        }
    }
}
