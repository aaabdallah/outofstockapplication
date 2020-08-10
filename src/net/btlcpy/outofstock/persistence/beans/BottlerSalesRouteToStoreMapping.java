package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bean to represent bottler sales route to store mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerSalesRouteToStoreMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrslsrtstostores";
	
	// ----- Instance members -------------------------------------------------
	private Integer bottlerSalesRoute;
	private Integer store;

	public BottlerSalesRouteToStoreMapping()
	{
	}

	public Integer getBottlerSalesRoute()
	{
		return bottlerSalesRoute;
	}

	public void setBottlerSalesRoute(Integer bottlerSalesRoute)
	{
		this.bottlerSalesRoute = bottlerSalesRoute;
	}

	public Integer getStore()
	{
		return store;
	}

	public void setStore(Integer store)
	{
		this.store = store;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		bottlerSalesRoute = null;
		store = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", bottlersalesroute, store"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + bottlerSalesRoute.intValue() + 
			", " + store.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", bottlersalesroute = " + bottlerSalesRoute.intValue() +
			", store = " + store.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setBottlerSalesRoute(new Integer(resultSet.getInt("bottlersalesroute")));
		setStore(new Integer(resultSet.getInt("store")));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, bottlerSalesRoute.intValue());
			preparedStatement.setInt(index++, store.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
