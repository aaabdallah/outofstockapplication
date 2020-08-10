package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;


/**
 * A persistent bean to represent Distributor districts.
 * 
 * @author Ahmed Abd-Allah
 */
public class DistributorDistrict extends NamedWithIdUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "dstbdstrcts";

	/**
	 * Finds distributor districts for a given distributor division.
	 * @param connection connection to use, if null, creates a new connection
	 * @param distributorDivisions primary keys of each distributor division desired
	 * @return a list ordered map of distributor districts
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByDistributorDivision(Connection connection, Integer[] distributorDivisions)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(distributorDivisions[0]);
		for (int i=1; i<distributorDivisions.length; i++)
		{
			sb.append(", " + distributorDivisions[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT dstbdstrcts.* FROM dstbdstrcts, dstbdvsnstodstbdstrcts WHERE " +
				"dstbdvsnstodstbdstrcts.distributordivision IN (" + sb.toString() + ") AND " +
				"dstbdvsnstodstbdstrcts.distributordistrict = dstbdstrcts.primarykey " +
				"ORDER BY name", 
				DistributorDistrict.class, null);
	}

	/** The number of active distributor districts (cached) */
	private static int totalActive = 0;
	public static int getTotalActive() { return totalActive; }

	/**
	 * Update the cached number of active distributor districts. Should be called when a new store list
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
	private String cd;

	public DistributorDistrict()
	{
	}
	
	public String getCd()
	{
		return cd;
	}

	public void setCd(String cd)
	{
		this.cd = cd;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		cd = null;
	}
	
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", cd"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 1;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			", " + BQ + dq(cd) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", cd = " + BQ + dq(cd) + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setCd(resultSet.getString("cd"));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setString(index++, cd);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
