<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<%@page import="net.btlcpy.outofstock.persistence.beans.UserAction"%>
<%@page import="java.sql.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>
<%
String actionCategory = renderRequest.getParameter("actionCategory");
String startingDate = renderRequest.getParameter("startingDate");
String timePeriod = renderRequest.getParameter("timePeriod");
%>
	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Request User Actions"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<portlet:actionURL var="actionURL"/>
		<form id="form1" class="formStyle1" method="post" action="<%=actionURL.toString()%>">
			<input type="hidden" name="portletAction" value="requestUserActions"/>
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/userManagement/requestUserActions.jsp")%>'/>

			<table class="formTable">
				<tr>
					<td>
						<label for="actionCategorySelect" class="formLabel1">Select Action Category:</label>					
					</td>
					<td>
						<select name="actionCategory" id="actionCategorySelect">
							<option value="all"> - All Categories - </option>
						<%
							String[] categories = UserAction.getCategories(); 
							for (int i=0; i<categories.length; i++)
							{
						%>
								<option value="<%=categories[i]%>"
									<%=actionCategory != null && actionCategory.equals(categories[i]) ? "selected=\"selected\"" : "" %>>							
									<%=categories[i]%>
								</option>
						<%
							} // for
						%>
						</select>
					</td>
				</tr>
				<tr>
					<td>
						<label for="startingDateField" class="formLabel1">[Optional] Enter starting date (MM/DD/YY, with earliest date being 
						<%=new SimpleDateFormat("MM'/'dd'/'yy").format(new Date(UserAction.getEarliestActionTime().getTime()))%>):</label>					
					</td>
					<td>
						<input type="text" name="startingDate" id="startingDateField" maxlength="8" 
						<%=startingDate != null ? "value='" + startingDate + "'" : "" %>/>
					</td>
				</tr>
				<tr>
					<td>
						<label for="timePeriodSelect" class="formLabel1">Select time period:</label>					
					</td>
					<td>
						<select name="timePeriod" id="timePeriodSelect">
							<option value="open" <%=timePeriod != null && timePeriod.equalsIgnoreCase("open") ? "selected=\"selected\"" : "" %>>Open-ended</option>
							<option value="week" <%=timePeriod != null && timePeriod.equalsIgnoreCase("week") ? "selected=\"selected\"" : "" %>>One week</option>
							<option value="month" <%=timePeriod != null && timePeriod.equalsIgnoreCase("month") ? "selected=\"selected\"" : "" %>>One month</option>
							<option value="year" <%=timePeriod != null && timePeriod.equalsIgnoreCase("year") ? "selected=\"selected\"" : "" %>>One year</option>
						</select>
					</td>
				</tr>
				<tr>
					<td>&nbsp;</td>
					<td>
						<input type="submit" name="displayUserActions" value="Display Actions" id="displayUserActions" class="formButton1"/>
					</td>
				</tr>
			</table>
		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>
