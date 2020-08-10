<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>
<portlet:renderURL var="manageEventDataUrl">
	<portlet:param name="destinationURL" value="/administration/eventManagement/index.jsp"/>
</portlet:renderURL>
<%
String unrecognizedStoreIds = renderRequest.getParameter("unrecognizedStoreIds");
%>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Manage Event Data|Upload Events Success"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|" + manageEventDataUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<%
			if (unrecognizedStoreIds != null && unrecognizedStoreIds.trim().length() > 0)
			{
		%>
		<p>The new list of events has been successfully uploaded, but the following store ID's were not found in the
		current list of active stores: <%=unrecognizedStoreIds%>. <strong>All events related to these stores 
		were NOT uploaded.</strong></p>
		<%
			} else {
		%>
		<p>The new list of events has been successfully uploaded.</p>
		<%
			}
		%>
	</div>

	<%@ include file="/common/footer.jsp"%>
