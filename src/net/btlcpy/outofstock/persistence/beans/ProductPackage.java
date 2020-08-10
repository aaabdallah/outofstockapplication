package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.BeanCache;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;


/**
 * A persistent bean to represent product packages.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductPackage extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctpkgs";

	/** Contains a cache of the product packages since this data rarely changes, and
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
			cache = new BeanCache(connection, tableName, null, ProductPackage.class, null, "name");
		return cache;
	}

	/** The number of active product packages (cached) */
	private static int totalActive = 0;
	public static int getTotalActive() { return totalActive; }

	/**
	 * Update the cached number of active product packages. Should be called when a new set of out of stock events
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
	public ProductPackage()
	{
	}

	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
