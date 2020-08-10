package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * A bean to represent bottler branch to store mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerBranchToStoreMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrbrchstostores";
	
	// ----- Instance members -------------------------------------------------
	private Integer bottlerBranch;
	private Integer store;

	public BottlerBranchToStoreMapping()
	{
	}

	public Integer getBottlerBranch()
	{
		return bottlerBranch;
	}

	public void setBottlerBranch(Integer bottlerBranch)
	{
		this.bottlerBranch = bottlerBranch;
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
		bottlerBranch = null;
		store = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", bottlerbranch, store"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + bottlerBranch.intValue() + 
			", " + store.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", bottlerbranch = " + bottlerBranch.intValue() +
			", store = " + store.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setBottlerBranch(new Integer(resultSet.getInt("bottlerbranch")));
		setStore(new Integer(resultSet.getInt("store")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, bottlerBranch.intValue());
			preparedStatement.setInt(index++, store.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}	
}
