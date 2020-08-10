package net.btlcpy.outofstock.utilities;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * <p>A thin wrapper class for constructing an SQL select statement semi-programmatically.
 * The class does not attempt to validate individual pieces or syntax. It is a simple
 * way to put the pieces of an SQL statement together where a piece is a condition in
 * WHERE for example, or a GROUP BY column, or a column to ORDER BY. The six "pieces":
 * </p>
 * 
 * <ul>
 * <li>select - i.e. the columns to select</li>
 * <li>from - i.e. the tables or nested selects to select from</li>
 * <li>where - i.e. conditions</li>
 * <li>group - i.e. group by columns
 * <li>having - i.e. having conditions on the group by columns
 * <li>order - i.e. order by columns 
 * </ul>
 * 
 * <p>Note that since the need to add 'from's is fairly common for a complicated SQL
 * statement, and that the possibility of listing the same table twice is high, this class
 * does check against that possible error. If duplicate tables are desired, one can do
 * so by forcing inclusion of the same table twice in the "FROM" fragment (see method
 * {@link SelectBuilder#addFrom(String, boolean)}).
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class SelectBuilder
{
	private StringBuffer select, where, group, having, order;
	// The 'from' element needs care to avoid duplicate tables, so simple searching is important. 
	private ArrayList from;

	public SelectBuilder()
	{
	}

	public SelectBuilder(String s, String f, String w, String g, String h, String o)
	{
		addSelect(s);
		addFrom(f);
		addWhere(w, true);
		addGroup(g);
		addHaving(h, true);
		addOrder(o);
	}

	public SelectBuilder(String s, SelectBuilder f, String w, String g, String h, String o)
	{
		addSelect(s);
		addFrom(f);
		addWhere(w, true);
		addGroup(g);
		addHaving(h, true);
		addOrder(o);
	}

	/**
	 * @param s examples: "firstname", "lastname, employees.salary", "*", "lastname AS surname" 
	 */
	public void addSelect(String s)
	{
		if (s != null)
		{
			if (select == null)
				select = new StringBuffer("SELECT ");
			else
				select.append(", ");
			select.append(s);
		}
	}

	/**
	 * @param f examples: "employees", "countries, states, cities"
	 */
	public void addFrom(String f)
	{
		addFrom(f, false);
	}

	/**
	 * @param f examples: "employees", "countries, states, cities"
	 * @param forceDuplicate true means allow duplicate table names, otherwise
	 * they are filtered to only allow one instance
	 */
	public void addFrom(String f, boolean forceDuplicate)
	{
		if (f != null)
		{
			if (from == null)
				from = new ArrayList();

			StringTokenizer tokenizer = new StringTokenizer(f, ",");
			while (tokenizer.hasMoreTokens())
			{
				String table = tokenizer.nextToken();
				if (forceDuplicate || !from.contains(table.trim()))
					from.add(table.trim());
			}
		}
	}
	
	/**
	 * Note this method converts the passed in select IMMEDIATELY to a string,
	 * so using this method and then updating the nested select will NOT result
	 * in the changes being propagated to the enclosing select.
	 * 
	 * @param s a SelectBuilder that represents a nested select
	 */
	public void addFrom(SelectBuilder s)
	{
		if (s != null)
		{
			if (from == null)
				from = new ArrayList();
			from.add("( " + s.toString() + " )");
		}		
	}

	/**
	 * @param w examples: "age > 10", "age > 10 AND weight > 100"
	 * @param and if true, the new condition is 'anded' else it is 'ored' to 
	 * the rest of the conditions
	 */
	public void addWhere(String w, boolean and)
	{
		if (w != null)
		{
			if (where == null)
				where = new StringBuffer(" WHERE ");
			else
				if (and) where.append(" AND "); 
				else where.append(" OR ");
			where.append(w);
		}
	}

	/**
	 * @param g examples: "department"
	 */
	public void addGroup(String g)
	{
		if (g != null)
		{
			if (group == null)
				group = new StringBuffer(" GROUP BY ");
			else
				group.append(", ");
			group.append(g);
		}
	}

	/**
	 * @param h examples: "weight > 100"
	 */
	public void addHaving(String h, boolean and)
	{
		if (h != null)
		{
			if (having == null)
				having = new StringBuffer(" HAVING ");
			else
				if (and) having.append(" AND "); 
				else having.append(" OR ");
			having.append(h);
		}
	}

	/**
	 * @param o examples: "salary", "firstname, lastname"
	 */
	public void addOrder(String o)
	{
		if (o != null)
		{
			if (order == null)
				order = new StringBuffer(" ORDER BY ");
			else
				order.append(", ");
			order.append(o);
		}
	}
	
	/**
	 * @return String representation of the select builder, i.e. the
	 * resulting SQL SELECT statement based on the contents of the various
	 * pieces. Does NOT perform any syntax checking.
	 */
	public String toString()
	{
		StringBuffer fromAsString = new StringBuffer("");
		
		if (from != null && from.size() > 0)
		{
			fromAsString.append(" FROM ");
			fromAsString.append((String) from.get(0));
			for (int i=1; i<from.size(); i++)
				fromAsString.append(", ").append((String) from.get(i));
		}
		
		return 
			(select != null ? select.toString() : "") +
			fromAsString.toString() +
			(where != null ? where.toString() : "") +
			(group != null ? group.toString() : "") +
			(having != null ? having.toString() : "") +
			(order != null ? order.toString() : "");
	}
}
