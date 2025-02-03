import java.io.RandomAccessFile;
public class ShowTables{

public static void showTables() {

		
		String table = "davisbase_tables";
		String[] cols = {"table_name"};
		String[] cmptr = new String[0];
		select(table, cols, cmptr); 
	}

public static void select(String table, String[] cols, String[] cmp){     //method for select command
	try{
		
		RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
		String[] columnName = TableImpl.getColName(table);
		String[] type = TableImpl.getDataType(table);
		
		Buffer buffer = new Buffer();
		
		TableImpl.filter(file, cmp, columnName, type, buffer);
		buffer.display(cols);
		file.close();
	}catch(Exception e){
		System.out.println(e);
	}
}

}
