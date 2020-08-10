package net.btlcpy.outofstock.reports.format.excel;

import org.apache.poi.hssf.usermodel.HSSFFont;

/**
 * Represents a font that will be used in exporting a report to Microsoft Excel.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class ReportExcelFont
{
	private String name = null;
	private short heightInPoints = 0;
	private short color = 0;
	private short boldWeight = HSSFFont.BOLDWEIGHT_NORMAL;
	private HSSFFont hssfFont = null;

	public ReportExcelFont(String name, short heightInPoints, short color, short boldWeight)
	{
		setName(name);
		setHeightInPoints(heightInPoints);
		setColor(color);
		setBoldWeight(boldWeight);
	}

	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public short getHeightInPoints()
	{
		return heightInPoints;
	}
	public void setHeightInPoints(short heightInPoints)
	{
		this.heightInPoints = heightInPoints;
	}
	public short getColor()
	{
		return color;
	}
	public void setColor(short color)
	{
		this.color = color;
	}
	public HSSFFont getHssfFont()
	{
		return hssfFont;
	}
	public void setHssfFont(HSSFFont hssfFont)
	{
		this.hssfFont = hssfFont;
	}
	public short getBoldWeight()
	{
		return boldWeight;
	}
	public void setBoldWeight(short boldWeight)
	{
		this.boldWeight = boldWeight;
	}
}
