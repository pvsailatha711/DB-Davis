public class InsertRows {
    public static void parseInsertString(String insertString) {
        
        try {
            // Check whether the command starts with the expected keywords
            if (!insertString.trim().toUpperCase().startsWith("INSERT INTO TABLE")) {
                throw new IllegalArgumentException("Invalid INSERT statement: Expected 'INSERT INTO TABLE'.");
            }

            // Extract the table name and VALUES clause
            String[] tokens = insertString.split(" ", 5);
            if (tokens.length < 5) {
                throw new IllegalArgumentException("Invalid INSERT statement: Missing table name or values.");
            }

            String tableName = tokens[3]; // Extract table name after "INSERT INTO TABLE"
            String values = tokens[4].trim(); // Extract everything after VALUES keyword

            // Ensure the values start with "VALUES" keyword
            if (!values.toUpperCase().startsWith("VALUES")) {
                throw new IllegalArgumentException("Invalid INSERT statement: Missing 'VALUES' keyword.");
            }

            // Extract and clean the values inside parentheses
            int openParen = values.indexOf("(");
            int closeParen = values.lastIndexOf(")");
            if (openParen == -1 || closeParen == -1 || openParen >= closeParen) {
                throw new IllegalArgumentException("Values must be enclosed in parentheses.");
            }

            String valuesString = values.substring(openParen + 1, closeParen).trim();
            String[] insertValues = valuesString.split(",");
            for (int i = 0; i < insertValues.length; i++) {
                insertValues[i] = parseValue(insertValues[i].trim());
            }

            // Check if the table exists
            if (!DavisBase.tableExists(tableName)) {
                System.out.println("Table " + tableName + " does not exist.");
                return;
            }

            // Insert the values into the table
            CreateTable.insertInto(tableName, insertValues);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String parseValue(String value) {
        // Handle strings enclosed in single or double quotes
        if ((value.startsWith("'") && value.endsWith("'")) || 
            (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1); 
        }

        // Validate numeric values
        try {
            Double.parseDouble(value); // Check if it's a valid number
            return value; 
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
    }
}