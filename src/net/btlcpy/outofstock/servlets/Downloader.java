package net.btlcpy.outofstock.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.reports.AveragesReport;
import net.btlcpy.outofstock.reports.Report;
import net.btlcpy.outofstock.spreadsheetwriters.ReportExporter;
import net.btlcpy.outofstock.spreadsheetwriters.TrendChart;
import net.btlcpy.outofstock.utilities.PortletServletSharedContext;
import net.btlcpy.outofstock.utilities.StringUtils;

/**
 * <p>
 * A class to manage downloads of different pieces of information for the Out of Stock application.
 * Examples at the time of writing include generated Microsoft Excel representations of report data
 * as well as Excel trend charts of certain reports. This class does more than simply pass on a
 * file to the user, it also can be used to manage the generation of the that file, and its
 * subsequent deletion from the server after download is complete.
 * </p>
 * 
 * <p>
 * Note that this class is a servlet.
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class Downloader extends HttpServlet implements HttpSessionListener
{
	/** Recommended for Serializable purposes */
	public static final long serialVersionUID = 1;
	
	/** The location to place temporary files. Configurable in web.xml. */
	private String temporaryFilesLocation = null;
	
	/** The location of templates used during file generation. Configurable in web.xml. */
	private String templatesLocation = null;
	
	public void init() 
		throws ServletException 
	{
		temporaryFilesLocation = getInitParameter("temporaryFilesLocation");
		templatesLocation = getInitParameter("templatesLocation");
	}
	
	/**
	 * "Exporting a report" in this context means generating a Microsoft Excel format file
	 * which has the report's data in it, along with a description of the report. This method
	 * generates the file, sends it to the user, then deletes the file. To avoid conflicts
	 * with other users generating files, the filename is constructed to include the current
	 * session number in it.
	 * 
	 * @param request HTTP request
	 * @param response HTTP response
	 */
	private void sendReportForExport(HttpServletRequest request, HttpServletResponse response)
	{
		String sessionId = null;
		File temporaryFile = null;
		
		try
		{
			// Retrieve the session ID to embed in the generated file's name
			sessionId = request.getParameter("sid");

			MainLog.getLog().debug("Sending report export for session: " + sessionId);
			
			Report report = (Report) PortletServletSharedContext.getInstance().get("report" + sessionId);
			
			if (report != null)
			{
				// Export the file into a temporary location
				String temporaryReportLocation = 
					getServletContext().getRealPath(temporaryFilesLocation + "/report" + sessionId + ".xls"); 
				ReportExporter.export(report, temporaryReportLocation);

				temporaryFile = new File(temporaryReportLocation);
				long lengthOfFile = temporaryFile.length();
				MainLog.getLog().debug( "Length of report: " + lengthOfFile );
				
				if (lengthOfFile <= 0)
					return;
			
				SimpleDateFormat formatter = new SimpleDateFormat ("_yyyy-MM-dd_HH-mm");
				String fileName = StringUtils.replaceSubstrings(report.getReportType(), " ", "") + 
					formatter.format(new Date()) + ".xls";
				
				// set content type header before accessing the Writer
				response.setContentType("application/octet-stream");
				response.setHeader("Content-disposition", "attachment; filename=" + fileName); // report.xls
				FileInputStream is = new FileInputStream( temporaryReportLocation );
	
				// write the file out to the downloading user
				int read = 0;
				byte[] bytes = new byte[1024];
				OutputStream os = response.getOutputStream();
				while( (read = is.read(bytes)) != -1 )
					os.write(bytes, 0, read);
				os.flush();
				os.close();
				is.close();
			}
			else
			{
				response.setContentType("application/octet-stream");
				response.setHeader("Content-disposition", "attachment; filename=sessionExpired.txt");
				
				PrintWriter pw = response.getWriter();
				pw.write("No report found. Usually this means your session expired. Please run the report query again.");
				pw.flush();
				pw.close();
			}			
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to send report export to client", e);
		}
		finally
		{
			if (sessionId != null && temporaryFile != null)
			{
				try
				{
					if (!temporaryFile.delete())
						throw new IOException("Unable to delete report export temporary file");
				}
				catch (Exception e)
				{
					MainLog.getLog().error("Unable to delete report export temporary file", e);
				}
			}
		}
	}
	
	/**
	 * "Trend chart" in this context means a Microsoft Excel format file that has a graph of
	 * the report's data in it showing the 'trend'. This method generates the file, sends it to 
	 * the user, then deletes the file. To avoid conflicts with other users generating files, 
	 * the filename is constructed to include the current session number in it.
	 * 
	 * @param request HTTP request
	 * @param response HTTP response
	 */
	private void sendTrendChart(HttpServletRequest request, HttpServletResponse response)
	{
		String sessionId = null;
		File temporaryFile = null;
		
		try
		{
			// Retrieve the session ID to embed in the generated file's name
			sessionId = request.getParameter("sid");

			MainLog.getLog().debug("Sending trend chart for session: " + sessionId);
			
			AveragesReport report = (AveragesReport) PortletServletSharedContext.getInstance().get("report" + sessionId);
			
			if (report != null)
			{
				String trendChartTemplateDirectory = 
					getServletContext().getRealPath(templatesLocation);
				String temporaryTrendChartFileName = 
					getServletContext().getRealPath(temporaryFilesLocation + "/trendchart" + sessionId + ".xls");

				TrendChart trendChart = new TrendChart(trendChartTemplateDirectory);
				// Create the trend chart and place it in a temporary location
				trendChart.createChart(temporaryTrendChartFileName, report, report.getTrendChartRelevantColumns());
				
				temporaryFile = new File(temporaryTrendChartFileName);
				long lengthOfFile = temporaryFile.length();
				MainLog.getLog().debug( "Length of trendchart: " + lengthOfFile );
				
				if (lengthOfFile <= 0)
					return;

				SimpleDateFormat formatter = new SimpleDateFormat ("_yyyy-MM-dd_HH-mm");
				String fileName = StringUtils.replaceSubstrings(report.getReportType(), " ", "") + 
					"Chart" + formatter.format(new Date()) + ".xls";

				// set content type header before accessing the Writer
				response.setContentType("application/octet-stream");
				response.setHeader("Content-disposition", "attachment; filename=" + fileName); // trendchart.xls
				FileInputStream is = new FileInputStream( temporaryTrendChartFileName );
	
				// write the file out to the downloading user
				int read = 0;
				byte[] bytes = new byte[1024];
				OutputStream os = response.getOutputStream();
				while( (read = is.read(bytes)) != -1 )
					os.write(bytes, 0, read);
				os.flush();
				os.close();
				is.close();
			}
			else
			{
				response.setContentType("application/octet-stream");
				response.setHeader("Content-disposition", "attachment; filename=sessionExpired.txt");
				
				PrintWriter pw = response.getWriter();
				pw.write("No report found. Usually this means your session expired. Please run the report query again.");
				pw.flush();
				pw.close();
			}			
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to download report to client", e);
		}
		finally
		{
			if (sessionId != null && temporaryFile != null)
			{
				try
				{
					if (!temporaryFile.delete())
						throw new IOException("Unable to delete temporary file");
				}
				catch (Exception e)
				{
					MainLog.getLog().error("Unable to delete temporary file", e);
				}
			}
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		String action = request.getParameter("action");
		
		if (action == null)
			return;
		
		if (action.equals("exportreport"))
			sendReportForExport(request, response);
		else if (action.equals("createtrendchart"))
			sendTrendChart(request, response);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		doGet(request, response);
	}
	
	public String getServletInfo()
	{
		return "A servlet to manage downloads of generated Excel reports.";
	}
	
	public void sessionCreated(HttpSessionEvent se)
	{
		// MainLog.getLog().trace("\n\n\n\tSESSION CREATED: " + se.getSession().getId() + "\n\n\n\n");
	}

	public void sessionDestroyed(HttpSessionEvent se)
	{
		// MainLog.getLog().trace("\n\n\n\tSESSION DESTRUCTION IMMINENT: " + se.getSession().getId() + "\n\n\n\n");
		// MainLog.getLog().trace("\n\n\n\t(PSSC size) " + PortletServletSharedContext.getInstance().size() +"\n\n\n\n");

		/*
		 * Removes the report from the shared memory space since the session is expired, otherwise
		 * all reports will slowly accumulate in memory, unable to be garbage collected.
		 */
		PortletServletSharedContext.getInstance().remove("report" + se.getSession().getId());
		
		// MainLog.getLog().trace("\n\n\n\t(PSSC size) " + PortletServletSharedContext.getInstance().size() +"\n\n\n\n");
	}
}
