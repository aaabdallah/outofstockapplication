<%@ page import="java.util.StringTokenizer"%>
<%@ page import="net.btlcpy.outofstock.utilities.AltURLDecoder"%>
<%@ page import="net.btlcpy.outofstock.utilities.Crumbs"%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<link rel="stylesheet" type="text/css"
href='<%= renderResponse.encodeURL(renderRequest.getContextPath() + "/styles/preferred/master.css") %>'
title="preferred">

<div class="container">

	<div class="header">
		<img class="headerImage" src="<%=AltURLDecoder.decode( ((Crumbs) pageContext.getAttribute("crumbs")).getHeaderImageUrl(), "UTF-8") %>"/>
		<div class="headerBar1 borderBackground">&nbsp;</div>
		<div class="headerBar2 titleBackground"><table class="pageTitle">
		<tr><td><%=AltURLDecoder.decode( ((Crumbs) pageContext.getAttribute("crumbs")).getHeaderTitle(), "UTF-8") %><br/>
		<span class="breadcrumbs">
		<% 
		String crumbNames = AltURLDecoder.decode(((Crumbs) pageContext.getAttribute("crumbs")).getHeaderCrumbNames(), "UTF-8");
		String crumbLinks = AltURLDecoder.decode(((Crumbs) pageContext.getAttribute("crumbs")).getHeaderCrumbLinks(), "UTF-8");
		
		StringTokenizer nameTokens = new StringTokenizer(crumbNames, "|");
		StringTokenizer linkTokens = new StringTokenizer(crumbLinks, "|");
		
		boolean doneFirst = false;
		while (nameTokens.hasMoreElements())
		{
			String crumbName = (String) nameTokens.nextElement();
			String crumbLink = (String) linkTokens.nextElement();
			
			if (doneFirst)
				out.print(" &gt; ");
			if (!crumbLink.equalsIgnoreCase("none"))
				out.print("<a href=\"" + crumbLink + "\" class=\"breadcrumbs\">");
			out.print(crumbName);
			if (!crumbLink.equalsIgnoreCase("none"))
				out.print("</a>");
			doneFirst = true;
		}
		%>
		</span></td></tr>
		</table></div>
	</div>
