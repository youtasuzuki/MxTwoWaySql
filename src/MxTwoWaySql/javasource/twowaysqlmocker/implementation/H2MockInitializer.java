package twowaysqlmocker.implementation;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive.PrimitiveType;

import twowaysql.implementation.CriteriaMappingRegistry.MockMapping;
import twowaysql.integration.ExtDataSourceBinder;
import twowaysql.proxies.constants.Constants;

public class H2MockInitializer {
	public static final ILogNode logger = Core.getLogger("TwoWaySql");

	// Fixed data source name for internal use only
	public static final String INTERNAL_MOCK_DS_NAME = "__MOCK_INTERNAL_H2__";

	private static final Set<String> loadedTables = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static volatile JdbcDataSource internalDataSource = null;

	/**
	 * Initializes a single H2 data source, creates the table, and returns the name of the table to be queried.
	 * Note: This is invoked via reflection.
	 */
	public static synchronized String prepareMockTable(MockMapping mockMapping) throws Exception {
		// 1. Creating and binding an internal H2 data source
		if (ExtDataSourceBinder.getExtDataSource(INTERNAL_MOCK_DS_NAME) == null) {
			internalDataSource = new JdbcDataSource();
			internalDataSource.setURL("jdbc:h2:mem:twowaysql_mocker_internal;DB_CLOSE_DELAY=-1");
			internalDataSource.setUser("sa");
			internalDataSource.setPassword("");

			ExtDataSourceBinder.putExtDataSource(INTERNAL_MOCK_DS_NAME, (DataSource)internalDataSource);
			ExtDataSourceBinder.putExtDataSourceOptions(INTERNAL_MOCK_DS_NAME, Constants.getEXDS_WO_RSLT_TZ_ADJST() + Constants.getEXDS_WO_PRAM_TZ_ADJST());
			logger.info("Registered internal H2 DataSource: " + INTERNAL_MOCK_DS_NAME);
		}

		String h2TableName = mockMapping.getH2TableName();

		// 2. If the table has not yet been loaded, stream the data in from Excel.
		if (!loadedTables.contains(h2TableName)) {
			String excelFilePath = mockMapping.getExcelPath();
			
			String twsMockerEnv = System.getenv("TWSMOCKER") != null ? System.getenv("TWSMOCKER") : "";
			String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : "";
			String resourcesPath = Core.getConfiguration().getResourcesPath() != null
					? Core.getConfiguration().getResourcesPath().getAbsolutePath()
					: "";
			String replacedFilePath = excelFilePath.replace("\\", "/").replace("$TWSMOCKER", twsMockerEnv)
					.replace("$HOME", homeDir)
					.replace("$RESOURCES", resourcesPath);
			File excelFile = new File(replacedFilePath);

			if (excelFile.exists()) {
				boolean roaded = false;
				try (Connection conn = internalDataSource.getConnection()) {
					H2StreamingProcessor processor = new H2StreamingProcessor(conn, h2TableName, mockMapping.getParamOrDtoType());
					XssfExcelReader reader = new XssfExcelReader(excelFile, mockMapping.getSheetName(), false,
							processor);
					roaded = reader.read();
					processor.flushBatch();
				}
				if (!roaded) {
					logger.error("Failed to load Excel sheet: " + mockMapping.getSheetName() + " from mock file: " + excelFile.getAbsolutePath());
					throw new RuntimeException("Failed to load Excel sheet: " + mockMapping.getSheetName() + " from mock file: " + excelFile.getAbsolutePath());
				}
				loadedTables.add(h2TableName);
				logger.info("Imported mock table into H2: " + h2TableName + " from Excel file: " + excelFile.getAbsolutePath() + " (sheet: " + mockMapping.getSheetName() + ")");
			} else {
				logger.error("Excel mock file not found: " + excelFile.getAbsolutePath());
				throw new RuntimeException("Excel mock file not found: " + excelFile.getAbsolutePath());
			}
		}
		return h2TableName;
	}

	/**
	 * A processor that receives notifications from XssfExcelReader and performs streaming INSERTs into H2.
	 */
	public static class H2StreamingProcessor implements XssfExcelReader.RowProcessor {

		private final Connection conn;
		private final String tableName;
		private final IMetaObject metaObject; 
		private final List<String> orderedColLetters = new ArrayList<>();
		private final List<IMetaPrimitive> orderedPrimitives = new ArrayList<>();
		private PreparedStatement insertPs;
		private int batchCount = 0;

		public H2StreamingProcessor(Connection conn, String tableName, String entityType) {
			this.conn = conn;
			this.tableName = tableName;
			// Retrieve the metamodel from the entity type (if it does not exist, return `null` and fall back to treating all columns as `VARCHAR`).
			this.metaObject = (entityType != null) ? Core.getMetaObject(entityType) : null;
		}

		@Override
		public void processRow(int rowIndex, Map<String, String> rowData) {
			try {
				// 1. Header row: Match against the metamodel, then generate and execute DDL
				if (rowIndex == 1) {
					setupTable(rowData);
					return;
				}
				if (insertPs == null || rowData == null || rowData.isEmpty()) {
					return;
				}
				// 2. Data row: Perform type conversion and binding in the order of column indices
				for (int i = 0; i < orderedColLetters.size(); i++) {
					String colLetter = orderedColLetters.get(i);
					String rawValue = rowData.get(colLetter);
					IMetaPrimitive primitive = orderedPrimitives.get(i);

					Object bindValue = convertToSqlValue(rawValue, primitive);
					insertPs.setObject(i + 1, bindValue);
				}
				insertPs.addBatch();
				batchCount++;
				if (batchCount % 500 == 0) {
					insertPs.executeBatch();
				}			
			} catch (Exception e) {
				logger.error("Error importing row " + rowIndex + " into " + tableName, e);
			}
		}

		private void setupTable(Map<String, String> headerRow) throws Exception {
			StringBuilder createSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
					.append("\"").append(tableName).append("\" (");
			StringBuilder insertSql = new StringBuilder("INSERT INTO ")
					.append("\"").append(tableName).append("\" VALUES (");
			for (Map.Entry<String, String> entry : headerRow.entrySet()) {
				String colLetter = entry.getKey();
				String headerName = entry.getValue().trim();

				orderedColLetters.add(colLetter);

				IMetaPrimitive primitive = (metaObject != null) ? metaObject.getMetaPrimitive(headerName) : null;
				orderedPrimitives.add(primitive);

				// Determine H2 SQL types from the metamodel
				String sqlType = resolveSqlType(primitive);

				createSql.append("\"").append(headerName).append("\" ").append(sqlType).append(", ");
				insertSql.append("?,");
			}
			createSql.setLength(createSql.length() - 2);
			createSql.append(")");

			insertSql.setCharAt(insertSql.length() - 1, ')');

			try (Statement stmt = conn.createStatement()) {
				stmt.execute(createSql.toString());
				logger.debug("Created H2 mock table: " + createSql.toString());
			}

			this.insertPs = conn.prepareStatement(insertSql.toString());
		}

		/**
		 * Mapping Mendix PrimitiveTypes to H2 type definitions
		 */
		private String resolveSqlType(IMetaPrimitive primitive) {
			if (primitive == null) {
				return "VARCHAR";
			}
			PrimitiveType type = primitive.getType();
			switch (type) {
			case DateTime:
				return "TIMESTAMP";
			case Integer:
				return "INTEGER";
			case Long:
				return "BIGINT";
			case Decimal:
				return "DECIMAL(28, 8)";
			case Boolean:
				return "BOOLEAN";
			default:
				return "VARCHAR";
			}
		}

		/**
		 * Cast the value to match the metamodel type (e.g., generate a Timestamp for a DateTime column).
		 */
		private Object convertToSqlValue(String rawValue, IMetaPrimitive primitive) {
			if (rawValue == null || rawValue.trim().isEmpty()) {
				return null;
			}
			String strVal = rawValue.trim();
			if (primitive != null && primitive.getType() == PrimitiveType.DateTime) {
				try {
					Date date = XssfExcelRowProcessor.parseIso8601(strVal, ZoneId.systemDefault());
					return new Timestamp(date.getTime());
				} catch (Exception ignored) {
					// In cases involving serial values ​​or alternative formats, it is possible to fall back to existing utilities such as DateUtil.
				}
			}
			return strVal;
		}

		public void flushBatch() throws Exception {
			if (insertPs != null && batchCount > 0) {
				insertPs.executeBatch();
				insertPs.close();
				logger.debug("Flushed remaining " + batchCount + " rows into H2 mock table: " + tableName);
			}
		}
	}
}
