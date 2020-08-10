package net.btlcpy.outofstock.persistence;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;

/**
 * <p>
 * Manages sets of prepared statements executing in batch mode. This class should be used
 * whenever a large number of statements need to be executed on a database. The methods
 * inside BasePersistentBean are good for one-off or small numbers of statements.
 * </p>
 * 
 * @author Ahmed Abd-Allah
 */
public class BatchManager
{
	// ----- Instance members -------------------------------------------------
	/** The threshold number of statements that are stored in a batch before the
	 * batch is automatically triggered (if autoTriggered is true). This value
	 * will almost certainly be optimized differently for different database
	 * vendors. A 'pretty good choice' is 2000, but ymmv.
	 */
	private int threshold = 2000;
	/** Whether autotriggered batch executions should occur or not */
	private boolean autoTriggered = true;
	/** Whether to check for failures on autotriggered updates; in general it
	 * is not a good idea to turn this off.
	 */
	private boolean checkingForAutoTriggeredUpdateFailures = true;
	
	/** Maps a set of 'handles' (unique Strings) to prepared statements (&lt;String, PreparedStatement>)*/
	private HashMap /* String, PreparedStatement */ preparedStatements = new HashMap();
	/** Maps a set of 'handles' (unique Strings) to batch sizes (&lt;String, Integer>)*/
	private HashMap /* String, Integer */ batchSizes = new HashMap();
	/** Maps a set of 'handles' (unique Strings) to batch priorities (&lt;String, Integer>). Note that
	 * priorities must be nonnegative numbers and the LOWER the number the HIGHER the priority. Hence
	 * priority zero is always given precedence. */
	private HashMap /* String, Integer */ batchPriorities = new HashMap();
	/** For convenience, maps priorities back to groups of batches that share the same priority (&lt;Integer, ArrayList&lt;String>>)*/
	private HashMap /* Integer, ArrayList<String> */ priorityGroups = new HashMap();
	/** The database connection to use */
	private Connection connection = null;
	/** A message to indicate what error occurred during a batch execution */
	private String batchUpdateExceptionMessage = null;
	
	public BatchManager(Connection connection)
	{
		setConnection(connection);
	}

	public Connection getConnection()
	{
		return connection;
	}

	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}
	
	public int getThreshold()
	{
		return threshold;
	}

	public void setThreshold(int threshold)
	{
		if (threshold <= 0)
			this.threshold = 1;
		else
			this.threshold = threshold;
	}

	public boolean isAutoTriggered()
	{
		return autoTriggered;
	}

	public void setAutoTriggered(boolean autoTriggered)
	{
		this.autoTriggered = autoTriggered;
	}

	public boolean isCheckingForAutoTriggeredUpdateFailures()
	{
		return checkingForAutoTriggeredUpdateFailures;
	}

	public void setCheckingForAutoTriggeredUpdateFailures(boolean checkingForAutoTriggeredUpdateFailures)
	{
		this.checkingForAutoTriggeredUpdateFailures = checkingForAutoTriggeredUpdateFailures;
	}

	public String getBatchUpdateExceptionMessage()
	{
		return batchUpdateExceptionMessage;
	}

	/**
	 * Adds a new query to be managed by the BatchManager. The query is converted internally to
	 * a prepared statement, and assigned the given handle and priority.
	 * 
	 * @param handle a unique name to refer to the batch easily
	 * @param query the query to build a prepared statement around
	 * @param priority a nonnegative number, where numbers closer to zero have HIGHER priority
	 * @throws SQLException
	 */
	public void addQuery(String handle, String query, Integer priority)
		throws SQLException
	{
		if (preparedStatements.containsKey(handle))
			throw new IllegalArgumentException("Query with that handle already exists!");
		if (priority.intValue() < 0)
			throw new IllegalArgumentException("Priorities must be at least zero (highest priority).");

		PreparedStatement preparedStatement = connection.prepareStatement(query);
		preparedStatements.put(handle, preparedStatement);
		batchSizes.put(handle, new Integer(0));

		batchPriorities.put(handle, priority);
		
		// For convenience, place the new handle within the group of already existing
		// handles (if any) that have the same priority. If no existing handles have the
		// same priority, a new group is created.
		ArrayList priorityGroup = (ArrayList) priorityGroups.get(priority);
		if (priorityGroup == null)
			priorityGroup = new ArrayList();
		priorityGroup.add(handle);
		priorityGroups.put(priority, priorityGroup);
	}
	
	/**
	 * Removes a query (prepared statement) from the BatchManager.
	 *  
	 * @param handle the query's handle (see addQuery)
	 */
	public void removeQuery(String handle)
	{
		PreparedStatement preparedStatement = (PreparedStatement) preparedStatements.get(handle);
		if (preparedStatement != null)
		{
			try { preparedStatement.close(); } catch (Exception e) {}
			preparedStatements.remove(handle);
			batchSizes.remove(handle);

			Integer priority = (Integer) batchPriorities.get(handle);
			((ArrayList) priorityGroups.get(priority)).remove(handle);
			batchPriorities.remove(handle);
		}
	}
	
	/**
	 * Returns the current batch size of the particular query (referenced by its handle).
	 * 
	 * @param handle the query's handle (see addQuery)
	 * @return current batch size for that query
	 */
	public int getBatchSize(String handle)
	{
		return ((Integer) batchSizes.get(handle)).intValue();
	}
	
	/**
	 * <p>
	 * For a given query (referenced by its handle), this method will add to its batch
	 * another instance of parameters.
	 * </p>
	 * 
	 * <p>
	 * Note that the method setObject from class PreparedStatement is used internally
	 * to set the values. Hence, there may be some rare cases where an array of types
	 * needs to be sent to this method as well (when the standard JDBC specification
	 * mapping is insufficient).
	 * </p>
	 * 
	 * @param handle the query's handle (see addQuery)
	 * @param values the array (hence order is important) of values to use as parameters for the query
	 * @param types the array of value types - this may be null in which case the standard JDBC specification
	 * mapping from Java Object's to SQL types will be used.
	 * @throws SQLException
	 */
	public void addParameters(String handle, Object values[], int types[])
		throws SQLException
	{
		PreparedStatement preparedStatement = (PreparedStatement) preparedStatements.get(handle);
		if (preparedStatement == null)
			throw new IllegalArgumentException("Handle not found.");
		
		for (int i=0; i<values.length; i++)
		{
			if (types == null)
				preparedStatement.setObject(i+1, values[i]);
			else
				preparedStatement.setObject(i+1, values[i], types[i]);
		}

		preparedStatement.addBatch();
		
		if (handle.equals("BottlerBusinessUnitToBottlerMarketUnitMappingCreate") || handle.equals("BottlerMarketUnitCreate") || handle.equals("BottlerBusinessUnitCreate"))
		{
			// MainLog.getLog().debug("Adding parameters to " + handle + ": " + ((OraclePreparedStatement) preparedStatements.get( handle )).getOriginalSql());
			String valuesString = "";
			for (int i=0; i<values.length; i++)
			{
				valuesString += values[i].toString() + ", ";
			}
			MainLog.getLog().debug("Values: " + valuesString + "\n");
		}
		
		batchSizes.put(handle, new Integer( ((Integer) batchSizes.get(handle)).intValue() + 1) );
		if (autoTriggered)
			triggeredExecute(handle);
	}

	/**
	 * <p>
	 * For a given query (referenced by its handle), this method will add to its batch
	 * another instance of parameters taken from the fields of the bean. The bean itself
	 * is responsible for doing the operation ultimately, using its addToBatch method
	 * (see BasePersistentBean).
	 * </p>
	 * 
	 * @param handle the query's handle (see addQuery)
	 * @param bean the bean to use as a source of data
	 * @param batchType the particular batchType: create, read, update, delete
	 * @throws SQLException
	 */
	public void addBeanParameters(String handle, BasePersistentBean bean, int batchType)
		throws SQLException
	{
		bean.addToBatch(((PreparedStatement) preparedStatements.get(handle)), batchType, true);
		
		if (handle.equals("BottlerBusinessUnitToBottlerMarketUnitMappingCreate") || handle.equals("BottlerMarketUnitCreate") || handle.equals("BottlerBusinessUnitCreate"))
		{
			// MainLog.getLog().debug("Adding parameters to " + handle + ": " + ((OraclePreparedStatement) preparedStatements.get( handle )).getOriginalSql());
			MainLog.getLog().debug("Values: " + bean.getFieldUpdatesAsString() + "\n");
		}

		batchSizes.put(handle, new Integer( ((Integer) batchSizes.get(handle)).intValue() + 1) );
		if (autoTriggered)
			triggeredExecute(handle);
	}
	
	/**
	 * Attempt to execute the batch for a given query (referenced by its handle). The execute
	 * may not occur if the threshold batch size has not been reached. This method will call
	 * the execute method; the reason for this particular method is simply to store possible
	 * exceptions on triggered executes. Note that as the caller of the execute method, this
	 * method is responsible for deciding whether to rollback or commit from batch update
	 * failures. The current design is that for safety's sake, an exception is thrown which
	 * will probably result in rollback.
	 * 
	 * @param handle the query's handle (see addQuery)
	 * @throws SQLException
	 */
	private void triggeredExecute(String handle)
		throws SQLException
	{
		if (!checkingForAutoTriggeredUpdateFailures)
			execute(handle, false);
		else
		{
			int results[] = execute(handle, false);
			if (results != null)
				for (int i=0; i<results.length; i++)
					if (results[i] == -3) 
						throw new SQLException("Failure to update on triggered execute" +
							(batchUpdateExceptionMessage == null ? "" : " (" + batchUpdateExceptionMessage + ")"));
		}
	}
	
	/**
	 * <p>
	 * Execute a particular batch for a query (referenced by its handle).
	 * </p>
	 * 
	 * <p>
	 * Note that if the particular batch to execute has other batches with higher priorities
	 * (technically with lower numbers - i.e. zero has highest priority), then those batches
	 * will be automatically executed first in priority order. This allows the batch manager
	 * to avoid integrity constraint errors if managing a group of batches that have
	 * relationships between them that need to be respected during insertion or update or
	 * deletion.
	 * </p>
	 * 
	 * @param handle the query's handle (see addQuery)
	 * @param force whether to force execution or not; if not, then execution will only proceed if
	 * the minimum batch size (the threshold) has been reached
	 * @return an array of batch update results which the caller is responsible for checking and
	 * determining whether to rollback or commit anyway
	 * @throws SQLException
	 */
	public int[] execute(String handle, boolean force)
		throws SQLException
	{
		int[] results = null;

		try
		{
			int batchSize = ((Integer) batchSizes.get(handle)).intValue();
			
			if (batchSize >= threshold || force)
			{
				Integer priority = (Integer) batchPriorities.get(handle);
				Set sortedPriorities = new TreeSet( priorityGroups.keySet() );
				Iterator iterator = sortedPriorities.iterator();
				
				// Execute higher priorities first
				while (iterator.hasNext())
				{
					Integer groupPriority = (Integer) iterator.next();
					if (groupPriority.intValue() < priority.intValue())
					{
						ArrayList handles = (ArrayList) priorityGroups.get( groupPriority );
						
						for (int i=0; i<handles.size(); i++)
						{
							String higherPriorityHandle = (String) handles.get(i);
							int higherPriorityHandleBatchSize = ((Integer) batchSizes.get(higherPriorityHandle)).intValue();
							
							if (higherPriorityHandleBatchSize > 0)
							{
								MainLog.getLog().debug("Due to higher priority, executing batch for: " + higherPriorityHandle + " ... ");
								// MainLog.getLog().debug( ((OraclePreparedStatement) preparedStatements.get( higherPriorityHandle )).getOriginalSql() );

								// MainLog.getLog().debug( ((OraclePreparedStatement) preparedStatements.get( higherPriorityHandle )) );

								results = ((PreparedStatement) preparedStatements.get( higherPriorityHandle )).executeBatch();
								MainLog.getLog().debug(results.length + " results\n");
		
								batchSizes.put(handles.get(i), new Integer(0));
							}
						}
					}
					else
						break;
				}
	
				if (batchSize > 0)
				{
					MainLog.getLog().debug("Executing batch for: " + handle + " ... ");
					// MainLog.getLog().debug( ((OraclePreparedStatement) preparedStatements.get( handle )).getOriginalSql() );
					results = ((PreparedStatement) preparedStatements.get(handle)).executeBatch();
					MainLog.getLog().debug(results.length + " results\n");
					
					batchSizes.put(handle, new Integer(0));
				}
			}
			return results;
		}
		catch (BatchUpdateException be)
		{
			/*
			int[] updateCounts = be.getUpdateCounts();
			if (updateCounts != null)
			{
				for (int i=0; i<updateCounts.length; i++)
					MainLog.getLog().error("(Update counts) for i=" + i + ": " + updateCounts[i]);
			}
			*/
			MainLog.getLog().error("BatchUpdateException");
			MainLog.getLog().error("Underlying exception stack trace");
			SQLException sqe = be;
			do
			{
				MainLog.getLog().error("Exception message: " + sqe.getMessage());
				MainLog.getLog().error("Exception in chain: ", sqe);
				sqe = sqe.getNextException();
			}
			while (sqe != null);
			throw be;
		}
	}
	
	/**
	 * Execute all currently stored queries (referenced by handle). This method should
	 * ALWAYS be called by a user of a BatchManager at the end in order to flush any
	 * batches that have not reached their thresholds. Naturally in such a case, the force
	 * parameter should be true.
	 * 
	 * @param force whether to force execution or not; if not, only if the batch size 
	 * threshold has been reached (each handle is checked independently)
	 * @return a map of handles to batch update results
	 * @throws SQLException
	 */
	public Map /*String, int[]*/ executeAll(boolean force)
		throws SQLException 
	{
		Set handles = preparedStatements.keySet();
		if (handles.size() == 0) return null;

		HashMap allResults = new HashMap();
		Iterator iterator = handles.iterator();
		
		while (iterator.hasNext())
		{
			String handle = (String) iterator.next();

			int results[] = execute(handle, force);
			if (results != null)
				for (int i=0; i<results.length; i++)
					if (results[i] == -3) 
						throw new SQLException("Failure to update on explicit execute" +
							(batchUpdateExceptionMessage == null ? "" : " (" + batchUpdateExceptionMessage + ")"));

			allResults.put(handle, results);
		}
		return allResults;
	}

	/**
	 * Removes all batches from the queries - however the queries remain.
	 * 
	 * @throws SQLException
	 */
	public void clearAll()
		throws SQLException
	{
		Set handles = preparedStatements.keySet();
		Iterator iterator = handles.iterator();
		
		while (iterator.hasNext())
		{
			String handle = (String) iterator.next();
			((PreparedStatement) preparedStatements.get(handle)).clearBatch();
			batchSizes.put(handle, new Integer(0));
		}		
	}

	/**
	 * Closes all existing queries, and removes them.
	 * 
	 * @throws SQLException
	 */
	public void closeAll()
		throws SQLException
	{
		Set handles = preparedStatements.keySet();
		Iterator iterator = handles.iterator();
		
		while (iterator.hasNext())
			((PreparedStatement) preparedStatements.get( (String) iterator.next() )).close();

		preparedStatements.clear();
		batchSizes.clear();
	}
	
	protected void finalize()
	{
		try { closeAll(); } catch (Exception e) {}
	}
}
