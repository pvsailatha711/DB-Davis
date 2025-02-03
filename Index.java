import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Index {

    public static void createIndex(String tableName, String indexName, String colName, String dType) {
        // Initialize record size and serial code based on data type
        int serialCode = 0;
        int recordSize = 0;

        if (dType.equalsIgnoreCase("int")) {
            recordSize = recordSize + 4;
            serialCode = 0x06;
        } else if (dType.equalsIgnoreCase("tinyint")) {
            recordSize = recordSize + 1;
            serialCode = 0x04;
        } else if (dType.equalsIgnoreCase("smallint")) {
            recordSize = recordSize + 2;
            serialCode = 0x05;
        } else if (dType.equalsIgnoreCase("bigint")) {
            recordSize = recordSize + 8;
            serialCode = 0x07;
        } else if (dType.equalsIgnoreCase("real")) {
            recordSize = recordSize + 4;
            serialCode = 0x08;
        } else if (dType.equalsIgnoreCase("double")) {
            recordSize = recordSize + 8;
            serialCode = 0x09;
        } else if (dType.equalsIgnoreCase("datetime")) {
            recordSize = recordSize + 8;
            serialCode = 0x0A;
        } else if (dType.equalsIgnoreCase("date")) {
            recordSize = recordSize + 8;
            serialCode = 0x0B;
        } else if (dType.equalsIgnoreCase("text")) {
            // Text data type does not need a fixed size
            serialCode = 0x0C;
        }

        // Set ordinal positions
        int ordinal_pos = 0;
        ArrayList<String> ord_position = new ArrayList<>();

        // Initialize ord_position with "." placeholders
        for (int o = 0; o < 16; o++) {
            ord_position.add(".");
        }

        try {
            // Create index file with the name of the index (indexName.ndx)
            RandomAccessFile colFile1 = new RandomAccessFile("data/" + indexName + ".ndx", "rw");

            // Set the length of the file to 512 bytes (standard page size)
            colFile1.setLength(512);

            // Seek to the beginning of the file and write initial values
            colFile1.seek(0);

            // Write the serial code (used for data type)
            colFile1.writeByte(serialCode);

            // Write a default value for null (assuming a placeholder value)
            int null_val = 1; // This could indicate whether null values are allowed or not
            colFile1.writeByte(null_val);

            // Write ordinal position (placeholder for now)
            colFile1.writeByte(ordinal_pos);

            // Write a placeholder value for the next byte
            colFile1.writeByte(0x00);

            // Write the file length (for example, writing 0x10 as a placeholder)
            colFile1.writeByte(0x10);

            colFile1.close();
            System.out.println("Index " + indexName + " created successfully for table " + tableName);
        } catch (Exception e) {
            System.out.println("Error creating index: " + e.getMessage());
        }
    }

    public static void dropIndex(String indexName) {
        if (!indexExists(indexName)) {
            System.out.println("Error: Index " + indexName + " does not exist.");
            return;
        }

        // Delete the .ndx index file
        File indexFile = new File("data/" + indexName + ".ndx");
        if (indexFile.delete()) {
            System.out.println("Index file " + indexName + ".ndx deleted successfully.");
        } else {
            System.out.println("Error: Unable to delete index file " + indexName + ".ndx.");
            return;
        }

        // Remove associated metadata from the data dictionary
        try {
            RandomAccessFile file = new RandomAccessFile("data/davisbase_indexes.tbl", "rw");

            // Scan through the index metadata in davisbase_indexes.tbl and remove the entry corresponding to the index
            long filePointer = 0;
            boolean indexFound = false;
            StringBuilder fileContent = new StringBuilder();

            while (filePointer < file.length()) {
                file.seek(filePointer);
                String currentLine = file.readLine();
                if (currentLine != null && currentLine.contains(indexName)) {
                    indexFound = true;
                    continue; // Skip this line to remove the index metadata
                }
                fileContent.append(currentLine).append("\n");
                filePointer = file.getFilePointer();
            }

            if (indexFound) {
                // Write the updated content back to the file, excluding the removed index
                file.setLength(0);  // Clear the file
                file.writeBytes(fileContent.toString());
                System.out.println("Index metadata removed from davisbase_indexes.");
            } else {
                System.out.println("Error: Index metadata not found.");
            }
            file.close();
        } catch (IOException e) {
            System.out.println("Error removing index metadata from the data dictionary.");
            e.printStackTrace();
        }
    }

    // Method to check if an index exists
    public static boolean indexExists(String indexName) {
        File indexFile = new File("data/" + indexName + ".ndx");
        return indexFile.exists();
    }
}
