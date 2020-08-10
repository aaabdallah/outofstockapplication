<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/administration/index.jsp"/>
</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Upload Active Stores"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<portlet:actionURL var="uploadFileURL"/>
		
		<p>Use this form to upload a file containing an updated list of the active stores.</p>
		<form id="form1" class="formStyle1" enctype="multipart/form-data" method="post" action="<%=uploadFileURL.toString()%>">
		<input type="hidden" name="portletAction" value="uploadUpdatedStoreList"/>
		<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/uploads/uploadUpdatedStoreList.jsp")%>'/>
		<label for="file" class="formLabel1">Select a file:</label><input id="file" type="file" name="file"/><br/>
		<input type="submit" value="Upload" class="formButton1"/><br/>
		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>
