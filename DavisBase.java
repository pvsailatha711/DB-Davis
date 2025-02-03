import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class DavisBase {

    static String prompt = "PalladiumSql> ";
    static String copyright = "Â©2016 Chris Irwin Davis";
    static String version = "v1.3";
    static boolean isExit = false;
    public static int pageSize = 512;

    static Scanner scanner = new Scanner(System.in).useDelimiter(";");

    public static void main(String[] args) {
        Initialization.init();

        displaySplashScreen();

        String userCommand = "";

        while (!isExit) {
            System.out.print(prompt);
            userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase();
            parseUserCommand(userCommand);
        }
        System.out.println("Exiting...");
    }

    public static void displaySplashScreen() {
        System.out.println(line("-", 80));
        System.out.println("Welcome to DavisBaseLite");
        System.out.println("DavisBase Version " + version);
        System.out.println("Team Palladium");
        System.out.println("\nType \"help;\" to display supported commands.");
        System.out.println(line("-", 80));
    }

    public static String line(String s, int num) {
        String a = "";
        for (int i = 0; i < num; i++) {
            a += s;
        }
        return a;
    }

    /**
     *  Help: Display supported commands
     */
    public static void help() {
        System.out.println(line("*",80));
        System.out.println("COMMAND SUMMARY");
        System.out.println("All commands below are case insensitive");
        System.out.println();
        System.out.println("SOURCE filename.sql;   Process contents of file as if they were SQL commands entered manually.");
        System.out.println("CREATE TABLE table_name ( col_1 type_1, col_2 type_2, ...);");
        System.out.println("CREATE INDEX index_name ON table_name (column_name);");
        System.out.println("\tSELECT * FROM table_name;                        Display all records in the table.");
        System.out.println("\tSELECT * FROM table_name WHERE [condition];      Display records where condition is true.");
        System.out.println("\tSHOW TABLES;                                     Display all table names.");
        System.out.println("\tDROP TABLE table_name;                           Remove table file and its meta-data.");
        System.out.println("\tDROP INDEX table_name;                           Remove index file and its meta-data.");
        System.out.println("\tVERSION;                                         Show the program version.");
        System.out.println("\tHELP;                                            Show this help information");
        System.out.println("\tEXIT;                                            Exit the program");
        System.out.println();
        System.out.println();
        System.out.println(line("*",80));
    }

    /** return the DavisBase version */
    public static String getVersion() {
        return version;
    }

    public static String getCopyrightString() {
        return copyright;
    }

    public static void displayVersion() {
        System.out.println("DavisBaseLite Version " + getVersion());
        System.out.println(getCopyrightString());
    }

    public static boolean tableExists(String tableName) {
        tableName = tableName + ".tbl";

        try {
            File dataDirectory = new File("data");
            String[] prevTableFiles;
            prevTableFiles = dataDirectory.list();

            for (int i = 0; i < prevTableFiles.length; i++) {
                if (prevTableFiles[i].equals(tableName))
                    return true;
            }
        } catch (SecurityException e) {
            System.out.println("Could not list contents of data directory");
            System.out.println(e);
        }

        return false;
    }

    public static boolean indexExists(String indexFileName) {
        File indexFile = new File("data/" + indexFileName + ".ndx");
        return indexFile.exists();
    }

    public static void dropIndex(String indexFilename) {
        if (!indexExists(indexFilename)) {
            System.out.println("ERROR. The index that you are trying to delete does not exist.");
            return;
        }

        File indexFile = new File("data/" + indexFilename + ".ndx");
        if (indexFile.delete()) {
            System.out.println("Successfully deleted " + indexFilename);
        } else {
            System.out.println("ERROR. Unable to drop index file");
            return;
        }

        // remove metadata
        try {
            RandomAccessFile file = new RandomAccessFile("data/davisbase_indexes.tbl", "rw");

            long filePointer = 0;
            boolean isIndexPresent = false;
            String line = null;
            StringBuilder fileContents = new StringBuilder();

            while ((line = file.readLine()) != null) {
                if (line.contains(indexFilename)) {
                    isIndexPresent = true; // Mark the entry as found
                    continue; // Skip the line containing the index entry
                }
                fileContents.append(line).append("\n");
            }

            if (isIndexPresent) {
                file.setLength(0);
                file.seek(0);
                file.writeBytes(fileContents.toString());
                System.out.println("removed metadata from davisbase_indexes.");
            }

            file.close();
        } catch (IOException e) {
            System.out.println("There was an error while trying to drop the index.");
            e.printStackTrace();
        }
    }

    public static void parseUserCommand(String userCommand) {

		ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));
	
		switch (commandTokens.get(0)) {
			case "show":
				ShowTables.showTables();
				break;
	
			case "create":
				CreateTable.parseCreateString(userCommand);
				break;
	
			case "insert":
				InsertRows.parseInsertString(userCommand);
				break;
	
			case "delete":
				DeleteTable.parseDeleteString(userCommand);
				break;
	
			case "update":
				UpdateTable.parseUpdateString(userCommand);
				break;
	
			case "select":
				parseQueryString(userCommand);
				break;
	
			case "drop":
				if (commandTokens.get(1).equals("index")) {
					// TODO: Drop index command
					String indexName = commandTokens.get(2);
					dropIndex(indexName);
				} else if (commandTokens.get(1).equals("table")) {
					DropTable.dropTable(userCommand);
				}
				break;
	
			case "help":
				help();
				break;
	
			case "version":
				System.out.println("DavisBase Version " + version);
				System.out.println(copyright);
				break;
	
			case "exit":
				isExit = true;
				break;
	
			case "quit":
				isExit = true;
				break;
	
			default:
				System.out.println("I didn't understand the command: \"" + userCommand + "\"");
				System.out.println();
				break;
		}
	}
	

    public static void parseQueryString(String queryString) {
        String[] compareCondition;
        String[] columns;
        String[] placeholder = queryString.split("where");

        if (placeholder.length > 1) {
            String temp = placeholder[1].trim();
            compareCondition = parserEquation(temp);
        } else {
            compareCondition = new String[0];
        }

        String[] selectClause = placeholder[0].split("from");
        String tableName = selectClause[1].trim();
        String cols = selectClause[0].replace("select", "").trim();

        if (cols.contains("*")) {
            columns = new String[1];
            columns[0] = "*";
        } else {
            columns = cols.split(",");
            for (int i = 0; i < columns.length; i++)
                columns[i] = columns[i].trim();
        }

        if (!tableExists(tableName)) {
            System.out.println("Table " + tableName + " could not be found.");
        } else {
            ShowTables.select(tableName, columns, compareCondition);
        }
    }

    public static String[] parserEquation(String eqn) {
        String comparator[] = new String[3];
        String placeholder[] = new String[2];

        if (eqn.contains("=")) {
            placeholder = eqn.split("=");
            comparator[0] = placeholder[0].trim();
            comparator[1] = "=";
            comparator[2] = placeholder[1].trim();
        }

        if (eqn.contains("<")) {
            placeholder = eqn.split("<");
            comparator[0] = placeholder[0].trim();
            comparator[1] = "<";
            comparator[2] = placeholder[1].trim();
        }

        if (eqn.contains(">")) {
            placeholder = eqn.split(">");
            comparator[0] = placeholder[0].trim();
            comparator[1] = ">";
            comparator[2] = placeholder[1].trim();
        }

        if (eqn.contains("<=")) {
            placeholder = eqn.split("<=");
            comparator[0] = placeholder[0].trim();
            comparator[1] = "<=";
            comparator[2] = placeholder[1].trim();
        }

        if (eqn.contains(">=")) {
            placeholder = eqn.split(">=");
            comparator[0] = placeholder[0].trim();
            comparator[1] = ">=";
            comparator[2] = placeholder[1].trim();
        }
        return comparator;
    }
}
