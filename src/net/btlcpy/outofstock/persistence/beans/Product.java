package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A persistent bean to represent products.
 * 
 * @author Ahmed Abd-Allah
 */
public class Product extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdcts";
	
	/**
	 * Finds products by their description.
	 * @param connection connection to use, if null, creates a new connection
	 * @param description product description
	 * @return a list ordered map of products
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByDescription(Connection connection, String description)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, tableName, "description = " + BQ + description + EQ, Product.class, null, "description");
	}
	
	/**
	 * Finds products by their product category.
	 * @param connection connection to use, if null, creates a new connection
	 * @param productCategories primary keys of each product category desired
	 * @return a list ordered map of products
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByProductCategory(Connection connection, Integer[] productCategories)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(productCategories[0]);
		for (int i=1; i<productCategories.length; i++)
		{
			sb.append(", " + productCategories[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT prdcts.* FROM prdcts, prdctctgrstoprdcts WHERE " +
				"prdctctgrstoprdcts.productcategory IN (" + sb.toString() + ") AND " +
				"prdctctgrstoprdcts.product = prdcts.primarykey " +
				"ORDER BY description", 
				Product.class, null);
	}

	/** The number of active products (cached) */
	private static int totalActive = 0;
	public static int getTotalActive() { return totalActive; }

	/**
	 * Update the cached number of active products. Should be called when a new set of out of stock events
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
	private Long upcId;
	private String description;

	public Product()
	{
	}

	public Long getUpcId()
	{
		return upcId;
	}

	public void setUpcId(Long upcId)
	{
		this.upcId = upcId;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		upcId = null;
		description = null;
	}
	
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", upcid, description"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			", " + upcId.toString() + 
			", " + BQ + dq(description) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", upcid = " + upcId.longValue() +
			", description = " + BQ + dq(description) + EQ; 
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setUpcId(new Long(resultSet.getLong("upcid")));
		setDescription(resultSet.getString("description"));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setLong(index++, upcId.longValue());
			preparedStatement.setString(index++, description);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
