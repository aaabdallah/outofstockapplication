<%@ page language="java"%>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet" %>

<%@ page import="net.btlcpy.outofstock.persistence.beans.ProductCategory"%>
<%@ page import="net.btlcpy.outofstock.persistence.BeanCache"%>
<%@ page import="java.util.Map"%>
<%@ page import="org.apache.commons.collections.map.ListOrderedMap"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.Product"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.ProductPackage"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.DistributorDivision"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.DistributorDistrict"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.Bottler"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerBusinessUnit"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerMarketUnit"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerBranch"%>
<%@ page import="net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute"%>
<%@ page import="javax.portlet.PortletSession"%>
<%@ page import="net.btlcpy.outofstock.reports.Report"%>
<%@ page import="java.util.Set"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.List"%>
<%@ page import="net.btlcpy.outofstock.reports.format.ReportCellFormat"%>
<%@ page import="java.text.Format"%>
<%@ page import="java.text.NumberFormat"%>
<%@ page import="java.util.Calendar"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.sql.Date"%>
<%@ page import="net.btlcpy.outofstock.utilities.PortletServletSharedContext"%>
<%@page import="java.sql.Timestamp"%>
<portlet:defineObjects/>

<%!
private static String id(String[] pkID)
{
	if (pkID == null)
		return "ALL";
	
	StringBuffer sb = new StringBuffer("");
	sb.append(pkID[0].substring(pkID[0].indexOf("|")+1, pkID[0].length()));
	for (int i=1; i<pkID.length; i++)
	{
		sb.append(", " + pkID[i].substring(pkID[i].indexOf("|")+1, pkID[i].length()));
	}
	
	return sb.toString();
}
%>

<%
String reportType = (String) renderRequest.getPortletSession().getAttribute("reportType");
String summaryTarget = (String) renderRequest.getPortletSession().getAttribute("summaryTarget");
String timePeriod = (String) renderRequest.getPortletSession().getAttribute("timePeriod");
String startingYear = (String) renderRequest.getPortletSession().getAttribute("startingYear");
String startingMonth = (String) renderRequest.getPortletSession().getAttribute("startingMonth");
String startingDate = (String) renderRequest.getPortletSession().getAttribute("startingDate");
Calendar calendar = Calendar.getInstance(Locale.US);
if (timePeriod != null && timePeriod.equals("ytd"))
{
	calendar.set(Calendar.DATE, 1);
	calendar.set(Calendar.MONTH, 0);
}
else
	calendar.set(Integer.parseInt(startingYear), Integer.parseInt(startingMonth), Integer.parseInt(startingDate));
String numberOfWeeks = (String) renderRequest.getPortletSession().getAttribute("numberOfWeeks");
String breakOutByDay = (String) renderRequest.getPortletSession().getAttribute("breakOutByDay");
String resultSize = (String) renderRequest.getPortletSession().getAttribute("resultSize");
String lostSalesMetric = (String) renderRequest.getPortletSession().getAttribute("lostSalesMetric");

String[] productCategoryPKID = (String[]) renderRequest.getPortletSession().getAttribute("productCategory");
String[] productPKID = (String[]) renderRequest.getPortletSession().getAttribute("product");
String[] productPackagePKID = (String[]) renderRequest.getPortletSession().getAttribute("productPackage");
String[] bottlerPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottler");
String[] bottlerBusinessUnitPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerBusinessUnit");
String[] bottlerMarketUnitPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerMarketUnit");
String[] bottlerBranchPKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerBranch");
String[] bottlerSalesRoutePKID = (String[]) renderRequest.getPortletSession().getAttribute("bottlerSalesRoute");
String[] distributorDivisionPKID = (String[]) renderRequest.getPortletSession().getAttribute("distributorDivision");
String[] distributorDistrictPKID = (String[]) renderRequest.getPortletSession().getAttribute("distributorDistrict");
String[] storePKID = (String[]) renderRequest.getPortletSession().getAttribute("store");

Report report = (Report) renderRequest.getPortletSession().getAttribute("reportResults");

String sortQueryNumber = renderRequest.getParameter("sortQueryNumber");
String sortOnColumn = renderRequest.getParameter("sortOnColumn");
String sortAscending = renderRequest.getParameter("sortAscending");

if (report != null && sortQueryNumber != null && sortOnColumn != null && sortAscending != null)
{
	String[] sortingColumns = { sortOnColumn };
	// int subRows = (lostSalesMetric != null && lostSalesMetric.equals("all") ? 1 : 0);
	report.sort(Integer.parseInt(sortQueryNumber), sortingColumns, Boolean.valueOf(sortAscending).booleanValue(), 0);
}
%>

	<portlet:renderURL var="indexUrl">
		<portlet:param name="destinationURL" value="/reports/index.jsp"/>
	</portlet:renderURL>
	<portlet:renderURL var="specifyReportParametersUrl">
		<portlet:param name="destinationURL" value="/reports/generate/specifyReportParameters.jsp"/>
	</portlet:renderURL>

	<jsp:useBean id="crumbs" scope="page" class="net.btlcpy.outofstock.utilities.Crumbs">
		<jsp:setProperty name="crumbs" property="headerTitle" value="Out Of Stock: Reports"/> 
		<jsp:setProperty name="crumbs" property="headerImageUrl" value='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/header.gif") %>'/> 
		<jsp:setProperty name="crumbs" property="headerCrumbNames" value="Main Menu|Specify Report Parameters|Display Report Results"/> 
		<jsp:setProperty name="crumbs" property="headerCrumbLinks" value='<%=indexUrl.toString() + "|" + specifyReportParametersUrl.toString() + "|none" %>'/> 
	</jsp:useBean>

	<%@ include file="/common/header.jsp"%>

	<div class="mainContent">
		<%if (report == null) { %>
		<p>There is no report to display.</p>
		
		<%} else { %>
		<table class="parametersTableA">
			<tr class="parametersHeadingRowA">
				<td colspan="2" class="parametersHeadingCellA">
				Report Description:
				<%=reportType.equals("averages") ? "Average lost sales" :
					(reportType.equals("totals") ? "Total lost sales" : "Average daily events per store"
					)
				%>
				<%=reportType.equals("dailyevents") ? "" :
					(lostSalesMetric.equals("all") ? "(dollars and units)" :
						(lostSalesMetric.equals("amount") ? "(dollars only)" : "(units only)")
					)
				%> over 
				<%=(summaryTarget != null && summaryTarget.equals("week") && numberOfWeeks != null) ? numberOfWeeks :
					(reportType.equals("averages") || 
					resultSize == null || resultSize.equals("all") || resultSize.equals("tospecifiedweeks") || 
					(summaryTarget != null && (summaryTarget.equals("day") || summaryTarget.equals("week"))) ? "all" :
						(resultSize.equals("top25") ? "top 25" : "top 50"
						)
					)
				%>
				<%=reportType.equals("dailyevents") ? "stores" :
					(summaryTarget.equals("store") ? "stores" :
						(summaryTarget.equals("storetvss") ? "stores - split by traditional versus distributor.com" :
							(summaryTarget.equals("bottler") ? "bottlers" :
								(summaryTarget.equals("bottlerbranch") ? "bottler branches" :
									(summaryTarget.equals("bottlersalesroute") ? "bottler sales routes" :
										(summaryTarget.equals("distributordistrict") ? "Distributor districts" :
											(summaryTarget.equals("day") ? "days" :
												(summaryTarget.equals("product") ? "products" :
													(summaryTarget.equals("week") ? "weeks" :
														(summaryTarget.equals("package") ? "packages" :
															(summaryTarget.equals("storewithproduct") ? "stores (with products)" : "&lt;unknown summary item&gt;"
															)
														)
													)
												)
											)
										)
									)
								)
							)
						)
					)
				%>, starting from <%=new Date(calendar.getTime().getTime()).toString()%> 
				<%=(summaryTarget != null && summaryTarget.equals("week") && numberOfWeeks != null) ? "" :
					(timePeriod.equals("ytd") ? "to " + new Date(System.currentTimeMillis()).toString() + " (today)" :
						(timePeriod.equals("week") ? "for one week" :
							(timePeriod.equals("month") ? "for one month" :
								(timePeriod.equals("year") ? "for one year" : "&lt;unknown time period&gt;"
								)
							)
						)
					)
				%><%=breakOutByDay == null || breakOutByDay.equals("no") ? "." : ", broken out by day."%>
				</td>
			</tr>
			<tr class="parametersDataRowA">
				<td class="parametersDataCellA" width="1%">Constraints:</td>
				<td class="parametersDataCellA">
					Product Category: [<%=id(productCategoryPKID) %>], Product: [<%=id(productPKID) %>]<br/>
					Distributor Division: [<%=id(distributorDivisionPKID) %>], Distributor District: [<%=id(distributorDistrictPKID) %>], Store: [<%=id(storePKID) %>]<br/>
					Bottler: [<%=id(bottlerPKID) %>], Bottler Region: [<%=id(bottlerBusinessUnitPKID) %>], Bottler Market Unit: [<%=id(bottlerMarketUnitPKID) %>], Bottler Branch: [<%=id(bottlerBranchPKID) %>]<br/>
					Bottler Sales Route: [<%=id(bottlerSalesRoutePKID) %>]<br/>
					Package: [<%=id(productPackagePKID) %>]
				</td>
			</tr>
		</table>

		<%
		ListOrderedMap reportResults = report.getResults();
		String[][] columnNames = null;

		if (reportResults == null || reportResults.size() == 0) // BEGIN IF no sections in report
		{
		%>
		<p>The resulting report data is empty.</p>
		<%
		} // END IF no sections in report
		else // BEGIN ELSE sections exist in report
		{
			// First, add the report reference to the portlet-servlet shared context to facilitate downloading Excel exports
			PortletServletSharedContext.getInstance().put("report" + session.getId(), report);
			
			Set queryIndexes = reportResults.keySet();
			columnNames = new String[queryIndexes.size()][];
			
			Iterator queryIndexIterator = queryIndexes.iterator();
			int queryCounter = 0;
			while (queryIndexIterator.hasNext()) // BEGIN LOOP over all sections of report
			{
				String queryIndex = (String) queryIndexIterator.next();
				//System.out.println("Query Index: " + queryIndex);
		%>
		<p class="parametersHeadingCellA"><%=report.getQueryTitle(queryCounter)%>&nbsp;&nbsp;&nbsp;
			<a href="<%=request.getContextPath()%>/downloader?action=exportreport&sid=<%=session.getId()%>">
			<img title="Download report as Microsoft Excel workbook" alt="Download report as Microsoft Excel workbook" 
			src='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/icon-ms-excel.gif") %>'/>
			</a>&nbsp;&nbsp;
			<a href="<%=request.getContextPath()%>/downloader?action=exportreport&sid=<%=session.getId()%>">
			Download report as Microsoft Excel workbook
			</a>
			<% if (reportType.equals("averages") && !lostSalesMetric.equals("all"))
				{
			%>
			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<a href="<%=request.getContextPath()%>/downloader?action=createtrendchart&sid=<%=session.getId()%>">
			<img title="Download trend chart as Microsoft Excel workbook" alt="Download trend chart as Microsoft Excel workbook" 
			src='<%=renderResponse.encodeURL(renderRequest.getContextPath() + "/images/icon-ms-excel.gif") %>'/>
			</a>&nbsp;&nbsp;
			<a href="<%=request.getContextPath()%>/downloader?action=createtrendchart&sid=<%=session.getId()%>">
			Download trend chart as Microsoft Excel workbook
			</a>
			<%
				}
			%>
		</p>
		<%
				boolean namesIdentified = false;
				Map oneSectionResults = (Map) reportResults.get(queryIndex);
				if (oneSectionResults == null)
				{
					// System.out.println("No query results for queryIndex: " + queryIndex);
		%>
		<p>No results found in section.</p>
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
					int rowCounter = 0;
					while (iterator.hasNext() ) // && (rowLimit == -1 || rowLimit-- > 0))
					{
						rowColorSwitcher = !rowColorSwitcher;
						ListOrderedMap row = (ListOrderedMap) oneSectionResults.get(iterator.next());
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

								//System.out.print("Column name [" + queryCounter + "," + i + "]: " + columnNames[queryCounter][i].toString() + ", ");
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
					<portlet:param name="destinationURL" value="/reports/generate/displayReportResults.jsp"/>
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
							String dataKey = (String) row.get(i);
							if (dataValue == null || dataKey.startsWith("__IGNORE__"))
							{
								// do nothing: move on to the next column (some columns might not have data since
								// we allow merged cells - they may extend down into a row or two).
							}
							else if (dataValue instanceof List)
							{
								List subList = (List) dataValue;

								int heightOfMergedRegion = report.getHeightOfMergedRegion(queryCounter, i, rowCounter);
								String rowSpan = "";
								if (heightOfMergedRegion != -1)
									rowSpan = "rowspan = \"" + heightOfMergedRegion + "\"";
		%>
				<td class="<%=dataCellFormat != null && dataCellFormat.getCssStyle() != null ? dataCellFormat.getCssStyle() : "reportDataCellA" %>" <%=rowSpan%>>
					<table class="reportDataCellListA">
		<%
								for (int jj=0; jj<subList.size(); jj++)
								{
									String val = subList.get(jj).toString();
									if (dataCellFormat != null)
									{
										Format formatter = dataCellFormat.getFormatter(jj);
										if (formatter != null)
										{
											val = formatter.format(subList.get(jj)).toString();
										}
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
							} 
							else 
							{
								String val = row.get(columnNames[queryCounter][i]).toString();
								if (dataCellFormat != null)
								{
									Format formatter = dataCellFormat.getFormatter(-1);
									if (formatter != null)
										val = formatter.format(row.get(columnNames[queryCounter][i])).toString();
								}
								
								int heightOfMergedRegion = report.getHeightOfMergedRegion(queryCounter, i, rowCounter);
								String rowSpan = "";
								if (heightOfMergedRegion != -1)
									rowSpan = "rowspan = \"" + heightOfMergedRegion + "\"";
		%>
				<td class="<%=dataCellFormat != null && dataCellFormat.getCssStyle() != null 
					? dataCellFormat.getCssStyle() 
					: "reportDataCellA" %>" <%=rowSpan%>><%=val%></td>
		<%
							}
		%>
		<%
						}
		%>
			</tr>
		<%
						rowCounter++;
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
