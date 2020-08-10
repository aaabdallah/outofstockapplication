<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Reports"/> 
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
			<input type="hidden" name="goBackFromErrorURL" value='<%=renderResponse.encodeURL("/reports/index.jsp")%>'/>
			<input type="submit" name="generateReports" value="GENERATE REPORTS" id="generateReports" class="mainButton" />
		</form>
	</div>

	<%@ include file="/common/footer.jsp"%>
