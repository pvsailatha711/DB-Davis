import java.io.RandomAccessFile;

public class CreateTable {

    public static void parseCreateString(String createString) {
        String[] tokens = createString.split(" ");

        if (tokens[1].compareTo("index") == 0) {
            String indexName = tokens[2];
            String tableName = tokens[3];
            String col = tokens[4];
            String colName = col.substring(1, col.length() - 1);

            Index.createIndex(tableName, indexName, colName, "String");
        } else {

            if (tokens[1].compareTo("table") > 0) {
                System.out.println("Wrong syntax");
            } else {
                String tableName = tokens[2];
                String[] temp = createString.split(tableName);
                String cols = temp[1].trim();
                String[] create_cols = cols.substring(1, cols.length() - 1).split(",");

                for (int i = 0; i < create_cols.length; i++)
                    create_cols[i] = create_cols[i].trim();

                if (DavisBase.tableExists(tableName)) {
                    System.out.println("Table " + tableName + " already exists.");
                } else {
                    createTable(tableName, create_cols);
                }
            }
        }
    }

    public static void createTable(String table, String[] col) {
        try {
            RandomAccessFile file = new RandomAccessFile("data/" + table + ".tbl", "rw");
            file.setLength(TableImpl.pageSize);
            file.seek(0);
            file.writeByte(0x0D);
            file.close();

            file = new RandomAccessFile("data/davisbase_tables.tbl", "rw");
            int numOfPages = TableImpl.pages(file);
            int page = 1;
            for (int p = 1; p <= numOfPages; p++) {
                int rm = PageImpl.getRightMost(file, p);
                if (rm == 0)
                    page = p;
            }

            int[] keys = PageImpl.getKeyArray(file, page);
            int l = keys[0];
            for (int i = 0; i < keys.length; i++)
                if (keys[i] > l)
                    l = keys[i];
            file.close();

            String[] values = {Integer.toString(l + 1), table};
            insertInto("davisbase_tables", values);

            file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
            numOfPages = TableImpl.pages(file);
            page = 1;
            for (int p = 1; p <= numOfPages; p++) {
                int rm = PageImpl.getRightMost(file, p);
                if (rm == 0)
                    page = p;
            }

            keys = PageImpl.getKeyArray(file, page);
            l = keys[0];
            for (int i = 0; i < keys.length; i++)
                if (keys[i] > l)
                    l = keys[i];
            file.close();

            String[] rowidColumn = {"rowid INTEGER NOT NULL"};

            String[] create_cols_with_rowid = concatenate(rowidColumn, col);

            for (int i = 0; i < create_cols_with_rowid.length; i++) {
                l = l + 1;
                String[] token = create_cols_with_rowid[i].split(" ");
                String col_name = token[0];
                String dt = token[1].toUpperCase();
                String pos = Integer.toString(i + 1);
                String nullable = "YES";
                if (token.length > 2) {
                    nullable = "NO";
                }

                if (col_name.equals("rowid")) {
                    continue;
                }
                if (token.length == 3 && token[2].equals("PRIMARY") && i == 0) {
                    System.out.println("PRIMARY KEY constraint violation: only one column can be PRIMARY KEY.");
                    return;
                }

                String[] value = {Integer.toString(l), table, col_name, dt, pos, nullable};
                insertInto("davisbase_columns", value);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String[] concatenate(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static void insertInto(String table, String[] values) {
        try {
            RandomAccessFile file = new RandomAccessFile("data/" + table + ".tbl", "rw");
            insertInto(file, table, values);
            file.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void insertInto(RandomAccessFile file, String table, String[] values) {
        String[] dtype = TableImpl.getDataType(table);
        String[] nullable = TableImpl.getNullable(table);

        for (int i = 0; i < nullable.length; i++)
            if (values[i].equals("null") && nullable[i].equals("NO")) {
                System.out.println("NULL-value constraint violation");
                System.out.println();
                return;
            }

        int key = new Integer(values[0]);
        int page = TableImpl.searchKeyPage(file, key);
        if (page != 0)
            if (PageImpl.hasKey(file, page, key)) {
                System.out.println("Uniqueness constraint violation");
                return;
            }
        if (page == 0)
            page = 1;

        byte[] stc = new byte[dtype.length - 1];
        short plSize = (short) TableImpl.calPayloadSize(table, values, stc);
        int cellSize = plSize + 6;
        int offset = PageImpl.checkLeafSpace(file, page, cellSize);

        if (offset != -1) {
            PageImpl.insertLeafCell(file, page, offset, plSize, key, stc, values);
        } else {
            PageImpl.splitLeaf(file, page);
            insertInto(file, table, values);
        }
    }
}
