package net.btlcpy.outofstock.spreadsheetwriters;

import java.io.FileOutputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.reports.Report;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelCellStyle;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFont;
import net.btlcpy.outofstock.reports.format.excel.ReportExcelFormat;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.Region;

/**
 * This class is responsible for generating Microsoft Excel compatible versions of the
 * reports that are run using the reports portlet. The Apache POI library does all the
 * heavy lifting; this class simply makes use of the POI API to generate the file.
 *  
 * @author Ahmed A. Abd-Allah
 */
public class ReportExporter extends SpreadsheetWriter
{
	/**
	 * A method which exports a Report object to an Excel file. The method uses
	 * the POI library to do the export. The contents of the report, if understood
	 * as a table, are simply written out cell by cell into a worksheet. Each
	 * report may have different sections (queries); each section becomes a
	 * separate worksheet. For each section, a supplementary worksheet is added
	 * describing that section. Note that each section is called a 'report', hence
	 * it may look slightly confusing: every Report object is exported to an
	 * Excel workbook that has multiple reports in it, each report being contained
	 * in two spreadsheets (one that has the data, another that has the description).
	 * 
	 * @param report the Report object to export
	 * @param fileLocation the location of the file to write to
	 */
	public static void export(Report report, String fileLocation)
	{
		HSSFWorkbook workbook = null;
		FileOutputStream fileOut = null;

		try
		{
			int queryCounter = 0;
			HSSFSheet sheet = null;
			HSSFRow sheetRow = null;
			HSSFCell sheetCell = null;
			int sheetRowNumber = 0;
			short sheetCellNumber = 0;
			ReportExcelCellStyle reportExcelCellStyle = null;
			
			MainLog.getLog().debug("Attempting to export a report.");

			workbook = new HSSFWorkbook();
			fileOut = new FileOutputStream(fileLocation);
			
			if (report != null)
			{
				Map queryResults = report.getResults();

				String[][] columnNames = null;

				if (queryResults == null || queryResults.size() == 0)
				{
					sheet = workbook.createSheet("Report");
					
					sheetRow = sheet.createRow(sheetRowNumber++);
					sheetCellNumber = 0;
					sheetCell = sheetRow.createCell(sheetCellNumber++, HSSFCell.CELL_TYPE_STRING);
					sheetCell.setCellValue(new HSSFRichTextString("No data in entire report."));
					return;
				}
				
				Set queryIndexes = queryResults.keySet();
				columnNames = new String[queryIndexes.size()][];

				Iterator queryIndexIterator = queryIndexes.iterator();
				while (queryIndexIterator.hasNext())
				{
					sheetRowNumber = 0;
					sheet = workbook.createSheet("Report " + (queryCounter + 1));
					String queryIndex = (String) queryIndexIterator.next();

					ReportExcelFormat excelFormat = report.getExcelFormat(queryCounter);
					
					boolean namesIdentified = false;
					Map oneQueryResults = (Map) queryResults.get(queryIndex);
					if (oneQueryResults == null)
					{
						sheetRow = sheet.createRow(sheetRowNumber++);
						sheetCellNumber = 0;
						sheetCell = sheetRow.createCell(sheetCellNumber++, HSSFCell.CELL_TYPE_STRING);
						
						sheetCell.setCellValue(new HSSFRichTextString("No data in report " + (queryCounter + 1) + "."));
					}
					else
					{
						Set keys = oneQueryResults.keySet();
						Iterator iterator = keys.iterator();
						
						while (iterator.hasNext())
						{
							Object key = iterator.next();
							ListOrderedMap row = (ListOrderedMap) oneQueryResults.get(key);
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
								
								sheetRow = sheet.createRow(sheetRowNumber);
								sheetCellNumber = 0;
								for (int i=0; i<columnNames[queryCounter].length; i++)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_STRING);
									sheetCell.setCellValue( new HSSFRichTextString(stripHTML(columnNames[queryCounter][i])) );
									//sheetCell.setCellStyle(cellStyleForColumnNames);
									reportExcelCellStyle = excelFormat.getCellStyle(sheetCellNumber, sheetRowNumber);
									if (reportExcelCellStyle.getHssfCellStyle() == null)
										createHssfCellStyle(workbook, reportExcelCellStyle);
									sheetCell.setCellStyle(reportExcelCellStyle.getHssfCellStyle());
									sheetCellNumber++;
								}
								sheetRowNumber++;
							}
							
							sheetRow = sheet.createRow(sheetRowNumber);
							sheetCellNumber = 0;
							for (int i=0; i<columnNames[queryCounter].length; i++)
							{
								Object value = row.get(columnNames[queryCounter][i]);
								String dataKey = (String) row.get(i);

								if (value == null || dataKey.startsWith("__IGNORE__"))
								{
									sheetCellNumber++;
									continue;
								}
								if (value instanceof Timestamp)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_NUMERIC);
									sheetCell.setCellValue( 
										HSSFDateUtil.getExcelDate(
											new java.util.Date( ((Timestamp) value).getTime() ) ) );
								}
								else if (value instanceof Date)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_NUMERIC);
									sheetCell.setCellValue( 
										HSSFDateUtil.getExcelDate(
											new java.util.Date( ((Date) value).getTime() ) ) );
								}
								else if (value instanceof Number)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_NUMERIC);
									sheetCell.setCellValue( ((Number) value).doubleValue() );
								}
								else if (value instanceof String)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_STRING);
									sheetCell.setCellValue( new HSSFRichTextString((String) value) );
								}
								else if (value instanceof ArrayList)
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_STRING);
									ArrayList values = (ArrayList) value;
									
									StringBuffer valuesString = new StringBuffer("");
									for (int j=0; j<values.size(); j++)
									{
										if (j>0)
											valuesString.append('\n');
										if (values.get(j) instanceof Number)
											valuesString.append( Math.round(((Number) values.get(j)).doubleValue()) );
										else
											valuesString.append(values.get(j).toString());
									}
									sheetCell.setCellValue( new HSSFRichTextString(valuesString.toString()) );
								}
								else
								{
									sheetCell = sheetRow.createCell(sheetCellNumber, HSSFCell.CELL_TYPE_STRING);
									sheetCell.setCellValue( new HSSFRichTextString( value.getClass().getName() ) );
								}

								reportExcelCellStyle = excelFormat.getCellStyle(sheetCellNumber, sheetRowNumber);
								if (reportExcelCellStyle.getHssfCellStyle() == null)
									createHssfCellStyle(workbook, reportExcelCellStyle);
								sheetCell.setCellStyle(reportExcelCellStyle.getHssfCellStyle());
								
								int heightOfMergedRegion = report.getHeightOfMergedRegion(queryCounter, i, sheetRowNumber-1);
								int widthOfMergedRegion = report.getWidthOfMergedRegion(queryCounter, i, sheetRowNumber-1);
								if (heightOfMergedRegion != -1)
								{
									//MainLog.getLog().debug("Adding merged region: " + 
									//	sheetRowNumber + ", " + i + " to " + 
									//	(sheetRowNumber + heightOfMergedRegion - 1) + ", " + (i + widthOfMergedRegion - 1));
									sheet.addMergedRegion(
										new Region(sheetRowNumber, (short) i,  
											sheetRowNumber + heightOfMergedRegion - 1, (short) (i + widthOfMergedRegion - 1)));
								}
								sheetCellNumber++;
							}
							sheetRowNumber++;
						}
					}
				
					// -- Problematic on Linux --
					for (short i=0; i<columnNames[queryCounter].length; i++)
					{
						// ZZZ:Experiment sheet.autoSizeColumn(i);
						// Add 10% because on Linux, autosizing acts differently
						// ZZZ:Experiment sheet.setColumnWidth(i, (short) (sheet.getColumnWidth(i) * 1.1));
					}
					
					sheet = workbook.createSheet("Report " + (queryCounter + 1) + " Description");
					sheetRowNumber = 0;
					sheetRow = sheet.createRow(sheetRowNumber++);
					sheetCellNumber = 0;
					sheetCell = sheetRow.createCell(sheetCellNumber++, HSSFCell.CELL_TYPE_STRING);
					sheetCell.setCellValue(new HSSFRichTextString("Report Parameters"));
					if (excelFormat.getDefaultCellStyle().getHssfCellStyle() == null)
						createHssfCellStyle(workbook, excelFormat.getDefaultCellStyle());
					sheetCell.setCellStyle(excelFormat.getDefaultCellStyle().getHssfCellStyle());

					sheetCell = sheetRow.createCell(sheetCellNumber++, HSSFCell.CELL_TYPE_STRING);
					sheetCell.setCellValue(new HSSFRichTextString( report.getExcelReportDescription() ));
					sheetCell.setCellStyle(excelFormat.getDefaultCellStyle().getHssfCellStyle());
					
					// -- Problematic on Linux --
					for (short i=0; i<2; i++)
					{
						// ZZZ:Experiment sheet.autoSizeColumn(i);
						// Add 10% because on Linux, autosizing acts differently
						// ZZZ:Experiment sheet.setColumnWidth(i, (short) (sheet.getColumnWidth(i) * 1.1));
					}

					queryCounter++;
				}
			}

		}
		catch (Exception e)
		{
			MainLog.getLog().error("Error while exporting", e);
		}
		finally
		{
			try
			{
				if (fileOut != null)
				{
					if (workbook != null)
						workbook.write(fileOut);
					fileOut.close();
				}
			}
			catch (Exception e)
			{
				MainLog.getLog().error("Error while closing file during export", e);
			}
		}
	}
	
	private static HSSFCellStyle createHssfCellStyle(HSSFWorkbook workbook, ReportExcelCellStyle reportExcelCellStyle)
	{
		HSSFCellStyle hssfCellStyle = workbook.createCellStyle();
		hssfCellStyle.setWrapText(reportExcelCellStyle.isWrapText());
		hssfCellStyle.setAlignment(reportExcelCellStyle.getAlignment());
		hssfCellStyle.setVerticalAlignment(reportExcelCellStyle.getVerticalAlignment());
		hssfCellStyle.setDataFormat(reportExcelCellStyle.getDataFormat());
		hssfCellStyle.setFillPattern(reportExcelCellStyle.getFillPattern());
		hssfCellStyle.setFillForegroundColor(reportExcelCellStyle.getFillForegroundColor());
		hssfCellStyle.setFillBackgroundColor(reportExcelCellStyle.getFillBackgroundColor());

		hssfCellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
		hssfCellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
		hssfCellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		hssfCellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		hssfCellStyle.setTopBorderColor(HSSFColor.GREY_25_PERCENT.index);
		hssfCellStyle.setRightBorderColor(HSSFColor.GREY_25_PERCENT.index);
		hssfCellStyle.setBottomBorderColor(HSSFColor.GREY_25_PERCENT.index);
		hssfCellStyle.setLeftBorderColor(HSSFColor.GREY_25_PERCENT.index);
		
		ReportExcelFont reportExcelFont = reportExcelCellStyle.getFont();
		if (reportExcelFont.getHssfFont() == null)
		{
			HSSFFont hssfFont = workbook.createFont();
			hssfFont.setFontName(reportExcelFont.getName());
			hssfFont.setFontHeightInPoints(reportExcelFont.getHeightInPoints());
			hssfFont.setColor(reportExcelFont.getColor());
			hssfFont.setBoldweight(reportExcelFont.getBoldWeight());
			
			reportExcelFont.setHssfFont(hssfFont);
		}
		hssfCellStyle.setFont(reportExcelFont.getHssfFont());
		
		reportExcelCellStyle.setHssfCellStyle(hssfCellStyle);

		return hssfCellStyle;
	}
}
