package twowaysqlmocker.implementation;

import java.util.List;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import twowaysql.implementation.TwoWaySqlExecutor;

public class TwoWaySqlMockerImpl implements TwoWaySqlExecutor.TwoWaySqlMocker {

	public static final ILogNode logger = Core.getLogger("TwoWaySql");

	@Override
	public List<IMendixObject> mockRetrieveByTwoWaySql(IContext context, String mockDirective, Map<String, Object> paramMap, String recordEntityType) throws Exception {
		if (mockDirective.startsWith(TwoWaySqlExecutor.MOCK_EXCEL_DIRECTIVE)) {
			String excelFilePath = mockDirective.substring(TwoWaySqlExecutor.MOCK_EXCEL_DIRECTIVE.length()).trim();
			List<IMendixObject> resultList = ExcelReader.readExcelFromFile(context, excelFilePath, recordEntityType, null, null);
			return resultList;
		} else if (mockDirective.startsWith(TwoWaySqlExecutor.MOCK_MICROFLOW_DIRECTIVE)) {
			String microflowName = mockDirective.substring(TwoWaySqlExecutor.MOCK_MICROFLOW_DIRECTIVE.length()).trim();
			List<IMendixObject> resultList = Core.microflowCall(microflowName).inTransaction(true).withParams(paramMap).execute(context);
			return resultList;
		}
		throw new MendixRuntimeException("Unsupported mock directive: " + mockDirective);
	}
	
	@Override
	public Long mockCountRowsByTwoWaySql(IContext context, String mockDirective, Map<String, Object> paramMap) throws Exception {
		if (mockDirective.startsWith(TwoWaySqlExecutor.MOCK_EXCEL_DIRECTIVE)) {
			String excelFilePath = mockDirective.substring(TwoWaySqlExecutor.MOCK_EXCEL_DIRECTIVE.length()).trim();
			List<IMendixObject> resultList = ExcelReader.readExcelFromFile(context, excelFilePath, null, null, null);
			return (long) resultList.size();
		} else if (mockDirective.startsWith(TwoWaySqlExecutor.MOCK_MICROFLOW_DIRECTIVE)) {
			String microflowName = mockDirective.substring(TwoWaySqlExecutor.MOCK_MICROFLOW_DIRECTIVE.length()).trim();
			Long count = Core.microflowCall(microflowName).inTransaction(true).withParams(paramMap).execute(context);
			return count;
		}
		throw new MendixRuntimeException("Unsupported mock directive: " + mockDirective);
	}
}
