package net.btlcpy.outofstock.reports;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * <p>
 * A class used when sorting a set of rows retrieved from a query. Each row is assumed to be
 * ListOrderedMap mapping column names to values. The class will be used to compare two rows
 * using a set of columns to compare, with provision made for whether the desired sort is
 * ascending or descending.
 * </p>
 * <p>
 * Note that columns as expressed here can either be expressed as plain column names, meaning
 * the value of the data for that column is a single value, or they can be expressed as 
 * column names followed by a number (separated by a colon), as in "names:2" or "amount:0"
 * where this indicates the value is a column of data, and the sort should be done on the
 * element in position X - the number, which is zero-based. So "names:2" would mean sort
 * using the third element in the data found in each row under the column "names". It
 * assumes that the "names" column has data in each row consisting of a column of data
 * itself, at least 3 elements high.
 * </p>
 * @author Ahmed A. Abd-Allah
 */
public class ReportRowComparator implements Comparator
{
	/** The columns to use when comparing two rows. The order is important:
	 * as soon as a nonequal relationship is found between a pair of values
	 * for a column, <b>all later columns are ignored.</b> 
	 */
	private String[] columnsToCompare = null;
	
	/** Whether the sort should be ascending or not. */
	private boolean ascending = true;
	
	public ReportRowComparator(String[] columnsToCompare, boolean ascending)
	{
		this.columnsToCompare = columnsToCompare;
		this.ascending = ascending;
	}

	/**
	 * Given a column to sort on, that may be of the form "column:position" e.g.
	 * "name:2", this extracts the column name.
	 * 
	 * @param columnWithPosition a column with a position potentially.
	 * @return the column name, null if not found
	 */
	private String extractName(String columnWithPosition)
	{
		if (columnWithPosition == null) return null;
		
		int positionOfColon = columnWithPosition.indexOf(":");
		
		if (positionOfColon < 0) return columnWithPosition;
		
		return columnWithPosition.substring(0, positionOfColon);
	}

	/**
	 * Given a column to sort on, that may be of the form "column:position" e.g.
	 * "name:2", this extracts the position.
	 * 
	 * @param columnWithPosition a column with a position potentially.
	 * @return the position, -1 if no position
	 */
	private int extractPosition(String columnWithPosition)
	{
		if (columnWithPosition == null) return -1;

		int positionOfColon = columnWithPosition.indexOf(":");
		
		if (positionOfColon < 0) return -1;
		
		return Integer.parseInt(columnWithPosition.substring(positionOfColon+1, columnWithPosition.length()));
	}

	public int compare(Object o1, Object o2)
	{
		if (columnsToCompare == null || columnsToCompare.length == 0 ||
			(o1 == null && o2 == null))
			return 0;

		ListOrderedMap row1 = (ListOrderedMap) o1;
		ListOrderedMap row2 = (ListOrderedMap) o2;
		
		// compare across all columns used for the comparison. If a nonequal
		// relationship is found for a particular column, exit the loop and
		// return (later columns are ignored).
		for (int i=0; i<columnsToCompare.length; i++)
		{
			if (columnsToCompare[i] == null)
				continue;

			Object value1 = row1.get(extractName(columnsToCompare[i]));
			Object value2 = row2.get(extractName(columnsToCompare[i]));
			
			if (value1 instanceof Comparable) // Scalar? Compare directly
			{
				int result = ((Comparable) value1).compareTo((Comparable) value2);
			
				if (result != 0)
					return ascending ? result : -result;
			}
			else if (value1 instanceof List) // List? Use position to determine what to compare
			{
				int position = extractPosition(columnsToCompare[i]);
				Object subValue1 = ((List) value1).get(position);
				Object subValue2 = ((List) value2).get(position);
				
				int result = ((Comparable) subValue1).compareTo((Comparable) subValue2);
				
				if (result != 0)
					return ascending ? result : -result;
			}
		}

		return 0;
	}
}
