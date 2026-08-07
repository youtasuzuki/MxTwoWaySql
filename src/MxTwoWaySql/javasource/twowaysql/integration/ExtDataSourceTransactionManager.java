package twowaysql.integration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mendix.systemwideinterfaces.core.IContext;

public class ExtDataSourceTransactionManager {

	public static void startTransaction(IContext context, String dsName) throws SQLException {
		Map<String, ExtConnectionWrapper> transactionMap = getTransactionMap(context);
		if (!transactionMap.containsKey(dsName)) {
			Connection con = ExtDataSourceBinder.getExtDataSource(dsName).getUnderlyingDataSource().getConnection();
			ExtConnectionWrapper conWrapper = new ExtConnectionWrapper(con);
			transactionMap.put(dsName, conWrapper);
		} else {
			throw new SQLException("Transaction already started for datasource: " + dsName);
		}
	}

	public static void endTransaction(IContext context, String dsName) throws SQLException {
		Map<String, ExtConnectionWrapper> transactionMap = getTransactionMap(context);
		if (transactionMap.containsKey(dsName)) {
			ExtConnectionWrapper conWrapper = transactionMap.get(dsName);
			conWrapper.commit();
			conWrapper.closeConnection();
			transactionMap.remove(dsName);
		} else {
			throw new SQLException("No transaction started for datasource: " + dsName);
		}
	}

	public static void abendTransaction(IContext context, String dsName) throws SQLException {
		Map<String, ExtConnectionWrapper> transactionMap = getTransactionMap(context);
		if (transactionMap.containsKey(dsName)) {
			ExtConnectionWrapper conWrapper = transactionMap.get(dsName);
			conWrapper.rollback();
			conWrapper.closeConnection();
			transactionMap.remove(dsName);
		} else {
			throw new SQLException("No transaction started for datasource: " + dsName);
		}
	}

	public static void commitTransaction(IContext context, String dsName) throws SQLException {
		Map<String, ExtConnectionWrapper> transactionMap = getTransactionMap(context);
		if (transactionMap.containsKey(dsName)) {
			ExtConnectionWrapper conWrapper = transactionMap.get(dsName);
			conWrapper.commit();
		} else {
			throw new SQLException("No transaction started for datasource: " + dsName);
		}
	}

	public static void rollbackTransaction(IContext context, String dsName) throws SQLException {
		Map<String, ExtConnectionWrapper> transactionMap = getTransactionMap(context);
		if (transactionMap.containsKey(dsName)) {
			ExtConnectionWrapper conWrapper = transactionMap.get(dsName);
			conWrapper.rollback();
		} else {
			throw new SQLException("No transaction started for datasource: " + dsName);
		}
	}

	public static Map<String, ExtConnectionWrapper> getTransactionMap(IContext context) {
		Map<String, ExtConnectionWrapper> transactionMap = (Map<String, ExtConnectionWrapper>)context.getData().get("ExtDataSourceTransactionMap");
		if (transactionMap == null) {
			transactionMap = new ConcurrentHashMap<>();
			context.getData().put("ExtDataSourceTransactionMap", transactionMap);
		}
		return transactionMap;
	}
}
