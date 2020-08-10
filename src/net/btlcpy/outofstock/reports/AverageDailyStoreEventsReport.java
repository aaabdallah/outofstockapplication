package net.btlcpy.outofstock.reports;

import java.sql.Connection;
import java.sql.Date;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;

import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.reports.format.ReportCellFormat;
import net.btlcpy.outofstock.reports.format.ReportColumnFormat;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelCellStyle;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFont;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFormat;
import net.btlcpy.outofstock.utilities.SelectBuilder;
import net.btlcpy.outofstock.utilities.StringUtils;

/**
 * This class produces reports of average daily out-of-stock events per store, over a specified
 * period of time.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class AverageDailyStoreEventsReport extends AveragesTotalsBaseReport
{
	/** 
	 * A query to return the total number of events reported by each store. Averaging will
	 * be done at this layer and not at the database to factor in days for which no losses
	 * were reported - this should reduce the average.
	 */
	private SelectBuilder sbEventsPerStore;

	/** The key columns to use for the query */
	private String[][] keyColumns = { null };
	
	public AverageDailyStoreEventsReport(Connection connection, String username, Date beginDate, Date endDate, String resultSize,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, 
		String[] bottlerPKID, String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, 
		String[] bottlerSalesRoutePKID, 
		String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
	{
		super(connection, username);
		reportBeginDate = beginDate;
		
		userActionDescription = constructUserActionDescription(beginDate, endDate, resultSize);
		
		excelReportDescription = constructExcelReportDescription(
			beginDate, endDate, resultSize,
			StringUtils.id(productCategoryPKID), StringUtils.id(productPKID), StringUtils.id(productPackagePKID),
			StringUtils.id(bottlerPKID), StringUtils.id(bottlerBusinessUnitPKID), 
			StringUtils.id(bottlerMarketUnitPKID), StringUtils.id(bottlerBranchPKID), StringUtils.id(bottlerSalesRoutePKID),
			StringUtils.id(distributorDivisionPKID), StringUtils.id(distributorDistrictPKID), StringUtils.id(storePKID));
		
		String[] productCategoryPK = StringUtils.pk(productCategoryPKID);
		String[] productPK = StringUtils.pk(productPKID);
		String[] productPackagePK = StringUtils.pk(productPackagePKID);
		String[] bottlerPK = StringUtils.pk(bottlerPKID);
		String[] bottlerBusinessUnitPK = StringUtils.pk(bottlerBusinessUnitPKID);
		String[] bottlerMarketUnitPK = StringUtils.pk(bottlerMarketUnitPKID);
		String[] bottlerBranchPK = StringUtils.pk(bottlerBranchPKID);
		String[] bottlerSalesRoutePK = StringUtils.pk(bottlerSalesRoutePKID);
		String[] distributorDivisionPK = StringUtils.pk(distributorDivisionPKID);
		String[] distributorDistrictPK = StringUtils.pk(distributorDistrictPKID);
		String[] storePK = StringUtils.pk(storePKID);
		
		constructDailyEventsPerStore(beginDate, endDate, resultSize,
			productCategoryPK, productPK, productPackagePK, 
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, 
			bottlerSalesRoutePK, 
			distributorDivisionPK, distributorDistrictPK, storePK);
	}
	
	/**
	 * Constructs a string suitable for logging as a user action record.
	 * 
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param resultSize how many results desired
	 * @return a single String that summarizes the report as a user action
	 */
	private String constructUserActionDescription(Date beginDate, Date endDate, String resultSize)
	{
		StringBuffer d = new StringBuffer("Average daily out-of-stock events per store report");
		
		d.append(" across");
		
		if (resultSize == null || resultSize.equals("all"))
			d.append(" all stores");
		else if (resultSize.equals("top25"))
			d.append(" the top 25 stores");
		else if (resultSize.equals("top50"))
			d.append(" the top 50 stores");
				
		if (beginDate != null)
		{
			d.append(", from ");
			d.append(beginDate.toString());
			if (endDate != null)
			{
				d.append(" to ");
				d.append(endDate.toString());
			}
		}

		d.append(".");
		return d.toString();
	}
	
	/**
	 * Constructs a string suitable for inclusion in an Excel spreadsheet mirroring this
	 * report's contents.
	 * 
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param resultSize how many results desired
	 * @param productCategoryID array of product categories (a possible constraint)
	 * @param productID array of products (a possible constraint)
	 * @param productPackageID array of product packages (a possible constraint)
	 * @param bottlerID array of bottlers (a possible constraint)
	 * @param bottlerBusinessUnitID array of bottler business units (a possible constraint)
	 * @param bottlerMarketUnitID array of bottler market units (a possible constraint)
	 * @param bottlerBranchID array of bottler branches (a possible constraint)
	 * @param bottlerSalesRouteID array of bottler sales routes (a possible constraint)
	 * @param distributorDivisionID array of distributor divisions (a possible constraint)
	 * @param distributorDistrictID array of distributor districts (a possible constraint)
	 * @param storeID array of store ID's (a possible constraint)
	 * @return a single String that summarizes the report description
	 */
	private String constructExcelReportDescription(Date beginDate, Date endDate, 
		String resultSize, 
		String[] productCategoryID, String[] productID, String[] productPackageID, 
		String[] bottlerID, String[] bottlerBusinessUnitID, String[] bottlerMarketUnitID, String[] bottlerBranchID, String[] bottlerSalesRouteID, 
		String[] distributorDivisionID, String[] distributorDistrictID, String[] storeID)
	{
		StringBuffer d = new StringBuffer("Average daily out-of-stock events per store report");
		
		d.append(" across");
		
		if (resultSize == null || resultSize.equals("all"))
			d.append(" all stores");
		else if (resultSize.equals("top25"))
			d.append(" the top 25 stores");
		else if (resultSize.equals("top50"))
			d.append(" the top 50 stores");
		
		if (beginDate != null)
		{
			d.append(", from ");
			d.append(beginDate.toString());
			if (endDate != null)
			{
				d.append(" to ");
				d.append(endDate.toString());
			}
		}
		
		d.append(".\n");
		
		d.append("Constraint Settings:\n");
		d.append("Product Category: " + StringUtils.stringArrayToString(productCategoryID));
		d.append(", Product: " + StringUtils.stringArrayToString(productID) + "\n");
		d.append("Bottler: " + StringUtils.stringArrayToString(bottlerID));
		d.append(", Bottler Region: " + StringUtils.stringArrayToString(bottlerBusinessUnitID));
		d.append(", Bottler Market Unit: " + StringUtils.stringArrayToString(bottlerMarketUnitID));
		d.append(", Bottler Branch: " + StringUtils.stringArrayToString(bottlerBranchID) + "\n");
		d.append("Distributor Division: " + StringUtils.stringArrayToString(distributorDivisionID));
		d.append(", Distributor District: " + StringUtils.stringArrayToString(distributorDistrictID));
		d.append(", Store: " + StringUtils.stringArrayToString(storeID) + "\n");
		d.append("Bottler Sales Route: " + StringUtils.stringArrayToString(bottlerSalesRouteID) + "\n");
		d.append("Package: " + StringUtils.stringArrayToString(productPackageID));
		
		return d.toString();
	}

	public String getUserActionDescription()
	{
		return userActionDescription;
	}
	
	public String getExcelReportDescription()
	{
		return excelReportDescription;
	}
	
	public String getReportType()
	{
		return "Average Daily Events Report";
	}

	public String getQueryTitle(int queryNumber)
	{
		if (queryNumber == 0)
			return "Report Data:";
		return "";
	}

	/**
	 * This report may be constrained across a number of different parameters. This
	 * method takes the parameters that were set on the Web page for specification of
	 * report parameters, and applies them to the SQL query that will be executed
	 * for this report.
	 * 
	 * @param selectBuilder the SQL query to apply the constraints to
	 * @param productCategoryPK array of product categories (a possible constraint)
	 * @param productPK array of products (a possible constraint)
	 * @param productPackagePK array of product packages (a possible constraint)
	 * @param bottlerPK array of bottlers (a possible constraint)
	 * @param bottlerBusinessUnitPK array of bottler business units (a possible constraint)
	 * @param bottlerMarketUnitPK array of bottler market units (a possible constraint)
	 * @param bottlerBranchPK array of bottler branches (a possible constraint)
	 * @param bottlerSalesRoutePK array of bottler sales routes (a possible constraint)
	 * @param distributorDivisionPK array of distributor divisions (a possible constraint)
	 * @param distributorDistrictPK array of distributor districts (a possible constraint)
	 * @param storePK array of store PK's (a possible constraint)
	 */
	private void applyConstraints(SelectBuilder selectBuilder,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		if (storePK != null)
		{
			selectBuilder.addWhere("outofstockevents.store IN (" + StringUtils.stringArrayToString(storePK) + ")", true);
		}
		else if (distributorDistrictPK != null)
		{
			selectBuilder.addFrom("dstbdstrctstostores");
			selectBuilder.addWhere("outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict IN (" + StringUtils.stringArrayToString(distributorDistrictPK) + ")", true);
		}
		else if (distributorDivisionPK != null)
		{
			// These constraints are a bit tricky because distributor divisions on the web
			// page include database-recognized distributor divisions, as well as an amorphous
			// division "Distributor.com" which represents all stores that are hooked up to
			// Distributor.com (presumably). Hence we have to make sure that if a real distributor
			// division is chosen AND "Distributor.com", that we remove the potential overlap 
			// between the two to avoid double counting.
			
			// Is "Distributor.com" one of the passed in divisions? We use "-1" as a special
			// marker to indicate "Distributor.com" since all primary keys of real divisions
			// are nonnegative numbers.
			boolean distributorcom = false;
			for (int j=0; j<distributorDivisionPK.length; j++)
			{
				if (distributorDivisionPK[j].equals("-1"))
				{
					distributorcom = true;
					break;
				}
			}

			if (!distributorcom) // only traditional (non-distributor.com) stores are wanted
			{
				selectBuilder.addFrom("dstbdstrctstostores, dstbdvsnstodstbdstrcts");
				selectBuilder.addWhere("outofstockevents.store = dstbdstrctstostores.store AND " +
					"dstbdstrctstostores.distributordistrict = dstbdvsnstodstbdstrcts.distributordistrict AND " + 
					"dstbdvsnstodstbdstrcts.distributordivision IN (" + StringUtils.stringArrayToString(distributorDivisionPK) + ")", true);
			}
			else if (distributorcom && distributorDivisionPK.length == 1) // only distributor.com stores wanted
			{
				selectBuilder.addFrom("stores");
				selectBuilder.addWhere("outofstockevents.store = stores.primarykey AND " +
					"length(stores.distributorcom) > 0 AND " +
					"stores.distributorcom != " + BasePersistentBean.BQ + "-" + BasePersistentBean.EQ, true);
			}
			else if (distributorcom && distributorDivisionPK.length > 1) // distributor.com and traditional stores wanted
			{
				selectBuilder.addFrom("stores, dstbdstrctstostores, dstbdvsnstodstbdstrcts");
				selectBuilder.addWhere(
					"(" +
						// get the traditional stores which are not distributorcom stores
						"(" +
						"outofstockevents.store = dstbdstrctstostores.store AND " +
						"dstbdstrctstostores.distributordistrict = dstbdvsnstodstbdstrcts.distributordistrict AND " + 
						"dstbdvsnstodstbdstrcts.distributordivision IN (" + StringUtils.stringArrayToString(distributorDivisionPK) + ") AND " +
						"stores.primarykey = outofstockevents.store AND " +
						"(length(stores.distributorcom) = 0 OR (length(stores.distributorcom) > 0 AND " +
						"stores.distributorcom = " + BasePersistentBean.BQ + "-" + BasePersistentBean.EQ + "))" +
						")" + 
	
						"OR " +
	
						// add the distributorcom stores
						"(" +
						"outofstockevents.store = dstbdstrctstostores.store AND " +
						"dstbdstrctstostores.distributordistrict = dstbdvsnstodstbdstrcts.distributordistrict AND " + 
						"stores.primarykey = outofstockevents.store AND " +
						"length(stores.distributorcom) > 0 AND " +
						"stores.distributorcom != " + BasePersistentBean.BQ + "-" + BasePersistentBean.EQ + "" +
						")" +
					")",
					true);
			}
		}
		
		if (bottlerBranchPK != null)
		{
			selectBuilder.addFrom("bttlrbrchstostores");
			selectBuilder.addWhere("outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch IN (" + StringUtils.stringArrayToString(bottlerBranchPK) + ")", true);
		}
		else if (bottlerMarketUnitPK != null)
		{
			selectBuilder.addFrom("bttlrbrchstostores, bttlrmktuntstobttlrbrchs");
			selectBuilder.addWhere("outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit IN (" + StringUtils.stringArrayToString(bottlerMarketUnitPK) + ")", true);
		}
		else if (bottlerBusinessUnitPK != null)
		{
			selectBuilder.addFrom("bttlrbrchstostores, bttlrmktuntstobttlrbrchs, bttlrbsnsuntstobttlrmktunts");
			selectBuilder.addWhere("outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit IN (" + StringUtils.stringArrayToString(bottlerBusinessUnitPK) + ")", true);
		}
		else if (bottlerPK != null)
		{
			selectBuilder.addFrom("bttlrbrchstostores, bttlrmktuntstobttlrbrchs, bttlrbsnsuntstobttlrmktunts, bttlrstobttlrbsnsunts");
			selectBuilder.addWhere("outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler IN (" + StringUtils.stringArrayToString(bottlerPK) + ")", true);
		}

		if (bottlerSalesRoutePK != null)
		{
			selectBuilder.addFrom("bttlrslsrtstostores");
			selectBuilder.addWhere("outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute IN (" + StringUtils.stringArrayToString(bottlerSalesRoutePK) +")", true);
		}
		
		if (productPackagePK != null)
		{
			selectBuilder.addFrom("prdctstoprdctpkgs");
			selectBuilder.addWhere("outofstockevents.product = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage IN (" + StringUtils.stringArrayToString(productPackagePK) + ")", true);
		}
		
		if (productPK != null)
		{
			selectBuilder.addWhere("outofstockevents.product IN (" + StringUtils.stringArrayToString(productPK) + ")", true);
		}
		else if (productCategoryPK != null)
		{
			selectBuilder.addFrom("prdctctgrstoprdcts");
			selectBuilder.addWhere("outofstockevents.product = prdctctgrstoprdcts.product AND " +
				"prdctctgrstoprdcts.productcategory IN (" + StringUtils.stringArrayToString(productCategoryPK) + ")", true);
		}
		
		// This last constraint masks all out of stock events that are marked ignore (see the 
		// class net.btlcpy.outofstock.persistence.beans.Instrumented).
		selectBuilder.addWhere("bitand(outofstockevents.metaflags, 1)+0 = 0", true);
	}
	
	/**
	 * Calculates the number of days between two dates, inclusive. If either is null, or if the
	 * start date is greater than or equal to the end date, it always returns ONE.
	 * 
	 * @param beginDate start date
	 * @param endDate end date
	 * @return number of days separating the two dates, inclusive
	 */
	private int calculateInterval(Date beginDate, Date endDate)
	{
		if (beginDate == null || endDate == null)
			return 1;
		
		Calendar beginCalendar = Calendar.getInstance(), endCalendar = Calendar.getInstance();
		beginCalendar.setTime(beginDate);
		endCalendar.setTime(endDate);
		
		beginCalendar.set(Calendar.HOUR_OF_DAY, 0);
		beginCalendar.set(Calendar.MINUTE, 0);
		beginCalendar.set(Calendar.SECOND, 0);
		endCalendar.set(Calendar.HOUR_OF_DAY, 0);
		endCalendar.set(Calendar.MINUTE, 0);
		endCalendar.set(Calendar.SECOND, 0);
		
		if (beginCalendar.getTime().getTime() >= endCalendar.getTime().getTime())
			return 1;
		
		int dayCounter = 1;
		while (true)
		{
			beginCalendar.add(Calendar.DAY_OF_YEAR, 1);
			dayCounter++;
			if (beginCalendar.getTime().getTime() >= endCalendar.getTime().getTime())
				return dayCounter;			
		}
	}

	/**
	 * Constructs the SQL query for calculating the total losses for the desired
	 * summary target (stores, bottlers, etc.).
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param resultSize how many results desired
	 * @param productCategoryPK array of product categories (a possible constraint)
	 * @param productPK array of products (a possible constraint)
	 * @param productPackagePK array of product packages (a possible constraint)
	 * @param bottlerPK array of bottlers (a possible constraint)
	 * @param bottlerBusinessUnitPK array of bottler business units (a possible constraint)
	 * @param bottlerMarketUnitPK array of bottler market units (a possible constraint)
	 * @param bottlerBranchPK array of bottler branches (a possible constraint)
	 * @param bottlerSalesRoutePK array of bottler sales routes (a possible constraint)
	 * @param distributorDivisionPK array of distributor divisions (a possible constraint)
	 * @param distributorDistrictPK array of distributor districts (a possible constraint)
	 * @param storePK array of store PK's (a possible constraint)
	 */
	private void constructDailyEventsPerStore(Date beginDate, Date endDate, 
		String resultSize, 
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		/*
		SELECT id, dateoccurred, count(*) AS countofevents
		FROM stores, outofstockevents 
		WHERE 
		id < 10 AND 
		stores.primarykey = outofstockevents.store 
		GROUP BY id, dateoccurred 
		ORDER BY id, dateoccurred
		 */
		sbEventsPerStore = new SelectBuilder();
		
		sbEventsPerStore.addSelect("id AS \"Store ID\", count(*)/" + calculateInterval(beginDate, endDate) + " AS \"Average Daily Events\"");
		sbEventsPerStore.addFrom("stores, outofstockevents");
		sbEventsPerStore.addWhere("stores.primarykey = outofstockevents.store", true);
		sbEventsPerStore.addGroup("id");
		sbEventsPerStore.addOrder("\"Average Daily Events\" DESC");
			
		if (beginDate != null)
			sbEventsPerStore.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
		if (endDate != null)
			sbEventsPerStore.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
		
		applyConstraints(sbEventsPerStore, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);
		
		if (resultSize.equals("top25"))
			sbEventsPerStore = new SelectBuilder("*", sbEventsPerStore, "ROWNUM <= 25", null, null, null);
		else if (resultSize.equals("top50"))
			sbEventsPerStore = new SelectBuilder("*", sbEventsPerStore, "ROWNUM <= 50", null, null, null);
		//else if (resultSize.equals("all"))
		//	sbEventsPerStore = new SelectBuilder("*", sbEventsPerStore, null, null, null, null);
	}

	/**
	 * @return the number of queries. If a daily breakout is desired, and the summary target is neither
	 * "day" nor "week", then the number of queries will be three. Otherwise, only two queries will be
	 * run, since the daily breakout does not make sense in the context of "day"/"week" summary targets.
	 */
	public Integer numberOfQueries() 
	{
		return new Integer(1);
	}
	
	public String getQuery(int queryNumber) 
	{ 
		switch (queryNumber)
		{
		case 0:
			return sbEventsPerStore.toString();
		}
		return null;
	}

	public String[][] getQueryKeyColumns()
	{
		return keyColumns;
	}
	
	public void doPreProcessing(Connection connection, int queryNumber) {}
	public void doPostProcessing(Connection connection, int queryNumber) 
	{
		if (queryNumber == 0)
		{
			columnFormats = new HashMap();
			NumberFormat averagesFormatter = NumberFormat.getNumberInstance(Locale.US);
			averagesFormatter.setMaximumFractionDigits(2);
			averagesFormatter.setMinimumFractionDigits(2);

			ReportCellFormat dataCellFormat = new ReportCellFormat("numericDataCell reportDataCellA");
			dataCellFormat.addFormatter(-1, averagesFormatter);
			ReportColumnFormat averagesColumnFormat = new ReportColumnFormat(null, dataCellFormat, null);
			
			columnFormats.put("Average Daily Events", averagesColumnFormat);

			resetExcelFormats(1);
			
			ReportExcelFormat excelFormat = new ReportExcelFormat();
			setExcelFormat(0, excelFormat);
			
			excelFormat.getDefaultCellStyle().getFont().setBoldWeight(HSSFFont.BOLDWEIGHT_BOLD);
			excelFormat.getDefaultCellStyle().setAlignment(HSSFCellStyle.ALIGN_RIGHT);
			
			ReportExcelCellStyle headerCellStyle = 
				new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_CENTER, HSSFCellStyle.VERTICAL_BOTTOM, (short) 0,
					new ReportExcelFont("Arial", (short) 10, HSSFColor.YELLOW.index, HSSFFont.BOLDWEIGHT_BOLD), 
					HSSFCellStyle.SOLID_FOREGROUND, HSSFColor.BLACK.index, HSSFColor.AUTOMATIC.index); 
			excelFormat.addCellStyle(0, 0, 1, 0, headerCellStyle);
		}
	}

	protected String[] getInitialSortedColumns(int queryNumber)
	{
		if (queryNumber == 0)
		{
			String isc[] = { "\"Average Daily Events\"" };
			return isc;
		}
		return null;
	}

	protected boolean isSortedAscendingInitially(int queryNumber)
	{
		if (queryNumber == 0)
			return false;
		return true;
	}
	
	public String[] getUnsortableColumns(int queryNumber)
	{
		return null;
	}
	
	public String getSortInfo(int queryNumber, String columnName)
	{
		if (queryNumber == 0)
		{
			if (sortInfo != null && sortInfo.size() > 0)
			{
				String si = (String) sortInfo.get(columnName);
				if (si != null)
					return si;
			}

			return columnName;
		}
		return null;
	}
	
	public ReportCellFormat getHeaderCellFormat(String column)
	{
		if (columnFormats == null)
			return null;
		
		ReportColumnFormat columnFormat = (ReportColumnFormat) columnFormats.get(column);
		if (columnFormat != null)
			return columnFormat.getHeaderCellFormat();
		return null;
	} 
	
	public ReportCellFormat getDataCellFormat(String column) 
	{ 
		if (columnFormats == null)
			return null;
		
		ReportColumnFormat columnFormat = (ReportColumnFormat) columnFormats.get(column);
		if (columnFormat != null)
			return columnFormat.getDataCellFormat();
		return null;
	} 

	public ReportCellFormat getFooterCellFormat(String column) 
	{ 
		if (columnFormats == null)
			return null;
		
		ReportColumnFormat columnFormat = (ReportColumnFormat) columnFormats.get(column);
		if (columnFormat != null)
			return columnFormat.getFooterCellFormat();
		return null;
	} 
}
