package net.btlcpy.outofstock.reports;

import java.sql.Connection;
import java.sql.Date;
import java.util.HashMap;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * Contains data members and methods which are common to the <code>AveragesReport</code>
 * <code>TotalsReport</code> classes.
 * @author Ahmed A. Abd-Allah
 */
public abstract class AveragesTotalsBaseReport extends Report
{
	/** The target of the report; i.e. what should the losses be calculated over?
	 * Possible choices include stores, bottlers, products, and so on.
	 */
	protected String reportSummaryTarget = null;
	
	/** The starting date for the report. */
	protected Date reportBeginDate;

	/** The metric to use for calculating losses: either quantity, dollars, or both. */
	protected String reportLostSalesMetric = null;

	/** Sorting information for the columns. See the superclass <code>Report</code>. */
	protected ListOrderedMap sortInfo = null;
	
	/** The user action description for this report. */
	protected String userActionDescription = null;
	
	/** The (generated) Excel file description for this report. */
	protected String excelReportDescription = null;
	
	/** The column formats to use for particular columns. */
	protected HashMap /*<String, ReportColumnFormat>*/ columnFormats = null;

	public AveragesTotalsBaseReport(Connection connection, String username)
	{
		super(connection, username);
	}
}
