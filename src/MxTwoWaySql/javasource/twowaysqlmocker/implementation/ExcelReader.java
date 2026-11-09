package twowaysqlmocker.implementation;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;
import twowaysql.implementation.CSVFormatUtil;
import twowaysql.implementation.FakeMendixObject;
import twowaysql.proxies.CsvFormat;

public class ExcelReader {

	public static java.util.List<IMendixObject> readExcelFromFileToEntityList(IContext context, String excelFilePath,
			String recordEntityType, String sheetName, String timeZoneId) throws Exception {
		List<IMendixObject> resultList = new ArrayList<IMendixObject>();
		XssfExcelReader.RowProcessor myRowProcessor = new XssfExcelRowProcessor() {
			@Override
			public void processRow(int rowIndex, Map<String, String> rowData) {
				if (rowIndex == 1) {
					// This is the header row, set up the column name map
					setupColumnNameMap(rowData);
					return;
				}
				if (recordEntityType == null) {
					// If recordEntityType is null, we are only counting rows, so just add a dummy object
					resultList.add(new FakeMendixObject());
				} else {
					resultList.add(createIMendixObject(context, recordEntityType, rowData, timeZoneId));
				}
				return;
			}
		};

		//String replacedFilePath = excelFilePath.replace("$HOME", System.getProperty("user.home")).replace("$RESOURCES",
		//		Core.getConfiguration().getResourcesPath().getAbsolutePath());
		//String twsMockerEnv = System.getenv("TWSMOCKER") != null ? System.getenv("TWSMOCKER") : "";
		//String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : "";
		//String resourcesPath = Core.getConfiguration().getResourcesPath() != null
		//		? Core.getConfiguration().getResourcesPath().getAbsolutePath()
		//		: "";
		//String replacedFilePath = excelFilePath.replace("\\", "/").replace("$TWSMOCKER", twsMockerEnv)
		//		.replace("$HOME", homeDir)
		//		.replace("$RESOURCES", resourcesPath);
		String replacedFilePath = computeFilePath(excelFilePath);
		XssfExcelReader excelReader = new XssfExcelReader(new File(replacedFilePath), sheetName, false,
				myRowProcessor);
		excelReader.read();
		return resultList;
	}

	public static String computeFilePath(String filePath) {
		String twsMockerEnv = System.getenv("TWSMOCKER") != null ? System.getenv("TWSMOCKER") : "";
		String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : "";
		String resourcesPath = Core.getConfiguration().getResourcesPath() != null
				? Core.getConfiguration().getResourcesPath().getAbsolutePath()
				: "";
		String replacedFilePath = filePath.replace("\\", "/").replace("$TWSMOCKER", twsMockerEnv)
				.replace("$HOME", homeDir)
				.replace("$RESOURCES", resourcesPath);
		return replacedFilePath;
	}

	public static void loadFileToFileDocument(String filePath, FileDocument fileDocument) throws Exception {
		String replacedFilePath = computeFilePath(filePath);
		File file = new File(replacedFilePath);
		if (!file.exists()) {
			throw new Exception("Excel file not found: " + replacedFilePath);
		}
		try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
			Core.storeFileDocumentContent(
					fileDocument.getContext(), fileDocument.getMendixObject(), in);
		}
	}

	public static void loadExcelFromFileToFileDocumentAsCSV(String excelFilePath,
			FileDocument fileDocument, CsvFormat csvFormat, String characterSet) throws Exception {
		final CSVFormat cmnCsvFormat = CSVFormatUtil.setupCSVFormat(csvFormat);
		File tmpFile = File.createTempFile("export-", ".csv");
		try {
			try (final Writer writer = new BufferedWriter(new OutputStreamWriter(//
					new FileOutputStream(tmpFile), characterSet != null ? characterSet : "UTF-8"));
					final CSVPrinter csvPrinter = new CSVPrinter(writer, cmnCsvFormat)) {
				XssfExcelReader.RowProcessor myRowProcessor = new XssfExcelRowProcessor() {
					private List<String> headerRow = new ArrayList<>();
					@Override
					public void processRow(int rowIndex, Map<String, String> rowData) {
						try {
							if (rowIndex == 1) {
								headerRow = new ArrayList<>(rowData.keySet());
							}
							for (String header : headerRow) {
								csvPrinter.print(rowData.get(header));
							}
							csvPrinter.println();
							//csvPrinter.printRecord(rowData.values().stream().toList());
						} catch (Exception e) {
							throw new RuntimeException("Error writing CSV record: " + e.getMessage(), e);
						}
					}
				};
				String replacedFilePath = computeFilePath(excelFilePath);
				XssfExcelReader excelReader = new XssfExcelReader(new File(replacedFilePath), null, false,
						myRowProcessor);
				excelReader.read();
			}
			String csvFileName = "";
			if (fileDocument.getName() == null || fileDocument.getName().isEmpty()) {
				csvFileName = tmpFile.getName();
			} else {
				csvFileName = fileDocument.getName();
			}
			Core.storeFileDocumentContent(fileDocument.getContext(), fileDocument.getMendixObject(), csvFileName,
					new BufferedInputStream(new FileInputStream(tmpFile)));
		} finally {
			tmpFile.delete();
		}
		return;
	}

}
