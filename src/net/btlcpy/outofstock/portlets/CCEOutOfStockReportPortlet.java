package net.btlcpy.outofstock.portlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.OutOfStockEvent;
import net.btlcpy.outofstock.reports.Report;
import net.btlcpy.outofstock.reports.ReportManager;

import org.apache.commons.codec.net.URLCodec;

/**
 * @author Ahmed A. Abd-Allah
 */
public class CCEOutOfStockReportPortlet extends GenericPortlet
{
	public void init() throws PortletException
	{
		Locale.setDefault(Locale.ENGLISH);
		
		// Set up the main logger
		MainLog.init( getPortletContext().getRealPath( getPortletConfig().getInitParameter("loggerConfiguration") ) );

		// Ensure a persistence manager is loaded
		PersistenceManager.initializePersistenceManager(getPortletConfig().getInitParameter("jndiDBName"));
		
		OutOfStockEvent.updateEarliestEventDate(null);

		MainLog.getLog().debug( "\n\n\n\tCCE Out-of-Stock report portlet instantiated.\n\n\n\n" );
	}

	protected void doView(RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException, IOException
	{
		PortletRequestDispatcher dispatcher = null;
	
		try
		{
			Enumeration parameterNames = renderRequest.getParameterNames();
			if (parameterNames == null)
				MainLog.getLog().debug("Render request parameters: EMPTY PARAMETER LIST");
			else
			{
				MainLog.getLog().debug("Render request parameters:");
				StringBuffer results = new StringBuffer("\n\n\n");
				while (parameterNames.hasMoreElements())
				{
					String parameterName = (String) parameterNames.nextElement();
					String[] values = renderRequest.getParameterValues(parameterName);

					results.append("\tParameter: " + parameterName + ", Value: "
						+ values[0]);
					for (int i=1; i<values.length; i++)
						results.append(", " + values[i]);
					results.append("\n");
				}
				results.append("\n\n\n\n");
				MainLog.getLog().debug(results.toString());
			}
	
			String destinationURL = (String) renderRequest.getParameter("destinationURL");

			if (destinationURL == null || renderRequest.getPortletSession() == null ||
				renderRequest.getPortletSession().getAttribute("loggedIn") == null)
				dispatcher = getPortletContext().getRequestDispatcher("/reports/index.jsp");
			else
				dispatcher = getPortletContext().getRequestDispatcher(destinationURL);
				
			renderResponse.setContentType("text/html");
			dispatcher.include(renderRequest, renderResponse);
		}
		catch (Exception e)
		{
			renderResponse.setContentType("text/html");
			MainLog.getLog().debug("Render Exception", e);
			String error = null;
			try 
			{ 
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				error = (new URLCodec("UTF-8")).encode("Exception while rendering view.<br/>Type: " +
					e.getClass().getName() + "<br/>Message: " + e.getMessage() + "<br/>Stack:<br/>" + 
					"<blockquote><pre>" + sw.toString() + "</pre></blockquote>"); 
			}
			catch (Exception ee) { error = "RenderViewError"; }
			
			dispatcher = getPortletContext().getRequestDispatcher("/reports/error.jsp");
			renderRequest.setAttribute("error", error);
			dispatcher.include(renderRequest, renderResponse);
		}
	}
	
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) 
		throws PortletException, IOException
	{
		String portletAction = null;
		String goBackFromErrorURL = null;
		
		try
		{
			portletAction = actionRequest.getParameter("portletAction");
			goBackFromErrorURL = actionRequest.getParameter("goBackFromErrorURL");
			PortletSession session = actionRequest.getPortletSession();

			if (portletAction != null)
			{
				if (portletAction.equalsIgnoreCase("mainPageAction"))
				{
					if (actionRequest.getParameter("generateReports") != null)
						actionResponse.
							setRenderParameter("destinationURL", "/reports/generate/specifyReportParameters.jsp");
				}
				else if (portletAction.equalsIgnoreCase("specifyReportParameters"))
				{
					String generateReportSubmit = actionRequest.getParameter("generateReportSubmit");
					
					String reportType = actionRequest.getParameter("reportType");
					session.removeAttribute("reportType");
					String summaryTarget = actionRequest.getParameter("summaryTarget");
					session.removeAttribute("summaryTarget");
					String startingYear = actionRequest.getParameter("startingYear");
					session.removeAttribute("startingYear");
					String startingMonth = actionRequest.getParameter("startingMonth");
					session.removeAttribute("startingMonth");
					String startingDate = actionRequest.getParameter("startingDate");
					session.removeAttribute("startingDate");
					String numberOfWeeks = actionRequest.getParameter("numberOfWeeks");
					session.removeAttribute("numberOfWeeks");
					String timePeriod = actionRequest.getParameter("timePeriod");
					session.removeAttribute("timePeriod");
					String breakOutByDay = actionRequest.getParameter("breakOutByDay");
					session.removeAttribute("breakOutByDay");
					String resultSize = actionRequest.getParameter("resultSize");
					session.removeAttribute("resultSize");
					String lostSalesMetric = actionRequest.getParameter("lostSalesMetric");
					session.removeAttribute("lostSalesMetric");

					String changeSource = actionRequest.getParameter("changeSource");
					String[] productCategoryPKID = actionRequest.getParameterValues("productCategory");
					session.removeAttribute("productCategory");
					String[] productPKID = actionRequest.getParameterValues("product");
					session.removeAttribute("product");
					String[] productPackagePKID = actionRequest.getParameterValues("productPackage");
					session.removeAttribute("productPackage");
					String[] bottlerPKID = actionRequest.getParameterValues("bottler");
					session.removeAttribute("bottler");
					String[] bottlerBusinessUnitPKID = actionRequest.getParameterValues("bottlerBusinessUnit");
					session.removeAttribute("bottlerBusinessUnit");
					String[] bottlerMarketUnitPKID = actionRequest.getParameterValues("bottlerMarketUnit");
					session.removeAttribute("bottlerMarketUnit");
					String[] bottlerBranchPKID = actionRequest.getParameterValues("bottlerBranch");
					session.removeAttribute("bottlerBranch");
					String[] bottlerSalesRoutePKID = actionRequest.getParameterValues("bottlerSalesRoute");
					session.removeAttribute("bottlerSalesRoute");
					String[] distributorDivisionPKID = actionRequest.getParameterValues("distributorDivision");
					session.removeAttribute("distributorDivision");
					String[] distributorDistrictPKID = actionRequest.getParameterValues("distributorDistrict");
					session.removeAttribute("distributorDistrict");
					String[] storePKID = actionRequest.getParameterValues("store");
					session.removeAttribute("store");

					if (reportType != null)
						session.setAttribute("reportType", reportType);
					if (summaryTarget != null)
						session.setAttribute("summaryTarget", summaryTarget);
					if (startingYear != null)
						session.setAttribute("startingYear", startingYear);
					if (startingMonth != null)
						session.setAttribute("startingMonth", startingMonth);
					if (startingDate != null)
						session.setAttribute("startingDate", startingDate);
					if (numberOfWeeks != null)
						session.setAttribute("numberOfWeeks", numberOfWeeks);
					if (timePeriod != null)
						session.setAttribute("timePeriod", timePeriod);
					if (breakOutByDay != null)
						session.setAttribute("breakOutByDay", breakOutByDay);
					if (resultSize != null)
						session.setAttribute("resultSize", resultSize);
					if (lostSalesMetric != null)
						session.setAttribute("lostSalesMetric", lostSalesMetric);

					if (productCategoryPKID != null)
					{
						session.setAttribute("productCategory", productCategoryPKID);
						if ((changeSource == null || (changeSource != null && !changeSource.equals("productCategory"))) &&
							productPKID != null)
						{
							session.setAttribute("product", productPKID);
						}
					}
					
					if (distributorDivisionPKID != null)
					{
						session.setAttribute("distributorDivision", distributorDivisionPKID);
						if ((changeSource == null || (changeSource != null && !changeSource.equals("distributorDivision"))) &&
							distributorDistrictPKID != null)
						{
							session.setAttribute("distributorDistrict", distributorDistrictPKID);
							if ((changeSource == null || (changeSource != null && !changeSource.equals("distributorDistrict"))) &&
								storePKID != null)
							{
								session.setAttribute("store", storePKID);
							}
						}
					}

					if (bottlerPKID != null)
					{
						session.setAttribute("bottler", bottlerPKID);
						if ((changeSource == null || (changeSource != null && !changeSource.equals("bottler"))) &&
							bottlerBusinessUnitPKID != null)
						{
							session.setAttribute("bottlerBusinessUnit", bottlerBusinessUnitPKID);
							if ((changeSource == null || (changeSource != null && !changeSource.equals("bottlerBusinessUnit"))) &&
								bottlerMarketUnitPKID != null)
							{
								session.setAttribute("bottlerMarketUnit", bottlerMarketUnitPKID);
								if ((changeSource == null || (changeSource != null && !changeSource.equals("bottlerMarketUnit"))) &&
									bottlerBranchPKID != null)
								{
									session.setAttribute("bottlerBranch", bottlerBranchPKID);
								}
							}
						}
					}

					if (bottlerSalesRoutePKID != null)
					{
						session.setAttribute("bottlerSalesRoute", bottlerSalesRoutePKID);
					}

					// Asymmetry desired by CCE
					// April 24, 2008: removed since this is now handled on the browser side via Javascript
					/*
					if (changeSource.equals("distributorDivision") || changeSource.equals("distributorDistrict") || changeSource.equals("store"))
					{
						session.removeAttribute("bottler");
						session.removeAttribute("bottlerBusinessUnit");
						session.removeAttribute("bottlerMarketUnit");
						session.removeAttribute("bottlerBranch");
						session.removeAttribute("bottlerSalesRoute");
					}
					if (changeSource.equals("bottler") || changeSource.equals("bottlerBusinessUnit") || changeSource.equals("bottlerMarketUnit") ||
						changeSource.equals("bottlerBranch"))
					{
						session.removeAttribute("bottlerSalesRoute");
					}
					*/

					if (productPackagePKID != null)
					{
						session.setAttribute("productPackage", productPackagePKID);
					}

					if (generateReportSubmit == null) // it was an "onchange" submit
					{
						actionResponse.
							setRenderParameter("destinationURL", "/reports/generate/specifyReportParameters.jsp");
					}
					else // it was a real submit
					{
						Calendar calendar = Calendar.getInstance(Locale.US);
						//DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
						//dateFormatter.setLenient(false);
						//Date beginDate = new Date(dateFormatter.parse(startingDate).getTime());
						//calendar.setTime(beginDate);
						if (startingYear != null)
							calendar.set(Integer.parseInt(startingYear), Integer.parseInt(startingMonth), Integer.parseInt(startingDate));
						Date beginDate = new Date(calendar.getTime().getTime());
						Date endDate = null;
						
						if (timePeriod != null && 
							!(summaryTarget != null && summaryTarget.equalsIgnoreCase("week")))
						{
							if (timePeriod.equalsIgnoreCase("week"))
							{
								calendar.add(Calendar.DATE, 6); // six days plus starting date = seven
								endDate = new Date(calendar.getTime().getTime());
							}
							else if (timePeriod.equalsIgnoreCase("month"))
							{
								calendar.add(Calendar.MONTH, 1);
								calendar.add(Calendar.DATE, -1); // to get "exactly one" month
								endDate = new Date(calendar.getTime().getTime());
							}
							else if (timePeriod.equalsIgnoreCase("year"))
							{
								calendar.add(Calendar.YEAR, 1);
								calendar.add(Calendar.DATE, -1); // to get "exactly one" year
								endDate = new Date(calendar.getTime().getTime());
							}
							else if (timePeriod.equalsIgnoreCase("ytd"))
							{
								endDate = new Date(System.currentTimeMillis());
								calendar.setTime(endDate);
								calendar.set(Calendar.DATE, 1);
								calendar.set(Calendar.MONTH, 0);
								beginDate = new Date(calendar.getTime().getTime());
							}
						}
						else if (numberOfWeeks != null)
						{
							int n = Integer.parseInt(numberOfWeeks);
							for (int cnt=0; cnt<n; cnt++)
								calendar.add(Calendar.DATE, 7);
							if (n>0) calendar.add(Calendar.DATE, -1); // to get n number of weeks
							endDate = new Date(calendar.getTime().getTime());
						}

						ReportManager reportManager = new ReportManager();
						Report report = null;
						
						if (reportType == null || reportType.equalsIgnoreCase("totals"))
							report = reportManager.totalsReport(actionRequest.getRemoteUser(),
								beginDate, endDate, summaryTarget, resultSize,
								breakOutByDay, lostSalesMetric,
								productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
								bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
								bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);
						else if (reportType.equalsIgnoreCase("averages"))
							report = reportManager.averagesReport(actionRequest.getRemoteUser(),
								beginDate, endDate, summaryTarget, lostSalesMetric,
								productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
								bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
								bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);
						else if (reportType.equalsIgnoreCase("dailyevents"))
							report = reportManager.averageDailyStoreEventsReport(actionRequest.getRemoteUser(),
								beginDate, endDate, resultSize,
								productCategoryPKID, productPKID, productPackagePKID, bottlerPKID, 
								bottlerBusinessUnitPKID, bottlerMarketUnitPKID, bottlerBranchPKID, 
								bottlerSalesRoutePKID, distributorDivisionPKID, distributorDistrictPKID, storePKID);
						
						session.setAttribute("reportResults", report); //, PortletSession.APPLICATION_SCOPE);						
						actionResponse.
							setRenderParameter("destinationURL", "/reports/generate/displayReportResults.jsp");
					}
				}
			}
		}
		catch (Exception e)
		{
			String error = null;
			try 
			{ 
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				
				error = (new URLCodec("UTF-8")).encode("Exception while processing action.<br/>Type: " +
					e.getClass().getName() + "<br/>Message: " + e.getMessage() + "<br/>Stack:<br/>" + 
					"<blockquote><pre>" + sw.toString() + "</pre></blockquote>"); 
			}
			catch (Exception ee) { error = "ActionProcessingError"; }
			actionResponse.setRenderParameter("error", error);
	
			if (goBackFromErrorURL != null)
				actionResponse.setRenderParameter("goBackFromErrorURL", goBackFromErrorURL);
			actionResponse.setRenderParameter("destinationURL", "/reports/error.jsp");
		}
	}
}
