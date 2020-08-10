package net.btlcpy.outofstock.spreadsheetwriters;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.reports.AveragesReport;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * <i>MAJOR ASSUMPTION: TREND CHARTS ONLY PLOT UP TO ONE YEAR'S WORTH OF DATA (53 WEEKS AT MOST).</i>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class TrendChart extends SpreadsheetWriter
{
	private String templateDirectory = null;
	
	public TrendChart(String templateDirectory)
	{
		setTemplateDirectory(templateDirectory);
	}

	public String getTemplateDirectory()
	{
		return templateDirectory;
	}

	public void setTemplateDirectory(String templateDirectory)
	{
		this.templateDirectory = templateDirectory;
	}

	public void createChart(String outputFileName, AveragesReport report, String[] relevantColumns)
		throws IOException
	{
		if (report == null)
			throw new IllegalArgumentException("No report found");
		if (relevantColumns == null || relevantColumns.length <= 0 || relevantColumns.length > 2)
			throw new IllegalArgumentException("Must specify 1 or 2 columns to plot from report");
		
		Map queryResults = report.getResults();

		if (queryResults == null || 
			queryResults.size() == 0 ||
			queryResults.get("query0") == null ||
			((Map) queryResults.get("query0")).size() == 0)
		{
			throw new IllegalArgumentException("No data found in report");
		}

		Map oneQueryResults = (Map) queryResults.get("query0");
		double weekValues[][] = new double[ relevantColumns.length ][ oneQueryResults.size() ];
		Set keys = oneQueryResults.keySet(); // every key maps to a row of average lost sales data per week
		Iterator iterator = keys.iterator();
		
		int rowCounter = 0;
		while (iterator.hasNext())
		{
			Object key = iterator.next();
			Map row = (Map) oneQueryResults.get(key);
			
			for (int j=0; j<relevantColumns.length; j++)
				weekValues[j][rowCounter] = Double.parseDouble( ((ArrayList) row.get( relevantColumns[j] )).get(0).toString() );
			
			rowCounter++;
		}

		FileInputStream chartInputStream = null;
		FileOutputStream chartOutputStream = null;
		HSSFWorkbook workbook = null;
		boolean donePreparingChart = false;
	
		try
		{
			HSSFSheet sheet = null;
			HSSFRow sheetRow = null;
			HSSFCell sheetCell = null;
	
			chartInputStream = new FileInputStream(templateDirectory + "/AverageLossTemplate" + /*relevantColumns.length +*/ ".xls");
			POIFSFileSystem poifsFileSystem = new POIFSFileSystem(chartInputStream);
			workbook = new HSSFWorkbook(poifsFileSystem, true);
			
			// write the report parameters
			sheet = workbook.getSheet("Parameters");
			sheetRow = sheet.getRow(0);
			sheetCell = sheetRow.createCell( (short) 1, HSSFCell.CELL_TYPE_STRING);
			sheetCell.setCellValue(new HSSFRichTextString( report.getExcelReportDescription() ));
			HSSFCellStyle hssfCellStyle = workbook.createCellStyle();
			hssfCellStyle.setWrapText(true);
			hssfCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
			hssfCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
			sheetCell.setCellStyle(hssfCellStyle);

			// write number of weeks of data to Sheet3, B2
			sheetRow = sheet.getRow(1);
			sheetCell = sheetRow.getCell( (short) 1);
			sheetCell.setCellValue( oneQueryResults.size() );
			
			// write the letter of the last column of relevant data to Sheet3, B3
			sheetRow = sheet.getRow(2);
			sheetCell = sheetRow.getCell( (short) 1);
			char lastColumnLetter[] = { 'A' };
			lastColumnLetter[0] += relevantColumns.length;
			sheetCell.setCellValue( new HSSFRichTextString( new String(lastColumnLetter) ) );

			// -- Problematic on Linux --
			for (short i=0; i<2; i++)
			{
				// ZZZ:Experiment sheet.autoSizeColumn(i);
				// Add 10% because on Linux, autosizing acts differently
				// ZZZ:Experiment sheet.setColumnWidth(i, (short) (sheet.getColumnWidth(i) * 1.1));
			}

			// write each week's worth of data to Sheet2
			sheet = workbook.getSheet("Weekly Data");
			// HSSFRichTextString emptyData = new HSSFRichTextString("");
	
			Calendar calendar = Calendar.getInstance(Locale.US);
			calendar.setTime(report.getBeginDate());
	
			// Write the exact dates to the first column in the weekly data sheet
			for (int i=0; i<oneQueryResults.size(); i++)
			{
				// Note the first row is presumed to have header info not real data
				sheetRow = sheet.getRow(i+1);
				sheetCell = sheetRow.getCell((short) 0);
				sheetCell.setCellValue( new HSSFRichTextString(new Date(calendar.getTime().getTime()).toString() ) );
				calendar.add(Calendar.DAY_OF_YEAR, 7);
			}

			// Write the names of the columns along the top
			for (int i=0; i<relevantColumns.length; i++)
			{
				sheetRow = sheet.getRow(0);
				sheetCell = sheetRow.getCell((short) (i+1));
				sheetCell.setCellValue( new HSSFRichTextString(stripHTML(relevantColumns[i])) );
			}
			
			// Write the data across columns/rows
			for (int i=0; i<relevantColumns.length; i++)
			{
				for (int j=0; j<oneQueryResults.size(); j++)
				{
					// Note the first row is presumed to have header info not real data
					sheetRow = sheet.getRow(j+1);
					// Likewise, the first column is assumed to say "Week (i+1)"
					sheetCell = sheetRow.getCell((short) (i+1));
					sheetCell.setCellValue( weekValues[i][j] );
				}
			}

			// Erase all unused rows (after the bottom of the data)
			for (int i=oneQueryResults.size(); i<53; i++)
			{
				// Note the first row is presumed to have header info not real data
				sheetRow = sheet.getRow(i+1);
				if (sheetRow != null) sheet.removeRow(sheetRow);
			}
			
			// Erase all unused columns (after the right edge of the data)
			for (int i=relevantColumns.length+1; i<5; i++)
			{
				for (short j=0; j<54; j++)
				{
					sheetRow = sheet.getRow(j);
					if (sheetRow != null)
					{
						sheetCell = sheetRow.getCell((short) i);
						if (sheetCell != null) sheetRow.removeCell(sheetCell);
					}
				}
			}

			donePreparingChart = true;
		}
		finally
		{
			if (chartInputStream != null)
			{
				try { chartInputStream.close(); } catch (Exception e) {}
			}
	
			try
			{
				if (donePreparingChart)
				{
					chartOutputStream = new FileOutputStream(outputFileName);
					if (chartOutputStream != null)
					{
						if (workbook != null)
							workbook.write(chartOutputStream);
						chartOutputStream.close();
					}
				}
			}
			catch (Exception e)
			{
				MainLog.getLog().error("Error while closing chart", e);
			}
		}
	}

	/*
	private void test(String outputFileName)
		throws IOException
	{
		// Only need one of these in theory but since we only have templates for 1, 2, and 10 years, we need both.
		int templateNumber = 1, lastColumnOfData = 0;
		FileInputStream chartInputStream = null;
		FileOutputStream chartOutputStream = null;
		HSSFWorkbook workbook = null;
		boolean donePreparingChart = false;
	
		templateNumber = 1;
		lastColumnOfData = 0;

		try
		{
			HSSFSheet sheet = null;
			HSSFRow sheetRow = null;
			HSSFCell sheetCell = null;
	
			chartInputStream = new FileInputStream(templateDirectory + "/NewAverageLossTemplate" + templateNumber + ".xls");
			POIFSFileSystem poifsFileSystem = new POIFSFileSystem(chartInputStream);
			workbook = new HSSFWorkbook(poifsFileSystem, true);
			
			// write number of weeks of data to Sheet3, B1
			sheet = workbook.getSheet("Sheet3");
			sheetRow = sheet.getRow(0);
			sheetCell = sheetRow.getCell( (short) 1);
			// TEMPORARILY OVERRIDDEN TILL WE SEE HOW THE REPORTS LOOK
			// sheetCell.setCellValue( numberOfWeeks );
			sheetCell.setCellValue( 53 );
	
			// write each week's worth of data to Sheet2
			sheet = workbook.getSheet("Sheet2");
			HSSFRichTextString emptyData = new HSSFRichTextString("");
	
			for (int i=0; i<=lastColumnOfData; i++)
			{
				for (int j=0; j<53; j++)
				{
					// Note the first row is presumed to have header info not real data
					sheetRow = sheet.getRow(j+1);
					// Likewise, the first column is assumed to say "Week (i+1)"
					sheetCell = sheetRow.getCell((short) (i));
					sheetCell.setCellValue( new HSSFRichTextString("Week 50") );
					// sheetRow.removeCell(sheetCell);
					// sheetCell.setCellValue( emptyData );
				}
			}
			
			donePreparingChart = true;
		}
		finally
		{
			if (chartInputStream != null)
			{
				try { chartInputStream.close(); } catch (Exception e) {}
			}
	
			try
			{
				if (donePreparingChart)
				{
					chartOutputStream = new FileOutputStream(outputFileName);
					if (chartOutputStream != null)
					{
						if (workbook != null)
							workbook.write(chartOutputStream);
						chartOutputStream.close();
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[])
	{
		TrendChart chart = new TrendChart("view/reports/generate/templates");
		
		try
		{
			System.out.print("Starting...");
			chart.test("output.xls");
			System.out.println(" Done.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	*/
}
