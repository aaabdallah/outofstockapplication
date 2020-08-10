package net.btlcpy.outofstock.reports.format.excel;


import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * Represents a cell style to be used if the report is exported to Microsoft Excel. It
 * contains concepts that return to the Apache POI HSSFCellStyle class.
 *  
 * @author Ahmed A. Abd-Allah
 */
public class ReportExcelCellStyle
{
	private boolean wrapText = true;
	private short alignment = HSSFCellStyle.ALIGN_LEFT;
	private short verticalAlignment = HSSFCellStyle.VERTICAL_TOP;
	private short dataFormat = 0;
	private ReportExcelFont font = new ReportExcelFont("Arial", (short) 10, HSSFColor.BLACK.index, HSSFFont.BOLDWEIGHT_NORMAL);
	private short fillPattern = HSSFCellStyle.NO_FILL;
	private short fillForegroundColor = HSSFColor.AUTOMATIC.index;
	private short fillBackgroundColor = HSSFColor.AUTOMATIC.index;
	private HSSFCellStyle hssfCellStyle = null;
	
	public ReportExcelCellStyle(boolean wrapText, short alignment, short verticalAlignment, short dataFormat, ReportExcelFont font,
		short fillPattern, short fillForegroundColor, short fillBackgroundColor)
	{
		setWrapText(wrapText);
		setAlignment(alignment);
		setVerticalAlignment(verticalAlignment);
		setDataFormat(dataFormat);
		setFont(font);
		setFillPattern(fillPattern);
		setFillForegroundColor(fillForegroundColor);
		setFillBackgroundColor(fillBackgroundColor);
	}
	
	public ReportExcelCellStyle(boolean wrapText, short alignment, short verticalAlignment, short dataFormat,
		short fillPattern, short fillForegroundColor, short fillBackgroundColor)
	{
		setWrapText(wrapText);
		setAlignment(alignment);
		setVerticalAlignment(verticalAlignment);
		setDataFormat(dataFormat);
		setFillPattern(fillPattern);
		setFillForegroundColor(fillForegroundColor);
		setFillBackgroundColor(fillBackgroundColor);
	}

	public boolean isWrapText()
	{
		return wrapText;
	}
	public void setWrapText(boolean wrapText)
	{
		this.wrapText = wrapText;
	}
	public short getAlignment()
	{
		return alignment;
	}
	public void setAlignment(short alignment)
	{
		this.alignment = alignment;
	}
	public short getVerticalAlignment()
	{
		return verticalAlignment;
	}
	public void setVerticalAlignment(short verticalAlignment)
	{
		this.verticalAlignment = verticalAlignment;
	}
	public short getDataFormat()
	{
		return dataFormat;
	}
	public void setDataFormat(short dataFormat)
	{
		this.dataFormat = dataFormat;
	}
	public ReportExcelFont getFont()
	{
		return font;
	}
	public void setFont(ReportExcelFont font)
	{
		this.font = font;
	}
	public HSSFCellStyle getHssfCellStyle()
	{
		return hssfCellStyle;
	}
	public void setHssfCellStyle(HSSFCellStyle hssfCellStyle)
	{
		this.hssfCellStyle = hssfCellStyle;
	}
	public short getFillPattern()
	{
		return fillPattern;
	}
	public void setFillPattern(short fillPattern)
	{
		this.fillPattern = fillPattern;
	}
	public short getFillForegroundColor()
	{
		return fillForegroundColor;
	}
	public void setFillForegroundColor(short fillForegroundColor)
	{
		this.fillForegroundColor = fillForegroundColor;
	}
	public short getFillBackgroundColor()
	{
		return fillBackgroundColor;
	}
	public void setFillBackgroundColor(short fillBackgroundColor)
	{
		this.fillBackgroundColor = fillBackgroundColor;
	}
}
