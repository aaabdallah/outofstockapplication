package net.btlcpy.outofstock.reports.format.excel;


import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * Represents formatting options for a report that is exported to Microsoft Excel.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class ReportExcelFormat
{
	private ListOrderedMap /* <String, ReportExcelCellStyle> */ regionCellStyles = null;
	private ReportExcelCellStyle defaultCellStyle = null;
	
	public ReportExcelFormat()
	{
		defaultCellStyle = 
			new ReportExcelCellStyle(true, HSSFCellStyle.ALIGN_LEFT, HSSFCellStyle.VERTICAL_TOP, (short) 0,
				HSSFCellStyle.NO_FILL, (short) HSSFColor.AUTOMATIC.index, (short) HSSFColor.AUTOMATIC.index);
	}
	
	public void addCellStyle(int x1, int y1, int x2, int y2, ReportExcelCellStyle cellStyle)
	{
		if (regionCellStyles == null)
			regionCellStyles = new ListOrderedMap();
		regionCellStyles.put(x1 + " " + y1 + " " + x2 + " " + y2, cellStyle);
	}
	
	private void toCoordinates(String regionString, int coordinates[])
	{
		int start = 0;
		int spacePosition = -1; 
		
		for (int i=0; i<4; i++)
		{
			if (i==3)
			{
				coordinates[i] = Integer.parseInt(regionString.substring(start));
				break;
			}
			spacePosition = regionString.indexOf(' ', start);
			
			coordinates[i] = Integer.parseInt(regionString.substring(start, spacePosition));
			start = spacePosition + 1;
		}
	}

	public ReportExcelCellStyle getDefaultCellStyle()
	{
		return defaultCellStyle;
	}
	public void setDefaultCellStyle(ReportExcelCellStyle defaultCellStyle)
	{
		this.defaultCellStyle = defaultCellStyle;
	}
	
	public ReportExcelCellStyle getCellStyle(int x, int y)
	{
		ReportExcelCellStyle cellStyle = defaultCellStyle;
		
		// zero based
		if (regionCellStyles != null)
		{
			int coordinates[] = new int[4];
			for (int i=0; i<regionCellStyles.size(); i++)
			{	// Note: later regions take precedence.
				String regionString = (String) regionCellStyles.get(i);
				toCoordinates(regionString, coordinates);
				if (coordinates[0] <= x && x <= coordinates[2] &&
					coordinates[1] <= y && y <= coordinates[3])
					cellStyle = (ReportExcelCellStyle) regionCellStyles.getValue(i);
			}
		}
		return cellStyle;
	}
}
