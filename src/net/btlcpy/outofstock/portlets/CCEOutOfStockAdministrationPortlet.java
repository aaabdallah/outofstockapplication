package net.btlcpy.outofstock.portlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

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
import net.btlcpy.outofstock.persistence.beans.Bottler;
import net.btlcpy.outofstock.persistence.beans.BottlerBranch;
import net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute;
import net.btlcpy.outofstock.persistence.beans.OutOfStockEvent;
import net.btlcpy.outofstock.persistence.beans.Product;
import net.btlcpy.outofstock.persistence.beans.ProductPackage;
import net.btlcpy.outofstock.persistence.beans.DistributorDistrict;
import net.btlcpy.outofstock.persistence.beans.Store;
import net.btlcpy.outofstock.persistence.beans.UserAction;
import net.btlcpy.outofstock.reports.Report;
import net.btlcpy.outofstock.reports.ReportManager;
import net.btlcpy.outofstock.settings.SettingsManager;
import net.btlcpy.outofstock.spreadsheetparsers.ActiveStoresSpreadsheetParser;
import net.btlcpy.outofstock.spreadsheetparsers.OutOfStockEventsSpreadsheetParser;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.portlet.PortletFileUpload;

/**
 * @author Ahmed A. Abd-Allah
 */
public class CCEOutOfStockAdministrationPortlet extends GenericPortlet
{
	public void init() throws PortletException
	{
		Locale.setDefault(Locale.ENGLISH);
		
		// Set up the main logger
		MainLog.init( getPortletContext().getRealPath( getPortletConfig().getInitParameter("loggerConfiguration") ) );

		// Ensure a persistence manager is loaded
		PersistenceManager.initializePersistenceManager(getPortletConfig().getInitParameter("jndiDBName"));

		UserAction.updateEarliestActionTime();
		Store.updateTotals(null);
		Bottler.updateTotalActive(null);
		BottlerBranch.updateTotalActive(null);
		BottlerSalesRoute.updateTotalActive(null);
		DistributorDistrict.updateTotalActive(null);
		Product.updateTotalActive(null);
		ProductPackage.updateTotalActive(null);
		
		MainLog.getLog().debug( "\n\n\n\tCCE Out-of-Stock administration portlet instantiated.\n\n\n\n" );
	}

	protected void doView(RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException, IOException
	{
		renderResponse.setContentType("text/html");
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
				dispatcher = getPortletContext().getRequestDispatcher("/administration/index.jsp");
			else
				dispatcher = getPortletContext().getRequestDispatcher(destinationURL);
			dispatcher.include(renderRequest, renderResponse);
		}
		catch (Exception e)
		{
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
			
			dispatcher = getPortletContext().getRequestDispatcher("/administration/error.jsp");
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
			// Check if it is a file upload
			if (PortletFileUpload.isMultipartContent(actionRequest))
			{
				// Create a factory for disk-based file items
				DiskFileItemFactory factory = new DiskFileItemFactory();
				// Contrary to the documentation, there is NO default repository
				factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
				
				// Create a new file upload handler
				PortletFileUpload upload = new PortletFileUpload(factory);
					
				/*MainLog.getLog().debug(
					"\n\n\nMaximum size per file: " + upload.getFileSizeMax() + "\n" +
					"Maximum size of whole request: " + upload.getSizeMax() + "\n\n\n\n");*/
	
				List /* FileItem */ items = null;

				// Parse the request
				items = upload.parseRequest(actionRequest);
				/*MainLog.getLog().debug(
					"\n\n\n" + "Number of items: " + items.size() + "\n\n\n");*/

				Iterator iterator = items.iterator();
				while( iterator.hasNext() )
				{
					FileItem item = (FileItem) iterator.next();
					
					if (item.isFormField())
					{
					    if (item.getFieldName().equals("portletAction"))
					    	portletAction = item.getString();
					    if (item.getFieldName().equals("goBackFromErrorURL"))
					    	goBackFromErrorURL = item.getString();
						// MainLog.getLog().debug("FORM FIELD: " + name + " " + value);
					}
				}
				
				iterator = items.iterator();
				while( iterator.hasNext() )
				{
					FileItem item = (FileItem) iterator.next();
					
					if (item.isFormField())
					{
						//String name = item.getFieldName();
					    //String value = item.getString();
						// MainLog.getLog().debug("FORM FIELD: " + name + " " + value);
					}
					else
					{
						/*String fieldName = item.getFieldName();
						String fileName = item.getName();
						String contentType = item.getContentType();
						boolean isInMemory = item.isInMemory();
						long sizeInBytes = item.getSize();
						
						MainLog.getLog().debug("FILE: " + fieldName + "\n" +
							fileName + "\n" + contentType + "\n" +
							isInMemory + "\n" + sizeInBytes);*/
	
						long time1 = System.currentTimeMillis();
						
						if (portletAction.equals("uploadUpdatedStoreList"))
						{
							InputStream fileInput = null;
							if (item.getName().toLowerCase().endsWith("zip"))
							{
								fileInput = new ZipInputStream(new BufferedInputStream(item.getInputStream()));
								((ZipInputStream) fileInput).getNextEntry();
							}
							else
								fileInput = item.getInputStream();
							
							ActiveStoresSpreadsheetParser parser = new ActiveStoresSpreadsheetParser(
								fileInput, item.getName(), "TTL", false);
							
							parser.parse(actionRequest.getRemoteUser());
							actionResponse.
								setRenderParameter("destinationURL", "/administration/uploads/uploadUpdatedStoreListSuccess.jsp");
						}
						else if (portletAction.equals("uploadEvents"))
						{
							InputStream fileInput = null;
							if (item.getName().toLowerCase().endsWith("zip"))
							{
								fileInput = new ZipInputStream(new BufferedInputStream(item.getInputStream()));
								((ZipInputStream) fileInput).getNextEntry();
							}
							else
								fileInput = item.getInputStream();

							OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
								fileInput, item.getName(), "Sheet1", false);
							
							parser.parse(actionRequest.getRemoteUser());
							if (parser.getUnrecognizedStoreIds() != null)
								actionResponse.setRenderParameter("unrecognizedStoreIds", parser.getUnrecognizedStoreIds().toString());
							
							actionResponse.
								setRenderParameter("destinationURL", "/administration/eventManagement/uploadEventsSuccess.jsp");
						}
						
						MainLog.getLog().debug("Elapsed time: " + ((double) System.currentTimeMillis() - time1)/1000 + " seconds");
					}
				}
			}
			else
			{
				portletAction = actionRequest.getParameter("portletAction");
				goBackFromErrorURL = actionRequest.getParameter("goBackFromErrorURL");
	
				if (portletAction != null)
				{
					if (portletAction.equalsIgnoreCase("mainPageAction"))
					{
						if (actionRequest.getParameter("manageEventData") != null)
							actionResponse.
								setRenderParameter("destinationURL", "/administration/eventManagement/index.jsp");
						else if (actionRequest.getParameter("uploadUpdatedStoreList") != null)
							actionResponse.
								setRenderParameter("destinationURL", "/administration/uploads/uploadUpdatedStoreList.jsp");
						else if (actionRequest.getParameter("requestUserActions") != null)
							actionResponse.
								setRenderParameter("destinationURL", "/administration/userManagement/requestUserActions.jsp");
						else if (actionRequest.getParameter("configureSettings") != null)
							actionResponse.
								setRenderParameter("destinationURL", "/administration/configureSettings/index.jsp");
					}
					else if (portletAction.equalsIgnoreCase("requestUserActions"))
					{
						String actionCategory = actionRequest.getParameter("actionCategory");
						String startingDate = actionRequest.getParameter("startingDate");
						String timePeriod = actionRequest.getParameter("timePeriod");
						
						if (actionCategory != null)
							actionResponse.setRenderParameter("actionCategory", actionCategory);
						if (startingDate != null)
							actionResponse.setRenderParameter("startingDate", startingDate);
						if (timePeriod != null)
							actionResponse.setRenderParameter("timePeriod", timePeriod);
						
						Date endDate = null;
						Date beginDate = null;
						if (startingDate != null && startingDate.trim().length() > 0)
						{
							DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
							dateFormatter.setLenient(false);
							beginDate = new Date(dateFormatter.parse(startingDate).getTime());
							Calendar calendar = Calendar.getInstance(Locale.US);
							calendar.setTime(beginDate);
	
							if (timePeriod.equalsIgnoreCase("open"))
							{
							}
							else if (timePeriod.equalsIgnoreCase("week"))
							{
								calendar.add(Calendar.DATE, 7);
								endDate = new Date(calendar.getTime().getTime());
							}
							else if (timePeriod.equalsIgnoreCase("month"))
							{
								calendar.add(Calendar.MONTH, 1);
								endDate = new Date(calendar.getTime().getTime());
							}
							else if (timePeriod.equalsIgnoreCase("year"))
							{
								calendar.add(Calendar.YEAR, 1);
								endDate = new Date(calendar.getTime().getTime());
							}
						}
						
						ReportManager reportManager = new ReportManager();
						Report report = 
							reportManager.userActionsReport(actionRequest.getRemoteUser(),
								beginDate, endDate, actionCategory, 0, 20);
						
						PortletSession session = actionRequest.getPortletSession();
						session.setAttribute("userActionsReportResults", report, PortletSession.APPLICATION_SCOPE);						
						actionResponse.
							setRenderParameter("destinationURL", "/administration/userManagement/displayUserActions.jsp");
					}
					else if (portletAction.equals("configureSettings"))
					{
						String ignoreBeverageCategory = actionRequest.getParameter("ignoreBeverageCategory");
						
						if (ignoreBeverageCategory != null)
						{
							SettingsManager.addIgnoredBeverageCategory(null, ignoreBeverageCategory);
							SettingsManager.applyIgnoredBeverageCategoryToEvents(null, ignoreBeverageCategory);
							SettingsManager.applyIgnoredBeverageCategoryToProductCategories(null, ignoreBeverageCategory);
						}

						actionResponse.
							setRenderParameter("destinationURL", "/administration/configureSettings/index.jsp");						
					}
					else if (portletAction.equals("deleteEventsInInterval"))
					{
						String startingYear = actionRequest.getParameter("startingYear");
						String startingMonth = actionRequest.getParameter("startingMonth");
						String startingDate = actionRequest.getParameter("startingDate");
						String endingYear = actionRequest.getParameter("endingYear");
						String endingMonth = actionRequest.getParameter("endingMonth");
						String endingDate = actionRequest.getParameter("endingDate");

						Calendar calendar = Calendar.getInstance(Locale.US);
						calendar.set(Integer.parseInt(startingYear), Integer.parseInt(startingMonth), Integer.parseInt(startingDate));
						Date startDate = new Date(calendar.getTime().getTime());
						calendar.set(Integer.parseInt(endingYear), Integer.parseInt(endingMonth), Integer.parseInt(endingDate));
						Date endDate = new Date(calendar.getTime().getTime());

						// delete events in interval
						OutOfStockEvent.deleteEventsInInterval(null, actionRequest.getRemoteUser(), startDate, endDate);
						
						actionResponse.
							setRenderParameter("destinationURL", "/administration/eventManagement/index.jsp");
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
			actionResponse.setRenderParameter("destinationURL", "/administration/error.jsp");
		}
	}
}

/*
Map parameterMap = renderRequest.getParameterMap();
if (parameterMap == null)
	MainLog.getLog().debug("EMPTY PARAMETER MAP");
else
{
	MainLog.getLog().debug("NON-NULL MAP, SIZE: " + parameterMap.size());
	Set names = parameterMap.keySet();
	Iterator iterator = names.iterator();
	while (iterator.hasNext())
	{
		String name = (String) iterator.next();
		MainLog.getLog().debug("Map " + name + " : " + parameterMap.get(name));
	}
}

Enumeration parameterNames = renderRequest.getParameterNames();
if (parameterNames == null)
	MainLog.getLog().debug("EMPTY PARAMETER LIST");
else
{
	MainLog.getLog().debug("Parameters exist");
	while (parameterNames.hasMoreElements())
	{
		MainLog.getLog().debug("\n\tParameter: " + parameterNames.nextElement().toString());
	}
}

*/