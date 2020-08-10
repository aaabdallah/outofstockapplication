package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.exceptions.FindException;

/**
 * <p>
 * An abstract class that is a superclass for other persistent beans. It represents
 * tables that have a trio of 'instrumentation' columns: a primary key, a set of
 * meta flags, and the time the row was created. There is no database table that
 * corresponds to this class directly: it is meant to be inherited by other classes
 * that do represent real tables with these three columns.
 * </p>
 * 
 * <p>
 * Note that primary keys are assumed to be generated by the database, but they
 * are retrieved in a call just before create - see the overridden method preCreate.
 * </p> 
 * 
 * @author Ahmed A. Abd-Allah
 */
public abstract class Instrumented extends BasePersistentBean
{
	/** 
	 * Represents the primary key for the table. Automatically generated. Integer size (4 bytes, signed). 
	 */
	private Integer primaryKey;
	/**
	 * Bit 0: 1 --> Data should be ignored during searches and reports
	 */
	private Integer metaFlags;
	/**
	 * The time the row was created. Automatically generated by the database; programmatic changes are
	 * not reflected (i.e. setting it via Java doesn't change it in the database ever).
	 */
	private Timestamp timeCreated;

	public Instrumented()
	{
	}

	public Integer getPrimaryKey()
	{
		return primaryKey;
	}

	public void setPrimaryKey(Integer primaryKey)
	{
		this.primaryKey = primaryKey;
	}

	public Integer getMetaFlags()
	{
		return metaFlags;
	}

	public void setMetaFlags(Integer metaFlags)
	{
		this.metaFlags = metaFlags;
	}

	public Timestamp getTimeCreated()
	{
		return timeCreated;
	}

	public void setTimeCreated(Timestamp timeCreated)
	{
		this.timeCreated = timeCreated;
	}

	// ----- Overridden abstract methods -------------------------------------- 

	public void resetFields()
	{
		primaryKey = null;
		metaFlags = null;
		timeCreated = null;
	}

	public String getFieldNames() 
	{ 
		return "primarykey" + (metaFlags == null ? "" : ", metaflags"); 
	}

	public int getFieldNameCount()
	{
		return 1 + (metaFlags == null ? 0 : 1);
	}
	
	public String getFieldValuesAsString() 
	{
		return primaryKey.intValue() +
			(metaFlags == null ? "" : ", " + metaFlags.intValue());
	}

	public String getFieldUpdatesAsString() 
	{
		return "primarykey = " + primaryKey.intValue() +
			(metaFlags == null ? "" : ", metaflags = " + metaFlags.intValue());
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		primaryKey = new Integer(resultSet.getInt("primarykey"));
		metaFlags = new Integer(resultSet.getInt("metaflags"));
		timeCreated = resultSet.getTimestamp("timecreated");
	}
	
	public Object getUniqueKey()
	{
		return primaryKey;
	}
	
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		if (batchType == BT_CREATE)
		{
			preCreate(preparedStatement.getConnection());
			preparedStatement.setInt(1, primaryKey.intValue());
			if (metaFlags != null)
				preparedStatement.setInt(2, metaFlags.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}

	// ----- Overridden lifecycle methods -------------------------------------
	/**
	 * This method is overridden to provide all objects of this class and derived
	 * subclasses automatically generated primary keys. The PersistenceManager class
	 * is responsible for giving beans unique primary keys via the method
	 * <code>generatePrimaryKey()</code>.
	 */
	public void preCreate(Connection connection)
		throws SQLException
	{
		primaryKey = PersistenceManager.getPersistenceManager().generatePrimaryKey(connection);
	}
	
	// ----- Other methods for ease-of-use ------------------------------------
	public void load(Connection connection)
		throws SQLException, FindException
	{
		load(connection, "primarykey = " + primaryKey.intValue());
	}
	
	public void update(Connection connection)
		throws SQLException
	{
		update(connection, "primarykey = " + primaryKey.intValue());
	}

	public void delete(Connection connection, boolean confirmDeletion)
		throws SQLException
	{
		delete(connection, "primarykey = " + primaryKey.intValue(), confirmDeletion);
	}
}