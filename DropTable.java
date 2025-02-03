import java.io.RandomAccessFile;
import java.io.File;

public class DropTable {
	public static void dropTable(String dropTableString) {
		System.out.println("DROP METHOD");
		System.out.println("Parsing the string:\"" + dropTableString + "\"");
		
		String[] vector=dropTableString.split(" ");
		String tableName = vector[2];
		if(!DavisBase.tableExists(tableName)){//checking if table does not exist
			System.out.println("Table "+tableName+" does not exist.");
		}
		else
		{
			drop(tableName);
		}		

	}
	public static void drop(String table){
		//code snippet showing process of deleting
        try{
			
			RandomAccessFile file = new RandomAccessFile("data/davisbase_tables.tbl", "rw");
			int no_Pages = TableImpl.pages(file);
			for(int page = 1; page <= no_Pages; page ++){
				file.seek((page-1)* TableImpl.pageSize);
				byte fileType = file.readByte();
				if(fileType == 0x0D)
				{
					short[] cellAddy = PageImpl.getCellArray(file, page);
					int k = 0;
					for(int i = 0; i < cellAddy.length; i++)
					{
						long locate = PageImpl.getCellLoc(file, page, i);
						String[] values = TableImpl.retrieveValues(file, locate);
						String tbl = values[1];
						if(!tbl.equals(table))
						{
							PageImpl.setCellOffset(file, page, k, cellAddy[i]);
							k++;
						}
					}
					PageImpl.setCellNumber(file, page, (byte)k);
				}
				else
					continue;
			}

			file = new RandomAccessFile("data/davisbase_columns.tbl", "rw");
			no_Pages = TableImpl.pages(file);
			for(int page = 1; page <= no_Pages; page ++){
				file.seek((page-1)* TableImpl.pageSize);
				byte fileType = file.readByte();
				if(fileType == 0x0D)
				{
					short[] cellAddy = PageImpl.getCellArray(file, page);
					int k = 0;
					for(int i = 0; i < cellAddy.length; i++)
					{
						long locate = PageImpl.getCellLoc(file, page, i);
						String[] values = TableImpl.retrieveValues(file, locate);
						String tbl = values[1];
						if(!tbl.equals(table))
						{
							PageImpl.setCellOffset(file, page, k, cellAddy[i]);
							k++;
						}
					}
					PageImpl.setCellNumber(file, page, (byte)k);
				}
				else
					continue;
			}

			File existingFile = new File("data", table+".tbl"); 
			existingFile.delete();
		}catch(Exception e){
			System.out.println(e);
		}

	}

}