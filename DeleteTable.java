import java.io.RandomAccessFile;

public class DeleteTable {
	public static void parseDeleteString(String deleteString) {
		System.out.println("DELETE METHOD");
		System.out.println("Parsing the string:\"" + deleteString + "\"");
		
		String[] tokens=deleteString.split(" ");
		String table = tokens[3];
		String[] tmp = deleteString.split("where");  // condition checking
		String compare_tmp = tmp[1];
		String[] compare = DavisBase.parserEquation(compare_tmp);
		if(!DavisBase.tableExists(table)){
			System.out.println("Table "+table+" does not exist.");
		}
		else
		{
			delete(table, compare);
		}
	}
	public static void delete(String table, String[] compare){
		try{
		int key = new Integer(compare[2]);

		RandomAccessFile file = new RandomAccessFile("data/"+table+".tbl", "rw");
		int no_Pages = TableImpl.pages(file);
		int page = 0;
		for(int p = 1; p <= no_Pages; p++)
			if(PageImpl.hasKey(file, p, key)& PageImpl.getPageType(file, p)==0x0D){
				page = p;
				break;
			}
		// for given query if page does not exist 
		if(page==0)
		{
			System.out.println("The given key value does not exist");
			return;
		}
		
		short[] cell_Addy = PageImpl.getCellArray(file, page);
		int k = 0;
		for(int i = 0; i < cell_Addy.length; i++)
		{
			long locate = PageImpl.getCellLoc(file, page, i);
			String[] ans = TableImpl.retrieveValues(file, locate);
			int x = new Integer(ans[0]);
			if(x!=key)
			{
				PageImpl.setCellOffset(file, page, k, cell_Addy[i]);
				k++;
			}
		}
		PageImpl.setCellNumber(file, page, (byte)k);
		
		}catch(Exception e)
		{
			System.out.println(e);
		}
		
	}

}
