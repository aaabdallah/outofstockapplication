package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bean to represent bottler business unit to bottler market unit mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerBusinessUnitToBottlerMarketUnitMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrbsnsuntstobttlrmktunts";
	
	// ----- Instance members -------------------------------------------------
	private Integer bottlerBusinessUnit;
	private Integer bottlerMarketUnit;

	public BottlerBusinessUnitToBottlerMarketUnitMapping()
	{
	}

	public Integer getBottlerBusinessUnit()
	{
		return bottlerBusinessUnit;
	}

	public void setBottlerBusinessUnit(Integer bottlerBusinessUnit)
	{
		this.bottlerBusinessUnit = bottlerBusinessUnit;
	}

	public Integer getBottlerMarketUnit()
	{
		return bottlerMarketUnit;
	}

	public void setBottlerMarketUnit(Integer bottlerMarketUnit)
	{
		this.bottlerMarketUnit = bottlerMarketUnit;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		bottlerBusinessUnit = null;
		bottlerMarketUnit = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", bottlerbusinessunit, bottlermarketunit"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + bottlerBusinessUnit.intValue() + 
			", " + bottlerMarketUnit.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", bottlerbusinessunit = " + bottlerBusinessUnit.intValue() +
			", bottlermarketunit = " + bottlerMarketUnit.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setBottlerBusinessUnit(new Integer(resultSet.getInt("bottlerbusinessunit")));
		setBottlerMarketUnit(new Integer(resultSet.getInt("bottlermarketunit")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, bottlerBusinessUnit.intValue());
			preparedStatement.setInt(index++, bottlerMarketUnit.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
