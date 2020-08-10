package net.btlcpy.outofstock.reports;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;

/**
 * A class which manages the processing of reports. It is responsible for any pre-processing and 
 * post-processing, including opening and closing database connections (with the associated
 * commit or rollback as the case may be).
 * 
 * @author Ahmed A. Abd-Allah
 */
public class ReportManager 
{
	public Report totalsReport(String username, Date beginDate, Date endDate, 
		String summaryTarget, String resultSize,
		String breakOutByDay, String lostSalesMetric,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, String[] bottlerPKID, 
		String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, 
		String[] bottlerSalesRoutePKID, String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
		throws SQLException, InstantiationException, IllegalAccessException
	{
		Connection connection = null;
		boolean committed = false;

		try
		{
			connection = PersistenceManager.getPersistenceManager().getConnection(false);
			
			TotalsReport totalsReport = new TotalsReport(connection, username,
				beginDate, endDate, summaryTarget, resultSize, breakOutByDay, lostSalesMetric,
				productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
				bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
				bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);

			totalsReport.execute();

			connection.commit();
			committed = true;
			return totalsReport;
		}
		finally
		{
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

	public Report averagesReport(String username, Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, String[] bottlerPKID, 
		String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, 
		String[] bottlerSalesRoutePKID, String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
		throws SQLException, InstantiationException, IllegalAccessException
	{
		Connection connection = null;
		boolean committed = false;

		try
		{
			connection = PersistenceManager.getPersistenceManager().getConnection(false);
			
			AveragesReport averagesReport = new AveragesReport(connection, username,
				beginDate, endDate, summaryTarget, lostSalesMetric,
				productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
				bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
				bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);

			averagesReport.execute();

			connection.commit();
			committed = true;
			return averagesReport;
		}
		finally
		{
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

	public Report userActionsReport(String username, Date beginDate, Date endDate, String actionCategory, 
		int start, int size) 
		throws SQLException, InstantiationException, IllegalAccessException
	{
		Connection connection = null;
		boolean committed = false;

		try
		{
			connection = PersistenceManager.getPersistenceManager().getConnection(false);
			
			UserActionsReport userActionsReport = new UserActionsReport(connection, 
				username, beginDate, endDate, actionCategory, start, size);

			userActionsReport.execute();

			connection.commit();
			committed = true;
			return userActionsReport;
		}
		finally
		{
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

	public Report averageDailyStoreEventsReport(String username, Date beginDate, Date endDate, 
		String resultSize,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, String[] bottlerPKID, 
		String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, 
		String[] bottlerSalesRoutePKID, String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
		throws SQLException, InstantiationException, IllegalAccessException
	{
		Connection connection = null;
		boolean committed = false;

		try
		{
			connection = PersistenceManager.getPersistenceManager().getConnection(false);
			
			AverageDailyStoreEventsReport averageDailyStoreEventsReport = 
				new AverageDailyStoreEventsReport(connection, username,
				beginDate, endDate, resultSize, 
				productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
				bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
				bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);

			averageDailyStoreEventsReport.execute();

			connection.commit();
			committed = true;
			return averageDailyStoreEventsReport;
		}
		finally
		{
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
