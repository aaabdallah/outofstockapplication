<%@page language="java"%>
<%@taglib prefix="portlet" uri="http://java.sun.com/portlet" %>

<%@page import="net.btlcpy.outofstock.persistence.beans.ProductCategory"%>
<%@page import="net.btlcpy.outofstock.persistence.BeanCache"%>
<%@page import="java.util.Map"%>
<%@page import="org.apache.commons.collections.map.ListOrderedMap"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.Product"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.ProductPackage"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.DistributorDivision"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.DistributorDistrict"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.Bottler"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.BottlerBusinessUnit"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.BottlerMarketUnit"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.BottlerBranch"%>
<%@page import="net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute"%>
<%@page import="javax.portlet.PortletSession"%>
<%@page import="net.btlcpy.outofstock.reports.Report"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.List"%>
<%@page import="net.btlcpy.outofstock.reports.format.ReportCellFormat"%>
<%@page import="java.text.Format"%>
<portlet:defineObjects/>

<%!
private static String id(String pkID)
{
	if (pkID == null || pkID.equals("all")) return "ALL";
	
	return pkID.substring(pkID.indexOf("|")+1, pkID.length());
}
%>


<%
String actionCategory = renderRequest.getParameter("actionCategory");
String startingDate = renderRequest.getParameter("startingDate");
String timePeriod = renderRequest.getParameter("timePeriod");

Report report = (Report) session.getAttribute("userActionsReportResults");

String sortQueryNumber = renderRequest.getParameter("sortQueryNumber");
String sortOnColumn = renderRequest.getParameter("sortOnColumn");
String sortAscending = renderRequest.getParameter("sortAscending");

if (report != null && sortQueryNumber != null && sortOnColumn != null && sortAscending != null)
{
	String[] sortingColumns = { sortOnColumn };
	report.sort(Integer.parseInt(sortQueryNumber), sortingColumns, Boolean.valueOf(sortAscending).booleanValue(), 0);
}
%>

	<portlet:renderURL var="indexUrl">
		<portlet:param name="destinationURL" value="/administration/index.jsp"/>
	</portlet:renderURL>
	<portlet:renderURL var="requestUserActionsUrl">
		<portlet:param name="destinationURL" value="/administration/userManagement/requestUserActions.jsp"/>
		<%if (actionCategory != null) { %> <portlet:param name="actionCategory" value="<%=actionCategory %>"/> <%} %>
		<%if (startingDate != null) { %> <portlet:param name="startingDate" value="<%=startingDate %>"/> <%} %>
		<%if (timePeriod != null) { %> <portlet:param name="timePeriod" value="<%=timePeriod %>"/> <%} %>
	</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Administration"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Request User Actions|Display User Actions"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|" + requestUserActionsUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<%if (report == null) { %>
		<p>There is no data to display.</p>
		
		<%} else { %>
		<table class="parametersTableA">
			<tr class="parametersHeadingRowA">
				<td colspan="2" class="parametersHeadingCellA">
				Searching for user actions<%=actionCategory != null && !actionCategory.equals("all") ? " restricted to category \"" + actionCategory + "\"" : ""
				%><%=(startingDate != null && startingDate.trim().length() > 0
					? ", starting from " + startingDate + 
						(timePeriod.equals("open") ? "" :
							(timePeriod.equals("week") ? " for one week" :
								(timePeriod.equals("month") ? " for one month" :
									(timePeriod.equals("year") ? " for one year" : " for &lt;unknown time period&gt;"
									)
								)
							)
						)
					: "")%>.
				</td>
			</tr>
		</table>

		<%
		ListOrderedMap reportResults = report.getResults();
		String[][] columnNames = null;

		if (reportResults == null || reportResults.size() == 0) // BEGIN IF no sections in report
		{
		%>
		<p>There is no user actions report.</p>
		<%
		} // END IF no sections in report
		else // BEGIN ELSE sections exist in report
		{
			Set queryIndexes = reportResults.keySet();
			columnNames = new String[queryIndexes.size()][];
			
			Iterator queryIndexIterator = queryIndexes.iterator();
			int queryCounter = 0;
			while (queryIndexIterator.hasNext()) // BEGIN LOOP over all sections of report
			{
				String queryIndex = (String) queryIndexIterator.next();
				//System.out.println("Query Index: " + queryIndex);
		%>

		<p class="parametersHeadingCellA"><%=report.getQueryTitle(queryCounter)%></p>
		<%
				boolean namesIdentified = false;
				Map oneSectionResults = (Map) reportResults.get(queryIndex);
				if (oneSectionResults == null)
				{
					// System.out.println("No query results for queryIndex: " + queryIndex);
		%>
		<p>There are no user actions to report.</p>
		<%
					
				}
				else
				{
					Set keys = oneSectionResults.keySet();
					Iterator iterator = keys.iterator();
		%>
		<table class="reportTableA">
		<%
					boolean rowColorSwitcher = false;
					while (iterator.hasNext() ) // && (rowLimit == -1 || rowLimit-- > 0))
					{
						rowColorSwitcher = !rowColorSwitcher;
						Map row = (Map) oneSectionResults.get(iterator.next());
						ReportCellFormat headerCellFormat = null;
						ReportCellFormat dataCellFormat = null;
						ReportCellFormat footerCellFormat = null;

						if (!namesIdentified)
						{
							if (columnNames[queryCounter] == null)
							{
								Object[] columnObjects = row.keySet().toArray();
								columnNames[queryCounter] = new String[ columnObjects.length ];
								for (int i=0; i<columnObjects.length; i++)
									columnNames[queryCounter][i] = columnObjects[i].toString();
							}
							namesIdentified = true;
		%>
			<tr class="reportHeadingRowA">
		<%
							for (int i=0; i<columnNames[queryCounter].length; i++)
							{
								headerCellFormat = report.getHeaderCellFormat(columnNames[queryCounter][i]);

								//System.out.print(columnNames[queryCounter][i].toString() + ", ");
								boolean sortable = true;
								String sortIndicator = null;
								String[] uc = report.getUnsortableColumns(queryCounter);
								if (uc != null)
								{
									for (int j=0; j<uc.length; j++)
									{
										if (columnNames[queryCounter][i].toString().equals(uc[j]))
											sortable = false;
									}
								}
		%> 
				<portlet:renderURL var="sortUrl">
					<portlet:param name="destinationURL" value="/administration/userManagement/displayUserActions.jsp"/>
					<portlet:param name="sortQueryNumber" value="<%=Integer.toString(queryCounter)%>"/>
					<portlet:param name="sortOnColumn" value="<%=columnNames[queryCounter][i].toString()%>"/>
					<%
								if (report.getSortedColumns(queryCounter)[0].equals(columnNames[queryCounter][i].toString()))
								{
									if (report.isSortedAscending(queryCounter))
										sortIndicator = renderResponse.encodeURL(renderRequest.getContextPath() + "/images/ascending.gif");
									else
										sortIndicator = renderResponse.encodeURL(renderRequest.getContextPath() + "/images/descending.gif");
					%>
					<portlet:param name="sortAscending" value='<%=report.isSortedAscending(queryCounter) ? "false" : "true"%>'/>
					<%  		} else { %>
					<portlet:param name="sortAscending" value="true"/>
					<%  		} %>
					<%if (actionCategory != null) { %> <portlet:param name="actionCategory" value="<%=actionCategory %>"/> <%} %>
					<%if (startingDate != null) { %> <portlet:param name="startingDate" value="<%=startingDate %>"/> <%} %>
					<%if (timePeriod != null) { %> <portlet:param name="timePeriod" value="<%=timePeriod %>"/> <%} %>
				</portlet:renderURL>
				<td class="<%=headerCellFormat != null && headerCellFormat.getCssStyle() != null ? headerCellFormat.getCssStyle() : "reportHeadingCellA" %>">
					<%	if (sortIndicator != null) 
						{
					%><img src="<%=sortIndicator%>"/>
					<%	} %>
					<%=sortable ? 
					"<a href=\"" + sortUrl.toString() + "\">" : ""%><%=columnNames[queryCounter][i].toString() 
					%><%=sortable ? "</a>" : "" %></td>
		<%
							}
		%>
			</tr>
		<%
						}
			
		%>
			<tr class="reportDataRowA<%=rowColorSwitcher ? 1 : 2 %>">
		<%
						for (int i=0; i<columnNames[queryCounter].length; i++)
						{
							dataCellFormat = report.getDataCellFormat(columnNames[queryCounter][i]);
							footerCellFormat = report.getFooterCellFormat(columnNames[queryCounter][i]);

							//System.out.print(row.get(columnNames[queryCounter][i]).toString() + ", ");
							Object dataValue = row.get(columnNames[queryCounter][i]);
							if (dataValue instanceof List)
							{
								List subList = (List) dataValue;
		%>
				<td class="<%=dataCellFormat != null && dataCellFormat.getCssStyle() != null ? dataCellFormat.getCssStyle() : "reportDataCellA" %>">
					<table class="reportDataCellListA">
		<%
								for (int jj=0; jj<subList.size(); jj++)
								{
									String val = subList.get(jj).toString();
									if (dataCellFormat != null)
									{
										Format formatter = dataCellFormat.getFormatter(jj);
										if (formatter != null)
											val = formatter.format(subList.get(jj)).toString();
									}	
		%>
						<tr class="reportDataCellListRowA">
							<td class="<%=dataCellFormat != null && dataCellFormat.getSubCellCssStyle(jj) != null 
								? dataCellFormat.getSubCellCssStyle(jj) 
								: "reportDataCellListElementA" %>"><%=val%></td>
						</tr>
		<%
								}
		%>
					</table>
				</td>
		<%
							} else {
								String val = row.get(columnNames[queryCounter][i]).toString();
								if (dataCellFormat != null)
								{
									Format formatter = dataCellFormat.getFormatter(-1);
									if (formatter != null)
										val = formatter.format(row.get(columnNames[queryCounter][i])).toString();
								}
		%>
				<td class="<%=dataCellFormat != null && dataCellFormat.getCssStyle() != null 
					? dataCellFormat.getCssStyle() 
					: "reportDataCellA" %>"><%=val%></td>
		<%
							}
		%>
		<%
						}
		%>
			</tr>
		<%
					}
		%>
		</table>
		<%
				}
				queryCounter++;
			} // END LOOP over all sections of report
		} // END ELSE sections exist in report
		%>
		
		<%}%>
	</div>

	<%@ include file="/common/footer.jsp"%>
