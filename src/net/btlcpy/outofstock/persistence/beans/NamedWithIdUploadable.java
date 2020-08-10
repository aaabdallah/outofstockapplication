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
 * tables that have a name column, a timeuploaded column, and an id column (an integer).
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public abstract class NamedWithIdUploadable extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/**
	 * Finds namedWithIdUploadables by their property <code>'id'</code>.
	 * @param connection the connection to use (null --> automatically created)
	 * @param tableName database table name
	 * @param typeOfBean type of bean to create
	 * @param id the id to use for finding
	 */
	public static ListOrderedMap findById(Connection connection, String tableName, Class typeOfBean, Integer id)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, tableName, "id = " + id.intValue(), typeOfBean, null, "id");
	}

	// ----- Instance members -------------------------------------------------
	private Integer id;

	public NamedWithIdUploadable()
	{
	}
	
	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	// ----- Overridden abstract methods --------------------------------------
	public void resetFields()
	{
		super.resetFields();
		id = null;
	}
	
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", id"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 1;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + 
			", " + BQ + id.toString() + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", id = " + BQ + id.toString() + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setId(new Integer(resultSet.getInt("id")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, id.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
