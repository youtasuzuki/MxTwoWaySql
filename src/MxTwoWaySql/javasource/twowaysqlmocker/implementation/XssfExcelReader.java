package twowaysqlmocker.implementation;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.XMLReader;

public class XssfExcelReader {
	private File excelFile;
	private String sheetName;
	boolean includePhoneticRuns;
	private RowProcessor rowProcessor;

	public XssfExcelReader(File excelFile, String sheetName, boolean includePhoneticRuns, RowProcessor rowProcessor) {
		this.excelFile = excelFile;
		this.sheetName = sheetName;
		this.includePhoneticRuns = includePhoneticRuns;
		this.rowProcessor = rowProcessor;
	}

	public void read() throws Exception {
		try (OPCPackage pkg = OPCPackage.open(excelFile)) {
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg, includePhoneticRuns);
			XSSFReader reader = new XSSFReader(pkg);
			StylesTable styles = reader.getStylesTable();
			XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
			while (sheets.hasNext()) {
				try (InputStream is = sheets.next()) {
					if (sheetName != null && !sheets.getSheetName().equals(sheetName)) {
						continue;
					}
					DataFormatter formatter = new DataFormatter();
					SheetContentsHandler handler = new SheetHandler();
					XSSFSheetXMLHandler sheetHandler = new XSSFSheetXMLHandler(styles, null, strings, handler, formatter, false);
					XMLReader parser = XMLHelper.newXMLReader();
					parser.setContentHandler(sheetHandler);
					parser.parse(new org.xml.sax.InputSource(is));
					break;
				}
			}
		}
	}

	public interface RowProcessor {
		void processRow(int rowIndex, Map<String, String> rowData);
	}

	private class SheetHandler implements SheetContentsHandler {
		private int currentRowIndex;
		Map<String, String> rowData;
		@Override
		public void startRow(int rowNum) {
			currentRowIndex = rowNum + 1;
			rowData = new java.util.HashMap<>();
		}
		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			String columnName = cellReference.replaceAll("\\d", "");
			rowData.put(columnName, formattedValue);
		}
		@Override
		public void endRow(int rowNum) {
			rowProcessor.processRow(currentRowIndex, rowData);
		}
	}
}