package net.btlcpy.outofstock.reports.format;

/**
 * Represents a column of data within a report. Each column is assumed to have three types
 * of cells: header cells, data cells, and footer cells. Each type of cell can have
 * associated with it a ReportCellFormat.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class ReportColumnFormat
{
	private ReportCellFormat headerCellFormat = null;
	private ReportCellFormat dataCellFormat = null;
	private ReportCellFormat footerCellFormat = null;
	
	public ReportColumnFormat(ReportCellFormat headerCellFormat, ReportCellFormat dataCellFormat, ReportCellFormat footerCellFormat)
	{
		this.headerCellFormat = headerCellFormat;
		this.dataCellFormat = dataCellFormat;
		this.footerCellFormat = footerCellFormat;
	}

	public ReportCellFormat getHeaderCellFormat()
	{
		return headerCellFormat;
	}

	public void setHeaderCellFormat(ReportCellFormat headerCellFormat)
	{
		this.headerCellFormat = headerCellFormat;
	}

	public ReportCellFormat getDataCellFormat()
	{
		return dataCellFormat;
	}

	public void setDataCellFormat(ReportCellFormat dataCellFormat)
	{
		this.dataCellFormat = dataCellFormat;
	}

	public ReportCellFormat getFooterCellFormat()
	{
		return footerCellFormat;
	}

	public void setFooterCellFormat(ReportCellFormat footerCellFormat)
	{
		this.footerCellFormat = footerCellFormat;
	}
}
