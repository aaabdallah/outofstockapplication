package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * <p>
 * An abstract class that is a superclass for other persistent beans. It represents
 * tables that have a name column and a timeuploaded column.
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public abstract class NamedUploadable extends Uploadable
{
	// ----- Static members ---------------------------------------------------

	/**
	 * Finds namedUploadables by their property <code>'name'</code>.
	 * @param connection the connection to use (null --> automatically created)
	 * @param tableName database table name
	 * @param typeOfBean type of bean to create
	 * @param name the name to use for finding
	 */
	public static ListOrderedMap findByName(Connection connection, String tableName, Class typeOfBean, String name)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, tableName, "name = " + BQ + dq(name) + EQ, typeOfBean, null, "name");
	}

	// ----- Instance members -------------------------------------------------
	private String name;

	public NamedUploadable()
	{
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	// ----- Overridden abstract methods --------------------------------------
	public void resetFields()
	{
		super.resetFields();
		name = null;
	}
	
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", name"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 1;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			", " + BQ + dq(name) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", name = " + BQ + dq(name) + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setName(resultSet.getString("name"));
	}
	
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setString(index++, name);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
