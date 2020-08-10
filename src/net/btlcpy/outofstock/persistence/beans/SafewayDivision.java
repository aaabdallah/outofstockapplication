package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;

import net.btlcpy.outofstock.persistence.BeanCache;

/**
 * A persistent bean to represent Distributor divisions.
 * 
 * @author Ahmed Abd-Allah
 */
public class DistributorDivision extends NamedWithIdUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "dstbdvsns";

	/** Contains a cache of the distributor divisions since this data rarely changes, and
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
			cache = new BeanCache(connection, tableName, null, DistributorDivision.class, null, "name");
		return cache;
	}

	// ----- Instance members -------------------------------------------------
	public DistributorDivision()
	{
	}
	
	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
