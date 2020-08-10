<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>
<portlet:renderURL var="uploadUpdatedStoreListUrl">
	<portlet:param name="destinationURL" value="/administration/uploads/uploadUpdatedStoreList.jsp"/>
</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Upload Active Stores|Success"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|" + uploadUpdatedStoreListUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<p>The updated list of stores has been successfully uploaded.</p>
	</div>

	<%@ include file="/common/footer.jsp"%>
