<%@ page language="java"%>
<%@ page import="net.btlcpy.outofstock.utilities.AltURLDecoder"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<portlet:defineObjects/>
<portlet:renderURL var="indexUrl">
	<portlet:param name="destinationURL" value="/reports/index.jsp"/>
</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Reports"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Error"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<%
	String error = (String) renderRequest.getAttribute("error"); // sent for render errors
	if (error == null)
		error = request.getParameter("error"); // sent for action errors

	if (error != null)
	{
		try 
		{ 
			error = "We're sorry, but an error has occurred. Additional details follow:<br/>" + 
				AltURLDecoder.decode(error, "UTF-8"); 
		}
		catch (Exception e) 
		{ 
			error = "Mangled error message"; 
		}
	}
	if (error == null)
		error = "We're sorry, but an unexpected system error has occurred. Please notify the administrator.";
	%>
	<div class="mainContent">
		<p><%=error %></p>

		<%
		String goBackFromErrorURL = request.getParameter("goBackFromErrorURL");
		if (goBackFromErrorURL != null)
		{
			goBackFromErrorURL = AltURLDecoder.decode(goBackFromErrorURL, "UTF-8");
		%>
		<portlet:renderURL var="goBackUrl">
			<portlet:param name="destinationURL" value="<%=goBackFromErrorURL%>"/>
		</portlet:renderURL>
		<p><a href="<%=goBackUrl.toString()%>">Go back</a></p>
		<%
		}
		%>

	</div>	

	<%@ include file="/common/footer.jsp"%>
