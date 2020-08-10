<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<%@page import="net.btlcpy.outofstock.persistence.beans.OutOfStockEvent"%>
<%@page import="org.apache.commons.collections.map.ListOrderedMap"%>
<%@page import="java.sql.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Locale"%>
<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>

<%
String startingYear = renderRequest.getParameter("startingYear");
String startingMonth = renderRequest.getParameter("startingMonth");
String startingDate = renderRequest.getParameter("startingDate");
String endingYear = renderRequest.getParameter("endingYear");
String endingMonth = renderRequest.getParameter("endingMonth");
String endingDate = renderRequest.getParameter("endingDate");
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
		endingYear = startingYear;
		endingMonth = startingMonth;
		endingDate = startingDate;
	}
}
%>
	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Manage Event Data"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<script language="JavaScript">
	// Function for keeping the month, date, and year fields of a simple trio of dropdown lists for
	// selecting a date consistent with each other.
	// Author: Ahmed A. Abd-Allah.
	// src='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/scripts/adjustDatePicker.js") %>'
	function adjustDatePicker(yearSelect, monthSelect, dateSelect)
	{
		// get month, day, & year.
		var month = parseInt(monthSelect.options[monthSelect.selectedIndex].value);
		var date = parseInt(dateSelect.options[dateSelect.selectedIndex].value);
		var year = parseInt(yearSelect.options[yearSelect.selectedIndex].value);
	
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
		if (maxDateForMonth < dateSelect.options.length)
		{
			for (var i=dateSelect.options.length; i>maxDateForMonth; i--)
				dateSelect.remove(i-1);
		}
		// if (x > current size of day menu) remove as many missing elements as necessary 
		if (maxDateForMonth > dateSelect.options.length)
		{
			for (var i=dateSelect.options.length; i<maxDateForMonth; i++)
			{
				var newOption = new Option("" + (i+1), "" + (i+1));
				// the add method is non-uniform across IE and Firefox, so we use the old
				// way of adding elements
				// dateSelect.add(newOption); // IE compatible only
				// dateSelect.add(newOption, null); // Firefox compatible only
				dateSelect.options[i] = newOption;
			}
		}
	
		// move date to within valid range for month if necessary
		if (date > maxDateForMonth)
		{
			date = maxDateForMonth;
			dateSelect.selectedIndex = maxDateForMonth-1;
		} 
	
		fullDate = new Date(year, month, date);
		//alert(fullDate.toDateString() + ", Max date for month is " + maxDateForMonth +
		//	", Number of dates in select is " + dateSelect.options.length);
	}
	</script>

	<div class="mainContent">
		<portlet:actionURL var="actionURL"/>
		
		<table class="formTable">
			<tr>
				<td>
					<form id="uploadForm" class="formStyle1" enctype="multipart/form-data" method="post" action="<%=actionURL.toString()%>">
					<input type="hidden" name="portletAction" value="uploadEvents"/>
					<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/eventManagement/index.jsp")%>'/>
					<p>To upload a file containing a new list of "out of stock" events (in Excel spreadsheet format):</p>
					<label for="file" class="formLabel1">Select a file:</label><input id="file" type="file" name="file"/><br/>
					<input type="submit" value="Upload" class="formButton1"/><br/>
					</form>
				</td>
			</tr>
			<tr>
				<td>
					<form id="deleteForm" class="formStyle1" method="post" action="<%=actionURL.toString()%>">
					<input type="hidden" name="portletAction" value="deleteEventsInInterval"/>
					<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/eventManagement/index.jsp")%>'/>
					<p>To delete all "out of stock" events in a particular interval of time (consult list below to see dates for which data exists):</p>
					<br/>
					
					<table><tr>
					<td valign="top" style="padding-right : 5em">

					<p>Currently stored data intervals (YY-MM-DD)</p>
					<p class="intervalDisplay">
					<%
						ListOrderedMap intervals = OutOfStockEvent.getEventDateIntervals(null);
						if (intervals == null)
						{
					%>
						-- No intervals at present --
					<% 	} else {
							for (int i = 0; i < intervals.size(); i++)
							{
					%>
							<%=intervals.get(i).toString()%> to <%=intervals.getValue(i).toString()%><br/> 
					<% 	
							}
						} 
					%>
					</p>
					</td>
					<td valign="top">
					<p>Pick the start of the interval:</p>
					<table style="margin-left : 1em;">
						<tr>
							<td>
							<select name="startingYear" id="startingYearSelect" 
								onchange="adjustDatePicker( this.form.startingYearSelect, this.form.startingMonthSelect, this.form.startingDateSelect )">
								<option value="2006" <%=startingYear != null && startingYear.equalsIgnoreCase("2006") ? "selected=\"selected\"" : "" %>>2006</option>
								<option value="2007" <%=startingYear != null && startingYear.equalsIgnoreCase("2007") ? "selected=\"selected\"" : "" %>>2007</option>
								<option value="2008" <%=startingYear != null && startingYear.equalsIgnoreCase("2008") ? "selected=\"selected\"" : "" %>>2008</option>
								<option value="2009" <%=startingYear != null && startingYear.equalsIgnoreCase("2009") ? "selected=\"selected\"" : "" %>>2009</option>
								<option value="2010" <%=startingYear != null && startingYear.equalsIgnoreCase("2010") ? "selected=\"selected\"" : "" %>>2010</option>
								<option value="2011" <%=startingYear != null && startingYear.equalsIgnoreCase("2011") ? "selected=\"selected\"" : "" %>>2011</option>
								<option value="2012" <%=startingYear != null && startingYear.equalsIgnoreCase("2012") ? "selected=\"selected\"" : "" %>>2012</option>
							</select>
							</td>
							<td>
							<select name="startingMonth" id="startingMonthSelect" 
								onchange="adjustDatePicker( this.form.startingYearSelect, this.form.startingMonthSelect, this.form.startingDateSelect )">
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
							<select name="startingDate" id="startingDateSelect" 
								onchange="adjustDatePicker( this.form.startingYearSelect, this.form.startingMonthSelect, this.form.startingDateSelect )" 
								onfocus="adjustDatePicker( this.form.startingYearSelect, this.form.startingMonthSelect, this.form.startingDateSelect )">
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
						</tr>
					</table>
					
					<p>Pick the end of the interval:</p>
					<table style="margin-left : 1em;">
						<tr>
							<td>
							<select name="endingYear" id="endingYearSelect"
								onchange="adjustDatePicker( this.form.endingYearSelect, this.form.endingMonthSelect, this.form.endingDateSelect )">
								<option value="2006" <%=endingYear != null && endingYear.equalsIgnoreCase("2006") ? "selected=\"selected\"" : "" %>>2006</option>
								<option value="2007" <%=endingYear != null && endingYear.equalsIgnoreCase("2007") ? "selected=\"selected\"" : "" %>>2007</option>
								<option value="2008" <%=endingYear != null && endingYear.equalsIgnoreCase("2008") ? "selected=\"selected\"" : "" %>>2008</option>
								<option value="2009" <%=endingYear != null && endingYear.equalsIgnoreCase("2009") ? "selected=\"selected\"" : "" %>>2009</option>
								<option value="2010" <%=endingYear != null && endingYear.equalsIgnoreCase("2010") ? "selected=\"selected\"" : "" %>>2010</option>
								<option value="2011" <%=endingYear != null && endingYear.equalsIgnoreCase("2011") ? "selected=\"selected\"" : "" %>>2011</option>
								<option value="2012" <%=endingYear != null && endingYear.equalsIgnoreCase("2012") ? "selected=\"selected\"" : "" %>>2012</option>
							</select>
							</td>
							<td>
							<select name="endingMonth" id="endingMonthSelect" 
								onchange="adjustDatePicker( this.form.endingYearSelect, this.form.endingMonthSelect, this.form.endingDateSelect )">
								<option value="0" <%=endingMonth != null && endingMonth.equalsIgnoreCase("0") ? "selected=\"selected\"" : "" %>>01</option>
								<option value="1" <%=endingMonth != null && endingMonth.equalsIgnoreCase("1") ? "selected=\"selected\"" : "" %>>02</option>
								<option value="2" <%=endingMonth != null && endingMonth.equalsIgnoreCase("2") ? "selected=\"selected\"" : "" %>>03</option>
								<option value="3" <%=endingMonth != null && endingMonth.equalsIgnoreCase("3") ? "selected=\"selected\"" : "" %>>04</option>
								<option value="4" <%=endingMonth != null && endingMonth.equalsIgnoreCase("4") ? "selected=\"selected\"" : "" %>>05</option>
								<option value="5" <%=endingMonth != null && endingMonth.equalsIgnoreCase("5") ? "selected=\"selected\"" : "" %>>06</option>
								<option value="6" <%=endingMonth != null && endingMonth.equalsIgnoreCase("6") ? "selected=\"selected\"" : "" %>>07</option>
								<option value="7" <%=endingMonth != null && endingMonth.equalsIgnoreCase("7") ? "selected=\"selected\"" : "" %>>08</option>
								<option value="8" <%=endingMonth != null && endingMonth.equalsIgnoreCase("8") ? "selected=\"selected\"" : "" %>>09</option>
								<option value="9" <%=endingMonth != null && endingMonth.equalsIgnoreCase("9") ? "selected=\"selected\"" : "" %>>10</option>
								<option value="10" <%=endingMonth != null && endingMonth.equalsIgnoreCase("10") ? "selected=\"selected\"" : "" %>>11</option>
								<option value="11" <%=endingMonth != null && endingMonth.equalsIgnoreCase("11") ? "selected=\"selected\"" : "" %>>12</option>
							</select>
							</td>
							<td>
							<select name="endingDate" id="endingDateSelect"
								onchange="adjustDatePicker( this.form.endingYearSelect, this.form.endingMonthSelect, this.form.endingDateSelect )"
								onfocus="adjustDatePicker( this.form.endingYearSelect, this.form.endingMonthSelect, this.form.endingDateSelect )">
								<option value="1" <%=endingDate != null && endingDate.equalsIgnoreCase("1") ? "selected=\"selected\"" : "" %>>01</option>
								<option value="2" <%=endingDate != null && endingDate.equalsIgnoreCase("2") ? "selected=\"selected\"" : "" %>>02</option>
								<option value="3" <%=endingDate != null && endingDate.equalsIgnoreCase("3") ? "selected=\"selected\"" : "" %>>03</option>
								<option value="4" <%=endingDate != null && endingDate.equalsIgnoreCase("4") ? "selected=\"selected\"" : "" %>>04</option>
								<option value="5" <%=endingDate != null && endingDate.equalsIgnoreCase("5") ? "selected=\"selected\"" : "" %>>05</option>
								<option value="6" <%=endingDate != null && endingDate.equalsIgnoreCase("6") ? "selected=\"selected\"" : "" %>>06</option>
								<option value="7" <%=endingDate != null && endingDate.equalsIgnoreCase("7") ? "selected=\"selected\"" : "" %>>07</option>
								<option value="8" <%=endingDate != null && endingDate.equalsIgnoreCase("8") ? "selected=\"selected\"" : "" %>>08</option>
								<option value="9" <%=endingDate != null && endingDate.equalsIgnoreCase("9") ? "selected=\"selected\"" : "" %>>09</option>
								<option value="10" <%=endingDate != null && endingDate.equalsIgnoreCase("10") ? "selected=\"selected\"" : "" %>>10</option>
								<option value="11" <%=endingDate != null && endingDate.equalsIgnoreCase("11") ? "selected=\"selected\"" : "" %>>11</option>
								<option value="12" <%=endingDate != null && endingDate.equalsIgnoreCase("12") ? "selected=\"selected\"" : "" %>>12</option>
								<option value="13" <%=endingDate != null && endingDate.equalsIgnoreCase("13") ? "selected=\"selected\"" : "" %>>13</option>
								<option value="14" <%=endingDate != null && endingDate.equalsIgnoreCase("14") ? "selected=\"selected\"" : "" %>>14</option>
								<option value="15" <%=endingDate != null && endingDate.equalsIgnoreCase("15") ? "selected=\"selected\"" : "" %>>15</option>
								<option value="16" <%=endingDate != null && endingDate.equalsIgnoreCase("16") ? "selected=\"selected\"" : "" %>>16</option>
								<option value="17" <%=endingDate != null && endingDate.equalsIgnoreCase("17") ? "selected=\"selected\"" : "" %>>17</option>
								<option value="18" <%=endingDate != null && endingDate.equalsIgnoreCase("18") ? "selected=\"selected\"" : "" %>>18</option>
								<option value="19" <%=endingDate != null && endingDate.equalsIgnoreCase("19") ? "selected=\"selected\"" : "" %>>19</option>
								<option value="20" <%=endingDate != null && endingDate.equalsIgnoreCase("20") ? "selected=\"selected\"" : "" %>>20</option>
								<option value="21" <%=endingDate != null && endingDate.equalsIgnoreCase("21") ? "selected=\"selected\"" : "" %>>21</option>
								<option value="22" <%=endingDate != null && endingDate.equalsIgnoreCase("22") ? "selected=\"selected\"" : "" %>>22</option>
								<option value="23" <%=endingDate != null && endingDate.equalsIgnoreCase("23") ? "selected=\"selected\"" : "" %>>23</option>
								<option value="24" <%=endingDate != null && endingDate.equalsIgnoreCase("24") ? "selected=\"selected\"" : "" %>>24</option>
								<option value="25" <%=endingDate != null && endingDate.equalsIgnoreCase("25") ? "selected=\"selected\"" : "" %>>25</option>
								<option value="26" <%=endingDate != null && endingDate.equalsIgnoreCase("26") ? "selected=\"selected\"" : "" %>>26</option>
								<option value="27" <%=endingDate != null && endingDate.equalsIgnoreCase("27") ? "selected=\"selected\"" : "" %>>27</option>
								<option value="28" <%=endingDate != null && endingDate.equalsIgnoreCase("28") ? "selected=\"selected\"" : "" %>>28</option>
								<option value="29" <%=endingDate != null && endingDate.equalsIgnoreCase("29") ? "selected=\"selected\"" : "" %>>29</option>
								<option value="30" <%=endingDate != null && endingDate.equalsIgnoreCase("30") ? "selected=\"selected\"" : "" %>>30</option>
								<option value="31" <%=endingDate != null && endingDate.equalsIgnoreCase("31") ? "selected=\"selected\"" : "" %>>31</option>
							</select>
							</td>
						</tr>
					</table>
					<br/>
					<input type="submit" value="Delete Events" class="formButton1"/><br/>
					</form>

					</td></tr></table>

				</td>
			</tr>
		</table>
	</div>

	<%@ include file="/common/footer.jsp"%>
