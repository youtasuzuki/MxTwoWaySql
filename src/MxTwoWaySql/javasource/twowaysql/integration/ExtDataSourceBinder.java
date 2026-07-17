package twowaysql.integration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

public class ExtDataSourceBinder {
	static Map<String, DataSource>extDataSourceMap = new ConcurrentHashMap<String, DataSource>();
	static Map<String, Long>extDataSourceOptionsMap = new ConcurrentHashMap<String, Long>();
	
	public static DataSource putExtDataSource(String name, DataSource ds) {
		return extDataSourceMap.put(name, ds);
	}
	public static Long putExtDataSourceOptions(String name, Long options) {
		return extDataSourceOptionsMap.put(name, options);
	}

	
	public static DataSource getExtDataSource(String name) {
		return extDataSourceMap.get(name);
	}
	public static Long getExtDataSourceOptions(String name) {
		Long options = extDataSourceOptionsMap.get(name);
		return options == null? 0L : options;
	}
}
