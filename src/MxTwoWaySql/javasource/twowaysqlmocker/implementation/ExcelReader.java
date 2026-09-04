package twowaysqlmocker.implementation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

public class ExcelReader {

	public static java.util.List<IMendixObject> readExcelFromFile(IContext context, String excelFilePath, String recordEntityType, String sheetName, String timeZoneId) throws Exception {
		List<IMendixObject> resultList = new ArrayList<IMendixObject>();
		XssfExcelReader.RowProcessor myRowProcessor = new XssfExcelRowProcessor() {
			@Override
			public void processRow(int rowIndex, Map<String, String> rowData) {
				if (rowIndex == 1) {
					// This is the header row, set up the column name map
					setupColumnNameMap(rowData);
					return;
				}
				IMendixObject newObject = createIMendixObject(context, recordEntityType, rowData, timeZoneId);
				resultList.add(newObject);
				return;
			}
		};
		String replacedFilePath = excelFilePath.replace("$HOME", System.getProperty("user.home")).replace("$RESOURCES", Core.getConfiguration().getResourcesPath().getAbsolutePath());
		XssfExcelReader excelReader = new XssfExcelReader(new File(replacedFilePath), sheetName, false, myRowProcessor);
		excelReader.read();
		return resultList;
	}

}
