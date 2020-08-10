package net.btlcpy.outofstock.reports;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.persistence.beans.Bottler;
import net.btlcpy.outofstock.persistence.beans.BottlerBranch;
import net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute;
import net.btlcpy.outofstock.persistence.beans.Product;
import net.btlcpy.outofstock.persistence.beans.ProductPackage;
import net.btlcpy.outofstock.persistence.beans.DistributorDistrict;
import net.btlcpy.outofstock.persistence.beans.Store;
import net.btlcpy.outofstock.reports.format.ReportCellFormat;
import net.btlcpy.outofstock.reports.format.ReportColumnFormat;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelCellStyle;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFont;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFormat;
import net.btlcpy.outofstock.utilities.SelectBuilder;
import net.btlcpy.outofstock.utilities.StringUtils;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * <p>
 * This class produces reports of average sales losses per week due to out of stock events 
 * across different entities such as stores, bottlers, and products. Two queries are always 
 * run for this report: one calculates the average loss per entity, another calculates
 * the average loss split by reason (bottler versus Distributor).
 * </p>
 * <p>
 * One major 'exception' running through this class is a special type of report: average
 * loss per traditional/distributor.com stores per week. In other words, whereas typically this
 * report gives the overall average loss per week for a particular entity, there is one
 * exception: calculating the average loss per traditional stores per week, and the same
 * for distributor.com stores per week - hence this exception actually produces three averages
 * (traditional, distributor.com, and overall).
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class AveragesReport extends AveragesTotalsBaseReport
{
	/**
	 * There are only two queries that are run. However in the case of the
	 * Distributor.com/traditional report, the first query is composed of three
	 * subqueries UNION'd together. This means the following:
	 * <ul>
	 * <li>sbAverages: for calculating average losses across all
	 * entities</li>
	 * <li>sbAveragesByReason: for calculating average losses per entity
	 * split by reason (bottler versus Distributor)</li>
	 * <li>sbAveragesForTraditionals: a special case; for calculating
	 * average losses across all traditional stores.</li>
	 * <li>sbAveragesForDistributorComs: a special case; for calculating
	 * average losses across all Distributor.com stores.</li>
	 * </ul>
	 */
	private SelectBuilder sbAverages, sbAveragesByReason, 
		sbAveragesForTraditionals, sbAveragesForDistributorComs;
	
	/** The key columns to use for the two queries */
	private String[][] keyColumns = { null, {"Week", "Reason"} };
	
	/** A switch to indicate whether the first column should be continuously
	 * ascending / descending or not. Essentially here because sometimes there
	 * is no data for a particular week. Instead of not displaying any data
	 * for that week, we want to show that the average was zero. So we are
	 * forcing the first column (which is always the week in this type of
	 * report) to be continuous.
	 */
	private boolean firstColumnContinuous = true;
	
	/** The number of weeks in the report. */
	private int numberOfWeeksInReport = 0;

	public AveragesReport(Connection connection, String username, Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, 
		String[] bottlerPKID, String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, String[] bottlerSalesRoutePKID, 
		String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
	{
		super(connection, username);
		
		reportBeginDate = beginDate;
		reportLostSalesMetric = lostSalesMetric;
		reportSummaryTarget = summaryTarget;
		Calendar beginCalendar = Calendar.getInstance(Locale.US);
		Calendar endCalendar = Calendar.getInstance(Locale.US);
		beginCalendar.setTime(beginDate);
		endCalendar.setTime(endDate);
		
		if (endCalendar.get(Calendar.YEAR) > beginCalendar.get(Calendar.YEAR))
		{
			// calculate the days in beginCalendar
			int numberOfDays = beginCalendar.get(Calendar.DAY_OF_YEAR);
			beginCalendar.set(beginCalendar.get(Calendar.YEAR), 11, 31);
			numberOfDays = beginCalendar.get(Calendar.DAY_OF_YEAR) - numberOfDays + 1;

			// calculate days in intervening years
			int year = beginCalendar.get(Calendar.YEAR);
			while (true)
			{
				year++;
				if (endCalendar.get(Calendar.YEAR) <= year)
					break;
				beginCalendar.set(year, 11, 31);
				numberOfDays += beginCalendar.get(Calendar.DAY_OF_YEAR);
			}
			
			// calculate days in endCalendar
			numberOfDays += endCalendar.get(Calendar.DAY_OF_YEAR);
			numberOfWeeksInReport = (int) Math.ceil((numberOfDays / 7.0));
		}
		else
			numberOfWeeksInReport = 
				(int) Math.ceil(((endCalendar.get(Calendar.DAY_OF_YEAR) - beginCalendar.get(Calendar.DAY_OF_YEAR)) / 7.0));
		
		
		userActionDescription = constructUserActionDescription(
			beginDate, endDate, summaryTarget, lostSalesMetric);
		
		excelReportDescription = constructExcelReportDescription(
			beginDate, endDate, summaryTarget, lostSalesMetric,
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
		
		constructAverages(beginDate, endDate, summaryTarget, lostSalesMetric,
			productCategoryPK, productPK, productPackagePK, 
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK, 
			distributorDivisionPK, distributorDistrictPK, storePK);
		constructAveragesByReason(beginDate, endDate, summaryTarget, lostSalesMetric,
			productCategoryPK, productPK, productPackagePK, 
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK, 
			distributorDivisionPK, distributorDistrictPK, storePK);
	}
	
	public Date getBeginDate()
	{
		return reportBeginDate;
	}
	
	public String[] getTrendChartRelevantColumns()
	{
		if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
		{
			String[] relevantColumns = { "Average Loss<br/>(Traditional)", "Average Loss<br/>(Distributor.com)" };
			return relevantColumns;
		}
		
		String[] relevantColumns = { "Average Loss" };
		return relevantColumns;
	}
	/**
	 * Constructs a string suitable for logging as a user action record.
	 * 
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param lostSalesMetric metric to use for measuring lost sales
	 * @return a single String that summarizes the report as a user action
	 */
	private String constructUserActionDescription(Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric)
	{
		StringBuffer d = new StringBuffer("Average lost sales report");
		
		if (lostSalesMetric.equals("quantity"))
			d.append(" (units only)");
		else if (lostSalesMetric.equals("amount"))
			d.append(" (dollars only)");
		else if (lostSalesMetric.equals("all"))
			d.append(" (dollars and units)");
		
		d.append(" across all");
		
		if (summaryTarget.equalsIgnoreCase("store"))
			d.append(" stores");
		else if (summaryTarget.equalsIgnoreCase("storetvss"))
			d.append(" stores (split by traditional versus distributor.com)");
		else if (summaryTarget.equalsIgnoreCase("bottler"))
			d.append(" bottlers");
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
			d.append(" bottler branches");
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
			d.append(" bottler sales routes");
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
			d.append(" Distributor districts");
		else if (summaryTarget.equalsIgnoreCase("product"))
			d.append(" products");
		else if (summaryTarget.equalsIgnoreCase("package"))
			d.append(" packages");
		
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
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param lostSalesMetric metric to use for measuring lost sales
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
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryID, String[] productID, String[] productPackageID, 
		String[] bottlerID, String[] bottlerBusinessUnitID, String[] bottlerMarketUnitID, String[] bottlerBranchID, String[] bottlerSalesRouteID, 
		String[] distributorDivisionID, String[] distributorDistrictID, String[] storeID)
	{
		StringBuffer d = new StringBuffer("Average lost sales report");
		
		if (lostSalesMetric.equals("quantity"))
			d.append(" (units only)");
		else if (lostSalesMetric.equals("amount"))
			d.append(" (dollars only)");
		else if (lostSalesMetric.equals("all"))
			d.append(" (dollars and units)");
		
		d.append(" across all");
		
		if (summaryTarget.equalsIgnoreCase("store"))
			d.append(" stores");
		else if (summaryTarget.equalsIgnoreCase("storetvss"))
			d.append(" stores (split by traditional versus distributor.com)");
		else if (summaryTarget.equalsIgnoreCase("bottler"))
			d.append(" bottlers");
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
			d.append(" bottler branches");
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
			d.append(" bottler sales routes");
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
			d.append(" Distributor districts");
		else if (summaryTarget.equalsIgnoreCase("product"))
			d.append(" products");
		else if (summaryTarget.equalsIgnoreCase("package"))
			d.append(" packages");
		
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
		return "Averages Report";
	}

	public String getQueryTitle(int queryNumber)
	{
		if (queryNumber == 0)
			return "Report Data:";
		return "";
	}

	/**
	 * Averages reports may be constrained across a number of different parameters. This
	 * method takes the parameters that were set on the Web page for specification of
	 * report parameters, and applies them to the SQL queries that will be executed
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
	 * Constructs the SQL query for calculating the average losses for the desired
	 * summary target (stores, bottlers, etc.) per week.
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param lostSalesMetric metric to use for measuring lost sales
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
	private void constructAverages(Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		int totalActive = 0;
		sbAverages = new SelectBuilder();
		
		sbAverages.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
		sbAverages.addFrom("outofstockevents");
		sbAverages.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");

		if (summaryTarget.equalsIgnoreCase("store") || summaryTarget.equalsIgnoreCase("storetvss"))
		{
			sbAverages.addSelect("stores.id AS \"Store ID\"");
			sbAverages.addFrom("stores");
			sbAverages.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbAverages.addGroup("stores.id");
			totalActive = Store.getTotalActive();
			
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals = new SelectBuilder();
				sbAveragesForTraditionals.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
				sbAveragesForTraditionals.addFrom("outofstockevents");
				sbAveragesForTraditionals.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");
				sbAveragesForTraditionals.addSelect("stores.id AS \"Store ID\"");
				sbAveragesForTraditionals.addFrom("stores");
				sbAveragesForTraditionals.addWhere("stores.primarykey = outofstockevents.store AND " +
					"bitand(stores.metaflags, 1)+0 = 0 AND " +
					"(length(stores.distributorcom) = 0 OR (length(stores.distributorcom) > 0 AND stores.distributorcom = '-'))", true);
				sbAveragesForTraditionals.addGroup("stores.id");

				sbAveragesForDistributorComs = new SelectBuilder();
				sbAveragesForDistributorComs.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
				sbAveragesForDistributorComs.addFrom("outofstockevents");
				sbAveragesForDistributorComs.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");
				sbAveragesForDistributorComs.addSelect("stores.id AS \"Store ID\"");
				sbAveragesForDistributorComs.addFrom("stores");
				sbAveragesForDistributorComs.addWhere("stores.primarykey = outofstockevents.store AND " +
					"bitand(stores.metaflags, 1)+0 = 0 AND " +
					"(length(stores.distributorcom) > 0 AND stores.distributorcom != '-')", true);
				sbAveragesForDistributorComs.addGroup("stores.id");
			}
		}
		else if (summaryTarget.equalsIgnoreCase("bottler"))
		{
			sbAverages.addSelect("bttlrs.name AS \"Bottler\"");
			sbAverages.addFrom("bttlrs, bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts, " +
				"bttlrmktuntstobttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbAverages.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey", true);
			sbAverages.addGroup("bttlrs.name");
			totalActive = Bottler.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
		{
			sbAverages.addSelect("bttlrbrchs.name AS \"Bottler Branch\"");
			sbAverages.addFrom("bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbAverages.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey", true);
			sbAverages.addGroup("bttlrbrchs.name");
			totalActive = BottlerBranch.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
		{
			sbAverages.addSelect("bttlrslsrts.name AS \"Bottler Sales Route\"");
			sbAverages.addFrom("bttlrslsrts, bttlrslsrtstostores, outofstockevents");
			sbAverages.addWhere(
				"outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey", true);
			sbAverages.addGroup("bttlrslsrts.name");
			totalActive = BottlerSalesRoute.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
		{
			sbAverages.addSelect("dstbdstrcts.name AS \"Distributor District\"");
			sbAverages.addFrom("dstbdstrcts, dstbdstrctstostores, outofstockevents");
			sbAverages.addWhere(
				"outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey", true);
			sbAverages.addGroup("dstbdstrcts.name");
			totalActive = DistributorDistrict.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("product"))
		{
			sbAverages.addSelect("prdcts.upcid as \"Product UPC\", description as \"Description\"");
			sbAverages.addFrom("prdcts, outofstockevents");
			sbAverages.addWhere(
				"prdcts.primarykey = outofstockevents.product", true);
			sbAverages.addGroup("prdcts.upcid, description");
			totalActive = Product.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("package"))
		{
			sbAverages.addSelect("prdctpkgs.name as \"Package\"");
			sbAverages.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs, outofstockevents");
			sbAverages.addWhere(
				"prdcts.primarykey = outofstockevents.product AND " +
				"prdcts.primarykey = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbAverages.addGroup("prdctpkgs.name");
			totalActive = ProductPackage.getTotalActive();
		}

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
		{
			sbAverages.addSelect(
				"sum(outofstockevents.lostsalesquantity) AS \"Total Lost Sales (Units)\"");
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addSelect(
					"sum(outofstockevents.lostsalesquantity) AS \"Total Lost Sales (Units)\"");
				sbAveragesForDistributorComs.addSelect(
					"sum(outofstockevents.lostsalesquantity) AS \"Total Lost Sales (Units)\"");
			}
		}
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
		{
			sbAverages.addSelect(
				"sum(outofstockevents.lostsalesamount) AS \"Total Lost Sales (Dollars)\"");
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addSelect(
					"sum(outofstockevents.lostsalesamount) AS \"Total Lost Sales (Dollars)\"");
				sbAveragesForDistributorComs.addSelect(
					"sum(outofstockevents.lostsalesamount) AS \"Total Lost Sales (Dollars)\"");
			}
		}
			
		if (beginDate != null)
		{
			sbAverages.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
				sbAveragesForDistributorComs.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
			}
		}
		if (endDate != null)
		{
			sbAverages.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
				sbAveragesForDistributorComs.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
			}
		}
		
		applyConstraints(sbAverages, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);
		if (summaryTarget.equalsIgnoreCase("storetvss"))
		{
			applyConstraints(sbAveragesForTraditionals, productCategoryPK, productPK, productPackagePK,
				bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
				distributorDivisionPK, distributorDistrictPK, storePK);
			applyConstraints(sbAveragesForDistributorComs, productCategoryPK, productPK, productPackagePK,
				bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
				distributorDivisionPK, distributorDistrictPK, storePK);	
		}
		
		if (summaryTarget.equalsIgnoreCase("storetvss"))
		{
			// the numbers in the stype flag are just for ordering purposes later on
			sbAverages = new SelectBuilder(
				// INCORRECT "avg(\"Total Lost Sales (Units)\") AS \"Average Loss (Units)\", "
				// INCORRECT "avg(\"Total Lost Sales (Dollars)\") AS \"Average Loss (Dollars)\", " +
				"\"Week\", '3total' as stype",
				sbAverages, null, "\"Week\"", null, null);
			sbAveragesForTraditionals = new SelectBuilder(
				// INCORRECT "avg(\"Total Lost Sales (Units)\") AS \"Average Loss (Units)\", "
				// INCORRECT "avg(\"Total Lost Sales (Dollars)\") AS \"Average Loss (Dollars)\", " +
				"\"Week\", '2traditional' as stype",
				sbAveragesForTraditionals, null, "\"Week\"", null, null);
			sbAveragesForDistributorComs = new SelectBuilder(
				// INCORRECT "avg(\"Total Lost Sales (Units)\") AS \"Average Loss (Units)\", "
				// INCORRECT "avg(\"Total Lost Sales (Dollars)\") AS \"Average Loss (Dollars)\", " +
				"\"Week\", '1distributor.com' as stype",
				sbAveragesForDistributorComs, null, "\"Week\"", null, null);
		}
		else
		{
			sbAverages = new SelectBuilder(
				// INCORRECT "avg(\"Total Lost Sales (Units)\") AS \"Average Loss (Units)\", "
				// INCORRECT "avg(\"Total Lost Sales (Dollars)\") AS \"Average Loss (Dollars)\", " +
				"\"Week\"",
				sbAverages, null, "\"Week\"", null, "\"Week\"");
		}

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
		{
			sbAverages.addSelect(
				"sum(\"Total Lost Sales (Units)\")/" + totalActive + " AS \"Average Lost Sales (Units)\"");
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addSelect(
					"sum(\"Total Lost Sales (Units)\")/" + Store.getTotalActiveTraditional() + " AS \"Average Lost Sales (Units)\"");
				sbAveragesForDistributorComs.addSelect(
					"sum(\"Total Lost Sales (Units)\")/" + Store.getTotalActiveDistributorCom() + " AS \"Average Lost Sales (Units)\"");
			}
		}
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
		{
			sbAverages.addSelect(
				"sum(\"Total Lost Sales (Dollars)\")/" + totalActive + " AS \"Average Lost Sales (Dollars)\"");
			if (summaryTarget.equalsIgnoreCase("storetvss"))
			{
				sbAveragesForTraditionals.addSelect(
					"sum(\"Total Lost Sales (Dollars)\")/" + Store.getTotalActiveTraditional() + " AS \"Average Lost Sales (Dollars)\"");
				sbAveragesForDistributorComs.addSelect(
					"sum(\"Total Lost Sales (Dollars)\")/" + Store.getTotalActiveDistributorCom() + " AS \"Average Lost Sales (Dollars)\"");
			}
		}
	}
	
	/**
	 * Constructs the SQL query for calculating the average losses for the desired
	 * summary target (stores, bottlers, etc.) split by reason (bottler versus
	 * Distributor).
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param lostSalesMetric metric to use for measuring lost sales
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
	private void constructAveragesByReason(Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		int totalActive = 0;
		sbAveragesByReason = new SelectBuilder();

		sbAveragesByReason.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
		sbAveragesByReason.addFrom("outofstockevents");
		sbAveragesByReason.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");

		if (summaryTarget.equalsIgnoreCase("store") || summaryTarget.equalsIgnoreCase("storetvss"))
		{
			sbAveragesByReason.addSelect("stores.id AS \"Store ID\"");
			sbAveragesByReason.addFrom("stores");
			sbAveragesByReason.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbAveragesByReason.addGroup("stores.id");
			sbAveragesByReason.addOrder("\"Store ID\"");
			totalActive = Store.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("bottler"))
		{
			sbAveragesByReason.addSelect("bttlrs.name AS \"Bottler\"");
			sbAveragesByReason.addFrom("bttlrs, bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts, " +
				"bttlrmktuntstobttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbAveragesByReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey", true);
			sbAveragesByReason.addGroup("bttlrs.name");
			sbAveragesByReason.addOrder("\"Bottler\"");
			totalActive = Bottler.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
		{
			sbAveragesByReason.addSelect("bttlrbrchs.name AS \"Bottler Branch\"");
			sbAveragesByReason.addFrom("bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbAveragesByReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey", true);
			sbAveragesByReason.addGroup("bttlrbrchs.name");
			sbAveragesByReason.addOrder("\"Bottler Branch\"");
			totalActive = BottlerBranch.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
		{
			sbAveragesByReason.addSelect("bttlrslsrts.name AS \"Bottler Sales Route\"");
			sbAveragesByReason.addFrom("bttlrslsrts, bttlrslsrtstostores, outofstockevents");
			sbAveragesByReason.addWhere(
				"outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey", true);
			sbAveragesByReason.addGroup("bttlrslsrts.name");
			sbAveragesByReason.addOrder("\"Bottler Sales Route\"");
			totalActive = BottlerSalesRoute.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
		{
			sbAveragesByReason.addSelect("dstbdstrcts.name AS \"Distributor District\"");
			sbAveragesByReason.addFrom("dstbdstrcts, dstbdstrctstostores, outofstockevents");
			sbAveragesByReason.addWhere(
				"outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey", true);
			sbAveragesByReason.addGroup("dstbdstrcts.name");
			sbAveragesByReason.addOrder("\"Distributor District\"");
			totalActive = DistributorDistrict.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("product"))
		{
			sbAveragesByReason.addSelect("prdcts.upcid as \"Product UPC\"");
			sbAveragesByReason.addFrom("prdcts, outofstockevents");
			sbAveragesByReason.addWhere(
				"prdcts.primarykey = outofstockevents.product", true);
			sbAveragesByReason.addGroup("prdcts.upcid");
			sbAveragesByReason.addOrder("\"Product UPC\"");
			totalActive = Product.getTotalActive();
		}
		else if (summaryTarget.equalsIgnoreCase("package"))
		{
			sbAveragesByReason.addSelect("prdctpkgs.name as \"Package\"");
			sbAveragesByReason.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs, outofstockevents");
			sbAveragesByReason.addWhere(
				"prdcts.primarykey = outofstockevents.product AND " +
				"prdcts.primarykey = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbAveragesByReason.addGroup("prdctpkgs.name");
			sbAveragesByReason.addOrder("\"Package\"");
			totalActive = ProductPackage.getTotalActive();
		}
		
		sbAveragesByReason.addSelect(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END as \"Reason\"");

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
			sbAveragesByReason.addSelect(
				"sum(outofstockevents.lostsalesquantity) AS \"Loss By Reason (Units)\"");
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
			sbAveragesByReason.addSelect(
				"sum(outofstockevents.lostsalesamount) AS \"Loss By Reason (Dollars)\"");

		if (beginDate != null)
			sbAveragesByReason.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
		if (endDate != null)
			sbAveragesByReason.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
		sbAveragesByReason.addGroup(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END");

		applyConstraints(sbAveragesByReason, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);

		sbAveragesByReason = new SelectBuilder(
			// INCORRECT "avg(\"Total Lost Sales (Units)\") AS \"Loss By Reason (Units)\", "
			// INCORRECT "avg(\"Total Lost Sales (Dollars)\") AS \"Loss By Reason (Dollars)\", " +
			"\"Week\", \"Reason\"",
			sbAveragesByReason, null, "\"Week\", \"Reason\"", null, "\"Week\", \"Reason\"");

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
			sbAveragesByReason.addSelect(
				"sum(\"Loss By Reason (Units)\")/" + totalActive + " AS \"Loss By Reason (Units)\"");
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
			sbAveragesByReason.addSelect(
				"sum(\"Loss By Reason (Dollars)\")/" + totalActive + " AS \"Loss By Reason (Dollars)\"");
	}

	public Integer numberOfQueries() 
	{
		return new Integer(2);
	}
	
	public String getQuery(int queryNumber) 
	{ 
		switch (queryNumber)
		{
		case 0:
			/*
			 * If the desired summary target is a Distributor.com/traditional comparison of
			 * stores, then there is a special case for that. The resulting SQL query is
			 * a union of three subqueries, one for calculating the overall average losses,
			 * a second for average losses for traditional stores, and a third for average
			 * losses for Distributor.com stores.
			 * 
			 * Otherwise, a query for the overall average losses across all entitities
			 * (stores, bottlers, or whatever the case may be) is executed.
			 */
			if (reportSummaryTarget.equals("storetvss"))
				return "SELECT * FROM (" + 
					sbAverages.toString() + " UNION " +
					sbAveragesForTraditionals.toString() + " UNION " +
					sbAveragesForDistributorComs.toString() + 
					") ORDER BY \"Week\", stype";
			return sbAverages.toString();
		case 1:
			/*
			 * This one does not change regardless of whether the special case of
			 * Distributor.com/traditional store comparison is desired or not. It is the
			 * query that returns the average losses split out by reason (bottler
			 * versus Distributor related).
			 */
			return sbAveragesByReason.toString();
		}
		return null;
	}

	public String[][] getQueryKeyColumns()
	{
		return keyColumns;
	}
	
	public boolean isFirstColumnContinuous()
	{
		return firstColumnContinuous;
	}

	public void setFirstColumnContinuous(boolean firstColumnContinuous)
	{
		this.firstColumnContinuous = firstColumnContinuous;
	}

	public void doPreProcessing(Connection connection, int queryNumber) {}
	public void doPostProcessing(Connection connection, int queryNumber) 
	{
		if (queryNumber == 1)
			mergeResults();
	}

	/**
	 * <p>
	 * Merges the results of the 2 queries' results into one table of data. The averages from
	 * the first query are placed on the same row with the averages split by reason from the
	 * second query. The rows are matched via the entities unique names or ID's.
	 * For example, if the report was over bottlers, the two queries will refer to bottler
	 * names that can then be cross-referenced to create a single row covering both
	 * query results for that bottler.
	 * </p>
	 * <p>
	 * Ultimately, the merged results are used to replace the original results.
	 * </p>
	 */
	private void mergeResults()
	{
		// Retrieve the original results first of all
		ListOrderedMap results = super.getResults();
		ListOrderedMap averages = null, averagesByReason = null;
		boolean hasAveragesByReason = false;
		String joinColumn = null;
		int firstColumnContinuousCounter = 1; // currently this would be for the "Week" column - no other option presently
		boolean showQuantity = reportLostSalesMetric.equals("all") || reportLostSalesMetric.equals("quantity");
		boolean showAmount = reportLostSalesMetric.equals("all") || reportLostSalesMetric.equals("amount");
		// the position of the item to sort on in a compound cell that contains quantity and/or amount
		// The amount is used if both are present (hence position 1 - i.e. second item, otherwise the
		// first item (position 0 since it is zero-based)
		char sortOnFirstOrSecond = (showQuantity && showAmount) ? '1' : '0';

		if (results == null)
			return;
		
		switch (results.size())
		{
		case 2:
			averagesByReason = (ListOrderedMap) super.getResults().getValue(1); // keys are some column + "reason"
			hasAveragesByReason = (averagesByReason != null && averagesByReason.size() > 0);
		case 1:
			averages = (ListOrderedMap) super.getResults().getValue(0); // keys are plain integers in order
			break;
		case 0:
			return;
		}

		// Set sorting and formatting information
		sortInfo = new ListOrderedMap();
		columnFormats = new HashMap();
		NumberFormat quantityFormatter = NumberFormat.getNumberInstance(Locale.US);
		quantityFormatter.setMaximumFractionDigits(0);
		NumberFormat amountFormatter = NumberFormat.getCurrencyInstance(Locale.US); 
		amountFormatter.setMaximumFractionDigits(0);

		ReportCellFormat bottlerDataCellFormat = new ReportCellFormat("numericDataCell bottlerDataCell");
		ReportCellFormat distributorDataCellFormat = new ReportCellFormat("numericDataCell distributorDataCell");
		ReportCellFormat grandTotalDataCellFormat = new ReportCellFormat("numericDataCell reportDataCellA");

		/* The following if statement is no longer useful since the report is supposed to place
		 * both amount and quantity in separate cells. This means that using a single formatter for the
		 * data is no longer very good since the format changes every other line. Until we hear otherwise,
		 * I am going to choose the formatter for the quantity for both types of data - this basically
		 * means that no dollar sign will currently appear for the amount.
		if (showQuantity && showAmount)
		{
			bottlerDataCellFormat.addFormatter(0, quantityFormatter);
			distributorDataCellFormat.addFormatter(0, quantityFormatter);
			grandTotalDataCellFormat.addFormatter(0, quantityFormatter);
			bottlerDataCellFormat.addFormatter(1, amountFormatter);
			distributorDataCellFormat.addFormatter(1, amountFormatter);
			grandTotalDataCellFormat.addFormatter(1, amountFormatter);
		}
		else if (showAmount)
		{
			bottlerDataCellFormat.addFormatter(0, amountFormatter);
			distributorDataCellFormat.addFormatter(0, amountFormatter);
			grandTotalDataCellFormat.addFormatter(0, amountFormatter);
		}
		else if (showQuantity)
		{
			bottlerDataCellFormat.addFormatter(0, quantityFormatter);
			distributorDataCellFormat.addFormatter(0, quantityFormatter);
			grandTotalDataCellFormat.addFormatter(0, quantityFormatter);
		}
		*/
		bottlerDataCellFormat.addFormatter(-1, quantityFormatter);
		distributorDataCellFormat.addFormatter(-1, quantityFormatter);
		grandTotalDataCellFormat.addFormatter(-1, quantityFormatter);

		bottlerDataCellFormat.addSubCellCssStyle(-1, "bottlerDataSubCell");
		distributorDataCellFormat.addSubCellCssStyle(-1, "distributorDataSubCell");

		ReportColumnFormat bottlerColumnFormat = new ReportColumnFormat(null, bottlerDataCellFormat, null);
		ReportColumnFormat distributorColumnFormat = new ReportColumnFormat(null, distributorDataCellFormat, null);
		ReportColumnFormat grandTotalColumnFormat = new ReportColumnFormat(null, grandTotalDataCellFormat, null);
		
		columnFormats.put("Average Loss", grandTotalColumnFormat);
		if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
		{
			columnFormats.put("Average Loss<br/>(Traditional)", grandTotalColumnFormat);
			columnFormats.put("Average Loss<br/>(Distributor.com)", grandTotalColumnFormat);

			sortInfo.put("Average Loss<br/>(Traditional)", "Average Loss<br/>(Traditional):" + sortOnFirstOrSecond);
			sortInfo.put("Average Loss<br/>(Distributor.com)", "Average Loss<br/>(Distributor.com):" + sortOnFirstOrSecond);
		}

		if (hasAveragesByReason)
		{
			ListOrderedMap firstRow = (ListOrderedMap) averagesByReason.getValue(0);
			// Get the column which is common across the three queries, to look up subtotals
			// and to join them into one row. This is ALWAYS the first column (see the
			// methods constructTotalsByXXX to understand and verify that. In this class,
			// as opposed to TotalsReport, the first column is always "Week", since
			// the average losses are reported on a per week basis.
			joinColumn = (String) firstRow.get(0); // "Week" so far always.
			columnFormats.put("Bottler<br/>Related", bottlerColumnFormat);
			columnFormats.put("Distributor<br/>Related", distributorColumnFormat);

			sortInfo.put("Bottler<br/>Related", "Bottler<br/>Related:" + sortOnFirstOrSecond);
			sortInfo.put("Distributor<br/>Related", "Distributor<br/>Related:" + sortOnFirstOrSecond);
		}

		sortInfo.put("Average Loss", "Average Loss:" + sortOnFirstOrSecond);
		
		resetExcelFormats(1);
		ReportExcelFormat excelFormat = new ReportExcelFormat();
		setExcelFormat(0, excelFormat);
		
		resetMergedRegions(1);

		if (averages != null && averages.size() > 0)
		{
			// This large if statement is for handling a rare type of report unfortunately,
			// namely the special case of Distributor.com/traditional comparisons across stores.
			if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
			{
				/* the query for the averages will have gotten data in the following format for example:
						      Week STYPE        Average Lost Sales (Amount)
						---------- ------------ ---------------------------
						         1 1distributor.com                   125.88686
						         1 2traditional                  89.8480753
						         1 3total                        92.6523794
						         2 1distributor.com                   125.88686
						         2 2traditional                  89.8480753
						         2 3total                        92.6523794
						         3 2traditional                  .017412831
						         3 3total                        .016057878
						         4 2traditional                  .009497908
						         4 3total                        .008758842
					And what is required is to change it to:
						Week - AvgTradLoss - AvgDstbLoss - AvgLossTotal
					Note that the example only shows for 'amount'. If quantity is present also, then
					the final result would be:
						Week - AvgTradLossQ - AvgDstbLossQ - AvgLossTotalQ - AvgTradLossA - AvgDstbLossA - AvgLossTotalA
					where A stands for amount and Q stands for quantity.
				 */
				ListOrderedMap oneSetOfAveragesOnOneLine = null;
				ListOrderedMap newAverages = new ListOrderedMap();
				String currentWeek = "current", previousWeek = "previous", stype = null;
				Object value = null;
				int newAveragesCounter = 0;
				for (int i=0; i<averages.size(); i++)
				{
					ListOrderedMap row = (ListOrderedMap) averages.getValue(i);

					currentWeek = row.get("Week").toString();
					if (!currentWeek.equals(previousWeek))
					{
						if (oneSetOfAveragesOnOneLine != null)
							newAverages.put(new Integer(newAveragesCounter++), oneSetOfAveragesOnOneLine);
						oneSetOfAveragesOnOneLine = new ListOrderedMap();
						oneSetOfAveragesOnOneLine.put("Week", row.get("Week"));
						if (showQuantity)
						{
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [T] (Units)", new BigDecimal(0));
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [S] (Units)", new BigDecimal(0));
							oneSetOfAveragesOnOneLine.put("Average Lost Sales (Units)", new BigDecimal(0));
						}
						if (showAmount)
						{
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [T] (Dollars)", new BigDecimal(0));
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [S] (Dollars)", new BigDecimal(0));
							oneSetOfAveragesOnOneLine.put("Average Lost Sales (Dollars)", new BigDecimal(0));
						}
						previousWeek = currentWeek;
					}
					
					stype = row.get("STYPE").toString();
					if (stype.equalsIgnoreCase("1distributor.com"))
					{
						if (showQuantity)
						{
							value = row.get("Average Lost Sales (Units)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [S] (Units)", value);
						}
						if (showAmount)
						{
							value = row.get("Average Lost Sales (Dollars)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [S] (Dollars)", value);
						}
					}
					else if (stype.equalsIgnoreCase("2traditional"))
					{
						if (showQuantity)
						{
							value = row.get("Average Lost Sales (Units)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [T] (Units)", value);
						}
						if (showAmount)
						{
							value = row.get("Average Lost Sales (Dollars)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales [T] (Dollars)", value);
						}
					}
					else if (stype.equalsIgnoreCase("3total"))
					{
						if (showQuantity)
						{
							value = row.get("Average Lost Sales (Units)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales (Units)", value);
						}
						if (showAmount)
						{
							value = row.get("Average Lost Sales (Dollars)");
							oneSetOfAveragesOnOneLine.put("Average Lost Sales (Dollars)", value);
						}
					}
					
					// have we reached the very last row of data?
					if (i == averages.size() - 1)
					{
						if (oneSetOfAveragesOnOneLine != null)
							newAverages.put(new Integer(newAveragesCounter++), oneSetOfAveragesOnOneLine);
					}
				}
				averages = newAverages;
			}
			// And now back to your regular programming

			for (int i=0; i<averages.size(); i++)
			{
				ListOrderedMap row = (ListOrderedMap) averages.getValue(i);
	
				if (hasAveragesByReason)
				{
					String joinColumnValue = row.get(joinColumn).toString();
					
					// stuff additional rows for continuity when necessary
					if (firstColumnContinuous && Integer.parseInt(joinColumnValue) > firstColumnContinuousCounter)
					{
						int insertions = insertEmptyDataRows(averages, i, row, Integer.parseInt(joinColumnValue), 
							joinColumn, firstColumnContinuousCounter, showQuantity, showAmount);
						firstColumnContinuousCounter += insertions;
						i += insertions;
					}
	
					ArrayList labels1 = new ArrayList();
	
					if (showQuantity)
						labels1.add("LOST Units");
					if (showAmount)
						labels1.add("LOST $ Sales");
					
					ArrayList bottlerSubAverages = new ArrayList(), distributorSubAverages = new ArrayList(),
						sumSubAverages = new ArrayList(),
						traditionalSubAverages = new ArrayList(), distributorComSubAverages = new ArrayList(); 
					
					ListOrderedMap value = null;
					
					value = ((ListOrderedMap) averagesByReason.get(joinColumnValue + "|NE/ND"));
					if (showQuantity)
						bottlerSubAverages.add(value != null ? value.get("Loss By Reason (Units)") : new BigDecimal(0));
					if (showAmount)
						bottlerSubAverages.add(value != null ? value.get("Loss By Reason (Dollars)") : new BigDecimal(0));
		
					value = ((ListOrderedMap) averagesByReason.get(joinColumnValue + "|SA/SI"));
					if (showQuantity)
						distributorSubAverages.add(value != null ? value.get("Loss By Reason (Units)") : new BigDecimal(0));
					if (showAmount)
						distributorSubAverages.add(value != null ? value.get("Loss By Reason (Dollars)") : new BigDecimal(0));
	
					if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
					{
						if (showQuantity)
						{
							traditionalSubAverages.add(row.get("Average Lost Sales [T] (Units)"));
							distributorComSubAverages.add(row.get("Average Lost Sales [S] (Units)"));
						}
						if (showAmount)
						{
							traditionalSubAverages.add(row.get("Average Lost Sales [T] (Dollars)"));
							distributorComSubAverages.add(row.get("Average Lost Sales [S] (Dollars)"));
						}
					}

					if (showQuantity)
						sumSubAverages.add(row.get("Average Lost Sales (Units)"));
					if (showAmount)
						sumSubAverages.add(row.get("Average Lost Sales (Dollars)"));
					
					row.remove("Average Lost Sales (Dollars)");
					row.remove("Average Lost Sales (Units)");
					if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
					{
						row.remove("Average Lost Sales [S] (Dollars)");
						row.remove("Average Lost Sales [S] (Units)");
						row.remove("Average Lost Sales [T] (Dollars)");
						row.remove("Average Lost Sales [T] (Units)");
					}
	
					row.put("&nbsp;", labels1);
					
					row.put("Bottler<br/>Related", bottlerSubAverages);
					row.put("Distributor<br/>Related", distributorSubAverages);
					if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
					{
						row.put("Average Loss<br/>(Traditional)", traditionalSubAverages);
						row.put("Average Loss<br/>(Distributor.com)", distributorComSubAverages);
					}			
					row.put("Average Loss", sumSubAverages);
					
					if (firstColumnContinuous)
						firstColumnContinuousCounter++;
				}
			}
			
			if (averages.size() < numberOfWeeksInReport)
			{
				insertEmptyDataRows(averages, averages.size(), (ListOrderedMap) averages.getValue(averages.size() - 1), 
					numberOfWeeksInReport, joinColumn, firstColumnContinuousCounter, showQuantity, showAmount);
			}
			
			ListOrderedMap firstRow = (ListOrderedMap) averages.getValue(0);
			int numberOfColumns = firstRow.size();
			int numberOfRows = averages.size();
			ReportExcelCellStyle headerCellStyle = 
				new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_CENTER, HSSFCellStyle.VERTICAL_BOTTOM, (short) 0,
					new ReportExcelFont("Arial", (short) 10, HSSFColor.YELLOW.index, HSSFFont.BOLDWEIGHT_BOLD), 
					HSSFCellStyle.SOLID_FOREGROUND, HSSFColor.BLACK.index, HSSFColor.AUTOMATIC.index); 
			excelFormat.addCellStyle(0, 0, numberOfColumns, 0, headerCellStyle);
			
			ReportExcelCellStyle bottlerCellStyle = 
				new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_RIGHT, HSSFCellStyle.VERTICAL_BOTTOM, (short) 1,
					new ReportExcelFont("Arial", (short) 10, HSSFColor.WHITE.index, HSSFFont.BOLDWEIGHT_BOLD), 
					HSSFCellStyle.SOLID_FOREGROUND, HSSFColor.RED.index, HSSFColor.AUTOMATIC.index); 
			ReportExcelCellStyle distributorCellStyle = 
				new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_RIGHT, HSSFCellStyle.VERTICAL_BOTTOM, (short) 1,
					new ReportExcelFont("Arial", (short) 10, HSSFColor.BLACK.index, HSSFFont.BOLDWEIGHT_BOLD), 
					HSSFCellStyle.SOLID_FOREGROUND, HSSFColor.GREY_40_PERCENT.index, HSSFColor.AUTOMATIC.index); 
			ReportExcelCellStyle grandTotalCellStyle = 
				new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_RIGHT, HSSFCellStyle.VERTICAL_BOTTOM, (short) 1,
					new ReportExcelFont("Arial", (short) 10, HSSFColor.BLACK.index, HSSFFont.BOLDWEIGHT_BOLD), 
					HSSFCellStyle.NO_FILL, HSSFColor.AUTOMATIC.index, HSSFColor.AUTOMATIC.index); 

			for (int k=0; k<numberOfColumns; k++)
			{
				String columnName = (String) firstRow.get(k);
				
				if (columnName.equals("Bottler<br/>Related"))
					excelFormat.addCellStyle(k, 1, k, numberOfRows, bottlerCellStyle);
				if (columnName.endsWith("Distributor<br/>Related"))
					excelFormat.addCellStyle(k, 1, k, numberOfRows, distributorCellStyle);
				if (columnName.indexOf("Average Loss") >= 0)
					excelFormat.addCellStyle(k, 1, k, numberOfRows, grandTotalCellStyle);
			}
		}
		else
			return;
		
		ListOrderedMap mergedResults = new ListOrderedMap();
		mergedResults.put("query0", averages);
		setResults(mergedResults);
		
		return;
	}
	
	/**
	 * There are times when the averages report will encounter weeks where there is no data. The SQL queries
	 * will report nothing for those weeks, and hence we artificially fill them in with zeroes.
	 * 
	 * @param rows the original ordered set of rows returned from the SQL query
	 * @param currentPosition the current position in the set of rows 
	 * @param currentRow the row at that position
	 * @param nextAvailableRealDataPosition the next available position of real data
	 * @param firstColumnName the first column name
	 * @param firstColumnCounter the counter for that column - this is what needs to rise continuously
	 * @param showQuantity whether to show quantity or not
	 * @param showAmount whether to show amount or not
	 * @return
	 */
	private int insertEmptyDataRows(ListOrderedMap rows, 
		int currentPosition, ListOrderedMap currentRow, int nextAvailableRealDataPosition,
		String firstColumnName, int firstColumnCounter,
		boolean showQuantity, boolean showAmount)
	{
		int i = 0;
		while (firstColumnCounter < nextAvailableRealDataPosition)
		{
			ListOrderedMap emptyDataRow = new ListOrderedMap();
			emptyDataRow.putAll(currentRow);
			emptyDataRow.put(firstColumnName, new BigDecimal(firstColumnCounter));
			
			ArrayList labels1 = new ArrayList();
			
			if (showQuantity)
				labels1.add("LOST Units");
			if (showAmount)
				labels1.add("LOST $ Sales");
			
			ArrayList bottlerSubAverages = new ArrayList(), distributorSubAverages = new ArrayList(),
				sumSubAverages = new ArrayList(), 
				traditionalSubAverages = new ArrayList(), distributorComSubAverages = new ArrayList(); 
			
			if (showQuantity)
				bottlerSubAverages.add(new BigDecimal(0));
			if (showAmount)
				bottlerSubAverages.add(new BigDecimal(0));

			if (showQuantity)
				distributorSubAverages.add(new BigDecimal(0));
			if (showAmount)
				distributorSubAverages.add(new BigDecimal(0));

			if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
			{
				if (showQuantity)
				{
					traditionalSubAverages.add(new BigDecimal(0));
					distributorComSubAverages.add(new BigDecimal(0));
				}
				if (showAmount)
				{
					traditionalSubAverages.add(new BigDecimal(0));
					distributorComSubAverages.add(new BigDecimal(0));
				}
			}

			if (showQuantity)
				sumSubAverages.add(new BigDecimal(0));
			if (showAmount)
				sumSubAverages.add(new BigDecimal(0));

			emptyDataRow.remove("Average Lost Sales (Dollars)");
			emptyDataRow.remove("Average Lost Sales (Units)");
			if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
			{
				emptyDataRow.remove("Average Lost Sales [S] (Dollars)");
				emptyDataRow.remove("Average Lost Sales [S] (Units)");
				emptyDataRow.remove("Average Lost Sales [T] (Dollars)");
				emptyDataRow.remove("Average Lost Sales [T] (Units)");
			}

			emptyDataRow.put("&nbsp;", labels1);
			
			emptyDataRow.put("Bottler<br/>Related", bottlerSubAverages);
			emptyDataRow.put("Distributor<br/>Related", distributorSubAverages);
			
			if (reportSummaryTarget.equalsIgnoreCase("storetvss"))
			{
				emptyDataRow.put("Average Loss<br/>(Traditional)", traditionalSubAverages);
				emptyDataRow.put("Average Loss<br/>(Distributor.com)", distributorComSubAverages);
			}			
			emptyDataRow.put("Average Loss", sumSubAverages);
										
			rows.put(currentPosition, nextAvailableRealDataPosition + "_" + (currentPosition+1), emptyDataRow);
			
			firstColumnCounter++;
			currentPosition++;
			i++;
		}
		return i;
	}

	protected String[] getInitialSortedColumns(int queryNumber)
	{
		if (queryNumber == 0)
		{
			String isc[] = { "Week" };
			return isc;
		}
		return null;
	}

	protected boolean isSortedAscendingInitially(int queryNumber)
	{
		if (queryNumber == 0)
			return true;
		return false;
	}
	
	public String[] getUnsortableColumns(int queryNumber)
	{
		if (queryNumber == 0)
		{
			String uc[] = { "&nbsp;" };
			return uc;
		}
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

