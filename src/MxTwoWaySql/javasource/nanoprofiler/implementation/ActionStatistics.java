package nanoprofiler.implementation;

public class ActionStatistics {
	private String microflowName;
	private String sectionName;
	private long totalExecutedCount = 0;
	private long totalNanos = 0;
	private long totalSelectSqlCount = 0;
	private long totalUpdateSqlCount = 0;

	public ActionStatistics(String _microflowName, String _sectionName) {
		this.microflowName = _microflowName;
		this.sectionName = _sectionName;
	}

	public void record(long executedCount, long accumulatedNanos, long selectSqlCount, long updateSqlCount) {
		totalExecutedCount += executedCount;
		totalNanos += accumulatedNanos;
		totalSelectSqlCount += selectSqlCount;
		totalUpdateSqlCount += updateSqlCount;
	}	

	public String getMicroflowName() {
		return microflowName;
	}

	public String getSectionName() {
		return sectionName;
	}

	public long getTotalExecutedCount() {
		return totalExecutedCount;
	}

	public long getTotalNanos() {
		return totalNanos;
	}

	public long getTotalSelectSqlCount() {
		return totalSelectSqlCount;
	}

	public long getTotalUpdateSqlCount() {
		return totalUpdateSqlCount;
	}

}
