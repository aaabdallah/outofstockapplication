package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A bean to represent Bottling Company bottler branches.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerBranch extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrbrchs";

	/**
	 * Finds bottler branches for a given bottler market unit.
	 * @param connection connection to use, if null, creates a new connection
	 * @param bottlerMarketUnits primary keys of each bottler market unit desired
	 * @return a list ordered map of bottler branchs
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByBottlerMarketUnit(Connection connection, Integer[] bottlerMarketUnits)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(bottlerMarketUnits[0]);
		for (int i=1; i<bottlerMarketUnits.length; i++)
		{
			sb.append(", " + bottlerMarketUnits[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT bttlrbrchs.* FROM bttlrbrchs, bttlrmktuntstobttlrbrchs WHERE " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit IN (" + sb.toString() + ") AND " +
				"bttlrmktuntstobttlrbrchs.bottlerbranch = bttlrbrchs.primarykey " +
				"ORDER BY name", 
				BottlerBranch.class, null);
	}

	/** The number of active bottler branches (cached) */
	private static int totalActive = 0;
	public static int getTotalActive() { return totalActive; }

	/**
	 * Update the cached number of active bottler branches. Should be called when a new store list
	 * is uploaded to the server.
	 * @param connection the connection to use, if null will use a new connection automatically
	 */
	public static void updateTotalActive(Connection connection)
	{
		boolean useOwnConnection = (connection == null);
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, "SELECT count(*) AS TOTAL FROM " + tableName + " WHERE bitand(metaflags, 1)+0 = 0", null);
		
			if (results != null && results.size() > 0)
				totalActive = ((Number) ((ListOrderedMap) results.getValue(0)).get("TOTAL")).intValue();
			else
				totalActive = 0;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update total active", e);
			totalActive = 0;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	// ----- Instance members -------------------------------------------------
	public BottlerBranch()
	{
	}
	
	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
