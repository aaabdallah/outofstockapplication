<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<%@ page import="net.btlcpy.outofstock.persistence.beans.ProductCategory"%>
<%@ page import="net.btlcpy.outofstock.persistence.BeanCache"%>
<%@ page import="java.util.Map"%>
<%@ page import="org.apache.commons.collections.map.ListOrderedMap"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.Product"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.ProductPackage"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.DistributorDivision"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.DistributorDistrict"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.Bottler"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerBusinessUnit"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerMarketUnit"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerBranch"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.Store"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.OutOfStockEvent"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.sql.Date"%>
<%@ page import="java.util.Calendar"%>
<%@ page import="java.util.Locale"%>
<%@page import="net.btlcpy.outofstock.persistence.views.StoreLocation"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="net.btlcpy.outofstock.persistence.views.ProductDescription"%>
<portlet:defineObjects/>

<%!
ListOrderedMap beans = null;
BeanCache cache = null;

private String pkID(String primaryKey, String identifier)
{
	return primaryKey + "|" + identifier;
}

private String pk(String pkID)
{
	if (pkID == null || pkID.equals("all")) return pkID;
	
	return pkID.substring(0, pkID.indexOf("|"));
}

private String id(String pkID)
{
	if (pkID == null || pkID.equals("all")) return pkID;
	
	return pkID.substring(pkID.indexOf("|") + 1);
}

private Integer[] convertStringPKIDsToIntegerPKs(String[] pkID)
{
	if (pkID != null)
	{
		Integer[] pkAsInts = new Integer[pkID.length];
		for (int i=0; i<pkID.length; i++)
			pkAsInts[i] = new Integer( pk(pkID[i]) );
		return pkAsInts;
	}
	return null;
}

private boolean isSelected(String pk, String[] selectedPKID)
{
	if (pk != null && selectedPKID != null)
	{
		for (int i=0; i<selectedPKID.length; i++)
		{
			if (pk.equals(pk(selectedPKID[i])) )
				return true;
		}
	}
	return false;
}
%>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/reports/index.jsp"/>
</portlet:renderURL>
<%
String reportType = (String) renderRequest.getPortletSession().getAttribute("reportType");
String summaryTarget = (String) renderRequest.getPortletSession().getAttribute("summaryTarget");
String timePeriod = (String) renderRequest.getPortletSession().getAttribute("timePeriod");
String startingYear = (String) renderRequest.getPortletSession().getAttribute("startingYear");
String startingMonth = (String) renderRequest.getPortletSession().getAttribute("startingMonth");
String startingDate = (String) renderRequest.getPortletSession().getAttribute("startingDate");
if (startingYear == null)
{
	Date earliestDate = OutOfStockEvent.getEarliestEventDate();
	if (earliestDate != null)
	{
		Calendar calendar = Calendar.getInstance(Locale.US);
		calendar.setTime(earliestDate);
		startingYear = "" + calendar.get(Calendar.YEAR);
		startingMonth = "" + calendar.get(Calendar.MONTH);
		startingDate = "" + calendar.get(Calendar.DATE);
	}
}
String numberOfWeeks = (String) renderRequest.getPortletSession().getAttribute("numberOfWeeks");
String breakOutByDay = (String) renderRequest.getPortletSession().getAttribute("breakOutByDay");
String resultSize = (String) renderRequest.getPortletSession().getAttribute("resultSize");
String lostSalesMetric = (String) renderRequest.getPortletSession().getAttribute("lostSalesMetric");

if ((reportType == null || reportType.equalsIgnoreCase("totals")) &&
	summaryTarget != null && summaryTarget.equalsIgnoreCase("storetvss"))
{
	summaryTarget = "store";
}

if (reportType != null && reportType.equalsIgnoreCase("averages") && summaryTarget != null &&
	(summaryTarget.equalsIgnoreCase("day") || summaryTarget.equalsIgnoreCase("week")))
{
	summaryTarget = "store";
	resultSize = "top25";
}

String[] productCategoryPKID = (String[]) renderRequest.getPortletSession().getAttribute("productCategory");
String[] productPKID = (String[]) renderRequest.getPortletSession().getAttribute("product");
String[] productPackagePKID = (String[]) renderRequest.getPortletSession().getAttribute("productPackage");
String[] bottlerPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottler");
String[] bottlerBusinessUnitPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerBusinessUnit");
String[] bottlerMarketUnitPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerMarketUnit");
String[] bottlerBranchPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerBranch");
String[] bottlerSalesRoutePKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerSalesRoute");
String[] distributorDivisionPKID = (String[]) renderRequest.getPortletSession().getAttribute("distributorDivision");
String[] distributorDistrictPKID = (String[]) renderRequest.getPortletSession().getAttribute("distributorDistrict");
String[] storePKID = (String[]) renderRequest.getPortletSession().getAttribute("store");
String[] pkids = null;
%>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Reports"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Specify Report Parameters"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<script language="JavaScript">
	var isIE = document.all;
	
	// Function for keeping the month, date, and year fields of a simple trio of dropdown lists for
	// selecting a date consistent with each other.
	// Author: Ahmed A. Abd-Allah.
	// src='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/scripts/adjustDatePicker.js") %>'
	function adjustDatePicker(objForm)
	{
		// get month, day, & year.
		var month = parseInt(objForm.startingMonthSelect.options[objForm.startingMonthSelect.selectedIndex].value);
		var date = parseInt(objForm.startingDateSelect.options[objForm.startingDateSelect.selectedIndex].value);
		var year = parseInt(objForm.startingYearSelect.options[objForm.startingYearSelect.selectedIndex].value);
	
		var fullDate, maxDateForMonth;
		
		while (false)
		{
			fullDate = new Date(year, month, date);
			if (isNaN(fullDate.getDate()) || fullDate.getDate() != date)
			{
				date--;
				continue;
			}
			break;
		}
		
		// if month is not feb, try to construct a date of month/31/year. OK ? x=31 : x=30
		if (month != 1)
		{
			fullDate = new Date(year, month, 31);
			if (isNaN(fullDate.getDate()) || fullDate.getDate() != 31)
				maxDateForMonth = 30;
			else
				maxDateForMonth = 31;
		}
		else // else try feb/29/year. OK ? x=29 : x=28
		{ 
			fullDate = new Date(year, month, 29);
			if (isNaN(fullDate.getDate()) || fullDate.getDate() != 29)
				maxDateForMonth = 28;
			else
				maxDateForMonth = 29;
		}
	
		// if (x < current size of day menu) add as many missing elements as necessary
		if (maxDateForMonth < objForm.startingDateSelect.options.length)
		{
			for (var i=objForm.startingDateSelect.options.length; i>maxDateForMonth; i--)
				objForm.startingDateSelect.remove(i-1);
		}
		// if (x > current size of day menu) remove as many missing elements as necessary 
		if (maxDateForMonth > objForm.startingDateSelect.options.length)
		{
			for (var i=objForm.startingDateSelect.options.length; i<maxDateForMonth; i++)
			{
				var newOption = new Option("" + (i+1), "" + (i+1));
				// the add method is non-uniform across IE and Firefox, so we use the old
				// way of adding elements
				// objForm.startingDateSelect.add(newOption); // IE compatible only
				// objForm.startingDateSelect.add(newOption, null); // Firefox compatible only
				objForm.startingDateSelect.options[i] = newOption;
			}
		}
	
		// move date to within valid range for month if necessary
		if (date > maxDateForMonth)
		{
			date = maxDateForMonth;
			objForm.startingDateSelect.selectedIndex = maxDateForMonth-1;
		} 
	
		fullDate = new Date(year, month, date);
		//alert(fullDate.toDateString() + ", Max date for month is " + maxDateForMonth +
		//	", Number of dates in select is " + objForm.startingDateSelect.options.length);
	}
	
	function removeOptionByValue(objSelect, optionValue)
	{
		for (var i=0; i<objSelect.options.length; i++)
		{
			if (objSelect.options[i].value == optionValue)
			{
				objSelect.options[i] = null;
				break;
			}
		}
	}

	var TARGETSTORE = 0;
	var TARGETDISTRIBUTORDISTRICT = 2;
	var TARGETDISTRIBUTORDIVISION = 4;
	var TARGETBOTTLERBRANCH = 6;
	var TARGETBOTTLERMARKETUNIT = 8;
	var TARGETBOTTLERBUSINESSUNIT = 10;
	var TARGETBOTTLER = 12;
	var TARGETBOTTLERSALESROUTE = 14;
	
	var TARGETPRODUCT = 0;
	var TARGETPRODUCTCATEGORY = 3;
	var TARGETPRODUCTPACKAGE = 6;

	function updateForm(objForm, changeSource)
	{
		/*
		Display the first summary target as "Store" if and only if:
			the report type is "Totals"
		Else display the first summary target as "Store (Average over all stores)" if and only if:
			the report type is "Averages"		
		*/
		if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'totals')
			objForm.summaryTargetSelect.options[0].text = "Store";
		else if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'averages')
			objForm.summaryTargetSelect.options[0].text = "Store (Average over all stores)";

		/*
		Display "day", "week", "storewithproduct" as summary targets if and only if:
			the report type is "totals"
		Display "storetvss" as summary target if and only if:
			the report type is "averages"
		*/
		if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'totals')
		{
			removeOptionByValue(objForm.summaryTargetSelect, "storetvss");

			var hasDay = false, hasWeek = false, hasStoreWithProduct = false;
			for (var i=0; i<objForm.summaryTargetSelect.options.length; i++)
			{
				if (objForm.summaryTargetSelect.options[i].value == "day")
					hasDay = true;
				else if (objForm.summaryTargetSelect.options[i].value == "week")
					hasWeek = true;
				else if (objForm.summaryTargetSelect.options[i].value == "storewithproduct")
					hasStoreWithProduct = true;
			}
			
			var currentLength = objForm.summaryTargetSelect.options.length;
			if (!hasDay)
				objForm.summaryTargetSelect.options[currentLength++] = new Option("Day", "day");
			if (!hasWeek)
				objForm.summaryTargetSelect.options[currentLength++] = new Option("Week", "week");
			if (!hasStoreWithProduct)
				objForm.summaryTargetSelect.options[currentLength++] = new Option("Store (with products)", "storewithproduct");
		}
		else if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'averages')
		{
			removeOptionByValue(objForm.summaryTargetSelect, "day");
			removeOptionByValue(objForm.summaryTargetSelect, "week");
			removeOptionByValue(objForm.summaryTargetSelect, "storewithproduct");

			var hasStoreTVSS = false; // TVSS = Traditional vs. Distributor.com
			for (var i=0; i<objForm.summaryTargetSelect.options.length; i++)
			{
				if (objForm.summaryTargetSelect.options[i].value == "storetvss")
					hasStoreTVSS = true;
			}

			var currentLength = objForm.summaryTargetSelect.options.length;

			if (!hasStoreTVSS)
				objForm.summaryTargetSelect.options[currentLength++] = new Option("Store (Traditional vs. Distributor.com)", "storetvss");
		}

		/*
		Display the break out by day option if and only if: 
			the report type is "Totals"
			the time period is "One Week"
			the summary target is NOT by "Day"
			the summary target is NOT by "Week"
			the summary target is NOT by "Store (with products)"
		*/
		if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'totals' &&
			objForm.timePeriodSelect != null &&
			objForm.timePeriodSelect.options[objForm.timePeriodSelect.selectedIndex].value == 'week' &&
			objForm.summaryTargetSelect != null &&
			objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value != 'day' &&
			objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value != 'week' &&
			objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value != 'storewithproduct')
		{
			var o = document.getElementById("rowBreakOutByDay");
			o.style.display = '';
		}
		else
		{
			var o = document.getElementById("chooseDayBreakoutNo");
			o.checked = true;
			o = document.getElementById("rowBreakOutByDay");
			o.style.display = 'none';
		}
		
		/*
		Display the number of weeks field if and only if:
			the summary target is "Week"
		Else display the time period selection.
		*/
		if (objForm.summaryTargetSelect != null &&
			objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value == 'week')
		{
			var o = document.getElementById("rowNumberOfWeeks");
			o.style.display = '';
			o = document.getElementById("rowTimePeriod");
			o.style.display = 'none';
		}
		else
		{
			var o = document.getElementById("rowNumberOfWeeks");
			o.style.display = 'none';
			o = document.getElementById("rowTimePeriod");
			o.style.display = '';
		}

		/*
		Display the date picker if and only if: 
			the time period is null
			OR
			the time period is NOT "Year to Date"
			OR
			the time period is "Year to Date", the report type is "Totals" and the summary target is "Week"
		*/
		if (objForm.timePeriodSelect == null ||
			(objForm.timePeriodSelect.options[objForm.timePeriodSelect.selectedIndex].value != 'ytd') ||
			(objForm.timePeriodSelect.options[objForm.timePeriodSelect.selectedIndex].value == 'ytd' &&
				objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'totals' &&
				objForm.summaryTargetSelect != null &&
				objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value == 'week')
			)
		{
			var o = document.getElementById("rowStartingDate");
			o.style.display = '';
		}
		else
		{
			var o = document.getElementById("rowStartingDate");
			o.style.display = 'none';
		}
		
		/*
		Display the summary target and lost sales metric if and only if:
			the report type is NOT "Average Daily Events per Store"
		*/
		if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value != 'dailyevents')
		{
			var o = document.getElementById("rowSummaryTarget");
			o.style.display = '';
			o = document.getElementById("rowLostSalesMetric");
			o.style.display = '';
	
			/*
			Display the lost sales metric of "Both dollars and units" if and only if:
				the report type is NOT "Averages"
			*/
			if (objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'averages')
			{
				if (objForm.lostSalesMetricSelect.options[2].selected)
					objForm.lostSalesMetricSelect.options[0].selected = true;
				objForm.lostSalesMetricSelect.options[2] = null;
			}
			else if (objForm.lostSalesMetricSelect.length == 2)
				objForm.lostSalesMetricSelect.options[2] = new Option("Both dollars and units", "all");
		}
		else
		{
			var o = document.getElementById("rowSummaryTarget");
			o.style.display = 'none';
			o = document.getElementById("rowLostSalesMetric");
			o.style.display = 'none';
		}
		
		/*
		Hide the result size parameter if and only if:
			the report type is "Average Lost Sales"
			OR
			the report type is "Totals" and the summary target is "Week"
		*/
		if ((objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'averages') ||
			(objForm.reportTypeSelect.options[objForm.reportTypeSelect.selectedIndex].value == 'totals' &&
				objForm.summaryTargetSelect != null &&
				objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value == 'week')
			)
		{
			var o = document.getElementById("rowResultSize");
			o.style.display = 'none';
		}
		else
		{
			var o = document.getElementById("rowResultSize");
			o.style.display = '';
		}
		
		/*
		***********************************************************************
		***********************************************************************
		***********************************************************************
		*/
		
		// find the selected options in the previous items
		var selectedDistributorDivisions = getSelectedOptionsPKs(objForm.distributorDivisionSelect);
		if (selectedDistributorDivisions != null && selectedDistributorDivisions[0] == "-1")
		{
			selectedDistributorDivisions.shift();
			if (selectedDistributorDivisions.length == 0)
				selectedDistributorDivisions = null;
		}
		var selectedDistributorDistricts = getSelectedOptionsPKs(objForm.distributorDistrictSelect);
		var selectedStores = getSelectedOptionsPKs(objForm.storeSelect);
		var selectedBottlers = getSelectedOptionsPKs(objForm.bottlerSelect);
		var selectedBottlerBusinessUnits = getSelectedOptionsPKs(objForm.bottlerBusinessUnitSelect);
		var selectedBottlerMarketUnits = getSelectedOptionsPKs(objForm.bottlerMarketUnitSelect);
		var selectedBottlerBranches = getSelectedOptionsPKs(objForm.bottlerBranchSelect);
		
		var selectedProductCategories = getSelectedOptionsPKs(objForm.productCategorySelect);
		var selectedProducts = getSelectedOptionsPKs(objForm.productSelect);
		var selectedProductPackages = getSelectedOptionsPKs(objForm.productPackageSelect);
		
		// apply the "ripple" effect
		if (changeSource != null && 
			changeSource.toLowerCase() == "distributordivision")
		{
			neutralizeSelect(objForm.storeSelect);
			selectedStores = null;
			
			// special case: DISTRIBUTOR.COM may be selected hence causing a false display of distributor districts
			if (selectedDistributorDivisions == null)
			{
				neutralizeSelect(objForm.distributorDistrictSelect);
				selectedDistributorDistricts = null;
			}
			else
			{
				repopulateSelect(objForm.distributorDistrictSelect, objForm.distributorDivisionSelect,
					findFromStoreLocations(TARGETDISTRIBUTORDISTRICT, 
						selectedDistributorDivisions, null, null,
						null, null, null, null,
						null).sort(comparePKID_Strings));
				selectedDistributorDistricts = null;
			}
		}
		
		if (changeSource != null && 
			changeSource.toLowerCase() == "distributordistrict")
		{
			repopulateSelect(objForm.storeSelect, objForm.distributorDistrictSelect,
				findFromStoreLocations(TARGETSTORE, 
					selectedDistributorDivisions, selectedDistributorDistricts, null,
					null, null, null, null,
					null).sort(comparePKID_Integers));
			selectedStores = null;
		}

		if (changeSource != null && 
			(changeSource.toLowerCase() == "distributordivision" ||
			changeSource.toLowerCase() == "distributordistrict" ||
			changeSource.toLowerCase() == "store"))
		{
			neutralizeSelect(objForm.bottlerBusinessUnitSelect);
			selectedBottlerBusinessUnits = null;
			neutralizeSelect(objForm.bottlerMarketUnitSelect);
			selectedBottlerMarketUnits = null;
			neutralizeSelect(objForm.bottlerBranchSelect);
			selectedBottlerBranches = null;

			repopulateSelect(objForm.bottlerSelect, null,
				findFromStoreLocations(TARGETBOTTLER, 
					selectedDistributorDivisions, selectedDistributorDistricts, selectedStores,
					null, null, null, null,
					null).sort(comparePKID_Strings));
			selectedBottlers = null;
		}

		if (changeSource != null && 
			changeSource.toLowerCase() == "bottler")
		{
		
			neutralizeSelect(objForm.bottlerMarketUnitSelect);
			selectedBottlerMarketUnits = null;
			neutralizeSelect(objForm.bottlerBranchSelect);
			selectedBottlerBranches = null;

			repopulateSelect(objForm.bottlerBusinessUnitSelect, objForm.bottlerSelect,
				findFromStoreLocations(TARGETBOTTLERBUSINESSUNIT, 
					selectedDistributorDivisions, selectedDistributorDistricts, selectedStores,
					selectedBottlers, null, null, null,
					null).sort(comparePKID_Strings));
			selectedBottlerBusinessUnits = null;
		}

		if (changeSource != null && 
			changeSource.toLowerCase() == "bottlerbusinessunit")
		{
			neutralizeSelect(objForm.bottlerBranchSelect);
			selectedBottlerBranches = null;

			repopulateSelect(objForm.bottlerMarketUnitSelect, objForm.bottlerBusinessUnitSelect,
				findFromStoreLocations(TARGETBOTTLERMARKETUNIT, 
					selectedDistributorDivisions, selectedDistributorDistricts, selectedStores,
					selectedBottlers, selectedBottlerBusinessUnits, null, null,
					null).sort(comparePKID_Strings));
			selectedBottlerMarketUnits = null;
		}

		if (changeSource != null && 
			changeSource.toLowerCase() == "bottlermarketunit")
		{
			repopulateSelect(objForm.bottlerBranchSelect, objForm.bottlerMarketUnitSelect,
				findFromStoreLocations(TARGETBOTTLERBRANCH, 
					selectedDistributorDivisions, selectedDistributorDistricts, selectedStores,
					selectedBottlers, selectedBottlerBusinessUnits, selectedBottlerMarketUnits, null,
					null).sort(comparePKID_Strings));
			selectedBottlerBranches = null;
		}

		if (changeSource != null &&
			(changeSource.toLowerCase() == "distributordivision" ||
			changeSource.toLowerCase() == "distributordistrict" ||
			changeSource.toLowerCase() == "store" ||
			changeSource.toLowerCase() == "bottler" ||
			changeSource.toLowerCase() == "bottlerbusinessunit" ||
			changeSource.toLowerCase() == "bottlermarketunit"))
		{
			/*alert(selectedDistributorDivisions);
			alert(selectedDistributorDistricts);
			alert(selectedStores);
			alert(selectedBottlers);
			alert(selectedBottlerBusinessUnits);
			alert(selectedBottlerMarketUnits);
			alert(selectedBottlerBranches);*/
			repopulateSelect(objForm.bottlerSalesRouteSelect, null,
				findFromStoreLocations(TARGETBOTTLERSALESROUTE, 
					selectedDistributorDivisions, selectedDistributorDistricts, selectedStores,
					selectedBottlers, selectedBottlerBusinessUnits, selectedBottlerMarketUnits, selectedBottlerBranches,
					null).sort(comparePKID_Strings));
			
		}
		
		if (changeSource != null && 
			changeSource.toLowerCase() == "productcategory")
		{
			repopulateSelect(objForm.productSelect, objForm.productCategorySelect,
				findFromProductDescriptions(TARGETPRODUCT, 
					selectedProductCategories, null, null).sort(comparePKID_Strings));
			selectedProducts = null;
		}

		if (changeSource != null && 
			(changeSource.toLowerCase() == "productcategory" ||
			changeSource.toLowerCase() == "product"))
		{
			repopulateSelect(objForm.productPackageSelect, null,
				findFromProductDescriptions(TARGETPRODUCTPACKAGE, 
					selectedProductCategories, selectedProducts, null).sort(comparePKID_Strings));
			selectedProductPackages = null;
		}

		if (changeSource == null)
		{
			if (selectedDistributorDivisions == null ||
				(selectedDistributorDivisions.length == 1 && selectedDistributorDivisions[0] == "-1"))
			{
				neutralizeSelect(objForm.distributorDistrictSelect);
				selectedDistributorDistricts = null;
				neutralizeSelect(objForm.storeSelect);
				selectedStores = null;
			}
			else
			{
				if (selectedDistributorDistricts == null)
				{
					neutralizeSelect(objForm.storeSelect);
					selectedStores = null;
				}
			}
			
			if (selectedBottlers == null)
			{
				neutralizeSelect(objForm.bottlerBusinessUnitSelect);
				selectedBottlerBusinessUnits = null;
				neutralizeSelect(objForm.bottlerMarketUnitSelect);
				selectedBottlerMarketUnits = null;
				neutralizeSelect(objForm.bottlerBranchSelect);
				selectedBottlerBranches = null;
			}
			else
			{
				if (selectedBottlerBusinessUnits == null)
				{
					neutralizeSelect(objForm.bottlerMarketUnitSelect);
					selectedBottlerMarketUnits = null;
					neutralizeSelect(objForm.bottlerBranchSelect);
					selectedBottlerBranches = null;
				}
				else
				{
					if (selectedBottlerMarketUnits == null)
					{
						neutralizeSelect(objForm.bottlerBranchSelect);
						selectedBottlerBranches = null;
					}
				}
			}
			
			if (selectedProductCategories == null)
			{
				neutralizeSelect(objForm.productSelect);
				selectedProducts = null;
			}
		}
	}
	
	function getSelectedOptionsPKs(selectObject)
	{
		var selectedOptions = new Array();
		if (selectObject != null && selectObject.style.display != "none")
		{
			for (var i=0; i<selectObject.options.length; i++)
			{
				if (selectObject.options[i].selected)
				{
					selectedOptions[selectedOptions.length] = 
						selectObject.options[i].value.substring(0, selectObject.options[i].value.indexOf("|"));
				}
			}
		}
		if (selectedOptions.length > 0)
			return selectedOptions;
		else
			return null;
	}
	
	function repopulateSelect(selectObject, parentSelectObject, keyValues)
	{
		if (selectObject != null)
		{
			if (parentSelectObject != null && parentSelectObject.selectedIndex < 0)
			{
				selectObject.selectedIndex = -1;
				selectObject.style.display = 'none';
				return;
			}
			else
				selectObject.style.display = '';

			selectObject.options.length = 0;
			
			if (keyValues != null)
				for (var i=0; i<keyValues.length; i++)
				{
					selectObject.options[i] = new Option(
						keyValues[i].substring(keyValues[i].indexOf("|") + 1),
						keyValues[i]);
				}
		}
	}
	
	function neutralizeSelect(selectObject)
	{
		if (selectObject != null)
		{
			selectObject.selectedIndex = -1;
			selectObject.style.display = "none";
		}
	}
	
	function comparePKID_Strings(pkid1, pkid2)
	{
		if (pkid1 == null && pkid2 == null)
			return 0;
		
		if (pkid1 != null && pkid2 != null)
		{
			var value1 = pkid1.substring(pkid1.indexOf("|") + 1);
			var value2 = pkid2.substring(pkid2.indexOf("|") + 1);
			
			if (value1 == value2)
				return 0;
			else if (value1 < value2)
				return -1;
			else
				return 1;
		}
		
		return 0;
	}
	
	function comparePKID_Integers(pkid1, pkid2)
	{
		if (pkid1 == null && pkid2 == null)
			return 0;
		
		if (pkid1 != null && pkid2 != null)
		{
			var value1 = parseInt(pkid1.substring(pkid1.indexOf("|") + 1));
			var value2 = parseInt(pkid2.substring(pkid2.indexOf("|") + 1));
			
			if (value1 == value2)
				return 0;
			else if (value1 < value2)
				return -1;
			else
				return 1;
		}
		
		return 0;
	}

	function checkedSubmit(formId)
	{
		var objForm = document.getElementById(formId);

		if (isIE)
			document.cookie = "spftop=" + document.body.scrollTop;
		else
		{
			document.cookie = "spftop=" + document.documentElement.scrollTop;
			// alert(document.cookie);
		}
			
		objForm.submit();
	}
	
	function clearConstraints(objForm)
	{
		neutralizeSelect(objForm.productSelect);
		neutralizeSelect(objForm.distributorDistrictSelect);
		neutralizeSelect(objForm.storeSelect);
		neutralizeSelect(objForm.bottlerBusinessUnitSelect);
		neutralizeSelect(objForm.bottlerMarketUnitSelect);
		neutralizeSelect(objForm.bottlerBranchSelect);
		
		objForm.productCategorySelect.selectedIndex = -1;

		objForm.distributorDivisionSelect.selectedIndex = -1;

		repopulateSelect(objForm.bottlerSelect, null,
			findFromStoreLocations(TARGETBOTTLER, 
				null, null, null,
				null, null, null, null,
				null).sort(comparePKID_Strings));

		repopulateSelect(objForm.bottlerSalesRouteSelect, null,
			findFromStoreLocations(TARGETBOTTLERSALESROUTE, 
				null, null, null,
				null, null, null, null,
				null).sort(comparePKID_Strings));

		repopulateSelect(objForm.productPackageSelect, null,
			findFromProductDescriptions(TARGETPRODUCTPACKAGE, 
				null, null, null).sort(comparePKID_Strings));

		return false;
	}

	// ------------------------------------------------------------------------
	function findFromStoreLocations(target, distributorDivisions, distributorDistricts, stores,
		bottlers, bottlerBusinessUnits, bottlerMarketUnits, bottlerBranches, bottlerSalesRoutes)
	{
		var hiddenTable = document.getElementById("storeLocations");
		
		if (hiddenTable == null)
			return "__NODATA__";

		var results = new Array();
		var rTotal = 0;
		var isMatchingRow;
		
		for (var i=1; i<hiddenTable.rows.length; i++)
		{
			if (results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] == true)
				continue;

			isMatchingRow = false;
			if (target == TARGETBOTTLER || target == TARGETBOTTLERBUSINESSUNIT || 
				target == TARGETBOTTLERMARKETUNIT || target == TARGETBOTTLERBRANCH)
			{
				if (stores != null && stores.length > 0)
				{
					for (var j=0; j<stores.length; j++)
						if (stores[j] == hiddenTable.rows[i].cells[0].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (distributorDistricts != null && distributorDistricts.length > 0)
				{
					for (var j=0; j<distributorDistricts.length; j++)
						if (distributorDistricts[j] == hiddenTable.rows[i].cells[2].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (distributorDivisions != null && distributorDivisions.length > 0)
				{
					for (var j=0; j<distributorDivisions.length; j++)
						if (distributorDivisions[j] == hiddenTable.rows[i].cells[4].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else
					isMatchingRow = true;
				
				if (isMatchingRow)
				{
					if (bottlerSalesRoutes != null && bottlerSalesRoutes.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerSalesRoutes.length; j++)
							if (bottlerSalesRoutes[j] == hiddenTable.rows[i].cells[14].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow)
				{
					if (target == TARGETBOTTLERBUSINESSUNIT && 
						(bottlers != null && bottlers.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<bottlers.length; j++)
							if (bottlers[j] == hiddenTable.rows[i].cells[12].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (target == TARGETBOTTLERMARKETUNIT && 
						(bottlerBusinessUnits != null && bottlerBusinessUnits.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerBusinessUnits.length; j++)
							if (bottlerBusinessUnits[j] == hiddenTable.rows[i].cells[10].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (target == TARGETBOTTLERBRANCH &&
						(bottlerMarketUnits != null && bottlerMarketUnits.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerMarketUnits.length; j++)
							if (bottlerMarketUnits[j] == hiddenTable.rows[i].cells[8].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow == true)
				{
					results[rTotal++] = hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + hiddenTable.rows[i].cells[target + 1].firstChild.nodeValue;
					results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] = true;
				}
			}
			else if (target == TARGETDISTRIBUTORDIVISION || target == TARGETDISTRIBUTORDISTRICT || 
				target == TARGETSTORE)
			{
				if (bottlerBranches != null && bottlerBranches.length > 0)
				{
					for (var j=0; j<bottlerBranches.length; j++)
						if (bottlerBranches[j] == hiddenTable.rows[i].cells[6].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (bottlerMarketUnits != null && bottlerMarketUnits.length > 0)
				{
					for (var j=0; j<bottlerMarketUnits.length; j++)
						if (bottlerMarketUnits[j] == hiddenTable.rows[i].cells[8].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (bottlerBusinessUnits != null && bottlerBusinessUnits.length > 0)
				{
					for (var j=0; j<bottlerBusinessUnits.length; j++)
						if (bottlerBusinessUnits[j] == hiddenTable.rows[i].cells[10].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (bottlers != null && bottlers.length > 0)
				{
					for (var j=0; j<bottlers.length; j++)
						if (bottlers[j] == hiddenTable.rows[i].cells[12].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else
					isMatchingRow = true;

				if (isMatchingRow)
				{
					if (bottlerSalesRoutes != null && bottlerSalesRoutes.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerSalesRoutes.length; j++)
							if (bottlerSalesRoutes[j] == hiddenTable.rows[i].cells[14].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow)
				{
					if (target == TARGETDISTRIBUTORDISTRICT && 
						(distributorDivisions != null && distributorDivisions.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<distributorDivisions.length; j++)
							if (distributorDivisions[j] == hiddenTable.rows[i].cells[4].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (target == TARGETSTORE && 
						(distributorDistricts != null && distributorDistricts.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<distributorDistricts.length; j++)
							if (distributorDistricts[j] == hiddenTable.rows[i].cells[2].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow == true)
				{
					results[rTotal++] = unescape( hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + hiddenTable.rows[i].cells[target + 1].firstChild.nodeValue );
					// alert(results[rTotal - 1]);
					results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] = true;
				}
			}
			else if (target == TARGETBOTTLERSALESROUTE)
			{
				if (stores != null && stores.length > 0)
				{
					for (var j=0; j<stores.length; j++)
						if (stores[j] == hiddenTable.rows[i].cells[0].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (distributorDistricts != null && distributorDistricts.length > 0)
				{
					for (var j=0; j<distributorDistricts.length; j++)
						if (distributorDistricts[j] == hiddenTable.rows[i].cells[2].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (distributorDivisions != null && distributorDivisions.length > 0)
				{
					for (var j=0; j<distributorDivisions.length; j++)
						if (distributorDivisions[j] == hiddenTable.rows[i].cells[4].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else
					isMatchingRow = true;

				if (isMatchingRow)
				{
					if (bottlerBranches != null && bottlerBranches.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerBranches.length; j++)
							if (bottlerBranches[j] == hiddenTable.rows[i].cells[6].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (bottlerMarketUnits != null && bottlerMarketUnits.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerMarketUnits.length; j++)
							if (bottlerMarketUnits[j] == hiddenTable.rows[i].cells[8].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (bottlerBusinessUnits != null && bottlerBusinessUnits.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlerBusinessUnits.length; j++)
							if (bottlerBusinessUnits[j] == hiddenTable.rows[i].cells[10].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
					else if (bottlers != null && bottlers.length > 0)
					{
						isMatchingRow = false;
						for (var j=0; j<bottlers.length; j++)
							if (bottlers[j] == hiddenTable.rows[i].cells[12].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow == true)
				{
					results[rTotal++] = hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + hiddenTable.rows[i].cells[target + 1].firstChild.nodeValue;
					results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] = true;
				}
			}
		}

		return results;
	}

	function findFromProductDescriptions(target, productCategories, products, productPackages)
	{
		var hiddenTable = document.getElementById("productDescriptions");
		
		if (hiddenTable == null)
			return "__NODATA__";

		var results = new Array();
		var rTotal = 0;
		var isMatchingRow;
		
		for (var i=1; i<hiddenTable.rows.length; i++)
		{
			if (results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] == true)
				continue;

			isMatchingRow = false;
			if (target == TARGETPRODUCTCATEGORY || target == TARGETPRODUCT)
			{
				if (productPackages != null && productPackages.length > 0)
				{
					for (var j=0; j<productPackages.length; j++)
						if (productPackages[j] == hiddenTable.rows[i].cells[6].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else
					isMatchingRow = true;
				
				if (isMatchingRow)
				{
					if (target == TARGETPRODUCT && 
						(productCategories != null && productCategories.length > 0))
					{
						isMatchingRow = false;
						for (var j=0; j<productCategories.length; j++)
							if (productCategories[j] == hiddenTable.rows[i].cells[3].firstChild.nodeValue)
							{ 
								isMatchingRow = true; 
								break;
							}
					}
				}

				if (isMatchingRow == true)
				{
					if (target == TARGETPRODUCTCATEGORY)
					{
						results[rTotal++] = hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + hiddenTable.rows[i].cells[target + 2].firstChild.nodeValue;
					}
					else if (target == TARGETPRODUCT)
					{
						results[rTotal++] = hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + 
							hiddenTable.rows[i].cells[target + 2].firstChild.nodeValue + 
							" [" + hiddenTable.rows[i].cells[target + 1].firstChild.nodeValue + "]";
					}
					results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] = true;
				}
			}
			else if (target == TARGETPRODUCTPACKAGE)
			{
				if (products != null && products.length > 0)
				{
					for (var j=0; j<products.length; j++)
						if (products[j] == hiddenTable.rows[i].cells[0].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else if (productCategories != null && productCategories.length > 0)
				{
					for (var j=0; j<productCategories.length; j++)
						if (productCategories[j] == hiddenTable.rows[i].cells[3].firstChild.nodeValue)
						{ 
							isMatchingRow = true; 
							break;
						}
				}
				else
					isMatchingRow = true;


				if (isMatchingRow == true)
				{
					results[rTotal++] = unescape( hiddenTable.rows[i].cells[target].firstChild.nodeValue + "|" + hiddenTable.rows[i].cells[target + 1].firstChild.nodeValue );
					results["k" + hiddenTable.rows[i].cells[target].firstChild.nodeValue] = true;
				}
			}
		}

		return results;
	}
	
	function getCookieData(labelName) 
	{
		//from Danny Goodman
		var labelLen = labelName.length;
		// read cookie property only once for speed
		var cookieData = document.cookie;
		var cLen = cookieData.length;
		var i = 0;
		var cEnd;
		while (i < cLen) 
		{
			var j = i + labelLen;
			if (cookieData.substring(i,j) == labelName) 
			{
				cEnd = cookieData.indexOf(";",j);
				if (cEnd == -1) 
				{
					cEnd = cookieData.length;
				}
				return unescape(cookieData.substring(j+1, cEnd));
			}
			i++;
		}
		return "";
	}
	</script>

	<div class="mainContent">
		<portlet:actionURL var="actionURL"/>
		<form id="specifyParametersForm" class="formStyle1" method="post" action="<%=actionURL.toString()%>"
			onSubmit='checkedSubmit("specifyParametersForm")'>
			<input type="hidden" name="portletAction" value="specifyReportParameters"/>
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/reports/generate/specifyReportParameters.jsp")%>'/>

			<input type="hidden" name="changeSource" value="none"/>

			<table class="formTable">
				<tr>
					<td colspan="2">
						<label for="reportTypeSelect" class="formLabel1">Choose type of report:</label>
					</td>
					<td colspan="2">
						<select name="reportType" id="reportTypeSelect" onchange='updateForm(this.form)'>
							<option value="totals" <%=reportType != null && reportType.equalsIgnoreCase("totals") ? "selected=\"selected\"" : "" %>>Total Lost Sales</option>
							<option value="averages" <%=reportType != null && reportType.equalsIgnoreCase("averages") ? "selected=\"selected\"" : "" %>>Average Lost Sales</option>
							<option value="dailyevents" <%=reportType != null && reportType.equalsIgnoreCase("dailyevents") ? "selected=\"selected\"" : "" %>>Average Daily Events per Store</option>
						</select>
					</td>
				</tr>
				<tr>
					<td colspan="4">
					<hr noshade="noshade" color="white" size="1px"/>
					</td>
				</tr>
				<tr id="rowSummaryTarget">
					<td colspan="2">
						<label for="summaryTargetSelect" class="formLabel1">Select target to summarize lost sales by:</label>
					</td>
					<td colspan="2">
						<select name="summaryTarget" id="summaryTargetSelect" onchange='updateForm(this.form)'>
							<option value="store" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("store") ? "selected=\"selected\"" : "" %>>Store</option>
							<option value="bottler" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottler") ? "selected=\"selected\"" : "" %>>Bottler</option>
							<option value="bottlerbranch" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottlerbranch") ? "selected=\"selected\"" : "" %>>Bottler Branch</option>
							<option value="bottlersalesroute" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottlersalesroute") ? "selected=\"selected\"" : "" %>>Bottler Sales Route</option>
							<option value="distributordistrict" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("distributordistrict") ? "selected=\"selected\"" : "" %>>Distributor District</option>
							<option value="product" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("product") ? "selected=\"selected\"" : "" %>>Product</option>
							<option value="package" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("package") ? "selected=\"selected\"" : "" %>>Package</option>
							<option value="day" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("day") ? "selected=\"selected\"" : "" %>>Day</option>
							<option value="week" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("week") ? "selected=\"selected\"" : "" %>>Week</option>
							<option value="storetvss" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("storetvss") ? "selected=\"selected\"" : "" %>>Store (Traditional vs. Distributor.com)</option>
							<option value="storewithproduct" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("storewithproduct") ? "selected=\"selected\"" : "" %>>Store (with products)</option>
							</select>
					</td>
				</tr>
				<tr id="rowNumberOfWeeks">
					<td colspan="2">
						<label for="numberOfWeeks" class="formLabel1">Enter number of weeks:</label>					
					</td>
					<td colspan="2">
						<input type="text" name="numberOfWeeks" id="numberOfWeeksField" size="10" maxlength="3" value='<%=numberOfWeeks != null ? numberOfWeeks : "4"%>'/>
					</td>
				</tr>
				<tr id="rowTimePeriod">
					<td colspan="2">
						<label for="timePeriodSelect" class="formLabel1">Select time period:</label>					
					</td>
					<td colspan="2">
						<select name="timePeriod" id="timePeriodSelect" onchange='updateForm(this.form)'>
							<option value="week" <%=timePeriod != null && timePeriod.equalsIgnoreCase("week") ? "selected=\"selected\"" : "" %>>One week</option>
							<option value="month" <%=timePeriod != null && timePeriod.equalsIgnoreCase("month") ? "selected=\"selected\"" : "" %>>One month</option>
							<option value="year" <%=timePeriod != null && timePeriod.equalsIgnoreCase("year") ? "selected=\"selected\"" : "" %>>One year</option>
							<option value="ytd" <%=timePeriod != null && timePeriod.equalsIgnoreCase("ytd") ? "selected=\"selected\"" : "" %>>Year to date</option>
						</select>
					</td>
				</tr>
				<tr id="rowBreakOutByDay">
					<td colspan="2">
						<span class="formLabel1 formIndented">Break out results by day?</span>					
					</td>
					<td colspan="2">
						<input id="chooseDayBreakoutYes" type="radio" name="breakOutByDay" value="yes"
						<%=breakOutByDay != null && breakOutByDay.equalsIgnoreCase("yes") ? "checked=\"checked\"" : "" %>/>
						<label for="chooseDayBreakoutYes" class="formLabel1">Yes</label>
						<input id="chooseDayBreakoutNo" type="radio" name="breakOutByDay" value="no"
						<%=breakOutByDay == null || breakOutByDay.equalsIgnoreCase("no") ? "checked=\"checked\"" : "" %>/>
						<label for="chooseDayBreakoutNo" class="formLabel1">No</label>
					</td>
				</tr>
				<tr id="rowStartingDate">
					<td colspan="2">
						<label for="startingMonthSelect" class="formLabel1">Enter starting date<%=OutOfStockEvent.getEarliestEventDate() == null 
							? " (MM/DD/YY):" 
							: " (MM/DD/YY, with earliest events date being " + new SimpleDateFormat("MM'/'dd'/'yy").format(OutOfStockEvent.getEarliestEventDate()) + "):"%></label>					
					</td>
					<td colspan="2">
						<table>
							<tr>
								<td>
								<select name="startingMonth" id="startingMonthSelect" onchange="adjustDatePicker( this.form )">
									<option value="0" <%=startingMonth != null && startingMonth.equalsIgnoreCase("0") ? "selected=\"selected\"" : "" %>>01</option>
									<option value="1" <%=startingMonth != null && startingMonth.equalsIgnoreCase("1") ? "selected=\"selected\"" : "" %>>02</option>
									<option value="2" <%=startingMonth != null && startingMonth.equalsIgnoreCase("2") ? "selected=\"selected\"" : "" %>>03</option>
									<option value="3" <%=startingMonth != null && startingMonth.equalsIgnoreCase("3") ? "selected=\"selected\"" : "" %>>04</option>
									<option value="4" <%=startingMonth != null && startingMonth.equalsIgnoreCase("4") ? "selected=\"selected\"" : "" %>>05</option>
									<option value="5" <%=startingMonth != null && startingMonth.equalsIgnoreCase("5") ? "selected=\"selected\"" : "" %>>06</option>
									<option value="6" <%=startingMonth != null && startingMonth.equalsIgnoreCase("6") ? "selected=\"selected\"" : "" %>>07</option>
									<option value="7" <%=startingMonth != null && startingMonth.equalsIgnoreCase("7") ? "selected=\"selected\"" : "" %>>08</option>
									<option value="8" <%=startingMonth != null && startingMonth.equalsIgnoreCase("8") ? "selected=\"selected\"" : "" %>>09</option>
									<option value="9" <%=startingMonth != null && startingMonth.equalsIgnoreCase("9") ? "selected=\"selected\"" : "" %>>10</option>
									<option value="10" <%=startingMonth != null && startingMonth.equalsIgnoreCase("10") ? "selected=\"selected\"" : "" %>>11</option>
									<option value="11" <%=startingMonth != null && startingMonth.equalsIgnoreCase("11") ? "selected=\"selected\"" : "" %>>12</option>
								</select>
								</td>
								<td>
								<select name="startingDate" id="startingDateSelect" onchange="adjustDatePicker( this.form )" onfocus="adjustDatePicker( this.form )">
									<option value="1" <%=startingDate != null && startingDate.equalsIgnoreCase("1") ? "selected=\"selected\"" : "" %>>01</option>
									<option value="2" <%=startingDate != null && startingDate.equalsIgnoreCase("2") ? "selected=\"selected\"" : "" %>>02</option>
									<option value="3" <%=startingDate != null && startingDate.equalsIgnoreCase("3") ? "selected=\"selected\"" : "" %>>03</option>
									<option value="4" <%=startingDate != null && startingDate.equalsIgnoreCase("4") ? "selected=\"selected\"" : "" %>>04</option>
									<option value="5" <%=startingDate != null && startingDate.equalsIgnoreCase("5") ? "selected=\"selected\"" : "" %>>05</option>
									<option value="6" <%=startingDate != null && startingDate.equalsIgnoreCase("6") ? "selected=\"selected\"" : "" %>>06</option>
									<option value="7" <%=startingDate != null && startingDate.equalsIgnoreCase("7") ? "selected=\"selected\"" : "" %>>07</option>
									<option value="8" <%=startingDate != null && startingDate.equalsIgnoreCase("8") ? "selected=\"selected\"" : "" %>>08</option>
									<option value="9" <%=startingDate != null && startingDate.equalsIgnoreCase("9") ? "selected=\"selected\"" : "" %>>09</option>
									<option value="10" <%=startingDate != null && startingDate.equalsIgnoreCase("10") ? "selected=\"selected\"" : "" %>>10</option>
									<option value="11" <%=startingDate != null && startingDate.equalsIgnoreCase("11") ? "selected=\"selected\"" : "" %>>11</option>
									<option value="12" <%=startingDate != null && startingDate.equalsIgnoreCase("12") ? "selected=\"selected\"" : "" %>>12</option>
									<option value="13" <%=startingDate != null && startingDate.equalsIgnoreCase("13") ? "selected=\"selected\"" : "" %>>13</option>
									<option value="14" <%=startingDate != null && startingDate.equalsIgnoreCase("14") ? "selected=\"selected\"" : "" %>>14</option>
									<option value="15" <%=startingDate != null && startingDate.equalsIgnoreCase("15") ? "selected=\"selected\"" : "" %>>15</option>
									<option value="16" <%=startingDate != null && startingDate.equalsIgnoreCase("16") ? "selected=\"selected\"" : "" %>>16</option>
									<option value="17" <%=startingDate != null && startingDate.equalsIgnoreCase("17") ? "selected=\"selected\"" : "" %>>17</option>
									<option value="18" <%=startingDate != null && startingDate.equalsIgnoreCase("18") ? "selected=\"selected\"" : "" %>>18</option>
									<option value="19" <%=startingDate != null && startingDate.equalsIgnoreCase("19") ? "selected=\"selected\"" : "" %>>19</option>
									<option value="20" <%=startingDate != null && startingDate.equalsIgnoreCase("20") ? "selected=\"selected\"" : "" %>>20</option>
									<option value="21" <%=startingDate != null && startingDate.equalsIgnoreCase("21") ? "selected=\"selected\"" : "" %>>21</option>
									<option value="22" <%=startingDate != null && startingDate.equalsIgnoreCase("22") ? "selected=\"selected\"" : "" %>>22</option>
									<option value="23" <%=startingDate != null && startingDate.equalsIgnoreCase("23") ? "selected=\"selected\"" : "" %>>23</option>
									<option value="24" <%=startingDate != null && startingDate.equalsIgnoreCase("24") ? "selected=\"selected\"" : "" %>>24</option>
									<option value="25" <%=startingDate != null && startingDate.equalsIgnoreCase("25") ? "selected=\"selected\"" : "" %>>25</option>
									<option value="26" <%=startingDate != null && startingDate.equalsIgnoreCase("26") ? "selected=\"selected\"" : "" %>>26</option>
									<option value="27" <%=startingDate != null && startingDate.equalsIgnoreCase("27") ? "selected=\"selected\"" : "" %>>27</option>
									<option value="28" <%=startingDate != null && startingDate.equalsIgnoreCase("28") ? "selected=\"selected\"" : "" %>>28</option>
									<option value="29" <%=startingDate != null && startingDate.equalsIgnoreCase("29") ? "selected=\"selected\"" : "" %>>29</option>
									<option value="30" <%=startingDate != null && startingDate.equalsIgnoreCase("30") ? "selected=\"selected\"" : "" %>>30</option>
									<option value="31" <%=startingDate != null && startingDate.equalsIgnoreCase("31") ? "selected=\"selected\"" : "" %>>31</option>
								</select>
								</td>
								<td>
								<select name="startingYear" id="startingYearSelect" onchange="adjustDatePicker( this.form )">
									<option value="2006" <%=startingYear != null && startingYear.equalsIgnoreCase("2006") ? "selected=\"selected\"" : "" %>>2006</option>
									<option value="2007" <%=startingYear != null && startingYear.equalsIgnoreCase("2007") ? "selected=\"selected\"" : "" %>>2007</option>
									<option value="2008" <%=startingYear != null && startingYear.equalsIgnoreCase("2008") ? "selected=\"selected\"" : "" %>>2008</option>
									<option value="2009" <%=startingYear != null && startingYear.equalsIgnoreCase("2009") ? "selected=\"selected\"" : "" %>>2009</option>
									<option value="2010" <%=startingYear != null && startingYear.equalsIgnoreCase("2010") ? "selected=\"selected\"" : "" %>>2010</option>
									<option value="2011" <%=startingYear != null && startingYear.equalsIgnoreCase("2011") ? "selected=\"selected\"" : "" %>>2011</option>
									<option value="2012" <%=startingYear != null && startingYear.equalsIgnoreCase("2012") ? "selected=\"selected\"" : "" %>>2012</option>
								</select>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr id="rowResultSize">
					<td colspan="2">
						<label for="resultSizeSelect" class="formLabel1">Restrict size of results?</label>					
					</td>
					<td colspan="2">
						<select name="resultSize" id="resultSizeSelect">
							<option value="top25" <%=resultSize != null && resultSize.equalsIgnoreCase("top25") ? "selected=\"selected\"" : "" %>>Show top 25 only</option>
							<option value="top50" <%=resultSize != null && resultSize.equalsIgnoreCase("top50") ? "selected=\"selected\"" : "" %>>Show top 50 only</option>
							<option value="all" <%=resultSize != null && resultSize.equalsIgnoreCase("all") ? "selected=\"selected\"" : "" %>>Show all</option>
						</select>
					</td>
				</tr>
				<tr id="rowLostSalesMetric">
					<td colspan="2">
						<label for="lostSalesMetricSelect" class="formLabel1">Display lost sales by dollars or units?</label>					
					</td>
					<td colspan="2">
						<select name="lostSalesMetric" id="lostSalesMetricSelect">
							<option value="amount" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("amount") ? "selected=\"selected\"" : "" %>>Dollars only</option>
							<option value="quantity" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("quantity") ? "selected=\"selected\"" : "" %>>Units only</option>
							<option value="all" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("all") ? "selected=\"selected\"" : "" %>>Both dollars and units</option>
						</select>
					</td>
				</tr>
				<tr>
					<td colspan="4">
					<hr noshade="noshade" color="white" size="1px"/>
					</td>
				</tr>
				<tr>
					<td colspan="4">
						<label for="productCategorySelect" class="formLabel2">Select Beverage Category / Product [UPC]:</label>					
					</td>
				</tr>
				<tr>
					<td valign="top">
						<select name="productCategory" id="productCategorySelect" multiple="multiple" size="10"
							onchange='this.form.changeSource.value="productCategory"; updateForm(document.getElementById("specifyParametersForm"), "productCategory")'>
							<%
							pkids = ProductDescription.find(null, ProductDescription.PRODUCTCATEGORY, productCategoryPKID, productPKID, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), productCategoryPKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
						</select>
					</td>
					<td colspan="3" valign="top">
						<select name="product" id="productSelect" multiple="multiple" size="10" style="display : none"
							onchange='this.form.changeSource.value="product"; updateForm(document.getElementById("specifyParametersForm"), "product")'>
						<%
						pkids = ProductDescription.find(null, ProductDescription.PRODUCT, productCategoryPKID, productPKID, null);
						if (pkids != null)
						{
							for (int i=0; i<pkids.length; i++)
							{
								boolean selected = isSelected(pk(pkids[i]), productPKID);
						%>
							<option value="<%=pkids[i]%>"
							<%=selected ? "selected=\"selected\"" : "" %>>
								<%=id(pkids[i])%>
							</option>
						<%
							}
						}
						%>
						</select>
					</td>
				</tr>
				<tr>
					<td class="formLabelCell" colspan="4">
						<label for="distributorDivisionSelect" class="formLabel2">Select Distributor Division / District / Store:</label>					
					</td>
				</tr>
				<tr>
					<td valign="top">
						<select name="distributorDivision" id="distributorDivisionSelect" multiple="multiple" size="10" 
							onchange='this.form.changeSource.value="distributorDivision"; updateForm(document.getElementById("specifyParametersForm"), "distributorDivision")'>
							<option style="color : red" value="-1|DISTRIBUTOR.COM"
							<%=isSelected("-1", distributorDivisionPKID) ? "selected=\"selected\"" : "" %>>
								DISTRIBUTOR.COM
							</option>
						<%
						pkids = StoreLocation.find(null, StoreLocation.DISTRIBUTORDIVISION, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
							null, null, null, null, null);
						if (pkids != null)
						{
							for (int i=0; i<pkids.length; i++)
							{
								boolean selected = isSelected(pk(pkids[i]), distributorDivisionPKID);
						%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
						<%
							}
						}
						%>
						</select>
					</td>
					<td valign="top">
							<select name="distributorDistrict" id="distributorDistrictSelect" multiple="multiple" size="10" style="display : none"
								onchange='this.form.changeSource.value="distributorDistrict"; updateForm(document.getElementById("specifyParametersForm"), "distributorDistrict")'>
							<%
							pkids = StoreLocation.find(null, StoreLocation.DISTRIBUTORDISTRICT, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
								null, null, null, null, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), distributorDistrictPKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
							</select>
					</td>
					<td valign="top">
							<select name="store" id="storeSelect" multiple="multiple" size="10" style="display : none"
								onchange='this.form.changeSource.value="store"; updateForm(document.getElementById("specifyParametersForm"), "store")'>
							<%
							pkids = StoreLocation.find(null, StoreLocation.STORE, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
								null, null, null, null, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), storePKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
							</select>
					</td>
					<td>&nbsp;</td>
				</tr>
				<tr>
					<td class="formLabelCell" colspan="4">
						<label for="bottlerSelect" class="formLabel2">Select Bottler / Region / Market Unit / Branch:</label>					
					</td>
				</tr>
				<tr>
					<td valign="top">
						<select name="bottler" id="bottlerSelect" multiple="multiple" size="10" 
							onchange='this.form.changeSource.value="bottler"; updateForm(document.getElementById("specifyParametersForm"), "bottler")'>
						<%
						pkids = StoreLocation.find(null, StoreLocation.BOTTLER, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
							bottlerPKID, bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, null);
						if (pkids != null)
						{
							for (int i=0; i<pkids.length; i++)
							{
								boolean selected = isSelected(pk(pkids[i]), bottlerPKID);
						%>
							<option value="<%=pkids[i]%>"
							<%=selected ? "selected=\"selected\"" : "" %>>
								<%=id(pkids[i])%>
							</option>
						<%
							}
						}
						%>
						</select>
					</td>
					<td valign="top">
							<select name="bottlerBusinessUnit" id="bottlerBusinessUnitSelect" multiple="multiple" size="10" style="display : none"
								onchange='this.form.changeSource.value="bottlerBusinessUnit"; updateForm(document.getElementById("specifyParametersForm"), "bottlerBusinessUnit")'>
							<%
							pkids = StoreLocation.find(null, StoreLocation.BOTTLERBUSINESSUNIT, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
								bottlerPKID, bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), bottlerBusinessUnitPKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
							</select>
					</td>
					<td valign="top">
							<select name="bottlerMarketUnit" id="bottlerMarketUnitSelect" multiple="multiple" size="10" style="display : none"
								onchange='this.form.changeSource.value="bottlerMarketUnit"; updateForm(document.getElementById("specifyParametersForm"), "bottlerMarketUnit")'>
							<%
							pkids = StoreLocation.find(null, StoreLocation.BOTTLERMARKETUNIT, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
								bottlerPKID, bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), bottlerMarketUnitPKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
							</select>
					</td>
					<td valign="top">
							<select name="bottlerBranch" id="bottlerBranchSelect" multiple="multiple" size="10" style="display : none"
								onchange='this.form.changeSource.value="bottlerBranch"; updateForm(document.getElementById("specifyParametersForm"), "bottlerBranch")'>
							<%
							pkids = StoreLocation.find(null, StoreLocation.BOTTLERBRANCH, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
								bottlerPKID, bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, null);
							if (pkids != null)
							{
								for (int i=0; i<pkids.length; i++)
								{
									boolean selected = isSelected(pk(pkids[i]), bottlerBranchPKID);
							%>
								<option value="<%=pkids[i]%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=id(pkids[i])%>
								</option>
							<%
								}
							}
							%>
							</select>
					</td>
				</tr>
				<tr>
					<td class="formLabelCell" colspan="4">
						<label for="bottlerSalesRouteSelect" class="formLabel2">Select Bottler Sales Route:</label>					
					</td>
				</tr>
				<tr>
					<td colspan="4" valign="top">
						<select name="bottlerSalesRoute" id="bottlerSalesRouteSelect" multiple="multiple" size="10">
						<%
						// onchange='this.form.changeSource.value="bottlerSalesRoute"; checkedSubmit(this.form)'
						pkids = StoreLocation.find(null, StoreLocation.BOTTLERSALESROUTE, distributorDivisionPKID, distributorDistrictPKID, storePKID, 
							bottlerPKID, bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, bottlerSalesRoutePKID);
						if (pkids != null)
						{
							for (int i=0; i<pkids.length; i++)
							{
								boolean selected = isSelected(pk(pkids[i]), bottlerSalesRoutePKID);
						%>
							<option value="<%=pkids[i]%>"
							<%=selected ? "selected=\"selected\"" : "" %>>
								<%=id(pkids[i])%>
							</option>
						<%
							}
						}
						%>
						</select>
					</td>
				</tr>
				<tr>
					<td class="formLabelCell" colspan="4">
						<label for="productPackageSelect" class="formLabel2">Select Product Package:</label>
					</td>
				</tr>
				<tr>
					<td colspan="4" valign="top">
						<select name="productPackage" id="productPackageSelect" multiple="multiple" size="10">
						<%
						pkids = ProductDescription.find(null, ProductDescription.PRODUCTPACKAGE, productCategoryPKID, productPKID, productPackagePKID);
						if (pkids != null)
						{
							for (int i=0; i<pkids.length; i++)
							{
								boolean selected = isSelected(pk(pkids[i]), productPackagePKID);
						%>
							<option value="<%=pkids[i]%>"
							<%=selected ? "selected=\"selected\"" : "" %>>
								<%=id(pkids[i])%>
							</option>
						<%
							}
						}
						%>
						</select>
					</td>
				</tr>
				<tr>
					<td>&nbsp;</td>
					<td>
						<input type="button" name="reset" value="Reset Constraints" id="resetConstraints" class="formButton1" onclick='clearConstraints(this.form); return false;'/>
					</td>
					<td>
						<input type="submit" name="generateReportSubmit" value="Generate Report" id="generateReport" class="formButton1"/>
					</td>
					<td>&nbsp;</td>
				</tr>
			</table>
		</form>

	</div>

	<%
	ListOrderedMap storeLocations = StoreLocation.findAll(null);
	if (storeLocations != null)
	{
	%>
	<table id="storeLocations" class="noDisplay">
	<%
		Set rowKeys = storeLocations.keySet();
		Iterator iterator = rowKeys.iterator();
		
		boolean namesIdentified = false; 
		while (iterator.hasNext())
		{
			ListOrderedMap row = (ListOrderedMap) storeLocations.get(iterator.next());
			if (!namesIdentified)
			{
				List columnNames = row.keyList();
	%>
		<tr>
	<%
				for (int i=0; i<columnNames.size(); i++)
				{
	%>
			<th><%=columnNames.get(i).toString().toUpperCase()%></th>
	<%				
				}
				namesIdentified = true;
	%>
		</tr>
	<%
			}
			
			List columnValues = row.valueList();
	%>
		<tr>
	<%
			for (int i=0; i<columnValues.size(); i++)
			{
	%>
			<th><%=columnValues.get(i).toString()%></th>
	<%				
			}
	%>
		</tr>
	<%
		}
	%>
	</table>
	<%
	}
	%>

	<%
	ListOrderedMap productDescriptions = ProductDescription.findAll(null);
	if (productDescriptions != null)
	{
	%>
	<table id="productDescriptions" class="noDisplay">
	<%
		Set rowKeys = productDescriptions.keySet();
		Iterator iterator = rowKeys.iterator();
		
		boolean namesIdentified = false; 
		while (iterator.hasNext())
		{
			ListOrderedMap row = (ListOrderedMap) productDescriptions.get(iterator.next());
			if (!namesIdentified)
			{
				List columnNames = row.keyList();
	%>
		<tr>
	<%
				for (int i=0; i<columnNames.size(); i++)
				{
	%>
			<th><%=columnNames.get(i).toString().toUpperCase()%></th>
	<%				
				}
				namesIdentified = true;
	%>
		</tr>
	<%
			}
			
			List columnValues = row.valueList();
	%>
		<tr>
	<%
			for (int i=0; i<columnValues.size(); i++)
			{
	%>
			<th><%=columnValues.get(i).toString()%></th>
	<%				
			}
	%>
		</tr>
	<%
		}
	%>
	</table>
	<%
	}
	%>

	<script language="JavaScript">
	updateForm(document.getElementById('specifyParametersForm'), null);
	if (document.cookie != null)
	{
		var scrollTop = getCookieData("spftop");
		if (scrollTop != null)
		{
			window.scrollTo(0, scrollTop);
		}
	}
	</script>
	<%@ include file="/common/footer.jsp"%>
