package twowaysql.implementation;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mendix.systemwideinterfaces.core.IContext;

import twowaysql.integration.ExtDataSourceBinder;

public class CriteriaMappingRegistry {
	private static final CriteriaMappingRegistry instance = new CriteriaMappingRegistry();

	private final Set<MappingKey> registeredMappings = ConcurrentHashMap.newKeySet();

	private CriteriaMappingRegistry() {
	}

	public static CriteriaMappingRegistry getInstance() {
		return instance;
	}

	/**
	 * Register mappings for data sources, tables, and DTO/Param types in AfterStartUp.
	 */
	public boolean registerMapping(String dataSourceName, String tableName, String paramOrDtoType) {
		if (dataSourceName == null || tableName == null || paramOrDtoType == null) {
			return false;
		}
		MappingKey key = new MappingKey(dataSourceName, tableName, paramOrDtoType);
		return registeredMappings.add(key);
	}

	/**
	 * Verify that the provided data source, table, and DTO/Param types are correctly registered.
	 */
	public boolean isValidMapping(String dataSourceName, String tableName, String dtoType) {
		if (dataSourceName == null || tableName == null || dtoType == null) {
			return false;
		}
		MappingKey key = new MappingKey(dataSourceName, tableName, dtoType);
		return registeredMappings.contains(key);
	}

	// The class for holding keys to maintain the uniqueness of combinations internally.
	private static class MappingKey {
		private final String dataSourceName;
		private final String tableName;
		private final String dtoType;

		public MappingKey(String dataSourceName, String tableName, String dtoType) {
			this.dataSourceName = dataSourceName.trim();
			this.tableName = tableName.trim().toLowerCase(); // Normalize table names to lowercase.
			this.dtoType = dtoType.trim();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			MappingKey that = (MappingKey) o;
			return Objects.equals(dataSourceName, that.dataSourceName) &&
					Objects.equals(tableName, that.tableName) &&
					Objects.equals(dtoType, that.dtoType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(dataSourceName, tableName, dtoType);
		}
	}

	// Key: "DS name.table name (lowercase)" ➔ Value: TableMetadata (exact table name + column map)
	private final Map<String, TableMetadata> tableCache = new ConcurrentHashMap<>();

	/**
	 * Retrieve the exact table name from the database (automatically load if not cached).
	 */
	public String resolveTableName(IContext context, String dataSourceName, String rawTableName) {
		if (dataSourceName == null || rawTableName == null)
			return rawTableName;

		TableMetadata meta = getOrLoadTableMetadata(context, dataSourceName, rawTableName);
		return meta != null && meta.actualTableName != null ? meta.actualTableName : rawTableName;
	}

	/**
	 * Retrieve the exact column name from the database (automatically load if not cached).
	 */
	public String resolveColumnName(IContext context, String dataSourceName, String rawTableName, String attrName) {
		if (dataSourceName == null || rawTableName == null || attrName == null)
			return attrName;

		TableMetadata meta = getOrLoadTableMetadata(context, dataSourceName, rawTableName);
		if (meta != null) {
			String actualCol = meta.columnMap.get(attrName.toLowerCase());
			if (actualCol != null) {
				return actualCol;
			}
		}
		return attrName; // If no match is found, fall back to the original attribute name.
	}

	/**
	 * Cache retrieval or automatic loading from JDBC (Lazy Load)
	 */
	private TableMetadata getOrLoadTableMetadata(IContext context, String dataSourceName, String rawTableName) {
		String cacheKey = (dataSourceName + "." + rawTableName).trim().toLowerCase();

		return tableCache.computeIfAbsent(cacheKey, key -> {
			TableMetadata meta = new TableMetadata();
			try (Connection conn = ExtDataSourceBinder.getExtDataSource(dataSourceName).getConnection(context)) {
				DatabaseMetaData dbMeta = conn.getMetaData();

				try (ResultSet rs = dbMeta.getTables(null, null, rawTableName, new String[] { "TABLE", "VIEW" })) {
					if (rs.next()) {
						meta.actualTableName = rs.getString("TABLE_NAME");
					}
				}
				if (meta.actualTableName == null) {
					try (ResultSet rs = dbMeta.getTables(null, null, rawTableName.toUpperCase(),
							new String[] { "TABLE", "VIEW" })) {
						if (rs.next()) {
							meta.actualTableName = rs.getString("TABLE_NAME");
						}
					}
				}
				if (meta.actualTableName == null) {
					try (ResultSet rs = dbMeta.getTables(null, null, rawTableName.toLowerCase(),
							new String[] { "TABLE", "VIEW" })) {
						if (rs.next()) {
							meta.actualTableName = rs.getString("TABLE_NAME");
						}
					}
				}
				if (meta.actualTableName == null) {
					meta.actualTableName = rawTableName; // Fall back to the input value if not found.
				}

				// 2. Retrieving the list of column names
				try (ResultSet rs = dbMeta.getColumns(null, null, meta.actualTableName, null)) {
					while (rs.next()) {
						String colName = rs.getString("COLUMN_NAME");
						meta.columnMap.put(colName.toLowerCase(), colName);
					}
				}
			} catch (Exception e) {
				meta.actualTableName = rawTableName;
			}
			return meta;
		});
	}

	// Class for internal cache
	private static class TableMetadata {
		String actualTableName;
		Map<String, String> columnMap = new HashMap<>();
	}
}
