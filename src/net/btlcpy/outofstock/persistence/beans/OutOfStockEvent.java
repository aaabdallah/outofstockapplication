package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;


/**
 * A persistent bean to represent out-of-stock events.
 * 
 * @author Ahmed Abd-Allah
 */
public class OutOfStockEvent extends Instrumented
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "outofstockevents";
	
	/**
	 * The earliest date for which there exists out of stock events.
	 */
	private static Date earliestEventDate = null;
	public static Date getEarliestEventDate() { return earliestEventDate; }
	/**
	 * Updates the earliest event date from the database.
	 * @param connection connection to use, if null --> create a new connection automatically
	 */
	public static void updateEarliestEventDate(Connection connection)
	{
		boolean useOwnConnection = (connection == null);
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, "SELECT MIN(dateoccurred) AS earliest FROM " + tableName, null);
		
			if (results != null && results.size() > 0)
				earliestEventDate = (Date) ((ListOrderedMap) results.getValue(0)).get("EARLIEST");
			else
				earliestEventDate = null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update earliest event date", e);
			earliestEventDate = null;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Computes the intervals for which there is out of stock events. The intervals are
	 * expressed as pairs of dates (inclusive). The results are returned in the form of
	 * a list ordered map, keys being the starting dates, and the mapped values being the
	 * ending dates.
	 * @param connection connection to use, if null --> create a new connection automatically
	 * @return map of intervals
	 */
	public static ListOrderedMap getEventDateIntervals(Connection connection)
	{
		boolean useOwnConnection = (connection == null);
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, "SELECT DISTINCT dateoccurred FROM " + tableName + " ORDER BY dateoccurred", null);

			ListOrderedMap/*<Date, Date>*/ intervals = new ListOrderedMap();

			if (results != null && results.size() > 0)
			{
				Date startDate = null, endDate = null, testDate = null;
				Calendar calendar1 = Calendar.getInstance(Locale.US);
				Calendar calendar2 = Calendar.getInstance(Locale.US);

				for (int i=0; i<results.size(); i++)
				{
					if (startDate == null)
						startDate = (Date) ((ListOrderedMap) results.getValue(i)).get("DATEOCCURRED");
					else
					{
						testDate = (Date) ((ListOrderedMap) results.getValue(i)).get("DATEOCCURRED");

						// First check if we already have a possible interval
						if (endDate != null)
						{
							// is the new date right after the current interval?
							// This does NOT work: if (testDate.getTime() - endDate.getTime() <= 86400000)
							// For some reason, at least one pair of consecutive dates is reported to have a 
							// difference of 90M seconds. Not sure why... hence we use Calendar.
							calendar1.setTime(testDate);
							calendar2.setTime(endDate);
							calendar2.add(Calendar.DATE, 1);
							if (calendar1.get(Calendar.DATE) == calendar2.get(Calendar.DATE) &&
								calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
								calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR))
								endDate = testDate;
							else
							{
								intervals.put(startDate, endDate);
								startDate = testDate;
								endDate = null;
							}
						}
						// else we just have an interval start
						else
						{
							// is the new date right after the start? (see note above)
							calendar1.setTime(testDate);
							calendar2.setTime(startDate);
							calendar2.add(Calendar.DATE, 1);
							if (calendar1.get(Calendar.DATE) == calendar2.get(Calendar.DATE) &&
								calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
								calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR))
								endDate = testDate;
							else
							{
								// no, so we have a 'singleton' interval
								intervals.put(startDate, startDate);
								startDate = testDate;
							}
						}
					}
					
					// if the data has ended, need to close any unfinished intervals.
					if (i == results.size() - 1)
					{
						if (startDate != null && endDate != null)
							intervals.put(startDate, endDate);
						else if (startDate != null)
							intervals.put(startDate, startDate);
					}
				}
			}
			else
				return null;
			
			if (!intervals.isEmpty())
				return intervals;

			return null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to get event date intervals", e);
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
		return null;
	}

	/**
	 * Deletes all out of stock events between (inclusive) the two dates given.
	 * @param connection connection to use, if null --> create a new connection automatically
	 * @param username username of person deleting (for record-keeping purposes)
	 * @param startDate start date of interval (inclusive)
	 * @param endDate end date of interval (inclusive)
	 */
	public static void deleteEventsInInterval(Connection connection, String username, Date startDate, Date endDate)
	{
		boolean useOwnConnection = (connection == null);
		boolean committed = false;
		Date backupEarliestEventDate = earliestEventDate;
		
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(false);

			int deletions = PersistenceManager.getPersistenceManager().
				bulkDelete(connection, tableName, 
					"dateoccurred >= DATE " + BQ + startDate.toString() + EQ + " AND " +
					"dateoccurred <= DATE " + BQ + endDate.toString() + EQ
					);
			
			if (deletions > 0)
				updateEarliestEventDate(connection);

			// log the deletion in the record of user actions
			UserAction userAction = new UserAction();
			userAction.setCategory(UserAction.DELETEEVENTS);
			userAction.setTimeLastUploaded(new Timestamp(System.currentTimeMillis()));
			userAction.setDescription("Delete events in interval " + startDate.toString() + 
				" to " + endDate.toString());
			userAction.setName(username);
			userAction.create(connection);

			if (useOwnConnection)
			{
				connection.commit();
				committed = true;
			}
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to delete events", e);
			earliestEventDate = backupEarliestEventDate;
		}
		finally
		{
			if (useOwnConnection)
				try 
				{ 
					if (connection != null)
					{
						if (!committed)
						{
							try { connection.rollback(); } 
							catch (Exception rbe) { MainLog.getLog().error("Exception rolling back", rbe); }
						}
						connection.close();
					}
				} 
				catch (Exception e) 
				{
					MainLog.getLog().error("Exception cleaning up connection", e);
				}		
		}
	}

	// ----- Instance members -------------------------------------------------
	private Integer store;
	private Date dateOccurred;
	private Integer product;
	private String reason;
	private Integer count;
	private Float lostSalesQuantity;
	private Float lostSalesAmount;
	private Integer vendorNumber;
	private Integer vendorSubnumber;
	private String dsdwhsFlag;
	private String vendorName;

	public OutOfStockEvent()
	{
	}

	public Integer getStore()
	{
		return store;
	}

	public void setStore(Integer store)
	{
		this.store = store;
	}

	public Date getDateOccurred()
	{
		return dateOccurred;
	}

	public void setDateOccurred(Date dateOccurred)
	{
		this.dateOccurred = dateOccurred;
	}

	public Integer getProduct()
	{
		return product;
	}

	public void setProduct(Integer product)
	{
		this.product = product;
	}

	public String getReason()
	{
		return reason;
	}

	public void setReason(String reason)
	{
		this.reason = reason;
	}

	public Integer getCount()
	{
		return count;
	}

	public void setCount(Integer count)
	{
		this.count = count;
	}

	public Float getLostSalesQuantity()
	{
		return lostSalesQuantity;
	}

	public void setLostSalesQuantity(Float lostSalesQuantity)
	{
		this.lostSalesQuantity = lostSalesQuantity;
	}

	public Float getLostSalesAmount()
	{
		return lostSalesAmount;
	}

	public void setLostSalesAmount(Float lostSalesAmount)
	{
		this.lostSalesAmount = lostSalesAmount;
	}

	public Integer getVendorNumber()
	{
		return vendorNumber;
	}

	public void setVendorNumber(Integer vendorNumber)
	{
		this.vendorNumber = vendorNumber;
	}

	public Integer getVendorSubnumber()
	{
		return vendorSubnumber;
	}

	public void setVendorSubnumber(Integer vendorSubnumber)
	{
		this.vendorSubnumber = vendorSubnumber;
	}

	public String getDsdwhsFlag()
	{
		return dsdwhsFlag;
	}

	public void setDsdwhsFlag(String dsdwhsFlag)
	{
		this.dsdwhsFlag = dsdwhsFlag;
	}

	public String getVendorName()
	{
		return vendorName;
	}

	public void setVendorName(String vendorName)
	{
		this.vendorName = vendorName;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		store = null;
		dateOccurred = null;
		product = null;
		reason = null;
		count = null;
		lostSalesQuantity = null;
		lostSalesAmount = null;
		vendorNumber = null;
		vendorSubnumber = null;
		dsdwhsFlag = null;
		vendorName = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + 
			", store, dateoccurred, product, reason, count, lostsalesquantity, " +
			"lostsalesamount, vendornumber, vendorsubnumber, dsdwhsflag, vendorname";
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 11;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() +
			", " + store.intValue() +
			", DATE " + BQ + dateOccurred.toString() + EQ +
			", " + product.intValue() +
			", " + BQ + dq(reason) + EQ +
			", " + count.intValue() +
			", " + lostSalesQuantity.floatValue() +
			", " + lostSalesAmount.floatValue() +
			", " + vendorNumber.intValue() +
			", " + vendorSubnumber.intValue() +
			", " + BQ + dq(dsdwhsFlag) + EQ +
			", " + BQ + dq(vendorName) + EQ;
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", store = " + store.intValue() +
			", dateoccurred = DATE " + BQ + dateOccurred.toString() + EQ +
			", product = " + product.intValue() +
			", reason = " + BQ + dq(reason) + EQ +
			", count = " + count.intValue() +
			", lostsalesquantity = " + lostSalesQuantity.floatValue() +
			", lostsalesamount = " + lostSalesAmount.floatValue() +
			", vendornumber = " + vendorNumber.intValue() +
			", vendorsubnumber = " + vendorSubnumber.intValue() + 
			", dsdwhsflag = " + BQ + dq(dsdwhsFlag) + EQ +
			", vendorname = " + BQ + dq(vendorName) + EQ;
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setStore(new Integer(resultSet.getInt("store")));
		setDateOccurred(resultSet.getDate("dateoccurred"));
		setProduct(new Integer(resultSet.getInt("product")));
		setReason(resultSet.getString("reason"));
		setCount(new Integer(resultSet.getInt("count")));
		setLostSalesQuantity(new Float(resultSet.getFloat("lostsalesquantity")));
		setLostSalesAmount(new Float(resultSet.getFloat("lostsalesamount")));
		setVendorNumber(new Integer(resultSet.getInt("vendornumber")));
		setVendorSubnumber(new Integer(resultSet.getInt("vendorsubnumber")));
		setDsdwhsFlag(resultSet.getString("dsdwhsflag"));
		setVendorName(resultSet.getString("vendorname"));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, store.intValue());
			preparedStatement.setDate(index++, dateOccurred);
			preparedStatement.setInt(index++, product.intValue());
			preparedStatement.setString(index++, reason);
			preparedStatement.setInt(index++, count.intValue());
			preparedStatement.setFloat(index++, lostSalesQuantity.floatValue());
			preparedStatement.setFloat(index++, lostSalesAmount.floatValue());
			preparedStatement.setInt(index++, vendorNumber.intValue());
			preparedStatement.setInt(index++, vendorSubnumber.intValue());
			preparedStatement.setString(index++, dsdwhsFlag);
			preparedStatement.setString(index++, vendorName);
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
