package net.btlcpy.outofstock.spreadsheetwriters;

/**
 * A class that represents the base for all spreadsheet writers (classes that are used to
 * generate Microsoft Excel spreadsheets).
 * 
 * @author Ahmed A. Abd-Allah
 */
abstract public class SpreadsheetWriter
{
	/**
	 * A simple method that replaces certain HTML tokens from the report data (if any).
	 * This method is used to translate HTML-formatted column names (and other strings
	 * if necessary) to a format that is suitable for display within Excel.
	 * 
	 * @param s the string to parse and strip
	 * @return
	 */
	protected static String stripHTML(String s)
	{
		StringBuffer sb = null;
		int position = -1;
		
		do
		{
			position = s.indexOf("<br/>");
			if (position < 0)
				break;
			sb = new StringBuffer(s);
			sb.replace(position, position+5, "\n");
			s = sb.toString();
		} while (true);
		
		do
		{
			position = s.indexOf("&nbsp;");
			if (position < 0)
				break;
			sb = new StringBuffer(s);
			sb.replace(position, position+6, " ");
			s = sb.toString();
		} while (true);
		
		return s;
	}
}
