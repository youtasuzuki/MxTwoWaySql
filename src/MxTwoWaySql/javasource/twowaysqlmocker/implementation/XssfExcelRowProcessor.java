package twowaysqlmocker.implementation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.poi.ss.usermodel.DateUtil;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

public abstract class XssfExcelRowProcessor implements XssfExcelReader.RowProcessor {

	private final Map<String, String> columnToHeader = new HashMap<>();
	private final Map<String, String> headerToColumn = new HashMap<>();

	protected void setupColumnNameMap(Map<String, String> headerRow) {
		columnToHeader.clear();
		columnToHeader.putAll(headerRow);
		headerToColumn.clear();
		for (Map.Entry<String, String> entry : headerRow.entrySet()) {
			headerToColumn.put(entry.getValue(), entry.getKey());
		}
	}

	protected String getHeaderName(String cellReference) {
		String column = cellReference.replaceAll("\\d", "");
		return columnToHeader.get(column);
	}

	protected String getValueByHeader(Map<String, String> rowData, String headerName) {
		return rowData.get(headerToColumn.get(headerName));
	}

	protected IMendixObject createIMendixObject(IContext context, String entityType, Map<String, String> rowData,
			String timeZoneId) {
		IMendixObject newObject = Core.instantiate(context, entityType);
		for (Map.Entry<String, String> entry : rowData.entrySet()) {
			String headerName = getHeaderName(entry.getKey());
			if (headerName != null) {
				IMetaPrimitive metaPrimitive = newObject.getMetaObject().getMetaPrimitive(headerName);
				if (metaPrimitive != null) {
					// Convert the value to the appropriate type based on the metaPrimitive
					Object value = convertType(context, metaPrimitive, entry.getValue(), timeZoneId);
					newObject.setValue(context, headerName, value);
				}
			}
		}
		return newObject;
	}

	protected Object convertType(IContext context, IMetaPrimitive metaPrimitive, String value, String timeZoneId) {
		if (value == null) {
			return null;
		}
		switch (metaPrimitive.getType()) {
		case String:
		case Enum:
			return value;
		case Integer:
			return Integer.parseInt(value);
		case Long:
			return Long.parseLong(value);
		case Decimal:
			return new java.math.BigDecimal(value);
		case Boolean:
			return Boolean.parseBoolean(value);
		case DateTime:
			if (timeZoneId == null) {
				TimeZone sessionTimeZone = context.getSession().getTimeZone();
				if (sessionTimeZone != null) {
					timeZoneId = sessionTimeZone.getID();
				} else {
					timeZoneId = TimeZone.getDefault().getID();
				}
			}
			if (value.matches("^[0-9]+$")) {
				// If the value is a serial number, convert it to a date
				Double serialValue = Double.valueOf(value);
				java.util.Date dateValue = DateUtil.getJavaDate(serialValue, TimeZone.getTimeZone(timeZoneId));
				return dateValue;
			} else {
				// Assuming the date is in ISO 8601 format
				java.time.ZoneId zoneId = java.time.ZoneId.of(timeZoneId);
				//ZonedDateTime targetDateTime = OffsetDateTime.parse(value)
				//        .atZoneSameInstant(zoneId);
				//java.util.Date dateValue = java.util.Date.from(targetDateTime.toInstant());
				java.util.Date dateValue = parseIso8601(value, zoneId);
				return dateValue;
			}
		default:
			throw new IllegalArgumentException("Unsupported type: " + metaPrimitive.getType());
		}
	}

	public static Date parseIso8601(String isoString, ZoneId defaultZone) {
		// ISO 8601 format (a standard formatter that flexibly handles cases with or without time information)
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		try {
			// 1. First, attempt the analysis with "Time Zone Included."
			ZonedDateTime zdt = ZonedDateTime.parse(isoString, formatter);
			// 2. Convert to java.util.Date and return it.
			return Date.from(zdt.toInstant());
		} catch (DateTimeParseException e) {
			// 3. If an error occurs due to the absence of a time zone, parse it as "no time zone."
			LocalDateTime ldt = LocalDateTime.parse(isoString, formatter);
			// 4. Merge the specified time zone (defaultZone).
			ZonedDateTime zdtWithDefault = ldt.atZone(defaultZone);
			// 5. Convert to java.util.Date and return it.
			return Date.from(zdtWithDefault.toInstant());
		}
	}

}