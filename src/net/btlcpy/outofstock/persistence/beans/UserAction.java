package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;


/**
 * A persistent bean to represent user actions.
 * 
 * @author Ahmed Abd-Allah
 */
public class UserAction extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "useractions";

	/** A user action to represent the running of a report. */
	public static final String REPORT = "Report";
	/** A user action to represent the uploading of data. */
	public static final String UPLOAD = "Upload";
	/** A user action to represent the deletion of out of stock events. */
	public static final String DELETEEVENTS = "Delete Events";
	
	public static String[] getCategories()
	{
		String[] categories = { REPORT, UPLOAD, DELETEEVENTS };
		return categories;
	}

	/** The earliest date for all user actions stored. */
	private static Timestamp earliestActionTime = null;
	public static Timestamp getEarliestActionTime() { return earliestActionTime; }
	
	/** Updates the earliest action time. Should be called once when the application starts up. */
	public static void updateEarliestActionTime()
	{
		Connection connection = null;

		try
		{
			connection = PersistenceManager.getPersistenceManager().getConnection(true);

			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, "SELECT MIN(timelastuploaded) AS earliest FROM " + tableName, null);

			if (results != null && results.size() > 0)
				earliestActionTime = (Timestamp) ((ListOrderedMap) results.getValue(0)).get("EARLIEST");

			if (earliestActionTime == null)
				earliestActionTime = new Timestamp(System.currentTimeMillis());
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update earliest action time", e);
			earliestActionTime = null;
		}
		finally
		{
			try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	private String category;
	private String description;

	public UserAction()
	{
		setName("unknown"); // to avoid seeing 'null' as the username when not found
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
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
		category = null;
		description = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", category, description"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			", " + BQ + dq(category) + EQ +
			", " + BQ + dq(description) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", category = " + BQ + dq(category) + EQ +
			", description = " + BQ + dq(description) + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setCategory(resultSet.getString("category"));
		setDescription(resultSet.getString("description"));
	}
	
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setString(index++, category);
			preparedStatement.setString(index++, description);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
