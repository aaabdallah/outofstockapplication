package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A persistent bean to represent Distributor district to store mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class DistributorDistrictToStoreMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "dstbdstrctstostores";
	
	// ----- Instance members -------------------------------------------------
	private Integer distributorDistrict;
	private Integer store;

	public DistributorDistrictToStoreMapping()
	{
	}

	public Integer getDistributorDistrict()
	{
		return distributorDistrict;
	}

	public void setDistributorDistrict(Integer distributorDistrict)
	{
		this.distributorDistrict = distributorDistrict;
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
		distributorDistrict = null;
		store = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", distributordistrict, store"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + distributorDistrict.intValue() + 
			", " + store.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", distributordistrict = " + distributorDistrict.intValue() +
			", store = " + store.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setDistributorDistrict(new Integer(resultSet.getInt("distributordistrict")));
		setStore(new Integer(resultSet.getInt("store")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
	
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, distributorDistrict.intValue());
			preparedStatement.setInt(index++, store.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
