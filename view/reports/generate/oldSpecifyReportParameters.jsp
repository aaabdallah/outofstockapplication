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
%>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Reports"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Specify Report Parameters"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<script language="JavaScript">
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
	
	function adjustFormForWeek(objForm)
	{
		if (objForm.summaryTargetSelect.options[objForm.summaryTargetSelect.selectedIndex].value == 'week')
			objForm.submit();
		else if (objForm.numberOfWeeksField != null)
			objForm.submit();
	}
	
	</script>

	<div class="mainContent">
		<portlet:actionURL var="actionURL"/>
		<form id="form1" class="formStyle1" method="post" action="<%=actionURL.toString()%>">
			<input type="hidden" name="portletAction" value="specifyReportParameters"/>
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/reports/generate/specifyReportParameters.jsp")%>'/>

			<input type="hidden" name="changeSource" value="none"/>

			<table class="formTable">
				<tr>
					<td colspan="2">
						<label for="reportTypeSelect" class="formLabel1">Choose type of report:</label>
					</td>
					<td colspan="2">
						<select name="reportType" id="reportTypeSelect" onchange='this.form.submit()'>
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
				<%
				if (reportType == null || !reportType.equals("dailyevents"))
				{
				%>
				<tr>
					<td colspan="2">
						<label for="summaryTargetSelect" class="formLabel1">Select target to summarize lost sales by:</label>
					</td>
					<td colspan="2">
						<select name="summaryTarget" id="summaryTargetSelect" onchange="adjustFormForWeek( this.form )">
							<% if (reportType == null || reportType.equalsIgnoreCase("totals")) { %>
							<option value="store" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("store") ? "selected=\"selected\"" : "" %>>Store</option>
							<% } else { %>
							<option value="store" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("store") ? "selected=\"selected\"" : "" %>>Store (Average over all stores)</option>
							<option value="storetvss" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("storetvss") ? "selected=\"selected\"" : "" %>>Store (Traditional vs. Distributor.com)</option>
							<% } %>
							<option value="bottler" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottler") ? "selected=\"selected\"" : "" %>>Bottler</option>
							<option value="bottlerbranch" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottlerbranch") ? "selected=\"selected\"" : "" %>>Bottler Branch</option>
							<option value="bottlersalesroute" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("bottlersalesroute") ? "selected=\"selected\"" : "" %>>Bottler Sales Route</option>
							<option value="distributordistrict" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("distributordistrict") ? "selected=\"selected\"" : "" %>>Distributor District</option>
							<option value="product" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("product") ? "selected=\"selected\"" : "" %>>Product</option>
							<option value="package" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("package") ? "selected=\"selected\"" : "" %>>Package</option>
							<% if (reportType == null || reportType.equalsIgnoreCase("totals")) { %>
							<option value="day" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("day") ? "selected=\"selected\"" : "" %>>Day</option>
							<option value="week" <%=summaryTarget != null && summaryTarget.equalsIgnoreCase("week") ? "selected=\"selected\"" : "" %>>Week</option>
							<% } %>
						</select>
					</td>
				</tr>
				<%
				}
				%>
				<%
				if (summaryTarget != null && summaryTarget.equals("week"))
				{
				%>
				<tr>
					<td colspan="2">
						<label for="numberOfWeeks" class="formLabel1">Enter number of weeks:</label>					
					</td>
					<td colspan="2">
						<input type="text" name="numberOfWeeks" id="numberOfWeeksField" size="10" maxlength="3" value='<%=numberOfWeeks != null ? numberOfWeeks : "4"%>'/>
					</td>
				</tr>
				<%
				} else {
				%>
				<tr>
					<td colspan="2">
						<label for="timePeriodSelect" class="formLabel1">Select time period:</label>					
					</td>
					<td colspan="2">
						<select name="timePeriod" id="timePeriodSelect" 
							onchange='this.form.submit()'>
							<option value="week" <%=timePeriod != null && timePeriod.equalsIgnoreCase("week") ? "selected=\"selected\"" : "" %>>One week</option>
							<option value="month" <%=timePeriod != null && timePeriod.equalsIgnoreCase("month") ? "selected=\"selected\"" : "" %>>One month</option>
							<option value="year" <%=timePeriod != null && timePeriod.equalsIgnoreCase("year") ? "selected=\"selected\"" : "" %>>One year</option>
							<option value="ytd" <%=timePeriod != null && timePeriod.equalsIgnoreCase("ytd") ? "selected=\"selected\"" : "" %>>Year to date</option>
						</select>
					</td>
				</tr>
				<%
					if (reportType == null || reportType.equalsIgnoreCase("totals")) 
					{
						if (timePeriod == null || timePeriod.equalsIgnoreCase("week"))
						{
				%>
				<tr>
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
				<%
						}
					}
				}
				%>
				<%
				if (timePeriod == null || !timePeriod.equals("ytd") || (summaryTarget != null && summaryTarget.equals("week")))
				{
				%>
				<tr>
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
				<%
				}
				%>
				<%
				if (reportType == null || reportType.equalsIgnoreCase("totals") || reportType.equalsIgnoreCase("dailyevents")) 
				{
					if (summaryTarget == null || !summaryTarget.equals("week") || reportType.equalsIgnoreCase("dailyevents"))
					{
				%>
				<tr>
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
				<%
					}
				} else {
				%>
				<input type="hidden" name="resultSize" value="tospecifiedweeks"/>
				<% 
				} 
				%>
				<%
				if (reportType == null || !reportType.equalsIgnoreCase("dailyevents"))
				{
				%>
				<tr>
					<td colspan="2">
						<label for="lostSalesMetricSelect" class="formLabel1">Display lost sales by dollars or units?</label>					
					</td>
					<td colspan="2">
						<select name="lostSalesMetric" id="lostSalesMetricSelect">
						<%
						if (reportType == null || !reportType.equals("averages"))
						{
						%>
							<option value="all" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("all") ? "selected=\"selected\"" : "" %>>Both dollars and units</option>
						<%
						}
						%>
							<option value="amount" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("amount") ? "selected=\"selected\"" : "" %>>Dollars only</option>
							<option value="quantity" <%=lostSalesMetric != null && lostSalesMetric.equalsIgnoreCase("quantity") ? "selected=\"selected\"" : "" %>>Units only</option>
						</select>
					</td>
				</tr>
				<%
				}
				%>
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
						<select name="productCategory" id="productCategorySelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="productCategory"; this.form.submit()'>
						<%
						cache = ProductCategory.getCache(null);
						if (cache != null) 
						{
							beans = cache.getBeans();
							if (beans != null)
							{
								for (int i=0; i<beans.size(); i++)
								{
									ProductCategory productCategory = (ProductCategory) beans.getValue(i);
									boolean selected = isSelected(productCategory.getPrimaryKey().toString(), productCategoryPKID);
						%>
								<option value="<%=pkID(productCategory.getPrimaryKey().toString(), productCategory.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=productCategory.getName()%>
								</option>
						<%
								} // for
							} // if (beans != null)
						} // if (cache != null) 
						%>
						</select>
					</td>
					<td colspan="3" valign="top">
						<%
						if (productCategoryPKID != null)
						{
						%>
							<select name="product" id="productSelect" multiple="multiple" size="10">
							<%
							beans = Product.findByProductCategory(null, convertStringPKIDsToIntegerPKs(productCategoryPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									Product product = (Product) beans.getValue(i);
									boolean selected = isSelected(product.getPrimaryKey().toString(), productPKID);
							%>
								<option value="<%=pkID(product.getPrimaryKey().toString(), product.getDescription())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=product.getDescription()%> [<%=product.getUpcId()%>]
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>						
					</td>
				</tr>
				<tr>
					<td class="formLabelCell" colspan="4">
						<label for="distributorDivisionSelect" class="formLabel2">Select Distributor Division / District / Store:</label>					
					</td>
				</tr>
				<tr>
					<td valign="top">
						<select name="distributorDivision" id="distributorDivisionSelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="distributorDivision"; this.form.submit()'>
						<%
						cache = DistributorDivision.getCache(null);
						if (cache != null) 
						{
							// NOTE THAT -1 is SPECIFICALLY USED TO DENOTE DISTRIBUTOR.COM - no other 
							// division should have this value as the primary key
						%>
							<option value="-1|DISTRIBUTOR.COM"
							<%=isSelected("-1", distributorDivisionPKID) ? "selected=\"selected\"" : "" %>>
								DISTRIBUTOR.COM
							</option>
						<%
							beans = cache.getBeans();
							if (beans != null)
							{
								for (int i=0; i<beans.size(); i++)
								{
									DistributorDivision distributorDivision = (DistributorDivision) beans.getValue(i);
									boolean selected = isSelected(distributorDivision.getPrimaryKey().toString(), distributorDivisionPKID);
						%>
								<option value="<%=pkID(distributorDivision.getPrimaryKey().toString(), distributorDivision.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=distributorDivision.getName()%>
								</option>
						<%
								} // for
							} // if (beans != null)
						} // if (cache != null) 
						%>
						</select>
					</td>
					<td valign="top">
						<%
						if (distributorDivisionPKID != null && 
							!(distributorDivisionPKID.length == 1 && pk(distributorDivisionPKID[0]).equalsIgnoreCase("-1")))
						{
						%>
							<select name="distributorDistrict" id="distributorDistrictSelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="distributorDistrict"; this.form.submit()'>
							<%
							beans = DistributorDistrict.findByDistributorDivision(null, convertStringPKIDsToIntegerPKs(distributorDivisionPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									DistributorDistrict distributorDistrict = (DistributorDistrict) beans.getValue(i);
									boolean selected = isSelected(distributorDistrict.getPrimaryKey().toString(), distributorDistrictPKID);
							%>
								<option value="<%=pkID(distributorDistrict.getPrimaryKey().toString(), distributorDistrict.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=distributorDistrict.getName()%>
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>
					</td>
					<td valign="top">
						<%
						if (distributorDistrictPKID != null)
						{
						%>
							<select name="store" id="storeSelect" multiple="multiple" size="10">
							<%
							beans = Store.findByDistributorDistrict(null, convertStringPKIDsToIntegerPKs(distributorDistrictPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									Store store = (Store) beans.getValue(i);
									boolean selected = isSelected(store.getPrimaryKey().toString(), storePKID);
							%>
								<option value="<%=pkID(store.getPrimaryKey().toString(), store.getId().toString())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=store.getId().intValue()%>
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>
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
						<select name="bottler" id="bottlerSelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="bottler"; this.form.submit()'>
						<%
						cache = Bottler.getCache(null);
						if (cache != null) 
						{
							beans = cache.getBeans();
							if (beans != null)
							{
								for (int i=0; i<beans.size(); i++)
								{
									Bottler bottler = (Bottler) beans.getValue(i);
									boolean selected = isSelected(bottler.getPrimaryKey().toString(), bottlerPKID);
						%>
								<option value="<%=pkID(bottler.getPrimaryKey().toString(), bottler.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=bottler.getName()%>
								</option>
						<%
								} // for
							} // if (beans != null)
						} // if (cache != null) 
						%>
						</select>
					</td>
					<td valign="top">
						<%
						if (bottlerPKID != null)
						{
						%>
							<select name="bottlerBusinessUnit" id="bottlerBusinessUnitSelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="bottlerBusinessUnit"; this.form.submit()'>
							<%
							beans = BottlerBusinessUnit.findByBottler(null, convertStringPKIDsToIntegerPKs(bottlerPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									BottlerBusinessUnit bottlerBusinessUnit = (BottlerBusinessUnit) beans.getValue(i);
									boolean selected = isSelected(bottlerBusinessUnit.getPrimaryKey().toString(), bottlerBusinessUnitPKID);
							%>
								<option value="<%=pkID(bottlerBusinessUnit.getPrimaryKey().toString(), bottlerBusinessUnit.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=bottlerBusinessUnit.getName()%>
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>
					</td>
					<td valign="top">
						<%
						if (bottlerBusinessUnitPKID != null)
						{
						%>
							<select name="bottlerMarketUnit" id="bottlerMarketUnitSelect" multiple="multiple" size="10" onchange='this.form.changeSource.value="bottlerMarketUnit"; this.form.submit()'>
							<%
							beans = BottlerMarketUnit.findByBottlerBusinessUnit(null, convertStringPKIDsToIntegerPKs(bottlerBusinessUnitPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									BottlerMarketUnit bottlerMarketUnit = (BottlerMarketUnit) beans.getValue(i);
									boolean selected = isSelected(bottlerMarketUnit.getPrimaryKey().toString(), bottlerMarketUnitPKID);
							%>
								<option value="<%=pkID(bottlerMarketUnit.getPrimaryKey().toString(), bottlerMarketUnit.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=bottlerMarketUnit.getName()%>
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>
					</td>
					<td valign="top">
						<%
						if (bottlerMarketUnitPKID != null)
						{
						%>
							<select name="bottlerBranch" id="bottlerBranchSelect" multiple="multiple" size="10">
							<%
							beans = BottlerBranch.findByBottlerMarketUnit(null, convertStringPKIDsToIntegerPKs(bottlerMarketUnitPKID));
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									BottlerBranch bottlerBranch = (BottlerBranch) beans.getValue(i);
									boolean selected = isSelected(bottlerBranch.getPrimaryKey().toString(), bottlerBranchPKID);
							%>
								<option value="<%=pkID(bottlerBranch.getPrimaryKey().toString(), bottlerBranch.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=bottlerBranch.getName()%>
								</option>
							<%
								} // for
							} // if
							%>
							</select>
						<%
						}
						%>
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
						cache = BottlerSalesRoute.getCache(null);
						if (cache != null) 
						{
							beans = cache.getBeans();
							if (beans != null)
							{
								for (int i=0; i<beans.size(); i++)
								{
									BottlerSalesRoute bottlerSalesRoute = (BottlerSalesRoute) beans.getValue(i);
									boolean selected = isSelected(bottlerSalesRoute.getPrimaryKey().toString(), bottlerSalesRoutePKID);
						%>
								<option value="<%=pkID(bottlerSalesRoute.getPrimaryKey().toString(), bottlerSalesRoute.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=bottlerSalesRoute.getName()%>
								</option>
						<%
								} // for
							} // if (beans != null)
						} // if (cache != null) 
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
						cache = ProductPackage.getCache(null);
						if (cache != null) 
						{
							beans = cache.getBeans();
							if (beans != null) 
							{
								for (int i=0; i<beans.size(); i++)
								{
									ProductPackage productPackage = (ProductPackage) beans.getValue(i);
									boolean selected = isSelected(productPackage.getPrimaryKey().toString(), productPackagePKID);
						%>
								<option value="<%=pkID(productPackage.getPrimaryKey().toString(), productPackage.getName())%>"
								<%=selected ? "selected=\"selected\"" : "" %>>
									<%=productPackage.getName()%>
								</option>
						<%
								} // for
							} // if (beans != null)
						} // if (cache != null) 
						%>
						</select>
					</td>
				</tr>
				<tr>
					<td colspan="2">&nbsp;</td>
					<td>
						<input type="submit" name="generateReportSubmit" value="Generate Report" id="generateReport" class="formButton1"/>
					</td>
					<td>&nbsp;</td>
				</tr>
			</table>
		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>
