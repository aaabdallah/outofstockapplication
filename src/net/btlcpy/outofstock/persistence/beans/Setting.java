package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;


/**
 * A persistent bean to represent simple application settings. A setting consists
 * of a trio of strings: a category, a name, and a value. The semantics of these
 * strings are entirely application dependent.  
 * 
 * @author Ahmed Abd-Allah
 */
public class Setting extends Instrumented
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "settings";
	
	/**
	 * Finds setting given a category, name, and/or value.
	 * @param connection connection to use, if null, creates a new connection
	 * @param category category to search for
	 * @param name name to search for
	 * @param value value to search for
	 * @return a list ordered map of settings
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByCategoryNameValue(Connection connection, 
		String category, String name, String value)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		boolean useOwnConnection = (connection == null);
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			boolean where = false;
			
			if (category != null)
			{
				category = " category = " + BQ + dq(category.trim()) + EQ + " ";
				where = true;
			}
			else
				category = " ";
			
			if (name != null)
			{
				name = (where ? " AND " : "") + " name = " + BQ + dq(name.trim()) + EQ + " ";
				where = true;
			}
			else
				name = " ";
			
			if (value != null)
			{
				value = (where ? " AND " : "") + " value = " + BQ + dq(value.trim()) + EQ + " ";
				where = true;
			}
			else
				value = " ";
			
			return PersistenceManager.getPersistenceManager().
				findBeans(connection, 
					"SELECT * FROM settings " + (where ? "WHERE " : "") + 
					category + name + value + 
					"ORDER BY category, name, value", 
					Setting.class, null);
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to retrieve settings", e);
			return null;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	// ----- Instance members -------------------------------------------------
	private String category;
	private String name;
	private String value;

	public Setting()
	{
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		category = null;
		name = null;
		value = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + 
			", category, name, value";
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 3;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() +
			", " + BQ + dq(category) + EQ +
			", " + BQ + dq(name) + EQ +
			", " + BQ + dq(value) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", category = " + BQ + dq(category) + EQ +
			", name = " + BQ + dq(name) + EQ +
			", value = " + BQ + dq(value) + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setCategory(resultSet.getString("category"));
		setName(resultSet.getString("name"));
		setValue(resultSet.getString("value"));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setString(index++, category);
			preparedStatement.setString(index++, name);
			preparedStatement.setString(index++, value);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
