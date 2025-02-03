import java.io.RandomAccessFile;

public class UpdateTable {

    public static void parseUpdateString(String updateString) {


        String[] tokens = updateString.split(" ");
        String table = tokens[1];
        String[] temp1 = updateString.split("set");
        String[] temp2 = temp1[1].split("where");
        String cmpTemp = temp2[1];
        String setTemp = temp2[0];
        String[] cmp = DavisBase.parserEquation(cmpTemp);
        String[] set = DavisBase.parserEquation(setTemp);

        // Apply parsing to remove quotes
        cmp[2] = parseValue(cmp[2]);
        set[2] = parseValue(set[2]);

        if (!DavisBase.tableExists(table)) {
            System.out.println("Table " + table + " does not exist.");
        } else {
            update(table, cmp, set);
        }
    }

    public static void update(String table, String[] cmp, String[] set) {
        try {
            int key = Integer.parseInt(cmp[2]);

            RandomAccessFile file = new RandomAccessFile("data/" + table + ".tbl", "rw");
            int numPages = TableImpl.pages(file);
            int page = 0;

            for (int p = 1; p <= numPages; p++) {
                if (PageImpl.hasKey(file, p, key) & PageImpl.getPageType(file, p) == 0x0D) {
                    page = p;
                }
            }

            if (page == 0) {
                System.out.println("The given key value does not exist");
                return;
            }

            int[] keys = PageImpl.getKeyArray(file, page);
            int x = 0;
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == key) {
                    x = i;
                }
            }

            int offset = PageImpl.getCellOffset(file, page, x);
            long loc = PageImpl.getCellLoc(file, page, x);

            String[] cols = TableImpl.getColName(table);
            String[] values = TableImpl.retrieveValues(file, loc);

            String[] type = TableImpl.getDataType(table);
            for (int i = 0; i < type.length; i++) {
                if (type[i].equals("DATE") || type[i].equals("DATETIME")) {
                    values[i] = "'" + values[i] + "'";
                }
            }

            for (int i = 0; i < cols.length; i++) {
                if (cols[i].equals(set[0])) {
                    x = i;
                }
            }

            values[x] = parseValue(set[2]); // Parse and update value to remove quotes

            String[] nullable = TableImpl.getNullable(table);
            for (int i = 0; i < nullable.length; i++) {
                if (values[i].equals("null") && nullable[i].equals("NO")) {
                    System.out.println("NULL-value constraint violation");
                    return;
                }
            }

            byte[] stc = new byte[cols.length - 1];
            int plsize = TableImpl.calPayloadSize(table, values, stc);
            PageImpl.updateLeafCell(file, page, offset, plsize, key, stc, values);

            file.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Helper method to parse and clean input values
	private static String parseValue(String value) {
		// Handle strings enclosed in single or double quotes
		if ((value.startsWith("'") && value.endsWith("'")) ||
			(value.startsWith("\"") && value.endsWith("\""))) {
			return value.substring(1, value.length() - 1);
		}

		// Allow unquoted alphanumeric strings
		if (value.matches("^[a-zA-Z0-9_]+$")) {
			return value;
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
