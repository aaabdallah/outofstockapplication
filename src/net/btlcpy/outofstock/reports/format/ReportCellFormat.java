package net.btlcpy.outofstock.reports.format;

import java.text.Format;
import java.util.HashMap;

/**
 * <p>
 * Represents how to format a cell in a report that is displayed within a Web browser.
 * Currently only supports two notions: the CSS style to use, and how to format the dates,
 * messages, and numbers a la Java's <code>java.text.Format</code> class.
 * </p>
 * <p>
 * Data is assumed to be arranged within the cell as a column. Individual elements in this
 * column can be formatted and CSS-styled, and the entire column can be CSS-styled as well.
 * Finally, default styles can be added for the individual elements in the column as well
 * as default formatters. This makes a grand total of three CSS styles at play here: one
 * for the entire column (usually rendered as a table), one as the default style of individual
 * elements in the column, and one each per individual element in the column.
 * </p>
 * <p>
 * Note that the "cell of a report" is ultimately best rendered as a table itself. Hence
 * references throughout this class to a 'column of data'.
 * </p>
 * @author Ahmed A. Abd-Allah
 */
public class ReportCellFormat
{
	/** A space separated list of CSS styles to use for the *entire* cell (this
	 * can be used as a style for a table which contains the entire column of
	 * data). */
	private String cssStyle = null;
	
	/** The sub styles to use for the individual rows in the column of data (this
	 * can be used to format individual cells in the table mentioned above for
	 * the string <code>cssStyle</code>. */
	private HashMap /*<Integer, String>*/ subCellCssStyles = null;
	
	/** Formatters to use for the individual data elements in the column of 
	 * data. */
	private HashMap /*<Integer, Format>*/ formatters = null;
	
	public ReportCellFormat(String cssStyle)
	{
		addCssStyle(cssStyle);
	}
	
	public String getCssStyle()
	{
		return cssStyle;
	}

	/**
	 * Appends a CSS style to the existing one for the entire column of data.
	 * 
	 * @param cssStyle the new style(s) to append, e.g. "right", "right blue"
	 */
	public void addCssStyle(String cssStyle)
	{
		if (cssStyle != null)
		{
			cssStyle = cssStyle.trim();
			if (cssStyle.length() >= 0)
			{
				if (this.cssStyle == null)
					this.cssStyle = cssStyle;
				else
					this.cssStyle += " " + cssStyle;
				return;
			}
		}
	}
	
	/**
	 * Removes all style information for the entire column of data, but
	 * <b>NOT</b> the individual elements.
	 */
	public void clearCssStyle()
	{
		cssStyle = null;
	}

	/**
	 * Retrieves the specified CSS style for an individual element in the column
	 * of data.
	 * @param position the desired element (0-based). If -1, retrieve the default
	 * CSS style used for all elements.
	 * @return the style if found, else null
	 */
	public String getSubCellCssStyle(int position)
	{
		if (position < -1)
			throw new IllegalArgumentException("Positions of subcell styles must be -1 or more");
		if (subCellCssStyles == null || subCellCssStyles.size() == 0)
			return null;
		
		String subCellCssStyle = (String) subCellCssStyles.get(new Integer(position));
		
		// Look for default style for all items in cell
		if (subCellCssStyle == null)
			subCellCssStyle = (String) subCellCssStyles.get(new Integer(-1));
		
		return subCellCssStyle;
	}

	/**
	 * Add a new CSS style for an individual element in the column of data. The position is
	 * 0-based, but a position of -1 means all elements (i.e. use that style as the default
	 * which can be overridden if another style is later added for a specific position). If
	 * the parameter <code>subCellCssStyle</code> is null, this <b>removes</b> the style for 
	 * the specified position.
	 * 
	 * @param position zero-based position, allowing for -1 meaning the default
	 * @param subCellCssStyle the CSS style(s) to use
	 */
	public void addSubCellCssStyle(int position, String subCellCssStyle)
	{
		if (subCellCssStyle != null)
		{
			if (position < -1)
				throw new IllegalArgumentException("Positions of subcell styles must be -1 or more");
			if (subCellCssStyles == null)
			{
				if (subCellCssStyle == null)
					return;
				subCellCssStyles = new HashMap();
			}
			
			Integer oPosition = new Integer(position);
			
			if (subCellCssStyle == null)
			{
				subCellCssStyles.remove(oPosition);
				return;
			}
			
			String oldSubCellCssStyle = (String) subCellCssStyles.get(oPosition);
			if (oldSubCellCssStyle == null)
				subCellCssStyles.put(oPosition, subCellCssStyle);
			else
				subCellCssStyles.put(oPosition, oldSubCellCssStyle + " " + subCellCssStyle);
		}
	}

	/**
	 * Remove all styles associated with the individual elements in the column of
	 * data, but leaves the CSS style associated with the entire column.
	 */
	public void clearSubCellCssStyles()
	{
		if (subCellCssStyles != null)
			subCellCssStyles.clear();
	}

	/**
	 * Gets the formatter used for the individual element at the position specified.
	 * 
	 * @param position 0-based, with -1 meaning the default formatter used
	 * @return the formatter, null if not found
	 */
	public Format getFormatter(int position)
	{
		if (position < -1)
			throw new IllegalArgumentException("Positions of formatters must be -1 or more");
		if (formatters == null || formatters.size() == 0)
			return null;
		Format formatter = (Format) formatters.get(new Integer(position));
		
		// Look for default formatter for all items in cell
		if (formatter == null)
			formatter = (Format) formatters.get(new Integer(-1));
		
		return formatter;
	}

	/**
	 * Adds a formatter for an individual element in the column of data. If the position
	 * (which is zero-based) is -1, then a default formatter is set which is used to
	 * format all elements in the column unless overridden with another formatter for
	 * a specific position.
	 * 
	 * @param position position of an individual element, -1 meaning all elements
	 * @param formatter a <code>java.text.Format</code> formatter
	 */
	public void addFormatter(int position, Format formatter)
	{
		if (position < -1)
			throw new IllegalArgumentException("Positions of formatters must be -1 or more");
		if (formatters == null)
		{
			if (formatter == null)
				return;
			formatters = new HashMap();
		}
		
		Integer oPosition = new Integer(position);
		
		if (formatter == null)
		{
			formatters.remove(oPosition);
			return;
		}
		
		formatters.put(oPosition, formatter);
	}
	
	/**
	 * Removes all formatters associated with the column of data.
	 */
	public void clearFormatters()
	{
		if (formatters != null)
			formatters.clear();
	}
}
