package net.btlcpy.outofstock.settings;

import java.sql.Connection;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.persistence.beans.OutOfStockEvent;
import net.btlcpy.outofstock.persistence.beans.ProductCategory;
import net.btlcpy.outofstock.persistence.beans.Setting;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * <p>A class to manage the settings of the OOS application - all settings at
 * the moment. This includes:
 * </p>
 * 
 * <ul>
 * <li>beverage categories to ignore, including updating the events that
 * return back to that category</li>
 * </ul>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class SettingsManager
{
	/**
	 * Adds a new ignored beverage/product category to the settings.
	 * 
	 * @param connection the database connection to use; if null, then one is created
	 * @param beverageCategory the category to ignore
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public static void addIgnoredBeverageCategory(Connection connection, String beverageCategory)
		throws IllegalAccessException, InstantiationException, SQLException
	{
		boolean useOwnConnection = (connection == null);
		boolean committed = false;
		
		if (beverageCategory == null || beverageCategory.trim().length() == 0) return;
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(false);

			beverageCategory = beverageCategory.trim();
			ListOrderedMap ignoredBeverageCategories = 
				Setting.findByCategoryNameValue(null, "IgnoredEvent", "ProductCategory", null);
			
			boolean alreadyIgnored = false;
			if (ignoredBeverageCategories != null && ignoredBeverageCategories.size() > 0)
			{
				for (int i=0; i<ignoredBeverageCategories.size(); i++)
					if (((Setting) ignoredBeverageCategories.getValue(i)).getValue().equalsIgnoreCase(beverageCategory))
					{
						alreadyIgnored = true;
						break;
					}
			}
			
			if (!alreadyIgnored)
			{
				Setting newSetting = new Setting();
				newSetting.setCategory("IgnoredEvent");
				newSetting.setName("ProductCategory");
				newSetting.setValue(beverageCategory);
				newSetting.create(connection);
			}
			if (useOwnConnection)
			{
				connection.commit();
				committed = true;
			}
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
	
	/**
	 * Updates all events related to a particular beverage category to be ignored (they will not
	 * be included in reports).
	 * 
	 * @param connection the database connection to use; if null, then one is created
	 * @param beverageCategory the category to ignore
	 */
	public static void applyIgnoredBeverageCategoryToEvents(Connection connection, String beverageCategory)
	{
		boolean useOwnConnection = (connection == null);
		boolean committed = false;
		
		if (beverageCategory == null || beverageCategory.trim().length() == 0) return;
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(false);

			beverageCategory = beverageCategory.trim();
			
			PersistenceManager.getPersistenceManager().bulkUpdate(
				connection, OutOfStockEvent.tableName, "metaflags = bitor(metaflags, 1)+0", 
				"primarykey IN " +
				"( " +
					"select outofstockevents.primarykey " +
					"from outofstockevents, prdcts, prdctctgrstoprdcts, prdctctgrs " +
					"where outofstockevents.product = prdcts.primarykey and " +
					"prdctctgrstoprdcts.product = prdcts.primarykey and " +
					"prdctctgrstoprdcts.productcategory = prdctctgrs.primarykey and " +
					"prdctctgrs.name = " + BasePersistentBean.BQ + BasePersistentBean.dq(beverageCategory) + BasePersistentBean.EQ +
				")");
			if (useOwnConnection)
			{
				connection.commit();
				committed = true;
			}
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update events to be ignored (based on ignoreable beverage category)", e);
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

	/**
	 * Updates a beverage/product category to have 'ignored' status. 
	 * 
	 * @param connection the database connection to use; if null, then one is created
	 * @param beverageCategory the category to ignore
	 */
	public static void applyIgnoredBeverageCategoryToProductCategories(Connection connection, String beverageCategory)
	{
		boolean useOwnConnection = (connection == null);
		boolean committed = false;
		
		if (beverageCategory == null || beverageCategory.trim().length() == 0) return;
		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(false);

			beverageCategory = beverageCategory.trim();
			
			PersistenceManager.getPersistenceManager().bulkUpdate(
				connection, ProductCategory.tableName, "metaflags = bitor(metaflags, 1)+0", 
				ProductCategory.tableName + ".name = " + 
				BasePersistentBean.BQ + BasePersistentBean.dq(beverageCategory) + BasePersistentBean.EQ);
			ProductCategory.getCache(connection).reload(connection);

			if (useOwnConnection)
			{
				connection.commit();
				committed = true;
			}
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to update product/beverage category to be ignored", e);
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
}
