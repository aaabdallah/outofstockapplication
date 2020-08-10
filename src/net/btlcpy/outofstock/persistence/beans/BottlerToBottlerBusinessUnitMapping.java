package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A bean to represent bottler to bottler business unit mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerToBottlerBusinessUnitMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrstobttlrbsnsunts";
	
	// ----- Instance members -------------------------------------------------
	private Integer bottler;
	private Integer bottlerBusinessUnit;

	public BottlerToBottlerBusinessUnitMapping()
	{
	}

	public Integer getBottler()
	{
		return bottler;
	}

	public void setBottler(Integer bottler)
	{
		this.bottler = bottler;
	}

	public Integer getBottlerBusinessUnit()
	{
		return bottlerBusinessUnit;
	}

	public void setBottlerBusinessUnit(Integer bottlerBusinessUnit)
	{
		this.bottlerBusinessUnit = bottlerBusinessUnit;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		bottler = null;
		bottlerBusinessUnit = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", bottler, bottlerbusinessunit"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + bottler.intValue() + 
			", " + bottlerBusinessUnit.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", bottler = " + bottler.intValue() +
			", bottlerbusinessunit = " + bottlerBusinessUnit.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setBottler(new Integer(resultSet.getInt("bottler")));
		setBottlerBusinessUnit(new Integer(resultSet.getInt("bottlerbusinessunit")));
	}
	
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, bottler.intValue());
			preparedStatement.setInt(index++, bottlerBusinessUnit.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
