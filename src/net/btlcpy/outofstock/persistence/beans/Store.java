package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A persistent bean to represent Distributor stores.
 * 
 * @author Ahmed Abd-Allah
 */
public class Store extends NamedWithIdUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "stores";

	/**
	 * Finds stores for a given distributor district.
	 * @param connection connection to use, if null, creates a new connection
	 * @param distributorDistricts primary keys of each distributor district desired
	 * @return a list ordered map of stores
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByDistributorDistrict(Connection connection, Integer[] distributorDistricts)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(distributorDistricts[0]);
		for (int i=1; i<distributorDistricts.length; i++)
		{
			sb.append(", " + distributorDistricts[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT stores.* FROM stores, dstbdstrctstostores WHERE " +
				"dstbdstrctstostores.distributordistrict IN (" + sb.toString() + ") AND " +
				"dstbdstrctstostores.store = stores.primarykey " +
				"ORDER BY id", 
				Store.class, null);
	}
	
	/** The number of active stores, traditional and distributor.com (cached) */
	private static int totalActiveTraditional = 0, totalActiveDistributorCom = 0;
	public static int getTotalActive() { return totalActiveTraditional + totalActiveDistributorCom; }
	public static int getTotalActiveTraditional() { return totalActiveTraditional; }
	public static int getTotalActiveDistributorCom() { return totalActiveDistributorCom; }

	/**
	 * Update the cached number of active stores. Should be called when a new store list
	 * is uploaded to the server.
	 * @param connection the connection to use, if null will use a new connection automatically
	 */
	public static void updateTotals(Connection connection)
	{
		boolean useOwnConnection = (connection == null);
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, "SELECT count(*) AS TOTAL FROM " + tableName + " WHERE bitand(metaflags, 1)+0 = 0 AND " +
					"(length(stores.distributorcom) = 0 OR (length(stores.distributorcom) > 0 AND stores.distributorcom = '-'))", 
					null);

			if (results != null && results.size() > 0)
				totalActiveTraditional = ((Number) ((ListOrderedMap) results.getValue(0)).get("TOTAL")).intValue();
			else
				totalActiveTraditional = 0;

			results = PersistenceManager.getPersistenceManager().
			findRows(connection, "SELECT count(*) AS TOTAL FROM " + tableName + " WHERE bitand(metaflags, 1)+0 = 0 AND " +
				"(length(stores.distributorcom) > 0 AND stores.distributorcom != '-')", 
				null);

			if (results != null && results.size() > 0)
				totalActiveDistributorCom = ((Number) ((ListOrderedMap) results.getValue(0)).get("TOTAL")).intValue();
			else
				totalActiveDistributorCom = 0;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update totals", e);
			totalActiveTraditional = 0;
			totalActiveDistributorCom = 0;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	// ----- Instance members -------------------------------------------------
	private String address;
	private String city;
	private String state;
	private String zip; // while it seems this is a number always, it might one day be "12345-1234"
	private String phoneNumber; // while it seems this is a number always, it might one day be "123-456-7890"
	private Date dateOpened;
	private Date dateLastRemodeled;
	private Date dateClosed;
	private String lifeStyle;
	private String distributorCom;
	private Integer totalSellingArea;
	private Integer totalFlmCount;

	public Store()
	{
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public String getCity()
	{
		return city;
	}

	public void setCity(String city)
	{
		this.city = city;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getZip()
	{
		return zip;
	}

	public void setZip(String zip)
	{
		this.zip = zip;
	}

	public String getPhoneNumber()
	{
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber)
	{
		this.phoneNumber = phoneNumber;
	}

	public Date getDateOpened()
	{
		return dateOpened;
	}

	public void setDateOpened(Date dateOpened)
	{
		this.dateOpened = dateOpened;
	}

	public Date getDateLastRemodeled()
	{
		return dateLastRemodeled;
	}

	public void setDateLastRemodeled(Date dateLastRemodeled)
	{
		this.dateLastRemodeled = dateLastRemodeled;
	}

	public Date getDateClosed()
	{
		return dateClosed;
	}

	public void setDateClosed(Date dateClosed)
	{
		this.dateClosed = dateClosed;
	}

	public String getLifeStyle()
	{
		return lifeStyle;
	}

	public void setLifeStyle(String lifeStyle)
	{
		this.lifeStyle = lifeStyle;
	}

	public String getDistributorCom()
	{
		return distributorCom;
	}

	public void setDistributorCom(String distributorCom)
	{
		this.distributorCom = distributorCom;
	}

	public Integer getTotalSellingArea()
	{
		return totalSellingArea;
	}

	public void setTotalSellingArea(Integer totalSellingArea)
	{
		this.totalSellingArea = totalSellingArea;
	}

	public Integer getTotalFlmCount()
	{
		return totalFlmCount;
	}

	public void setTotalFlmCount(Integer totalFlmCount)
	{
		this.totalFlmCount = totalFlmCount;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		address = null;
		city = null;
		state = null;
		zip = null;
		phoneNumber = null;
		dateOpened = null;
		dateLastRemodeled = null;
		dateClosed = null;
		lifeStyle = null;
		distributorCom = null;
		totalSellingArea = null;
		totalFlmCount = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + 
			", address, city, state, zip, phonenumber, dateopened, " +
			"datelastremodeled, dateclosed, lifestyle, " +
			"distributorcom, totalsellingarea, totalflmcount"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 12;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() +
			", " + BQ + dq(address) + EQ + 
			", " + BQ + dq(city) + EQ + 
			", " + BQ + dq(state) + EQ + 
			", " + BQ + dq(zip) + EQ + 
			", " + BQ + dq(phoneNumber) + EQ +
			", DATE " + BQ + dateOpened.toString() + EQ +
			", DATE " + BQ + dateLastRemodeled.toString() + EQ +
			", DATE " + BQ + dateClosed.toString() + EQ +
			", " + BQ +	dq(lifeStyle) + EQ +
			", " + BQ +	dq(distributorCom) + EQ +
			", " + totalSellingArea.toString() +
			", " + totalFlmCount.toString();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() +
			", address = " + BQ + dq(address) + EQ +
			", city = " + BQ + dq(city) + EQ +
			", state = " + BQ + dq(state) + EQ +
			", zip = " + BQ + dq(zip) + EQ +
			", phonenumber = " + BQ + dq(phoneNumber) + EQ +
			", dateopened = DATE " + BQ + dateOpened.toString() + EQ +
			", datelastremodeled = DATE " + BQ + dateLastRemodeled.toString() + EQ +
			", dateclosed = DATE " + BQ + dateClosed.toString() + EQ +
			", lifestyle = " + BQ + dq(lifeStyle) + EQ +
			", distributorcom = " + BQ + dq(distributorCom) + EQ +
			", totalsellingarea = " + totalSellingArea.intValue() +
			", totalflmcount = " + totalFlmCount.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setAddress(resultSet.getString("address"));
		setCity(resultSet.getString("city"));
		setState(resultSet.getString("state"));
		setZip(resultSet.getString("zip"));
		setPhoneNumber(resultSet.getString("phonenumber"));
		setDateOpened(resultSet.getDate("dateopened"));
		setDateLastRemodeled(resultSet.getDate("datelastremodeled"));
		setDateClosed(resultSet.getDate("dateclosed"));
		setLifeStyle(resultSet.getString("lifestyle"));
		setDistributorCom(resultSet.getString("distributorcom"));
		setTotalSellingArea(new Integer(resultSet.getInt("totalsellingarea")));
		setTotalFlmCount(new Integer(resultSet.getInt("totalflmcount")));
	}

	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);

		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setString(index++, address);
			preparedStatement.setString(index++, city);
			preparedStatement.setString(index++, state);
			preparedStatement.setString(index++, zip);
			preparedStatement.setString(index++, phoneNumber);
			preparedStatement.setDate(index++, dateOpened);
			preparedStatement.setDate(index++, dateLastRemodeled);
			preparedStatement.setDate(index++, dateClosed);
			preparedStatement.setString(index++, lifeStyle);
			preparedStatement.setString(index++, distributorCom);
			preparedStatement.setInt(index++, totalSellingArea.intValue());
			preparedStatement.setInt(index++, totalFlmCount.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
