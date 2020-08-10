package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bean to represent bottler market unit to bottler branch mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerMarketUnitToBottlerBranchMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrmktuntstobttlrbrchs";
	
	// ----- Instance members -------------------------------------------------
	private Integer bottlerMarketUnit;
	private Integer bottlerBranch;
	
	public BottlerMarketUnitToBottlerBranchMapping()
	{
	}

	public Integer getBottlerMarketUnit()
	{
		return bottlerMarketUnit;
	}

	public void setBottlerMarketUnit(Integer bottlerMarketUnit)
	{
		this.bottlerMarketUnit = bottlerMarketUnit;
	}

	public Integer getBottlerBranch()
	{
		return bottlerBranch;
	}

	public void setBottlerBranch(Integer bottlerBranch)
	{
		this.bottlerBranch = bottlerBranch;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		bottlerMarketUnit = null;
		bottlerBranch = null;
	}
	
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", bottlermarketunit, bottlerbranch"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + bottlerMarketUnit.intValue() + 
			", " + bottlerBranch.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", bottlermarketunit = " + bottlerMarketUnit.intValue() +
			", bottlerbranch = " + bottlerBranch.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setBottlerMarketUnit(new Integer(resultSet.getInt("bottlermarketunit")));
		setBottlerBranch(new Integer(resultSet.getInt("bottlerbranch")));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
	
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, bottlerMarketUnit.intValue());
			preparedStatement.setInt(index++, bottlerBranch.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
