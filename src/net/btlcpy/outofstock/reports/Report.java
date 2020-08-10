package net.btlcpy.outofstock.reports;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.UserAction;
import net.btlcpy.outofstock.reports.format.ReportCellFormat;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFormat;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * <p>
 * A class to represent reports for the Out of Stock application. A report is a collection of
 * SQL query results ultimately. Subclasses of this class are supposed to fill in the queries
 * desired, and this class's methods can be used to execute all the queries, one by one. The
 * results of each query are saved in a list ordered map of rows, and all the results of the
 * individual queries are rolled up into an overall list ordered map, query-to-results.
 * </p>
 * <p> 
 * A report is displayed as a tabular collection of data usually rendered as an HTML table 
 * within a Web browser. The table can be sorted by different columns in ascending or 
 * descending order. The report also supports formatting information for header cells, data 
 * cells, and footer cells per column.
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
abstract public class Report
{
	/** 
	 * <p>
	 * A collection of query results, arranged in order of execution. The keys are
	 * strings, always in the form of "queryX" where X is the number of the query
	 * (zero-based) - e.g. "query0", "query1".
	 * </p>
	 * <p>
	 * Each query result is in itself a map where the values are rows retrieved, and
	 * the keys are either automatically generated ascending integers (zero-based),
	 * or a subset of the results based on key columns. What columns to use as keys
	 * for a particular query should be set by subclasses.
	 * </p>
	 */
	private ListOrderedMap /* <String, ListOrderedMap> */ results;
	
	/**
	 * <p>
	 * Represents regions that are 'merged' in the sets of results. The keys are
	 * Strings containing the upper left coordinates separated by a space (e.g.
	 * "5 2"), and the values are the lower right coordinates of the space in
	 * the same format ("5 4" - giving an area of three cells in the sixth column
	 * starting in the third row that are merged). 
	 * </p>
	 * <p>Note: zero-based.</p>
	 */
	public ListOrderedMap /* <String, String> */ mergedRegions[];
	
	/**
	 * The columns that are sortable per set of query results (hence it is a two
	 * dimensional array). For example if the second query had its third column 
	 * be sortable, and that column represented the first name of people, then
	 * <code>sortedColumns[1][2] = "firstname"</code> for example. This column
	 * name corresponds to the column name used in the database.
	 */
	private String[][] sortedColumns = null;
	
	/**
	 * A switch that indicates whether a query's results are currently sorted
	 * in ascending order or not.
	 */
	private boolean[] sortedColumnsAscending = null;
	
	/**
	 * The database connection to use for the report. If null, a connection is automatically
	 * created.
	 */
	private Connection connection;
	
	/**
	 * Indicates whether the report was given a connection from outside or not (for cleanup
	 * purposes).
	 */
	private boolean useOwnConnection;
	
	/**
	 * The username of the person running the report (for record-keeping purposes in the user
	 * logs).
	 */
	private String username = null;
	
	/**
	 * The Excel format to use if the report is exported to a Microsoft Excel file. It is
	 * an array to account for different queries.
	 */
	private ReportExcelFormat excelFormat[] = null;
	
	public Report(Connection connection, String username)
	{
		if (connection == null)
		{
			connection = null;
			useOwnConnection = true;
		}
		else
		{
			this.connection = connection; 
			useOwnConnection = false;
		}
		this.username = username; 
	}

	/** The number of queries in the report. */
	abstract public Integer numberOfQueries();
	
	/**
	 * @param queryNumber the report query to retrieve (zero-based)
	 * @return an SQL query that can be executed
	 */
	abstract public String getQuery(int queryNumber);
	
	/**
	 * @return All the key columns used for individual queries
	 */
	abstract public String[][] getQueryKeyColumns();
	
	/**
	 * @param queryNumber the report query to examine
	 * @return all columns that are initially sorted after query execution
	 */
	abstract protected String[] getInitialSortedColumns(int queryNumber);
	
	/**
	 * @param queryNumber the report query to examine
	 * @return whether or not the query is sorted ascending or not after query execution
	 */
	abstract protected boolean isSortedAscendingInitially(int queryNumber);
	
	/**
	 * Before a query is executed, this class can execute a set of pre-processing instructions
	 * that are specified in subclasses.
	 * @param connection the connection to use (in case pre-processing needs its own database
	 * connection)
	 * @param queryNumber the query number for which pre-processing is intended
	 */
	abstract public void doPreProcessing(Connection connection, int queryNumber);
	
	/**
	 * After a query is executed, this class can execute a set of post-processing instructions
	 * that are specified in subclasses.
	 * @param connection the connection to use (in case post-processing needs its own database
	 * connection)
	 * @param queryNumber the query number for which post-processing is intended
	 */
	abstract public void doPostProcessing(Connection connection, int queryNumber);

	/**
	 * @return A string indicating the user action which should be logged. May return null, in
	 * which case no user action will be logged.
	 */
	abstract public String getUserActionDescription();
	
	/**
	 * @return A string that is used inside the Microsoft Excel spreadsheet generated from the
	 * report, used to serve as a description of the report.
	 */
	abstract public String getExcelReportDescription();

	/**
	 * @return A brief description of the report type.
	 */
	abstract public String getReportType();

	/**
	 * Returns a string suitable for displaying as the header/title for that query's
	 * results.
	 * @param queryNumber the query number to examine
	 * @return a title string for the query's results
	 */
	abstract public String getQueryTitle(int queryNumber);

	// By default, the basic styles in the CSS stylesheets are enough... but these are
	// here for subclasses to override
	/** Represents the header cell format for all queries in this report. */
	public ReportCellFormat getHeaderCellFormat(String column) { return null; } 
	/** Represents the data cell format for all queries in this report. */
	public ReportCellFormat getDataCellFormat(String column) { return null; } 
	/** Represents the footer cell format for all queries in this report. */
	public ReportCellFormat getFooterCellFormat(String column) { return null; } 

	/**
	 * Indicates which columns are UNsortable. Typically, most data IS sortable, hence
	 * this method is used to retrieve the opposite (the rare case so to speak). Subclasses
	 * should override this, otherwise it will return null.
	 * 
	 * @param queryNumber the query number to examine
	 * @return an array of column names, or null if all columns are sortable
	 */
	public String[] getUnsortableColumns(int queryNumber)
	{
		return null;
	}
	
	/**
	 * <p>
	 * Retrieves specific sorting information for a particular column. Sorting information can either be
	 * the name of the column itself (meaning sort based on the natural order of the data), or it can be
	 * the column name PLUS a colon then a number - e.g. "names:3" (meaning sort based on the fourth
	 * element found in each row entry for that column). The second approach is useful when the data
	 * found in a particular report cell is a column of data.
	 * </p>
	 * <p>Subclasses should override this, otherwise the column name is automatically returned</p>
	 * 
	 * @param queryNumber
	 * @param columnName
	 * @return sorting information as described above
	 */
	public String getSortInfo(int queryNumber, String columnName)
	{
		return columnName;
	}

	/**
	 * <p>
	 * Returns all the query results of the report in the following format (expressed in terms of Java
	 * 5 generics):
	 * </p>
	 * <p><code>
	 * ListOrderedMap&lt;String, ListOrderedMap&lt;String, ListOrderedMap&lt;String, Object>>>
	 * </code></p>
	 * <p>
	 * <ul>
	 * <li>The results are a list ordered map, where the keys are strings "query0", "query1", etc., and the
	 * values are individual query results. Order is the order of execution of the queries.</li>
	 * <li>An individual query result is a list ordered map, where the keys are either automatically
	 * generated ascending integers (row numbers), or key columns taken from the results themselves. The
	 * key columns to use are specified by the subclass. Values are individual rows of data. The order
	 * is specified by the query.</li>
	 * <li>An individual row of data is a list ordered map, where the keys are column names, and the values
	 * are the row's data for those columns.
	 * </ul>
	 * </p>
	 * @return all query results
	 */
	public ListOrderedMap getResults() { return results; }
	
	/**
	 * Sets the results of the queries, is useful in certain situations where the report subclass wants to
	 * merge the original set of results into each other for example.
	 * @param overridingResults the new set of results - it should match the same form as specified in
	 * {@link Report#getResults()}
	 */
	protected void setResults(ListOrderedMap overridingResults) { results = overridingResults; }

	/**
	 * Executes all the queries for the report, one by one. Preprocessing and postprocessing calls are
	 * made per query.
	 * 
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void execute()
		throws SQLException, InstantiationException, IllegalAccessException
	{
		PersistenceManager manager = PersistenceManager.getPersistenceManager();
		boolean committed = false;
		
		try
		{
			// If using own connection, assume that the queries are related, and hence
			// all must either succeed (be committed at the end) or all must fail
			// (rollback).
			if (useOwnConnection)
				connection = manager.getConnection(false);
			
			results = new ListOrderedMap();
			String keyColumns[][] = getQueryKeyColumns();
			sortedColumns = new String[numberOfQueries().intValue()][];
			sortedColumnsAscending = new boolean[numberOfQueries().intValue()];

			resetMergedRegions(numberOfQueries().intValue());
			resetExcelFormats(numberOfQueries().intValue());
			
			for (int i=0; i<numberOfQueries().intValue(); i++)
			{
				doPreProcessing(connection, i);
				String query = getQuery(i);
				if (query != null)
				{
					MainLog.getLog().debug("Executing query " + i);
					MainLog.getLog().debug("\n\n" + query + "\n\n");
					results.put("query" + i, manager.findRows(connection, query, keyColumns[i]));
				}
				sortedColumns[i] = getInitialSortedColumns(i);
				sortedColumnsAscending[i] = isSortedAscendingInitially(i);
				doPostProcessing(connection, i);
			}

			// Log the report as a user action if there is a description
			if (getUserActionDescription() != null)
			{
				UserAction userAction = new UserAction();
				userAction.setCategory(UserAction.REPORT);
				userAction.setTimeLastUploaded(new Timestamp(System.currentTimeMillis()));
				userAction.setDescription(getUserActionDescription());
				userAction.setName(username);
				userAction.create(connection);
			}
			
			if (useOwnConnection)
			{
				connection.commit();
				committed = true;
			}
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(e.getMessage(), e);
			throw e;
		}
		catch (InstantiationException e)
		{
			MainLog.getLog().error(e.getMessage(), e);
			throw e;
		}
		catch (IllegalAccessException e)
		{
			MainLog.getLog().error(e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (useOwnConnection)
				try 
				{ 
					if (connection != null)
					{
						if (!committed)
						{
							try { connection.rollback(); } 
							catch (Exception rbe) { MainLog.getLog().error("Exception rolling back", rbe); }
						}
						connection.close();
					}
				} 
				catch (Exception e) 
				{
					MainLog.getLog().error("Exception cleaning up connection", e);
				}		
		}
	}
	
	/**
	 * Sort a particular set of query results based on the columns to use, and whether
	 * to use ascending or descending order.
	 * @param queryNumber zero-based
	 * @param sortingColumns the columns to sort on
	 * @param ascending true means ascending, false descending
	 * @param subRows indicates how many "sub-rows" exist per "master row" that should be ignored
	 * and that should follow the "master row" for the purposes of sorting. This is typically
	 * zero (meaning every row is meaningful), but if there are merged subRows, then this might
	 * be 1 or more.
	 */
	public void sort(int queryNumber, String[] sortingColumns, boolean ascending, int subRows)
	{
		// get one collection of results (collection of rows = (key to row(name to value)))
		ListOrderedMap queryResults = (ListOrderedMap) results.get("query" + queryNumber);
		
		if (queryResults == null || queryResults.size() == 0 || sortingColumns == null)
			return;

		// store the original key into the results temporarily. We will take the key back
		// out after we sort the results.
		for (int i=0; i<queryResults.size(); i++)
			((ListOrderedMap) queryResults.getValue(i)).put("__key__", queryResults.get(i));
		
		// Retrieve all the rows without the keys (leaving out the subrows)
		// List rows = queryResults.valueList();
		ArrayList rows = new ArrayList();
		for (int i=0; i<queryResults.size(); i = i + subRows + 1)
		{
			rows.add(queryResults.getValue(i));
		}
		
		// Get the sorting information for each sortable column
		String[] sortInfo = new String[sortingColumns.length];
		for (int i=0; i<sortingColumns.length; i++)
		{
			// MainLog.getLog().debug("Request to sort on column: >>>" + sortingColumns[i] + "<<<");
			sortInfo[i] = getSortInfo(queryNumber, sortingColumns[i]);
			// MainLog.getLog().debug("Using sort info: >>>" + sortInfo[i] + "<<<");
		}
		
		// Sort
		Collections.sort(rows, new ReportRowComparator(sortInfo, ascending));
		
		// stuff back the subrows (if any)
		if (subRows > 0)
		{
			ArrayList fixedRows = new ArrayList();
			for (int i=0; i<rows.size(); i++)
			{
				// add the master row first
				fixedRows.add(rows.get(i));
				
				// look for its position in the original result set in order to get its immediately following subrows
				int originalPosition = queryResults.indexOf( ((ListOrderedMap) rows.get(i)).get("__key__") );
				
				// retrieve those subrows and add them to the new set of results
				for (int j=0; j<subRows; j++)
					fixedRows.add( queryResults.getValue(originalPosition + 1 + j) );
			}
			rows = fixedRows;
		}

		// Stuff the new sorted results back into a map using the original keys.
		// Since we are using a list ordered map, the sort order is maintained via the
		// order of insertion.
		ListOrderedMap sortedQueryResults = new ListOrderedMap();
		for (int i=0; i<rows.size(); i++)
		{
			Object key = ((ListOrderedMap) rows.get(i)).remove("__key__");
			sortedQueryResults.put(key, rows.get(i));
		}

		// Put the sorted query results back into the overall collection of results
		results.put("query" + queryNumber, sortedQueryResults);
		sortedColumns[queryNumber] = sortingColumns;
		sortedColumnsAscending[queryNumber] = ascending;
	}
	
	/**
	 * @param queryNumber the query number to examine
	 * @return the columns which are sorted/sortable at the moment for a particular query
	 */
	public String[] getSortedColumns(int queryNumber)
	{
		if (sortedColumns != null && queryNumber >=0 && queryNumber < sortedColumns.length)
			return sortedColumns[queryNumber];
		
		return null;
	}
	
	/**
	 * @param queryNumber the query number to examine
	 * @return whether that query is currently sorted ascending or not
	 */
	public boolean isSortedAscending(int queryNumber)
	{
		if (sortedColumnsAscending != null && queryNumber >=0 && queryNumber < sortedColumnsAscending.length)
			return sortedColumnsAscending[queryNumber];
		
		return false;
	}
	
	/**
	 * Instantiates merged regions array to satisfy the number of queries.
	 */
	public void resetMergedRegions(int numberOfQueries)
	{
		mergedRegions = new ListOrderedMap[numberOfQueries];
	}

	/**
	 * Indicates whether a merged region starts at that coordinate or not.
	 */
	public boolean isStartOfMergedRegion(int queryNumber, int x, int y)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null && 
			mergedRegions[queryNumber].containsKey(x + " " + y))
			return true;
		return false;
	}

	/**
	 * Retrieve end coordinate of the merged region (or null if no merged
	 * region starts there).
	 */
	public String getEndOfMergedRegion(int queryNumber, int x, int y)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null)
		{
			String end = (String) mergedRegions[queryNumber].get(x + " " + y);
			return end;
		}
		return null;
	}
	
	/**
	 * Adds a merged region from x1,y1 to x2,y2.
	 */
	public void addMergedRegion(int queryNumber, int x1, int y1, int x2, int y2)
	{
		if (mergedRegions[queryNumber] == null)
			mergedRegions[queryNumber] = new ListOrderedMap();
		
		mergedRegions[queryNumber].put(x1 + " " + y1, x2 + " " + y2);
		//MainLog.getLog().debug("Adding merged region in query " + queryNumber + ": " + x1 + " " + y1 + ", " + x2 + " " + y2);
	}

	/**
	 * Removes any merged region starting at that coordinate.
	 */
	public void removeMergedRegion(int queryNumber, int x, int y)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null)
		{
			mergedRegions[queryNumber].remove(x + " " + y);
		}
	}
	
	/**
	 * @param xy x + " " + y format
	 * @return x coordinate
	 */
	public int getXCoordinateFromMergedRegionCoordinate(String xy)
	{
		return Integer.parseInt(xy.substring(0, xy.indexOf(" ")));
	}

	/**
	 * @param xy x + " " + y format
	 * @return y coordinate
	 */
	public int getYCoordinateFromMergedRegionCoordinate(String xy)
	{
		return Integer.parseInt(xy.substring(xy.indexOf(" ")+1, xy.length()));
	}
	
	/**
	 * Retrieves the width of the merged region starting at that coordinate; -1 if none.
	 */
	public int getWidthOfMergedRegion(int queryNumber, int x, int y)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null)
		{
			String end = (String) mergedRegions[queryNumber].get(x + " " + y);
			if (end != null)
			{
				int x2 = getXCoordinateFromMergedRegionCoordinate(end);
				
				return x2 - x + 1;
			}
		}
		return -1;
	}

	/**
	 * Retrieves the height of the merged region starting at that coordinate; -1 if none.
	 */
	public int getHeightOfMergedRegion(int queryNumber, int x, int y)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null)
		{
			String end = (String) mergedRegions[queryNumber].get(x + " " + y);
			if (end != null)
			{
				int y2 = getYCoordinateFromMergedRegionCoordinate(end);
				
				return y2 - y + 1;
			}
		}
		return -1;
	}
	
	public ListOrderedMap getMergedRegions(int queryNumber)
	{
		if (mergedRegions != null && mergedRegions[queryNumber] != null)
			return mergedRegions[queryNumber];
		return null;
	}
	
	/**
	 * Instantiates Excel format arrays to satisfy the number of queries.
	 */
	public void resetExcelFormats(int numberOfQueries)
	{
		excelFormat = new ReportExcelFormat[numberOfQueries];
	}

	public ReportExcelFormat getExcelFormat(int queryNumber)
	{
		return excelFormat[queryNumber];
	}
	
	public void setExcelFormat(int queryNumber, ReportExcelFormat excelFormat)
	{
		this.excelFormat[queryNumber] = excelFormat;
	}
}
