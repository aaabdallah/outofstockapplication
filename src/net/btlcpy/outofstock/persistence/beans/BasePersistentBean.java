package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.exceptions.FindException;
import net.btlcpy.outofstock.utilities.StringUtils;

/**
 * <p>
 * This class represents the superclass at the root of all persistent classes. Its
 * purpose is to provide the subclasses with the ability to perform individual
 * CRUD operations: create, read (called load here), update, and delete.
 * </p>
 * 
 * <p>
 * Subclasses are assumed to map to specific tables in the database: one class per
 * table. Each subclass is also assumed to have a set of fields (properties) which
 * also map one to one with a column in the corresponding table. This superclass
 * provides a set of abstract methods that must be overridden in the subclasses in
 * order for the CRUD operations to work that are defined in this class.
 * </p>
 * 
 * <p>
 * Bulk update/delete operations are handled in the class PersistenceManager.
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
abstract public class BasePersistentBean
{
	// ----- Static members ---------------------------------------------------
	// The next two static members are used for quoting strings in SQL.
	// These are hardcoded for Oracle SQL compatibility, and to allow the Java
	// compiler to optimize away string concatenation since they are declared
	// final. The drawback is that switching the application from Oracle to
	// another type of database will require a recompile if the new database
	// does not support the same Oracle syntax for quoting... but the likelihood
	// of this is low for Bottling Company.
	// UPDATE: The Q quote operator is not supported in Oracle 9i. Back to
	// traditional quoting.
	public static final String BQ = "'"; // "Q'["; // "$$"; // PostgreSQL
	public static final String EQ = "'"; // "]'"; // "$$"; // PostgreSQL
	
	// Batch types
	public static final int BT_CREATE = 1;
	public static final int BT_LOAD = 1;
	public static final int BT_UPDATE = 1;
	public static final int BT_DELETE = 1;
	
	/**
	 * Returns all beans (rows) for a particular class (table).
	 * 
	 * @param connection the connection to use
	 * @param tableName the table name
	 * @param typeOfBean the class of the corresponding bean
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static Map findAll(Connection connection, String tableName, Class typeOfBean)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		return PersistenceManager.getPersistenceManager().findBeans(connection, tableName, null, typeOfBean, null, null);
	}
	
	/**
	 * Use this method to turn on quoting (replacing single quotes with double quotes).
	 * 
	 * @param unquotedString
	 * @return double quoted equivalent
	 */
	public static String dq(String unquotedString)
	{
		return StringUtils.replaceSubstrings(unquotedString, "'", "''");
	}

	// ----- Instance members -------------------------------------------------
	public BasePersistentBean()
	{
		resetFields();
	}

	// The following methods MUST be overridden by derived concrete classes
	/** Returns the table name. */
	abstract public String getTable();
	
	/** Returns the column names in a comma delimited list. */
	abstract public String getFieldNames();
	
	/** Returns the total number of columns in the table. */
	abstract public int getFieldNameCount();
	
	/** Returns the values of the bean fields in a comma delimited list. */
	abstract public String getFieldValuesAsString();
	
	/** Returns the values of the bean fields in an SQL update-compatible comma
	 * delimited list. */
	abstract public String getFieldUpdatesAsString();
	
	/** Loads (reads) the values from a resultset into the bean's fields. */
	abstract public void loadFields(ResultSet resultSet)
		throws SQLException;
	
	/** Resets the bean's fields to 'initial' values (usually null). */
	abstract public void resetFields();
	
	/** Returns a unique value that can be used to identify the bean in a hashtable. */
	abstract public Object getUniqueKey();
	
	/**
	 * <p> 
	 * Adds the bean's field values into a prepared statement, and optionally adds the
	 * bean operation (create, update, etc.) to the statement's batch. The reason why
	 * adding to the batch is optional is to allow this method to be overridden in chains
	 * of classes related by inheritance; the subclass would call the addToBatch method
	 * of the superclass but with false for the batchNow parameter. The caller of the
	 * addToBatch method for the subclass would send in true however.
	 * </p>
	 * 
	 * @param preparedStatement The prepared statement to use for filling in with the bean's
	 * fields, and also to use for adding the completed statement to the batch.
	 * @param batchType an indicator (create, load - i.e. read, update, delete)
	 * @param batchNow add to the statement's batch now or not.
	 * @throws SQLException
	 */
	abstract public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException;
	
	// The following methods MAY be overridden by derived classes
	/** Called before the create operation */
	public void preCreate(Connection connection) throws SQLException {}
	/** Called after the create operation */
	public void postCreate(Connection connection) throws SQLException {}
	/** Called before the load operation */
	public void preLoad(Connection connection) throws SQLException {}
	/** Called after the load operation */
	public void postLoad(Connection connection) throws SQLException {}
	/** Called before the update operation */
	public void preUpdate(Connection connection) throws SQLException {}
	/** Called after the update operation */
	public void postUpdate(Connection connection) throws SQLException {}
	/** Called before the delete operation */
	public void preDelete(Connection connection) throws SQLException {}
	/** Called after the delete operation */
	public void postDelete(Connection connection) throws SQLException {}

	/**
	 * <p>
	 * Constructs a string that represents an SQL 'INSERT' statement using the
	 * bean's fields. The values are not stuffed into the string however; instead
	 * placeholders are used to allow the statement to be used as a parameterized
	 * statement. The placeholders are question marks.
	 * </p>
	 * 
	 * <p>
	 * Example of what might be returned: 
	 * <br/>
	 * <code>INSERT INTO employees (name, department, salary) VALUES (?, ?, ?)</code>
	 * </p>
	 * 
	 * @return an SQL parameterized string suitable for inserting values into the
	 * table
	 */
	public String makeParameterizedCreateQuery()
	{
		String valueParameters = "?"; // assume ought to be at least one field

		for (int i=1; i<getFieldNameCount(); i++)
			valueParameters += ", ?";
		
		return "INSERT INTO " + getTable() + 
			" (" + getFieldNames() + ") " +
			" VALUES (" + valueParameters + ")";
	}

	/**
	 * Inserts a row in the database corresponding to this bean.
	 * 
	 * @param connection the connection to use
	 * @throws SQLException
	 */
	public void create(Connection connection)
		throws SQLException
	{
		Statement statement = null;
		String query = null;
		int results = 0;

		try
		{
			preCreate(connection);
			
			statement = connection.createStatement();

			query = "INSERT INTO " + getTable() + 
				" (" + getFieldNames() + ") " +
				" VALUES (" + getFieldValuesAsString()  + ")";
			results = statement.executeUpdate(query);

			if (results != 1)
				throw new SQLException("Unable to create row via query: >>>" + query + "<<<");

			/*
			// Not supported in older versions of Oracle. For the moment, beans are responsible
			 * for filling in their generated fields in some other way.
			ResultSet keyResultSet = statement.getGeneratedKeys();
			ResultSetMetaData rsmd = keyResultSet.getMetaData();
			for (int i=1; i<=rsmd.getColumnCount(); i++)
				MainLog.getLog().debug("Column: " + rsmd.getColumnName(i) + " " + rsmd.getColumnType(i));

			if (keyResultSet.next())
				setPrimaryKey(keyResultSet.getInt(1));
			else
				throw new SQLException("Unable to retrieve generated key");
			*/
			postCreate(connection);
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query);
			throw e;
		}
		finally
		{
			try { if (statement != null) statement.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Reads a row from the database and uses it to fill in the values of this bean.
	 * A condition suitable for inclusion in a 'WHERE' clause may be included. Note if
	 * the condition matches more than one row, only the first row is loaded into
	 * the bean.
	 * 
	 * @param connection the connection to use
	 * @param condition a condition - use null if none
	 * @throws SQLException
	 */
	public void load(Connection connection, String condition)
		throws SQLException, FindException
	{
		Statement statement = null;
		ResultSet resultSet = null;
		String query = null;
		
		try
		{
			preLoad(connection);
			statement = connection.createStatement();
			query = "SELECT * FROM " + getTable() + (condition == null ? "" : " WHERE " + condition);
			resultSet = statement.executeQuery(query);
			if (!resultSet.next())
				throw new FindException(
					"No persistent bean found to match query: >>> " + query + " <<<");
			loadFields(resultSet);
			postLoad(connection);
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query);
			throw e;
		}
		finally
		{
			try { if (resultSet != null) resultSet.close(); } catch (Exception e) {} 
			try { if (statement != null) statement.close(); } catch (Exception e) {}
		}
	}

	/**
	 * <p>
	 * Updates a row in the database corresponding to this bean. Note that depending
	 * on the generality of the passed in condition, this might be used to do a bulk
	 * update, however this is not allowed: if other than one object is updated, the
	 * entire operation is rolled back. Hence, the condition here should always 
	 * include something to uniquely identify the current bean, and hence tie the
	 * update to only one row corresponding to said bean.
	 * </p>
	 * 
	 * <p>
	 * A good practice would be for subclasses to overload this method and provide
	 * an update that only takes a connection as a parameter, and forcibly includes
	 * a unique condition always. That method would make a call to this method here.
	 * </p>
	 * 
	 * @param connection the connection to use
	 * @param condition a condition to use
	 * @throws SQLException
	 */
	public void update(Connection connection, String condition)
		throws SQLException
	{
		Statement statement = null;
		String query = null;
		int results = 0;
	
		try
		{
			preUpdate(connection);
			statement = connection.createStatement();
			query = "UPDATE " + getTable() + 
				" SET " + getFieldUpdatesAsString() + 
				(condition == null ? "" : " WHERE " + condition);
			results = statement.executeUpdate(query);
			
			if (results != 1)
				throw new SQLException("Unable to update row via query: >>>" + query + "<<<");
			postUpdate(connection);
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query);
			throw e;
		}
		finally
		{
			try { if (statement != null) statement.close(); } catch (Exception e) {}
		}
	}

	/**
	 * <p>
	 * Deletes a row(s) from the database corresponding to the condition that is
	 * passed in. Note that this method FORCES the condition to be non-null. Nonetheless
	 * it is possible to use the condition to delete multiple rows, hence it is
	 * good practice for subclasses to overload this method and provide a delete
	 * method that uniquely ties a call to the overloaded method to the bean, and
	 * that method would call this one.
	 * </p>
	 * 
	 * @param connection the connection to use
	 * @param condition the condition to match against
	 * @param confirmDeletion if true, should roll back if other than exactly ONE object is deleted.
	 * @throws SQLException
	 */
	public void delete(Connection connection, String condition, boolean confirmDeletion)
		throws SQLException
	{
		Statement statement = null;
		String query = null;
		int results = 0;
	
		try
		{
			preDelete(connection);
			statement = connection.createStatement();
			query = "DELETE FROM " + getTable() + " WHERE " + condition;
			results = statement.executeUpdate(query);
	
			if (confirmDeletion && results != 1)
				throw new SQLException("Unable to delete row via query: >>>" + query + "<<<");
			postDelete(connection);
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query);
			throw e;
		}
		finally
		{
			try { if (statement != null) statement.close(); } catch (Exception e) {}
		}
	}
}
