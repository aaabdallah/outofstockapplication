package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * <p>
 * An abstract class that is a superclass for other persistent beans. It represents
 * tables that have a timeuploaded column (in addition to the Instrumented
 * superclass's columns).
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public abstract class Uploadable extends Instrumented
{
	/**
	 * The time this row was last uploaded.
	 */
	private Timestamp timeLastUploaded;

	public Uploadable()
	{
	}

	public Timestamp getTimeLastUploaded()
	{
		return timeLastUploaded;
	}

	public void setTimeLastUploaded(Timestamp timeLastUploaded)
	{
		this.timeLastUploaded = timeLastUploaded;
	}

	// ----- Overridden abstract methods -------------------------------------- 

	public void resetFields()
	{
		super.resetFields();
		timeLastUploaded = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", timelastuploaded"; 
	}

	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 1;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			(timeLastUploaded == null ? ", null" : ", TIMESTAMP " + BQ + timeLastUploaded.toString() + EQ);
	}

	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", timelastuploaded = " + 
			(timeLastUploaded == null ? ", null" : "TIMESTAMP " + BQ + timeLastUploaded.toString() + EQ);
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setTimeLastUploaded(resultSet.getTimestamp("timelastuploaded"));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);

		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setTimestamp(index++, timeLastUploaded);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
