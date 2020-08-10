<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<%@page import="net.btlcpy.outofstock.persistence.beans.Setting"%>
<%@page import="org.apache.commons.collections.map.ListOrderedMap"%>
<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Configure Settings"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<p>Use this page to configure application settings.</p>
		
		<portlet:actionURL var="actionURL"/>
		<form id="form1" class="formStyle1" method="post" action="<%=actionURL.toString()%>">
			<input type="hidden" name="portletAction" value="configureSettings"/>
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/configureSettings/index.jsp")%>'/>

			<table class="formTable">
				<tr>
					<td colspan="4">
						<span class="formLabel1">Beverage Categories currently ignored:<strong> 
						<%
						ListOrderedMap ignoredBeverageCategories = Setting.findByCategoryNameValue(null, "IgnoredEvent", "ProductCategory", null);
						if (ignoredBeverageCategories == null || ignoredBeverageCategories.size() == 0)
						{
						%>
						none.
						<%
						} else {
							for (int i=0; i<ignoredBeverageCategories.size(); i++)
							{
						%>
						<%=(i!=0 ? ", " : "") + ((Setting) ignoredBeverageCategories.getValue(i)).getValue()%> 
						<% 
							}
						} 
						%>
						</strong></span>
					</td>
				</tr>
				<tr>
					<td>
						<label for="ignoreBeverageCategoryField" class="formLabel1">Add Beverage Category to ignore:</label>					
					</td>
					<td>
						<input type="text" name="ignoreBeverageCategory" id="ignoreBeverageCategoryField" size="30"/>
					</td>
				</tr>
				<tr>
					<td colspan="2">&nbsp;</td>
					<td>
						<input type="submit" name="configureSettingsSubmit" value="Save Settings" id="configureSettings" class="formButton1"/>
					</td>
					<td>&nbsp;</td>
				</tr>
			</table>

		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>
