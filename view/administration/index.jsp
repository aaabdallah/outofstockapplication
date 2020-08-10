<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value="none"/> 
	</jsp:useBean>

	<%
	renderRequest.getPortletSession().setAttribute("loggedIn", new Boolean(true));
	%>
	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<portlet:actionURL var="navigateURL"/>
		<form id="form1" method="post" action="<%=navigateURL.toString()%>">
			<input type="hidden" name="portletAction" value="mainPageAction"/>
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/administration/index.jsp")%>'/>
			<input type="submit" name="manageEventData" value="MANAGE EVENT DATA" id="manageEventData" class="mainButton" />
			<input type="submit" name="uploadUpdatedStoreList" value="UPLOAD ACTIVE STORES" id="uploadUpdatedStoreList" class="mainButton" />
			<input type="submit" name="requestUserActions" value="VIEW USER ACTIONS" id="requestUserActions" class="mainButton" />
			<input type="submit" name="configureSettings" value="CONFIGURE SETTINGS" id="configureSettings" class="mainButton" />
		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>