package net.btlcpy.outofstock.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import oracle.jdbc.pool.OracleDataSource;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A singleton class that represents the database and hence manages persistence operations.
 * 
 * @author Ahmed Abd-Allah
 */
public class PersistenceManager
{
	// ----- Static members ---------------------------------------------------
	/** The singleton */
	static private PersistenceManager manager = null;

	/**
	 * This method should be called once on application startup.
	 * 
	 * @param jndiName the JNDI name of the database to use
	 */
	synchronized static public void initializePersistenceManager(String jndiName)
	{
		if (manager == null)
			manager = new PersistenceManager(jndiName);
	}

	/** Returns the same manager always. */
	static public PersistenceManager getPersistenceManager()
	{
		return manager;
	}
	
	// ----- Instance members -------------------------------------------------
	/** The JNDI name of the database to use */
	private String jndiName = null;
	
	/** The last key that the database generated */
	private int databaseLastGeneratedKey = -1;
	
	/** The increment value that the database is using to generate keys. 
	 * This is currently set to 1000, matching what is used in the current database schema.
	 * (TBD: this should be a run time parameter to read in from the XML configuration files.
	 * Low priority.)*/
	private int databaseKeyAutoIncrementValue = 1000;
	
	/** The last generated key to be given to callers. */
	private int generatedKey = -1;
	
	/** 
	 * The constructor is protected on purpose; use <code>getPersistenceManager()</code> instead.
	 * @param jndiName the JNDI name of the database to use 
	 */
	protected PersistenceManager(String jndiName)
	{
		setJndiName(jndiName);
	}

	public String getJndiName()
	{
		return jndiName;
	}

	public void setJndiName(String jndiName)
	{
		this.jndiName = jndiName;
	}
	
	/**
	 * Gets a connection to the database.
	 * 
	 * @param autocommit whether to turn on autocommit or not. Note that setting autocommit to false for
	 * 'read' queries (i.e. SELECT...) leads to problems on some application servers (notably Websphere)
	 * where their policy is to throw an exception if an uncommitted connection is closed - even if the
	 * connection was only used for reading. Hence as much as possible use this method with autocommit
	 * set to true for reads, and if doing writes, then you may use autocommit set to false if the set of
	 * writes is a long chain.
	 * @return the connection to use
	 */
	public Connection getConnection(boolean autocommit)
	{
		Connection connection = null;
		
		try
		{
			// TODO: REMOVE THIS IF FROM THE PRODUCTION CODE. LEAVE THE ELSE.
			if (jndiName == null)
			{
				OracleDataSource ds = new OracleDataSource();
				ds.setDataSourceName("OracleDataSource");
				ds.setImplicitCachingEnabled(false);
				ds.setNetworkProtocol("tcp");
				ds.setDatabaseName("XE");
				ds.setLoginTimeout(0);
				// ds.setPassword("development");
				ds.setPassword("kooutofstock");
				ds.setConnectionCachingEnabled(false);
				// ds.setUser("development");
				ds.setUser("kooutofstock");
				ds.setPortNumber(1521);
				ds.setServerName("localhost");
				// ds.setURL("jdbc:oracle:thin:development/development@localhost:1521:XE");
				ds.setURL("jdbc:oracle:thin:kooutofstock/kooutofstock@localhost:1521:XE");
	
				// PostgreSQL
				/*
				PGConnectionPoolDataSource ds = new PGConnectionPoolDataSource();
				ds.setDatabaseName("artlogic");
				ds.setServerName("localhost");
				ds.setPortNumber(5432);
				ds.setUser("postgres");
				ds.setPassword("postgres");
				ds.setDefaultAutoCommit(false);
				*/
				
				connection = ds.getConnection();
			}
			else
			{
				Context ctx = (Context) new InitialContext();
				DataSource dataSource = (DataSource) ctx.lookup(jndiName);
	
				if (dataSource == null)
					throw new NullPointerException("Null data source when looking up: " + jndiName);
				
				connection = dataSource.getConnection();
			}
			connection.setAutoCommit(autocommit);
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to get database connection", e);
			return null; // force caller to encounter runtime exception instead of checked exception
						 // This is so because usage of this method is ubiquitous
		}

		return connection;
	}

	/**
	 * Generates a primary key for use anywhere in the database. Keys should be globally unique.
	 * How it works: the database is responsible for generating a 'base' key that is reasonably
	 * far from the next base key. Currently the period is set to 1000, but this may change to
	 * be a run-time parameter potentially (low priority). When the database is used to generate
	 * this base key, subsequent calls to this method (which is synchronized) will programmatically
	 * increment the key, and return that new value. This is faster than going to the database
	 * for another key always. Hence, only every 1000th request actually goes to the database.
	 * 
	 * @param connection
	 * @return a new generated primary key
	 * @throws SQLException
	 */
	synchronized public Integer generatePrimaryKey(Connection connection)
		throws SQLException
	{
		if (generatedKey == -1 // no generated key yet 
			|| 
			// we have exhausted the primary keys since last accessing the database
			generatedKey >= databaseLastGeneratedKey + databaseKeyAutoIncrementValue - 1)
		{
			boolean useOwnConnection = (connection == null);
			Statement statement = null;
			ResultSet resultSet = null;
			Integer newPrimaryKey = null;
		
			try
			{
				if (useOwnConnection)
					connection = getConnection(true);
				statement = connection.createStatement();
				// Get a fresh base generated key from the database
				resultSet = statement.executeQuery("SELECT pkgenerator.nextval FROM DUAL");
				// PostgreSQL
				// resultSet = statement.executeQuery("SELECT nextval('pkgenerator')");
				
				if (resultSet.next())
					newPrimaryKey = new Integer(resultSet.getInt("nextval"));
				if (newPrimaryKey == null || newPrimaryKey.intValue() < 0)
					throw new SQLException("Unable to generate primary key");
				databaseLastGeneratedKey = newPrimaryKey.intValue();
				generatedKey = databaseLastGeneratedKey;
				return new Integer(generatedKey);
			}
			finally
			{
				try { if (resultSet != null) resultSet.close(); } catch (Exception e) {} 
				try { if (statement != null) statement.close(); } catch (Exception e) {}
				if (useOwnConnection)
					try { if (connection != null) connection.close(); } catch (Exception e) {}
			}
		}
		
		// we haven't exhausted the primary keys in the current interval, so just increment
		// and return the new value without bothering the database.
		generatedKey++;
		return new Integer(generatedKey);
	}

	// ----- Finder methods -----

	/**
	 * A connection is passed in independently into this method to allow the user to reuse
	 * the connection across queries efficiently, and also to use transactions if necessary
	 * (using <code>connection.setCommit(false)</code> and <code>connection.commit()</code>).
	 * 
	 * @param connection the connection to use (if null, then create a connection automatically)
	 * @param table the database table to search in
	 * @param conditions SQL-phrased conditions to constrain the search with
	 * @param typeOfBean the type of bean to use when converting the rows to objects
	 * @param keyColumns the columns to use as keys when returning the set of beans in a map
	 * @param orderBy the comma-separated set of columns to order the results by
	 * @return a list ordered map of beans (representing the rows retrieved, with order maintained)
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public ListOrderedMap findBeans(Connection connection, String table, String conditions, Class typeOfBean, String keyColumns[],
		String orderBy)
		throws IllegalAccessException, InstantiationException, SQLException
	{
		String query = "SELECT * FROM " + table + 
			(conditions == null || conditions.trim().equals("") ? "" : " WHERE " + conditions) +
			(orderBy == null || orderBy.trim().equals("") ? "" : " ORDER BY " + orderBy);
		return findBeans(connection, query, typeOfBean, keyColumns);
	}

	/**
	 * A connection is passed in independently into this method to allow the user to reuse
	 * the connection across queries efficiently, and also to use transactions if necessary
	 * (using <code>connection.setCommit(false)</code> and <code>connection.commit()</code>).
	 * 
	 * The caller must make sure the query does return data appropriate to fill the bean fields.
	 * 
	 * @param connection the connection to use (if null, then create a connection automatically)
	 * @param query the SQL query to use to find the beans
	 * @param typeOfBean the type of bean to use when converting the rows to objects
	 * @param keyColumns the columns to use as keys when returning the set of beans in a map
	 * @return a list ordered map of beans (representing the rows retrieved, with order maintained)
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public ListOrderedMap findBeans(Connection connection, String query, Class typeOfBean, String keyColumns[])
		throws IllegalAccessException, InstantiationException, SQLException
	{
		boolean useOwnConnection = (connection == null);
		Statement statement = null;
		ResultSet resultSet = null;

		try
		{
			if (useOwnConnection)
				connection = getConnection(true);
			statement = connection.createStatement();
			resultSet = statement.executeQuery(query);

			if (!resultSet.next())
				return null;
	
			ListOrderedMap results = new ListOrderedMap(); // LinkedHashMap requires Java 1.4
			
			BasePersistentBean oneBean = null;
			do
			{
				// create a new instance of that bean
				oneBean = (BasePersistentBean) typeOfBean.newInstance();
				// Load that bean's fields from the result set
				oneBean.loadFields(resultSet);

				// Now figure out how to place it in the map: what key to use?
				if (keyColumns != null)
				{
					String concatenatedKeyValues = "";
					for (int i=0; i<keyColumns.length; i++)
					{
						if (keyColumns[i] == null || keyColumns[i].trim() == "")
							throw new IllegalArgumentException("Keys must be nonempty strings");
						Object keyValue = resultSet.getObject(keyColumns[i]);
						if (keyValue == null)
							throw new IllegalArgumentException("Key value may not be null");
						concatenatedKeyValues += keyValue.toString();
					}
					results.put(concatenatedKeyValues, oneBean);
				}
				else // if no key columns are specified, use the bean class's getUniqueKey method
					results.put(oneBean.getUniqueKey().toString(), oneBean);
			} while (resultSet.next());
			
			return results;
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query, e);
			throw e;
		}
		finally
		{
			try { if (resultSet != null) resultSet.close(); } catch (Exception e) {} 
			try { if (statement != null) statement.close(); } catch (Exception e) {}
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	/**
	 * <p>
	 * A connection is passed in independently into this method to allow the user to reuse
	 * the connection across queries efficiently, and also to use transactions if necessary
	 * (using <code>connection.setCommit(false)</code> and <code>connection.commit()</code>).
	 * </p>
	 * <p>
	 * Rows are represented using list ordered maps. Every map contains a set of names (column
	 * names) mapping to values. The retrieved group of rows is in turn put in one big list
	 * ordered map (order maintained).
	 * </p>
	 * 
	 * @param connection the connection to use (if null, then create a connection automatically)
	 * @param query the SQL query to use to find the beans
	 * @param keyColumns the columns to use as keys when returning the set of beans in a map
	 * @return ListOrderedMap< String, ListOrderedMap<String, Object> > (keys to rows(names to objects))
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public ListOrderedMap findRows(Connection connection, String query, String keyColumns[])
		throws IllegalAccessException, InstantiationException, SQLException
	{
		boolean useOwnConnection = (connection == null);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try
		{
			if (useOwnConnection)
				connection = getConnection(true);
			statement = connection.createStatement();
			resultSet = statement.executeQuery(query);
	
			if (!resultSet.next())
				return null;
	
			ListOrderedMap /*String, ListOrderedMap*/ results = new ListOrderedMap(); // LinkedHashMap requires Java 1.4
			
			ListOrderedMap /*String, Object*/ variousColumnsNotInOneBean = null;
			ResultSetMetaData rsmd = resultSet.getMetaData();
			int columnCount = rsmd.getColumnCount();
			String columnName = null;
			int rowCounter = 0;
			
			// --BeginDriverKludgeRelated--
			// The Oracle 9.2.0.4 JDBC driver has two bugs in it that are corrected in later versions.
			// However to save time (regression testing all other portlets on NASO after upgrading),
			// we workaround the two bugs here. The two bugs: when calling ResultSet.getObject():
			// an SQL Date is returned as a Timestamp, (JDBC spec sez: should be a Date)
			// and an SQL Timestamp is returned as a oracle.sql.DATE (should be a Timestamp)
			// This is documented partially at:
			// http://download.oracle.com/docs/cd/B10501_01/java.920/a96654/datacc.htm
			boolean applyOracle9_2_0_4JDBCFix = false;
			String driverVersion = connection.getMetaData().getDriverVersion();
			if (driverVersion.indexOf("9.2.0.4") >= 0)
				applyOracle9_2_0_4JDBCFix = true;
			// --EndDriverKludgeRelated--
			
			do
			{
				variousColumnsNotInOneBean = new ListOrderedMap();
				
				for (int i=0; i<columnCount; i++)
				{
					columnName = rsmd.getColumnName(i+1);
					if (resultSet.getObject(columnName) != null)
					{
						// --BeginDriverKludgeRelated--
						if (applyOracle9_2_0_4JDBCFix && (resultSet.getObject(i+1) instanceof java.sql.Timestamp))
						{
							variousColumnsNotInOneBean.put(
								columnName/*.toLowerCase()*/,
								// PostgreSQL
								// columnName.toUpperCase(),
								new java.sql.Date( ((Timestamp) resultSet.getObject(i+1)).getTime() ) );
						}
						else if (applyOracle9_2_0_4JDBCFix && (resultSet.getObject(i+1) instanceof oracle.sql.DATE))
						{
							variousColumnsNotInOneBean.put(
								columnName/*.toLowerCase()*/,
								// PostgreSQL
								// columnName.toUpperCase(),
								((oracle.sql.DATE) resultSet.getObject(i+1)).timestampValue() );
						}
						// --EndDriverKludgeRelated--
						// java.sql.Timestamp will be sent for getObject() according to the Oracle docs
						// if the oracle.jdbc.J2EE13Compliant connection property is set to TRUE, 
						// else the method returns oracle.sql.TIMESTAMP
						else if (resultSet.getObject(i+1) instanceof oracle.sql.TIMESTAMP)
						{
							// Odd; the oracle.sql.TIMESTAMP class does not have an overridden toString()
							// method for use later on, so we convert it now to something that does.
							variousColumnsNotInOneBean.put(
								columnName/*.toLowerCase()*/,
								// PostgreSQL
								// columnName.toUpperCase(),
								((oracle.sql.TIMESTAMP) resultSet.getObject(i+1)).timestampValue());
						}
						else
							variousColumnsNotInOneBean.put(
								columnName/*.toLowerCase()*/,
								// PostgreSQL
								// columnName.toUpperCase(),
								resultSet.getObject(i+1));
					}
				}
				
				String concatenatedKeyValues = "";
				if (keyColumns == null)
					results.put(new Integer(rowCounter++).toString(), variousColumnsNotInOneBean);
				else
				{
					boolean processingFirstKey = true;
					for (int i=0; i<keyColumns.length; i++)
					{
						if (keyColumns[i] == null || keyColumns[i].trim() == "")
							throw new IllegalArgumentException("Keys must be nonempty strings");
						Object keyValue = variousColumnsNotInOneBean.get(keyColumns[i]);
						if (keyValue == null)
							throw new IllegalArgumentException("Key value may not be null");
						if (processingFirstKey)
						{
							concatenatedKeyValues += keyValue.toString();
							processingFirstKey = false;
						}
						else
							concatenatedKeyValues += ("|" + keyValue.toString());
					}
					results.put(concatenatedKeyValues, variousColumnsNotInOneBean);
				}
				
			} while (resultSet.next());
			
			return results;
		}
		catch (SQLException e)
		{
			MainLog.getLog().error(query, e);
			throw e;
		}
		finally
		{
			try { if (resultSet != null) resultSet.close(); } catch (Exception e) {} 
			try { if (statement != null) statement.close(); } catch (Exception e) {}
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	// ----- Bulk Update -----
	
	/**
	 * Simple method for doing bulk updates.
	 * @param connection the connection to use, if null then a connection is automatically created
	 * @param table the table to update
	 * @param values the values to use while updating
	 * @param conditions the conditions to use to identify which rows to update
	 * @return the number of updates done
	 */
	public int bulkUpdate(Connection connection, String table, String values, String conditions)
		throws SQLException
	{
		boolean useOwnConnection = (connection == null);
		Statement statement = null;
		String query = "UPDATE " + table + " SET " + values + 
			(conditions == null || conditions.trim().equals("") ? "" : " WHERE " + conditions);
	
		try
		{
			if (useOwnConnection)
				connection = getConnection(true);
			statement = connection.createStatement();

			int results = statement.executeUpdate(query);

			return results;
		}
		catch (SQLException e)
		{
			MainLog.getLog().error("Unable to perform bulkUpdate with query: " + query, e);
			throw e;
		}
		finally
		{
			try { if (statement != null) statement.close(); } catch (Exception e) {}
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Simple method for doing bulk deletes.
	 * @param connection the connection to use, if null then a connection is automatically created
	 * @param table the table to delete rows from it
	 * @param conditions the conditions to use to identify which rows to delete
	 * @return the number of deletes done
	 */
	public int bulkDelete(Connection connection, String table, String conditions)
		throws SQLException
	{
		boolean useOwnConnection = (connection == null);
		Statement statement = null;
		String query = "DELETE FROM " + table +  
			(conditions == null || conditions.trim().equals("") ? "" : " WHERE " + conditions);
		
		MainLog.getLog().debug(query);
	
		try
		{
			if (useOwnConnection)
				connection = getConnection(true);
			statement = connection.createStatement();
	
			int results = statement.executeUpdate(query);

			return results;
		}
		catch (SQLException e)
		{
			MainLog.getLog().error("Unable to perform bulkDelete with query: " + query, e);
			throw e;
		}
		finally
		{
			try { if (statement != null) statement.close(); } catch (Exception e) {}
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}
}
