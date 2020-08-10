package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;

import org.apache.commons.collections.map.ListOrderedMap;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.BeanCache;
import net.btlcpy.outofstock.persistence.PersistenceManager;

/**
 * A persistent bean to represent Bottling Company bottlers.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class Bottler extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrs";

	/** Contains a cache of the bottlers since this data rarely changes, and
	 * is often seen since it is immediately seen when opening the report
	 * parameter specification page.
	 */
	private static BeanCache cache = null;
	/**
	 * Will return the current cache. If not found, will force a load of the cache.
	 * @param connection the connection to use (if null, will open a new connection automatically)
	 * @return the cache
	 */
	public static BeanCache getCache(Connection connection)
	{
		if (cache == null)
			cache = new BeanCache(connection, tableName, null, Bottler.class, null, "name");
		return cache;
	}

	/** The number of active bottlers (cached) */
	private static int totalActive = 0;
	public static int getTotalActive() { return totalActive; }

	/**
	 * Update the cached number of active bottlers. Should be called when a new store list
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
	public Bottler()
	{
	}

	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
