package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;

import net.btlcpy.outofstock.persistence.BeanCache;

/**
 * A persistent bean to represent product categories.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductCategory extends NamedWithIdUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctctgrs";

	/** Contains a cache of the product categories since this data rarely changes, and
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
			cache = new BeanCache(connection, tableName, "bitand(metaflags, 1)+0 = 0", ProductCategory.class, null, "name");
		return cache;
	}
	
	// ----- Instance members -------------------------------------------------
	public ProductCategory()
	{
	}
	
	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
