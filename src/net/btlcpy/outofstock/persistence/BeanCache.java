package net.btlcpy.outofstock.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A class that represents a cache in RAM of a particular database table. Only tables that rarely
 * change and that are often accessed should be cached (i.e. 'read-only' tables). Examples include
 * the tables for bottlers, Distributor divisions, and bottler sales routes.
 * @author Ahmed A. Abd-Allah
 */
public class BeanCache
{
	/** The rows of the table are stored as a list ordered map of beans. These beans will typically
	 * inherit from <code>net.btlcpy.outofstock.persistence.beans.BasePersistentBean</code>.*/
	private ListOrderedMap beans = null;

	/** The database table name */
	private String tableName;
	
	/** An SQL-phrased set of conditions to use ("WHERE &lt;conditions&gt;") whilst retrieving
	 * data for caching purposes. */
	private String conditions;
	
	/** The type of bean that is going to be cached. */
	private Class typeOfBean;
	
	/** The columns that will be used as keys to the beans in the cache (see {@link #beans}).*/
	private String keyColumns[];
	
	/** The SQL phrase that represents the ordering columns to use on retrieval. Examples:
	 * "firstname", "firstname, lastname". Do not include the words "ORDER BY". */
	private String orderBy;
	
	public BeanCache(Connection connection, String tableName, String conditions, Class typeOfBean, 
		String keyColumns[], String orderBy)
	{
		setTableName(tableName);
		setConditions(conditions);
		setTypeOfBean(typeOfBean);
		setKeyColumns(keyColumns);
		setOrderBy(orderBy);

		try
		{
			reload(connection);
		}
		catch (Exception e)
		{
			beans = null;
		}
	}
	
	public ListOrderedMap getBeans()
	{
		return beans;
	}

	public void setBeans(ListOrderedMap beans)
	{
		this.beans = beans;
	}

	public String getTableName()
	{
		return tableName;
	}

	public void setTableName(String tableName)
	{
		this.tableName = tableName;
	}

	public String getConditions()
	{
		return conditions;
	}

	public void setConditions(String conditions)
	{
		this.conditions = conditions;
	}

	public Class getTypeOfBean()
	{
		return typeOfBean;
	}

	public void setTypeOfBean(Class typeOfBean)
	{
		this.typeOfBean = typeOfBean;
	}

	public String[] getKeyColumns()
	{
		return keyColumns;
	}

	public void setKeyColumns(String[] keyColumns)
	{
		this.keyColumns = keyColumns;
	}

	public String getOrderBy()
	{
		return orderBy;
	}

	public void setOrderBy(String orderBy)
	{
		this.orderBy = orderBy;
	}

	/**
	 * Reloads the cache using a simple select query on the database.
	 * 
	 * @param connection connection to use, if null then a new connection is automatically created
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void reload(Connection connection)
		throws SQLException, IllegalAccessException, InstantiationException
	{
		boolean useOwnConnection = (connection == null);

		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			//MainLog.getLog().debug("\n\n\n************************* PRE-RELOAD CACHE: " + typeOfBean.getName() + " " + beans + " *****************************\n\n\n\n");

			beans = PersistenceManager.getPersistenceManager().
				findBeans(connection, tableName, conditions, typeOfBean, keyColumns, orderBy);
			
			//if (useOwnConnection)
			//	connection.commit();

			//MainLog.getLog().debug("\n\n\n************************* POST-RELOAD CACHE: " + typeOfBean.getName() + " " + beans + " *****************************\n\n\n\n");
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}
}
