package net.btlcpy.outofstock.reports;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
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
 * This class produces reports of total sales losses due to out of stock events across different
 * entities such as stores, bottlers, products, and days. Up to three queries may be run for a
 * particular run of this report: one calculates the total loss per entity, another calculates
 * the loss split by reason (bottler versus Distributor), and another calculates the loss per day
 * (this last query is only run when the total time period of the report is one week, otherwise
 * the performance would be prohibitive).
 * 
 * @author Ahmed A. Abd-Allah
 */
public class TotalsReport extends AveragesTotalsBaseReport
{
	/** There are up to three queries: total losses, total losses split by reason,
	 * and total losses per day.
	 */
	private SelectBuilder sbTotals, sbTotalsByReason, sbTotalsByDailyReason;

	/** The key columns to use for the three queries */
	private String[][] keyColumns = { null, null, null };
	
	private Vector /*<String>*/ unsortableColumns = null;
	
	/** Indicates whether to break out reports by day (either "yes" or "no"). */
	private String reportBreakOutByDay = null;
	
	public TotalsReport(Connection connection, String username, Date beginDate, Date endDate, 
		String summaryTarget, String resultSize, String breakOutByDay, String lostSalesMetric,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID, 
		String[] bottlerPKID, String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID, String[] bottlerBranchPKID, String[] bottlerSalesRoutePKID, 
		String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID)
	{
		super(connection, username);
		reportSummaryTarget = summaryTarget;
		reportBreakOutByDay = breakOutByDay;
		if (reportBreakOutByDay == null)
			reportBreakOutByDay = "no";
		reportBeginDate = beginDate;
		
		reportLostSalesMetric = lostSalesMetric;
		
		userActionDescription = constructUserActionDescription(
			beginDate, endDate, summaryTarget, resultSize, breakOutByDay, lostSalesMetric);
		
		excelReportDescription = constructExcelReportDescription(
			beginDate, endDate, summaryTarget, resultSize, breakOutByDay, lostSalesMetric,
			StringUtils.id(productCategoryPKID), StringUtils.id(productPKID), StringUtils.id(productPackagePKID),
			StringUtils.id(bottlerPKID), StringUtils.id(bottlerBusinessUnitPKID), 
			StringUtils.id(bottlerMarketUnitPKID), StringUtils.id(bottlerBranchPKID), 
			StringUtils.id(bottlerSalesRoutePKID),
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
		
		constructTotals(beginDate, endDate, summaryTarget, resultSize, lostSalesMetric,
			productCategoryPK, productPK, productPackagePK, 
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK, 
			distributorDivisionPK, distributorDistrictPK, storePK);
		constructTotalsByReason(beginDate, endDate, summaryTarget, lostSalesMetric,
			productCategoryPK, productPK, productPackagePK, 
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK, 
			distributorDivisionPK, distributorDistrictPK, storePK);
		if (reportBreakOutByDay.equalsIgnoreCase("yes") && !summaryTarget.equalsIgnoreCase("day") && 
			!summaryTarget.equalsIgnoreCase("week") && !summaryTarget.equalsIgnoreCase("storewithproduct"))
			constructTotalsByDailyReason(beginDate, endDate, summaryTarget, lostSalesMetric,
				productCategoryPK, productPK, productPackagePK, 
				bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK, 
				distributorDivisionPK, distributorDistrictPK, storePK);
	}
	
	/**
	 * Constructs a string suitable for logging as a user action record.
	 * 
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param resultSize how many results desired
	 * @param breakOutByDay break out by day or not
	 * @param lostSalesMetric metric to use for measuring lost sales
	 * @return a single String that summarizes the report as a user action
	 */
	private String constructUserActionDescription(Date beginDate, Date endDate, 
		String summaryTarget, String resultSize, String breakOutByDay, String lostSalesMetric)
	{
		StringBuffer d = new StringBuffer("Total lost sales report");
		
		if (lostSalesMetric.equals("quantity"))
			d.append(" (units only)");
		else if (lostSalesMetric.equals("amount"))
			d.append(" (dollars only)");
		else if (lostSalesMetric.equals("all"))
			d.append(" (dollars and units)");
		
		d.append(" across");
		
		if (resultSize == null || resultSize.equals("all") || resultSize.equals("tospecifiedweeks") || 
			summaryTarget.equals("day") || summaryTarget.equals("week"))
			d.append(" all");
		else if (resultSize.equals("top25"))
			d.append(" the top 25");
		else if (resultSize.equals("top50"))
			d.append(" the top 50");
		
		if (summaryTarget.equalsIgnoreCase("store"))
			d.append(" stores");
		else if (summaryTarget.equalsIgnoreCase("bottler"))
			d.append(" bottlers");
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
			d.append(" bottler branches");
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
			d.append(" bottler sales routes");
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
			d.append(" Distributor districts");
		else if (summaryTarget.equalsIgnoreCase("day"))
			d.append(" days of the week");
		else if (summaryTarget.equalsIgnoreCase("week"))
			d.append(" weeks");
		else if (summaryTarget.equalsIgnoreCase("product"))
			d.append(" products");
		else if (summaryTarget.equalsIgnoreCase("package"))
			d.append(" packages");
		else if (summaryTarget.equalsIgnoreCase("storewithproduct"))
			d.append(" stores (with products)");
		
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

		if (breakOutByDay != null && breakOutByDay.equalsIgnoreCase("yes") && 
			!summaryTarget.equalsIgnoreCase("day"))
			d.append(", broken out by day");
		
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
	 * @param resultSize how many results desired
	 * @param breakOutByDay break out by day or not
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
		String summaryTarget, String resultSize, String breakOutByDay, String lostSalesMetric,
		String[] productCategoryID, String[] productID, String[] productPackageID, 
		String[] bottlerID, String[] bottlerBusinessUnitID, String[] bottlerMarketUnitID, String[] bottlerBranchID, String[] bottlerSalesRouteID, 
		String[] distributorDivisionID, String[] distributorDistrictID, String[] storeID)
	{
		StringBuffer d = new StringBuffer("Total lost sales report");
		
		if (lostSalesMetric.equals("quantity"))
			d.append(" (units only)");
		else if (lostSalesMetric.equals("amount"))
			d.append(" (dollars only)");
		else if (lostSalesMetric.equals("all"))
			d.append(" (dollars and units)");
		
		d.append(" across");
		
		if (resultSize == null || resultSize.equals("all") || resultSize.equals("tospecifiedweeks") ||
			summaryTarget.equals("day") || summaryTarget.equals("week"))
			d.append(" all");
		else if (resultSize.equals("top25"))
			d.append(" the top 25");
		else if (resultSize.equals("top50"))
			d.append(" the top 50");
		
		if (summaryTarget.equalsIgnoreCase("store"))
			d.append(" stores");
		else if (summaryTarget.equalsIgnoreCase("bottler"))
			d.append(" bottlers");
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
			d.append(" bottler branches");
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
			d.append(" bottler sales routes");
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
			d.append(" Distributor districts");
		else if (summaryTarget.equalsIgnoreCase("day"))
			d.append(" days of the week");
		else if (summaryTarget.equalsIgnoreCase("week"))
			d.append(" weeks");
		else if (summaryTarget.equalsIgnoreCase("product"))
			d.append(" products");
		else if (summaryTarget.equalsIgnoreCase("package"))
			d.append(" packages");
		else if (summaryTarget.equalsIgnoreCase("storewithproduct"))
			d.append(" stores (with products)");
		
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

		if (breakOutByDay != null && breakOutByDay.equalsIgnoreCase("yes") && 
			!summaryTarget.equalsIgnoreCase("day"))
			d.append(", broken out by day");
		
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
		return "Totals Report";
	}

	public String getQueryTitle(int queryNumber)
	{
		if (queryNumber == 0)
			return "Report Data:";
		return "";
	}

	/**
	 * Totals reports may be constrained across a number of different parameters. This
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
	 * Constructs the SQL query for calculating the total losses for the desired
	 * summary target (stores, bottlers, etc.).
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param resultSize how many results desired
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
	private void constructTotals(Date beginDate, Date endDate, 
		String summaryTarget, String resultSize, String lostSalesMetric,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		sbTotals = new SelectBuilder();
		
		if (!summaryTarget.equalsIgnoreCase("day") && !summaryTarget.equalsIgnoreCase("week"))
		{
			if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
				sbTotals.addSelect("rank() over (ORDER BY sum(outofstockevents.lostsalesamount) desc) \"Rank\"");
			else if (lostSalesMetric.equals("quantity"))
				sbTotals.addSelect("rank() over (ORDER BY sum(outofstockevents.lostsalesquantity) desc) \"Rank\"");
		}
		
		if (summaryTarget.equalsIgnoreCase("store") || summaryTarget.equalsIgnoreCase("storewithproduct"))
		{
			sbTotals.addSelect("stores.id AS \"Store ID\", dstbdstrcts.name AS \"Distributor District\", " +
				"bttlrbrchs.name AS \"Bottler Branch\"");
			sbTotals.addFrom("stores, dstbdstrcts, dstbdstrctstostores, " +
				"bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotals.addWhere(
				"stores.primarykey = dstbdstrctstostores.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0 AND " + 
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey AND " +
				"stores.primarykey = bttlrbrchstostores.store AND " +
				"bttlrbrchs.primarykey = bttlrbrchstostores.bottlerbranch AND " +
				"stores.primarykey = outofstockevents.store", true);
			sbTotals.addGroup("stores.id, dstbdstrcts.name, bttlrbrchs.name");
		}
		else if (summaryTarget.equalsIgnoreCase("bottler"))
		{
			sbTotals.addSelect("bttlrs.name AS \"Bottler\"");
			sbTotals.addFrom("bttlrs, bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts, " +
				"bttlrmktuntstobttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotals.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey", true);
			sbTotals.addGroup("bttlrs.name");
		}
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
		{
			sbTotals.addSelect("bttlrbrchs.name AS \"Bottler Branch\"");
			sbTotals.addFrom("bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotals.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey", true);
			sbTotals.addGroup("bttlrbrchs.name");
		}
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
		{
			sbTotals.addSelect("bttlrslsrts.name AS \"Bottler Sales Route\"");
			sbTotals.addFrom("bttlrslsrts, bttlrslsrtstostores, outofstockevents");
			sbTotals.addWhere(
				"outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey", true);
			sbTotals.addGroup("bttlrslsrts.name");
		}
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
		{
			sbTotals.addSelect("dstbdstrcts.name AS \"Distributor District\"");
			sbTotals.addFrom("dstbdstrcts, dstbdstrctstostores, outofstockevents");
			sbTotals.addWhere(
				"outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey", true);
			sbTotals.addGroup("dstbdstrcts.name");
		}
		else if (summaryTarget.equalsIgnoreCase("day"))
		{
			sbTotals.addSelect("dateoccurred AS \"Day\"");
			sbTotals.addFrom("outofstockevents");
			sbTotals.addGroup("dateoccurred");
			sbTotals.addOrder("\"Day\"");
		}
		else if (summaryTarget.equalsIgnoreCase("week"))
		{
			sbTotals.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
			sbTotals.addFrom("outofstockevents");
			sbTotals.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");
			sbTotals.addOrder("\"Week\"");
		}
		else if (summaryTarget.equalsIgnoreCase("product"))
		{
			sbTotals.addSelect("prdcts.upcid as \"Product UPC\", description as \"Description\"");
			sbTotals.addFrom("prdcts, outofstockevents");
			sbTotals.addWhere(
				"prdcts.primarykey = outofstockevents.product", true);
			sbTotals.addGroup("prdcts.upcid, description");
		}
		else if (summaryTarget.equalsIgnoreCase("package"))
		{
			sbTotals.addSelect("prdctpkgs.name as \"Package\"");
			sbTotals.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs, outofstockevents");
			sbTotals.addWhere(
				"prdcts.primarykey = outofstockevents.product AND " +
				"prdcts.primarykey = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbTotals.addGroup("prdctpkgs.name");
		}

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
			sbTotals.addSelect(
				"sum(outofstockevents.lostsalesquantity) AS \"Total Lost Sales (Units)\"");
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
			sbTotals.addSelect(
				"sum(outofstockevents.lostsalesamount) AS \"Total Lost Sales (Dollars)\"");
			
		if (beginDate != null)
			sbTotals.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
		if (endDate != null)
			sbTotals.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
		
		applyConstraints(sbTotals, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);
		
		if (!summaryTarget.equals("day") && !summaryTarget.equals("week"))
		{
			if (resultSize.equals("top25"))
				sbTotals = new SelectBuilder("*", sbTotals, "\"Rank\" <= 25", null, null, "\"Rank\"");
			else if (resultSize.equals("top50"))
				sbTotals = new SelectBuilder("*", sbTotals, "\"Rank\" <= 50", null, null, "\"Rank\"");
			else if (resultSize.equals("all"))
				sbTotals = new SelectBuilder("*", sbTotals, null, null, null, "\"Rank\"");
		}
	}
	
	/**
	 * Constructs the SQL query for calculating the total losses for the desired
	 * summary target (stores, bottlers, etc.) split by reason (bottler versus
	 * Distributor).
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param resultSize how many results desired
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
	private void constructTotalsByReason(Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		sbTotalsByReason = new SelectBuilder();

		if (summaryTarget.equalsIgnoreCase("store"))
		{
			sbTotalsByReason.addSelect("stores.id AS \"Store ID\"");
			sbTotalsByReason.addFrom("outofstockevents, stores");
			sbTotalsByReason.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbTotalsByReason.addGroup("stores.id");
			sbTotalsByReason.addOrder("\"Store ID\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Store ID";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("storewithproduct"))
		{
			sbTotalsByReason.addSelect("stores.id AS \"Store ID\"");
			sbTotalsByReason.addSelect("prdcts.upcid AS \"Product UPC\"");
			sbTotalsByReason.addSelect("prdcts.description AS \"Product Description\"");
			sbTotalsByReason.addSelect("prdctpkgs.name AS \"Product Package\"");
			sbTotalsByReason.addFrom("outofstockevents, stores");
			sbTotalsByReason.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs");
			sbTotalsByReason.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbTotalsByReason.addWhere("prdcts.primarykey = outofstockevents.product " +
				"AND prdctstoprdctpkgs.product = prdcts.primarykey " +
				"AND prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbTotalsByReason.addGroup("stores.id, prdcts.upcid, prdcts.description, prdctpkgs.name");
			sbTotalsByReason.addOrder("\"Store ID\", \"Product UPC\", \"Reason\"");
			keyColumns[1] = new String[3];
			keyColumns[1][0] = "Store ID";
			keyColumns[1][1] = "Product UPC";
			keyColumns[1][2] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("bottler"))
		{
			sbTotalsByReason.addSelect("bttlrs.name AS \"Bottler\"");
			sbTotalsByReason.addFrom("bttlrs, bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts, " +
				"bttlrmktuntstobttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotalsByReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey", true);
			sbTotalsByReason.addGroup("bttlrs.name");
			sbTotalsByReason.addOrder("\"Bottler\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Bottler";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
		{
			sbTotalsByReason.addSelect("bttlrbrchs.name AS \"Bottler Branch\"");
			sbTotalsByReason.addFrom("bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotalsByReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey", true);
			sbTotalsByReason.addGroup("bttlrbrchs.name");
			sbTotalsByReason.addOrder("\"Bottler Branch\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Bottler Branch";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
		{
			sbTotalsByReason.addSelect("bttlrslsrts.name AS \"Bottler Sales Route\"");
			sbTotalsByReason.addFrom("bttlrslsrts, bttlrslsrtstostores, outofstockevents");
			sbTotalsByReason.addWhere(
				"outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey", true);
			sbTotalsByReason.addGroup("bttlrslsrts.name");
			sbTotalsByReason.addOrder("\"Bottler Sales Route\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Bottler Sales Route";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
		{
			sbTotalsByReason.addSelect("dstbdstrcts.name AS \"Distributor District\"");
			sbTotalsByReason.addFrom("dstbdstrcts, dstbdstrctstostores, outofstockevents");
			sbTotalsByReason.addWhere(
				"outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey", true);
			sbTotalsByReason.addGroup("dstbdstrcts.name");
			sbTotalsByReason.addOrder("\"Distributor District\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Distributor District";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("day"))
		{
			sbTotalsByReason.addSelect("dateoccurred AS \"Day\"");
			sbTotalsByReason.addFrom("outofstockevents");
			sbTotalsByReason.addGroup("dateoccurred");
			sbTotalsByReason.addOrder("\"Day\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Day";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("week"))
		{
			sbTotalsByReason.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
			sbTotalsByReason.addFrom("outofstockevents");
			sbTotalsByReason.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");
			sbTotalsByReason.addOrder("\"Week\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Week";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("product"))
		{
			sbTotalsByReason.addSelect("prdcts.upcid as \"Product UPC\"");
			sbTotalsByReason.addFrom("prdcts, outofstockevents");
			sbTotalsByReason.addWhere(
				"prdcts.primarykey = outofstockevents.product", true);
			sbTotalsByReason.addGroup("prdcts.upcid");
			sbTotalsByReason.addOrder("\"Product UPC\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Product UPC";
			keyColumns[1][1] = "Reason";
		}
		else if (summaryTarget.equalsIgnoreCase("package"))
		{
			sbTotalsByReason.addSelect("prdctpkgs.name as \"Package\"");
			sbTotalsByReason.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs, outofstockevents");
			sbTotalsByReason.addWhere(
				"prdcts.primarykey = outofstockevents.product AND " +
				"prdcts.primarykey = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbTotalsByReason.addGroup("prdctpkgs.name");
			sbTotalsByReason.addOrder("\"Package\"");
			keyColumns[1] = new String[2];
			keyColumns[1][0] = "Package";
			keyColumns[1][1] = "Reason";
		}
		
		sbTotalsByReason.addSelect(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END as \"Reason\"");

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
			sbTotalsByReason.addSelect(
				"sum(outofstockevents.lostsalesquantity) AS \"Loss By Reason (Units)\"");
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
			sbTotalsByReason.addSelect(
				"sum(outofstockevents.lostsalesamount) AS \"Loss By Reason (Dollars)\"");

		if (beginDate != null)
			sbTotalsByReason.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
		if (endDate != null)
			sbTotalsByReason.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
		sbTotalsByReason.addGroup(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END");

		applyConstraints(sbTotalsByReason, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);
	}

	/**
	 * Constructs the SQL query for calculating the total losses for the desired
	 * summary target (stores, bottlers, etc.) split up by day and reason.
	 *  
	 * @param beginDate start date of the report
	 * @param endDate end date of the report
	 * @param summaryTarget the report's target (store, bottler, etc.)
	 * @param resultSize how many results desired
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
	private void constructTotalsByDailyReason(Date beginDate, Date endDate, 
		String summaryTarget, String lostSalesMetric,
		String[] productCategoryPK, String[] productPK, String[] productPackagePK, 
		String[] bottlerPK, String[] bottlerBusinessUnitPK, String[] bottlerMarketUnitPK, String[] bottlerBranchPK, String[] bottlerSalesRoutePK, 
		String[] distributorDivisionPK, String[] distributorDistrictPK, String[] storePK)
	{
		sbTotalsByDailyReason = new SelectBuilder();

		if (summaryTarget.equalsIgnoreCase("store"))
		{
			sbTotalsByDailyReason.addSelect("stores.id AS \"Store ID\"");
			sbTotalsByDailyReason.addFrom("outofstockevents, stores");
			sbTotalsByDailyReason.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbTotalsByDailyReason.addGroup("stores.id");
			sbTotalsByDailyReason.addOrder("\"Store ID\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Store ID";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("storewithproduct"))
		{
			sbTotalsByDailyReason.addSelect("stores.id AS \"Store ID\"");
			sbTotalsByDailyReason.addSelect("prdcts.upcid AS \"Product UPC\"");
			sbTotalsByDailyReason.addSelect("prdcts.description AS \"Product Description\"");
			sbTotalsByDailyReason.addSelect("prdctpkgs.name AS \"Product Package\"");
			sbTotalsByDailyReason.addFrom("outofstockevents, stores");
			sbTotalsByDailyReason.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs");
			sbTotalsByDailyReason.addWhere("stores.primarykey = outofstockevents.store AND " +
				"bitand(stores.metaflags, 1)+0 = 0", true);
			sbTotalsByDailyReason.addWhere("prdcts.primarykey = outofstockevents.product " +
				"AND prdctstoprdctpkgs.product = prdcts.primarykey " +
				"AND prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbTotalsByDailyReason.addGroup("stores.id, prdcts.upcid, prdcts.description, prdctpkgs.name");
			sbTotalsByDailyReason.addOrder("\"Store ID\", \"Product UPC\", \"Reason\"");
			keyColumns[2] = new String[4];
			keyColumns[2][0] = "Store ID";
			keyColumns[2][1] = "Product UPC";
			keyColumns[2][2] = "Reason";
			keyColumns[2][3] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("bottler"))
		{
			sbTotalsByDailyReason.addSelect("bttlrs.name AS \"Bottler\"");
			sbTotalsByDailyReason.addFrom("bttlrs, bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts, " +
				"bttlrmktuntstobttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch AND " +
				"bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit AND " +
				"bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey", true);
			sbTotalsByDailyReason.addGroup("bttlrs.name");
			sbTotalsByDailyReason.addOrder("\"Bottler\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Bottler";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("bottlerbranch"))
		{
			sbTotalsByDailyReason.addSelect("bttlrbrchs.name AS \"Bottler Branch\"");
			sbTotalsByDailyReason.addFrom("bttlrbrchs, bttlrbrchstostores, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"outofstockevents.store = bttlrbrchstostores.store AND " +
				"bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey", true);
			sbTotalsByDailyReason.addGroup("bttlrbrchs.name");
			sbTotalsByDailyReason.addOrder("\"Bottler Branch\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Bottler Branch";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("bottlersalesroute"))
		{
			sbTotalsByDailyReason.addSelect("bttlrslsrts.name AS \"Bottler Sales Route\"");
			sbTotalsByDailyReason.addFrom("bttlrslsrts, bttlrslsrtstostores, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"outofstockevents.store = bttlrslsrtstostores.store AND " +
				"bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey", true);
			sbTotalsByDailyReason.addGroup("bttlrslsrts.name");
			sbTotalsByDailyReason.addOrder("\"Bottler Sales Route\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Bottler Sales Route";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("distributordistrict"))
		{
			sbTotalsByDailyReason.addSelect("dstbdstrcts.name AS \"Distributor District\"");
			sbTotalsByDailyReason.addFrom("dstbdstrcts, dstbdstrctstostores, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"outofstockevents.store = dstbdstrctstostores.store AND " +
				"dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey", true);
			sbTotalsByDailyReason.addGroup("dstbdstrcts.name");
			sbTotalsByDailyReason.addOrder("\"Distributor District\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Distributor District";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("day")) // Should not be used; illogical
		{
			sbTotalsByDailyReason.addSelect("dateoccurred AS \"Day\"");
			sbTotalsByDailyReason.addFrom("outofstockevents");
			sbTotalsByDailyReason.addGroup("dateoccurred");
			sbTotalsByDailyReason.addOrder("\"Day\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Day";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("week")) // Should not be used; illogical
		{
			sbTotalsByDailyReason.addSelect("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1 AS \"Week\"");
			sbTotalsByDailyReason.addFrom("outofstockevents");
			sbTotalsByDailyReason.addGroup("floor((dateoccurred - DATE '" + beginDate.toString() + "')/7)+1");
			sbTotalsByDailyReason.addOrder("\"Week\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Week";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("product"))
		{
			sbTotalsByDailyReason.addSelect("prdcts.upcid as \"Product UPC\"");
			sbTotalsByDailyReason.addFrom("prdcts, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"prdcts.primarykey = outofstockevents.product", true);
			sbTotalsByDailyReason.addGroup("prdcts.upcid");
			sbTotalsByDailyReason.addOrder("\"Product UPC\"");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Product UPC";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}
		else if (summaryTarget.equalsIgnoreCase("package"))
		{
			sbTotalsByDailyReason.addSelect("prdctpkgs.name as \"Package\"");
			sbTotalsByDailyReason.addFrom("prdcts, prdctpkgs, prdctstoprdctpkgs, outofstockevents");
			sbTotalsByDailyReason.addWhere(
				"prdcts.primarykey = outofstockevents.product AND " +
				"prdcts.primarykey = prdctstoprdctpkgs.product AND " +
				"prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey", true);
			sbTotalsByDailyReason.addGroup("prdctpkgs.name");
			sbTotalsByDailyReason.addOrder("prdctpkgs.name");
			keyColumns[2] = new String[3];
			keyColumns[2][0] = "Package";
			keyColumns[2][1] = "Reason";
			keyColumns[2][2] = "Day";
		}

		if (!summaryTarget.equals("day") && !summaryTarget.equals("week"))
		{
			sbTotalsByDailyReason.addSelect("dateoccurred AS \"Day\"");
			sbTotalsByDailyReason.addGroup("dateoccurred");
			sbTotalsByDailyReason.addOrder("\"Day\"");
		}

		sbTotalsByDailyReason.addSelect(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END as \"Reason\"");

		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("quantity"))
			sbTotalsByDailyReason.addSelect(
				"sum(outofstockevents.lostsalesquantity) AS \"Daily Loss (Units)\"");
		if (lostSalesMetric.equals("all") || lostSalesMetric.equals("amount"))
			sbTotalsByDailyReason.addSelect(
				"sum(outofstockevents.lostsalesamount) AS \"Daily Loss (Dollars)\"");
		
		if (beginDate != null)
			sbTotalsByDailyReason.addWhere("dateoccurred >= DATE '" + beginDate.toString() + "'", true);
		if (endDate != null)
			sbTotalsByDailyReason.addWhere("dateoccurred <= DATE '" + endDate.toString() + "'", true);
		sbTotalsByDailyReason.addGroup(
			"CASE WHEN reason IN ('NE', 'ND') THEN 'NE/ND' WHEN reason IN ('SA', 'SI') THEN 'SA/SI' ELSE reason END");

		applyConstraints(sbTotalsByDailyReason, productCategoryPK, productPK, productPackagePK,
			bottlerPK, bottlerBusinessUnitPK, bottlerMarketUnitPK, bottlerBranchPK, bottlerSalesRoutePK,
			distributorDivisionPK, distributorDistrictPK, storePK);
	}

	/**
	 * @return the number of queries. If a daily breakout is desired, and the summary target is neither
	 * "day" nor "week", then the number of queries will be three. Otherwise, only two queries will be
	 * run, since the daily breakout does not make sense in the context of "day"/"week" summary targets.
	 */
	public Integer numberOfQueries() 
	{
		if (reportBreakOutByDay.equalsIgnoreCase("yes") && 
			!reportSummaryTarget.equalsIgnoreCase("day") && 
			!reportSummaryTarget.equalsIgnoreCase("week"))
			return new Integer(3);
		return new Integer(2);
	}
	
	public String getQuery(int queryNumber) 
	{ 
		switch (queryNumber)
		{
		case 0:
			return sbTotals.toString();
		case 1:
			return sbTotalsByReason.toString();
		case 2:
			return sbTotalsByDailyReason.toString();
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
		// After query 0, use the list of returned results to restrict
		// the subsequent queries (by reason, and by daily reason).
		if (queryNumber == 0)
		{
			// No need to add a list of dates to search in since the original 
			// date range will do the job.
			if (reportSummaryTarget.equalsIgnoreCase("day"))
				return;
			
			ListOrderedMap rows = (ListOrderedMap) super.getResults().getValue(0);
			String searchColumn = null, alias = null, bq = "", eq = "";
			boolean doublequote = false;
			if (reportSummaryTarget.equalsIgnoreCase("store"))
			{
				searchColumn = "stores.id";
				alias = "Store ID";
			}
			else if (reportSummaryTarget.equalsIgnoreCase("storewithproduct"))
			{
				searchColumn = "stores.id";
				alias = "Store ID";
			}
			else if (reportSummaryTarget.equalsIgnoreCase("bottler"))
			{
				searchColumn = "bttlrs.name";
				alias = "Bottler";
				bq = BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
				doublequote = true;
			}
			else if (reportSummaryTarget.equalsIgnoreCase("bottlerbranch"))
			{
				searchColumn = "bttlrbrchs.name";
				alias = "Bottler Branch";
				bq = BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
				doublequote = true;
			}
			else if (reportSummaryTarget.equalsIgnoreCase("bottlersalesroute"))
			{
				searchColumn = "bttlrslsrts.name";
				alias = "Bottler Sales Route";
				bq = BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
				doublequote = true;
			}
			else if (reportSummaryTarget.equalsIgnoreCase("distributordistrict"))
			{
				searchColumn = "dstbdstrcts.name";
				alias = "Distributor District";
				bq = BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
				doublequote = true;
			}
			/*else if (reportSummaryTarget.equalsIgnoreCase("day"))
			{
				searchColumn = "dateoccurred";
				alias = "Day";
				bq = "DATE " + BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
			}*/
			else if (reportSummaryTarget.equalsIgnoreCase("week"))
			{
				searchColumn = "floor((dateoccurred - DATE '" + reportBeginDate.toString() + "')/7)+1"; // Not "\"Week\"" - it's an SQL syntax thing...
				alias = "Week";
			}
			else if (reportSummaryTarget.equalsIgnoreCase("product"))
			{
				searchColumn = "prdcts.upcid";
				alias = "Product UPC";
			}
			else if (reportSummaryTarget.equalsIgnoreCase("package"))
			{
				searchColumn = "prdctpkgs.name";
				alias = "Package";
				bq = BasePersistentBean.BQ;
				eq = BasePersistentBean.EQ;
				doublequote = true;
			}

			if (rows != null && rows.size() > 0)
			{
				/* Oracle only allows up to 1000 items in an "IN" list, so we
				 * have to split up the list in case there is more.
				 * x in (a, b, c, d) ===> (x in (a,b) OR x in (c,d)).
				 * We split into 200-item lists just to be on the safe side.
				 */
				int inListCounter = 0;
				StringBuffer allInLists = new StringBuffer("(");
				boolean firstList = true;
				do
				{
					StringBuffer inList = new StringBuffer(searchColumn + " IN (");
					boolean firstItem = true;
					for (int i=inListCounter; i<rows.size(); i++)
					{
						ListOrderedMap row = (ListOrderedMap) rows.getValue(i);
						if (firstItem)
							firstItem = false;
						else
							inList.append(',');
						inList.append(bq);
						inList.append(doublequote ? BasePersistentBean.dq(row.get(alias).toString()) : row.get(alias));
						inList.append(eq);
						inListCounter++;
						if ((i+1) % 200 == 0)
							break;
					}
					inList.append(")");
					if (firstList)
					{
						allInLists.append(inList);
						firstList = false;
					}
					else
					{
						allInLists.append(" OR " + inList);
					}
				} while (inListCounter < rows.size());

				allInLists.append(")");

				sbTotalsByReason.addWhere(allInLists.toString(), true);
				if (reportBreakOutByDay.equalsIgnoreCase("yes") &&
					!reportSummaryTarget.equalsIgnoreCase("day"))
					sbTotalsByDailyReason.addWhere(allInLists.toString(), true);
			}
		}
		else if (queryNumber == 1 && numberOfQueries().intValue() == 2)
			mergeResults();
		else if (queryNumber == 2 && numberOfQueries().intValue() == 3)
			mergeResults();
	}

	/**
	 * <p>
	 * Merges the results of the 2/3 queries' results into one table of data. The totals from
	 * the first query are placed on the same row with the totals split by reason from the
	 * second query, and if the third query was run, its daily breakout results are also
	 * placed on the same row. The rows are matched via the entities unique names or ID's.
	 * For example, if the report was over bottlers, the three queries will refer to bottler
	 * names that can then be cross-referenced to create a single row covering all three
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
		ListOrderedMap totals = null, totalsByReason = null, totalsByDailyReason = null;
		ListOrderedMap totalsByReasonGroupedByStoreId = null;
		// ListOrderedMap totalsByDailyReasonGroupedByStoreId = null;
		boolean hasTotalsByReason = false, hasTotalsByDailyReason = false;
		String joinColumn = null;
		String days[] = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
		boolean showQuantity = reportLostSalesMetric.equals("all") || reportLostSalesMetric.equals("quantity");
		boolean showAmount = reportLostSalesMetric.equals("all") || reportLostSalesMetric.equals("amount");
		ReportCellFormat bottlerQuantityDataCellFormat, distributorQuantityDataCellFormat, grandTotalQuantityDataCellFormat,
			bottlerAmountDataCellFormat, distributorAmountDataCellFormat, grandTotalAmountDataCellFormat;
		ReportColumnFormat bottlerQuantityColumnFormat = null, bottlerAmountColumnFormat = null, 
			distributorQuantityColumnFormat = null, distributorAmountColumnFormat = null,
			grandTotalQuantityColumnFormat, grandTotalAmountColumnFormat;
		
		if (results == null)
			return;
		
		switch (results.size())
		{
		case 3:
			totalsByDailyReason = (ListOrderedMap) super.getResults().getValue(2); // keys are some column + "reason" + "dateoccurred"
			hasTotalsByDailyReason = (totalsByDailyReason != null && totalsByDailyReason.size() > 0);
		case 2:
			totalsByReason = (ListOrderedMap) super.getResults().getValue(1); // keys are some column + "reason"
			hasTotalsByReason = (totalsByReason != null && totalsByReason.size() > 0);
		case 1:
			totals = (ListOrderedMap) super.getResults().getValue(0); // keys are plain integers in order
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

		if (showQuantity)
		{
			bottlerQuantityDataCellFormat = new ReportCellFormat("numericDataCell bottlerDataCell");
			distributorQuantityDataCellFormat = new ReportCellFormat("numericDataCell distributorDataCell");
			grandTotalQuantityDataCellFormat = new ReportCellFormat("numericDataCell reportDataCellA");
			bottlerQuantityDataCellFormat.addFormatter(-1, quantityFormatter);
			distributorQuantityDataCellFormat.addFormatter(-1, quantityFormatter);
			grandTotalQuantityDataCellFormat.addFormatter(-1, quantityFormatter);

			bottlerQuantityDataCellFormat.addSubCellCssStyle(-1, "bottlerDataSubCell");
			distributorQuantityDataCellFormat.addSubCellCssStyle(-1, "distributorDataSubCell");
			
			bottlerQuantityColumnFormat = new ReportColumnFormat(null, bottlerQuantityDataCellFormat, null);
			distributorQuantityColumnFormat = new ReportColumnFormat(null, distributorQuantityDataCellFormat, null);
			grandTotalQuantityColumnFormat = new ReportColumnFormat(null, grandTotalQuantityDataCellFormat, null);

			columnFormats.put("Bottler<br/>(units)", bottlerQuantityColumnFormat);
			columnFormats.put("Distributor<br/>(units)", distributorQuantityColumnFormat);
			columnFormats.put("Grand Total<br/>(units)", grandTotalQuantityColumnFormat);
		}
		if (showAmount)
		{
			bottlerAmountDataCellFormat = new ReportCellFormat("numericDataCell bottlerDataCell");
			distributorAmountDataCellFormat = new ReportCellFormat("numericDataCell distributorDataCell");
			grandTotalAmountDataCellFormat = new ReportCellFormat("numericDataCell reportDataCellA");
			bottlerAmountDataCellFormat.addFormatter(-1, amountFormatter);
			distributorAmountDataCellFormat.addFormatter(-1, amountFormatter);
			grandTotalAmountDataCellFormat.addFormatter(-1, amountFormatter);

			bottlerAmountDataCellFormat.addSubCellCssStyle(-1, "bottlerDataSubCell");
			distributorAmountDataCellFormat.addSubCellCssStyle(-1, "distributorDataSubCell");

			bottlerAmountColumnFormat = new ReportColumnFormat(null, bottlerAmountDataCellFormat, null);
			distributorAmountColumnFormat = new ReportColumnFormat(null, distributorAmountDataCellFormat, null);
			grandTotalAmountColumnFormat = new ReportColumnFormat(null, grandTotalAmountDataCellFormat, null);

			columnFormats.put("Bottler<br/>(dollars)", bottlerAmountColumnFormat);
			columnFormats.put("Distributor<br/>(dollars)", distributorAmountColumnFormat);
			columnFormats.put("Grand Total<br/>(dollars)", grandTotalAmountColumnFormat);
		}

		unsortableColumns = new Vector();

		if (hasTotalsByReason)
		{
			ListOrderedMap firstRow = (ListOrderedMap) totalsByReason.getValue(0);
			
			// Get the column which is common across the three queries, to look up subtotals
			// and to join them into one row. This is ALWAYS the first column (see the
			// methods constructTotalsByXXX to understand and verify that.
			// Examples: bottler name, store id, etc.
			// April 25, 2008: Introduced an exception to this rule: when the summary target
			// is "Store (with products)". There the join column is "Store ID", but it cannot
			// be used to join the query for totals with the other subtotals because the latter
			// have as keys: "Store ID" + "Product UPC" + "Reason" [+ "Date"]. This will have a
			// negative effect below unfortunately ... will have to introduce a 'special case'
			// line of code.
			joinColumn = (String) firstRow.get(0);
			
			// first effect of the "Stores (with products)" special case: construct maps that
			// are easier to search based on "Store ID" and "Product UPC". This takes advantage
			// of the fact that the results are *sorted* by "Store ID", "Product UPC", then
			// "Reason"
			if (reportSummaryTarget.equalsIgnoreCase("storewithproduct"))
			{
				totalsByReasonGroupedByStoreId = new ListOrderedMap();
				
				Object storeId = null;
				ListOrderedMap rowsWithSameStoreId = null;
				for (int i=0; i<totalsByReason.size(); i++)
				{
					ListOrderedMap oneRow = (ListOrderedMap) totalsByReason.getValue(i);
					// if the store id is different, start another submap
					if (storeId == null || !storeId.equals(oneRow.get("Store ID")))
					{
						if (storeId != null)
							totalsByReasonGroupedByStoreId.put(storeId, rowsWithSameStoreId);
						rowsWithSameStoreId = new ListOrderedMap();
						storeId = oneRow.get("Store ID");
					}
					rowsWithSameStoreId.put(oneRow.get("Product UPC") + "|" + oneRow.get("Reason"), oneRow);
					if (i == totalsByReason.size() - 1)
						totalsByReasonGroupedByStoreId.put(storeId, rowsWithSameStoreId);
				}
				
				ReportCellFormat simpleCellFormat = new ReportCellFormat("reportDataCellA whiteBackground topAligned");

				ReportColumnFormat simpleColumnFormat = new ReportColumnFormat(null, simpleCellFormat, null);

				columnFormats.put("Rank", simpleColumnFormat);
				columnFormats.put("Store ID", simpleColumnFormat);
				columnFormats.put("Distributor District", simpleColumnFormat);
				columnFormats.put("Bottler Branch", simpleColumnFormat);

				unsortableColumns.add("Rank");
				unsortableColumns.add("Store ID");
				unsortableColumns.add("Distributor District");
				unsortableColumns.add("Bottler Branch");
				unsortableColumns.add("Product UPC");
				unsortableColumns.add("Product Description");
				unsortableColumns.add("Product Package");
				unsortableColumns.add("Bottler<br/>(dollars)");
				unsortableColumns.add("Bottler<br/>(units)");
				unsortableColumns.add("Distributor<br/>(dollars)");
				unsortableColumns.add("Distributor<br/>(units)");
				unsortableColumns.add("Grand Total<br/>(dollars)");
				unsortableColumns.add("Grand Total<br/>(units)");
				
				/*
				 * Removed because daily totals are not required for store with product reports
				if (hasTotalsByDailyReason)
				{
					totalsByDailyReasonGroupedByStoreId = new ListOrderedMap();
					
					storeId = null;
					rowsWithSameStoreId = null;
					for (int i=0; i<totalsByDailyReason.size(); i++)
					{
						ListOrderedMap oneRow = (ListOrderedMap) totalsByDailyReason.getValue(i);
						// if the store id is different, start another submap
						if (storeId == null || !storeId.equals(oneRow.get("Store ID")))
						{
							if (storeId != null)
								totalsByDailyReasonGroupedByStoreId.put(storeId, rowsWithSameStoreId);
							rowsWithSameStoreId = new ListOrderedMap();
							storeId = oneRow.get("Store ID");
						}
						rowsWithSameStoreId.put(oneRow.get("Product UPC") + "|" + oneRow.get("Reason") + "|" + oneRow.get(???DATE???), oneRow);
						if (i == totalsByDailyReason.size() - 1)
							totalsByDailyReasonGroupedByStoreId.put(storeId, rowsWithSameStoreId);
					}
				}
				*/
			}
		}
		
		resetExcelFormats(1);
		ReportExcelFormat excelFormat = new ReportExcelFormat();
		setExcelFormat(0, excelFormat);
		
		resetMergedRegions(1);

		int rowCounter = 0;
		ListOrderedMap newTotals = new ListOrderedMap();
		if (totals != null && totals.size() > 0)
		{
			for (int i=0; i<totals.size(); i++)
			{
				ListOrderedMap totalsRow = (ListOrderedMap) totals.getValue(i);
				
				if (hasTotalsByReason)
				{
					// Using the common column, get its value for this row
					String joinColumnValue = totalsRow.get(joinColumn).toString();
					ListOrderedMap subtotalsRow = null;
						
					if (hasTotalsByDailyReason)
					{
						Calendar calendar = Calendar.getInstance(Locale.US);
						calendar.setTime(reportBeginDate);

						for (int j=0; j<7; j++)
						{
							//ArrayList bottlerDailyTotals = new ArrayList(), distributorDailyTotals = new ArrayList();
							String date = new Date(calendar.getTime().getTime()).toString();
							String dateWithDay = date + ", " + days[ calendar.get(Calendar.DAY_OF_WEEK) - 1 ]; 
							
							subtotalsRow = ((ListOrderedMap) totalsByDailyReason.get(joinColumnValue + "|NE/ND|" + date));
							if (showQuantity)
							{
								totalsRow.put("Bottler<br/>" + dateWithDay + "<br/>(units)", subtotalsRow != null ? subtotalsRow.get("Daily Loss (Units)") : new BigDecimal(0));
								columnFormats.put("Bottler<br/>" + dateWithDay + "<br/>(units)", bottlerQuantityColumnFormat);
							}
							if (showAmount)
							{
								totalsRow.put("Bottler<br/>" + dateWithDay + "<br/>(dollars)", subtotalsRow != null ? subtotalsRow.get("Daily Loss (Dollars)") : new BigDecimal(0));
								columnFormats.put("Bottler<br/>" + dateWithDay + "<br/>(dollars)", bottlerAmountColumnFormat);
							}
	
							subtotalsRow = ((ListOrderedMap) totalsByDailyReason.get(joinColumnValue + "|SA/SI|" + date));
							if (showQuantity)
							{
								totalsRow.put("Distributor<br/>" + dateWithDay + "<br/>(units)", subtotalsRow != null ? subtotalsRow.get("Daily Loss (Units)") : new BigDecimal(0));
								columnFormats.put("Distributor<br/>" + dateWithDay + "<br/>(units)", distributorQuantityColumnFormat);
							}
							if (showAmount)
							{
								totalsRow.put("Distributor<br/>" + dateWithDay + "<br/>(dollars)", subtotalsRow != null ? subtotalsRow.get("Daily Loss (Dollars)") : new BigDecimal(0));
								columnFormats.put("Distributor<br/>" + dateWithDay + "<br/>(dollars)", distributorAmountColumnFormat);
							}

							calendar.add(Calendar.DATE, 1);
						}
					}
					
					if (!reportSummaryTarget.equalsIgnoreCase("storewithproduct"))
					{
						subtotalsRow = ((ListOrderedMap) totalsByReason.get(joinColumnValue + "|NE/ND"));
						if (showQuantity)
							totalsRow.put("Bottler<br/>(units)", subtotalsRow != null ? subtotalsRow.get("Loss By Reason (Units)") : new BigDecimal(0));
						if (showAmount)
							totalsRow.put("Bottler<br/>(dollars)", subtotalsRow != null ? subtotalsRow.get("Loss By Reason (Dollars)") : new BigDecimal(0));
			
						subtotalsRow = ((ListOrderedMap) totalsByReason.get(joinColumnValue + "|SA/SI"));
						if (showQuantity)
							totalsRow.put("Distributor<br/>(units)", subtotalsRow != null ? subtotalsRow.get("Loss By Reason (Units)") : new BigDecimal(0));
						if (showAmount)
							totalsRow.put("Distributor<br/>(dollars)", subtotalsRow != null ? subtotalsRow.get("Loss By Reason (Dollars)") : new BigDecimal(0));
	
						if (showQuantity)
							totalsRow.put("Grand Total<br/>(units)", totalsRow.get("Total Lost Sales (Units)"));						
						if (showAmount)
							totalsRow.put("Grand Total<br/>(dollars)", totalsRow.get("Total Lost Sales (Dollars)"));
	
						totalsRow.remove("Total Lost Sales (Units)");
						totalsRow.remove("Total Lost Sales (Dollars)");
	
						newTotals.put(totals.get(i), totalsRow);
						rowCounter++;
					}
					else
					{
						Object storeID = totalsRow.get("Store ID");
						ListOrderedMap rowsWithSameStoreId = (ListOrderedMap) totalsByReasonGroupedByStoreId.get(storeID);
						
						Object upc = null;
						ListOrderedMap newRow = null;
						double sumBottlerUnitsForStore, sumBottlerDollarsForStore, sumDistributorUnitsForStore, sumDistributorDollarsForStore;
						double sumUnitsForProduct, sumDollarsForProduct;
						sumBottlerUnitsForStore = sumBottlerDollarsForStore = sumDistributorUnitsForStore = sumDistributorDollarsForStore = 0;
						sumUnitsForProduct = sumDollarsForProduct = 0;
						int rowCounterForThisStore = 0;
						for (int k=0; k<rowsWithSameStoreId.size(); k++)
						{
							subtotalsRow = (ListOrderedMap) rowsWithSameStoreId.getValue(k);

							if (upc == null || !upc.equals(subtotalsRow.get("Product UPC")))
							{
								if (upc != null)
								{
									if (showQuantity)
										newRow.put("Grand Total<br/>(units)", new BigDecimal(sumUnitsForProduct));
									if (showAmount)
										newRow.put("Grand Total<br/>(dollars)", new BigDecimal(sumDollarsForProduct));
									sumUnitsForProduct = sumDollarsForProduct = 0;
									newTotals.put(totals.get(i) + "|" + upc, newRow);
									rowCounter++;
									rowCounterForThisStore++;
								}
								newRow = new ListOrderedMap();
								newRow.putAll(totalsRow);
								
								newRow.put("Product UPC", subtotalsRow.get("Product UPC"));
								newRow.put("Product Description", subtotalsRow.get("Product Description"));
								newRow.put("Product Package", subtotalsRow.get("Product Package"));
								if (showQuantity)
								{
									newRow.put("Bottler<br/>(units)", new BigDecimal(0));
									newRow.put("Distributor<br/>(units)", new BigDecimal(0));
								}
								if (showAmount)
								{
									newRow.put("Bottler<br/>(dollars)", new BigDecimal(0));
									newRow.put("Distributor<br/>(dollars)", new BigDecimal(0));
								}
								
								newRow.remove("Total Lost Sales (Units)");
								newRow.remove("Total Lost Sales (Dollars)");
								upc = subtotalsRow.get("Product UPC");
							}
							
							if (rowCounterForThisStore != 0) 
							{
								// remove first four columns since they are merged (Rank, Store ID, Distributor District, Bottler Branch) 
								newRow.put("Rank", null);
								newRow.put("Store ID", null);
								newRow.put("Distributor District", null);
								newRow.put("Bottler Branch", null);
							}
							
							if (subtotalsRow.get("Reason").equals("NE/ND"))
							{
								if (showQuantity)
								{
									BigDecimal loss = (BigDecimal) subtotalsRow.get("Loss By Reason (Units)");
									newRow.put("Bottler<br/>(units)", loss);
									sumUnitsForProduct += loss.doubleValue();
									sumBottlerUnitsForStore += loss.doubleValue();
								}
								if (showAmount)
								{
									BigDecimal loss = (BigDecimal) subtotalsRow.get("Loss By Reason (Dollars)");
									newRow.put("Bottler<br/>(dollars)", loss);
									sumDollarsForProduct += loss.doubleValue();
									sumBottlerDollarsForStore += loss.doubleValue();
								}
							}
							else if (subtotalsRow.get("Reason").equals("SA/SI"))
							{
								if (showQuantity)
								{
									BigDecimal loss = (BigDecimal) subtotalsRow.get("Loss By Reason (Units)");
									newRow.put("Distributor<br/>(units)", loss);
									sumUnitsForProduct += loss.doubleValue();
									sumDistributorUnitsForStore += loss.doubleValue();
								}
								if (showAmount)
								{
									BigDecimal loss = (BigDecimal) subtotalsRow.get("Loss By Reason (Dollars)");
									newRow.put("Distributor<br/>(dollars)", loss);
									sumDollarsForProduct += loss.doubleValue();
									sumDistributorDollarsForStore += loss.doubleValue();
								}
							}

							if (k == rowsWithSameStoreId.size() - 1)
							{
								if (showQuantity)
									newRow.put("Grand Total<br/>(units)", new BigDecimal(sumUnitsForProduct));
								if (showAmount)
									newRow.put("Grand Total<br/>(dollars)", new BigDecimal(sumDollarsForProduct));
								sumUnitsForProduct = sumDollarsForProduct = 0;
								newTotals.put(totals.get(i) + "|" + upc, newRow);
								rowCounter++;
								rowCounterForThisStore++;
							}
						}

						// put in the subtotals for that store ID
						newRow = new ListOrderedMap();
						newRow.put("Rank", null);
						newRow.put("Store ID", null);
						newRow.put("Distributor District", null);
						newRow.put("Bottler Branch", null);
						newRow.put("Product UPC", " ");
						newRow.put("Product Description", " ");
						newRow.put("Product Package", "SUBTOTALS");
						if (showQuantity)
						{
							newRow.put("Distributor<br/>(units)", new BigDecimal(sumDistributorUnitsForStore));
							newRow.put("Bottler<br/>(units)", new BigDecimal(sumBottlerUnitsForStore));
							newRow.put("Grand Total<br/>(units)", new BigDecimal(sumDistributorUnitsForStore + sumBottlerUnitsForStore));
						}
						if (showAmount)
						{
							newRow.put("Distributor<br/>(dollars)", new BigDecimal(sumDistributorDollarsForStore));
							newRow.put("Bottler<br/>(dollars)", new BigDecimal(sumBottlerDollarsForStore));
							newRow.put("Grand Total<br/>(dollars)", new BigDecimal(sumDistributorDollarsForStore + sumBottlerDollarsForStore));
						}
						newTotals.put(totals.get(i) + "|" + storeID, newRow);
						rowCounter++;
						rowCounterForThisStore++;

						addMergedRegion(0, 0, rowCounter - rowCounterForThisStore, 0, rowCounter - 1 );
						addMergedRegion(0, 1, rowCounter - rowCounterForThisStore, 1, rowCounter - 1 );
						addMergedRegion(0, 2, rowCounter - rowCounterForThisStore, 2, rowCounter - 1 );
						addMergedRegion(0, 3, rowCounter - rowCounterForThisStore, 3, rowCounter - 1 );
					}
				}
			}
			// MainLog.getLog().debug("Number of rows according to counter: " + rowCounter);
			
			if (newTotals.size() > 0)
			{
				ListOrderedMap firstRow = (ListOrderedMap) newTotals.getValue(0);
				int numberOfColumns = firstRow.size();
				int numberOfRows = newTotals.size();
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
					
					if (columnName.indexOf("Bottler") >= 0 && 
						(columnName.indexOf("units") >= 0 || columnName.indexOf("dollars") >= 0))
						excelFormat.addCellStyle(k, 1, k, numberOfRows, bottlerCellStyle);
					if (columnName.indexOf("Distributor") >= 0 &&
						(columnName.indexOf("units") >= 0 || columnName.indexOf("dollars") >= 0))
						excelFormat.addCellStyle(k, 1, k, numberOfRows, distributorCellStyle);
					if (columnName.indexOf("Grand Total") >= 0 &&
						(columnName.indexOf("units") >= 0 || columnName.indexOf("dollars") >= 0))
						excelFormat.addCellStyle(k, 1, k, numberOfRows, grandTotalCellStyle);
				}
			}
		}
		else
			return;
		
		ListOrderedMap mergedResults = new ListOrderedMap();
		mergedResults.put("query0", newTotals);
		setResults(mergedResults);
		
		return;
	}

	protected String[] getInitialSortedColumns(int queryNumber)
	{
		if (queryNumber == 0)
		{
			String isc[] = { "Rank" };
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
			if (unsortableColumns == null || unsortableColumns.size() == 0)
				return null;
			
			Object[] asObjects = unsortableColumns.toArray();
			String[] asStrings = new String[asObjects.length];
			for (int i=0; i<asObjects.length; i++)
				asStrings[i] = asObjects[i].toString();
			return asStrings;
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
