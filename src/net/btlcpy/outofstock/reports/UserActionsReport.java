package net.btlcpy.outofstock.reports;

import java.sql.Connection;
import java.sql.Date;
import java.util.Iterator;
import java.util.TreeMap;

import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFormat;
import net.btlcpy.outofstock.utilities.SelectBuilder;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * Represents reports showing user action logs. Runs one SQL query, and artificially
 * creates the results of a second query showing "statistics" - how many reports did
 * each user run based on the results of the first (SQL) query.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class UserActionsReport extends Report
{
	/** There is only one SQL query run for this report. */
	private SelectBuilder sbQuery;

	public UserActionsReport(Connection connection, String username, Date beginDate, Date endDate, 
		String actionCategory, int start, int size)
	{
		super(connection, username);
		
		constructQuery(connection, beginDate, endDate, actionCategory, start, size);
	}
	
	/**
	 * @return always returns null since user action reports are not logged
	 */
	public String getUserActionDescription()
	{
		return null; // don't log these; no need
	}

	public String getExcelReportDescription()
	{
		return "User Actions Report";
	}
	
	public String getReportType()
	{
		return "User Actions Report";
	}

	public String getQueryTitle(int queryNumber)
	{
		if (queryNumber == 0)
			return "User actions:";
		else if (queryNumber == 1)
			return "Total actions taken per user:";
		return "";
	}

	private void constructQuery(Connection connection, Date beginDate, Date endDate, 
		String actionCategory, int start, int size)
	{
		sbQuery = new SelectBuilder();
		sbQuery.addFrom("useractions");
		sbQuery.addSelect("timelastuploaded AS \"Timestamp\", name AS \"Username\", category AS \"Category\", description AS \"Description\"");
		sbQuery.addOrder("\"Timestamp\"");
		
		if (actionCategory != null && !actionCategory.equals("all"))
			sbQuery.addWhere("category = " + 
				BasePersistentBean.BQ + BasePersistentBean.dq(actionCategory) + BasePersistentBean.EQ, true);

		if (beginDate != null)
		{
			sbQuery.addWhere("timelastuploaded >= DATE '" + beginDate.toString() + "'", true);
			if (endDate != null)
				sbQuery.addWhere("timelastuploaded < DATE '" + endDate.toString() + "'", true);
		}
	}
	
	public Integer numberOfQueries() 
	{
		return new Integer(2);
	}
	
	public String getQuery(int queryNumber) 
	{
		if (queryNumber == 0)
			return sbQuery.toString();
		return null;
	}

	public String[][] getQueryKeyColumns()
	{
		String keyColumns[][] = { null, null };
		return keyColumns;
	}
	
	public void doPreProcessing(Connection connection, int queryNumber) {}
	public void doPostProcessing(Connection connection, int queryNumber) 
	{
		/*
		 * After the SQL query is run which gets back all the user actions run for a 
		 * particular period of time, we create a second set of query results for an
		 * artificial second query that summarizes the reports run per user over the
		 * original query's data.
		 */
		if (queryNumber == 0)
		{
			// Get the results of the SQL query
			ListOrderedMap rows = (ListOrderedMap) super.getResults().getValue(0);
			
			if (rows != null && rows.size() > 0)
			{
				// Build a sorted map of the users-to-number-of-reports
				ListOrderedMap row = null;
				String username = null;
				TreeMap /*<String, Integer>*/ users = new TreeMap();
				for (int i=0; i<rows.size(); i++)
				{
					row = (ListOrderedMap) rows.getValue(i);
					username = (String) row.get("Username");
					
					// Unfortunately need to worry about previous version of software where
					// the username was sometimes logged as null
					if (username == null || username.equals("null"))
						username = "unknown";
					
					if (users.get(username) == null)
						users.put(username, new Integer(1));
					else
						users.put(username, new Integer(((Integer) users.get(username)).intValue() + 1));
				}

				// Create a ListOrderedMap of the results that is compatible with the
				// display page that will be used to show the results to the user
				ListOrderedMap userStatsRow = null;
				ListOrderedMap userStatsRows = new ListOrderedMap();
				if (users.size() > 0)
				{
					Iterator iterator = users.keySet().iterator();
					int userCount = 1;
					while (iterator.hasNext())
					{
						//MainLog.getLog().debug("Username: " + username + " --> " + users.get(username) + " actions."); 
						username = (String) iterator.next();
						userStatsRow = new ListOrderedMap();
						userStatsRow.put("&nbsp;", new Integer(userCount++));
						userStatsRow.put("Username", username);
						userStatsRow.put("Number of Actions", users.get(username));
						
						userStatsRows.put(username, userStatsRow);
					}
					
					if (userStatsRows.size() > 0)
					{
						((ListOrderedMap) super.getResults()).put("query1", userStatsRows);
					}
				}

				// Technically this report is not exported to Excel...
				resetExcelFormats(2);
				ReportExcelFormat excelFormat = new ReportExcelFormat();
				setExcelFormat(0, excelFormat);
				setExcelFormat(1, excelFormat);
			}
		}
	}

	protected String[] getInitialSortedColumns(int queryNumber)
	{
		if (queryNumber == 0)
		{
			String isc[] = { "Timestamp" };
			return isc;
		}
		else if (queryNumber == 1)
		{
			String isc[] = { "Username" };
			return isc;
		}
		return null;
	}

	protected boolean isSortedAscendingInitially(int queryNumber)
	{
		if (queryNumber == 0 || queryNumber == 1)
			return true;
		return false;
	}
	
	public String[] getUnsortableColumns(int queryNumber)
	{
		if (queryNumber == 1)
		{
			String uc[] = { "&nbsp;" };
			return uc;
		}
		return null;
	}
}
