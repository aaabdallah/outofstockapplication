package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * A persistent bean to represent Distributor division to Distributor district mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class DistributorDivisionToDistributorDistrictMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "dstbdvsnstodstbdstrcts";
	
	// ----- Instance members -------------------------------------------------
	private Integer distributorDivision;
	private Integer distributorDistrict;

	public DistributorDivisionToDistributorDistrictMapping()
	{
	}

	public Integer getDistributorDivision()
	{
		return distributorDivision;
	}

	public void setDistributorDivision(Integer distributorDivision)
	{
		this.distributorDivision = distributorDivision;
	}

	public Integer getDistributorDistrict()
	{
		return distributorDistrict;
	}

	public void setDistributorDistrict(Integer distributorDistrict)
	{
		this.distributorDistrict = distributorDistrict;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		distributorDivision = null;
		distributorDistrict = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", distributordivision, distributordistrict"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + distributorDivision.intValue() + 
			", " + distributorDistrict.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", distributordivision = " + distributorDivision.intValue() +
			", distributordistrict = " + distributorDistrict.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setDistributorDivision(new Integer(resultSet.getInt("distributordivision")));
		setDistributorDistrict(new Integer(resultSet.getInt("distributordistrict")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, distributorDivision.intValue());
			preparedStatement.setInt(index++, distributorDistrict.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
