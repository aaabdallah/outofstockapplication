package net.btlcpy.outofstock.utilities;

/**
 * Contains widely used utility static methods within the project.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class StringUtils
{
	/**
	 * For string replacement; see http://forum.java.sun.com/thread.jspa?threadID=5246170&messageID=10015224
	 * (no replace method in Java 1.3).
	 * 
	 * @param str the string that should be modified
	 * @param sub the substring to search for and replace
	 * @param rep the substring replacement
	 * @return same as input but with all substrings replaced
	 */
	public static String replaceSubstrings(String str, String sub, String rep)
	{
		if (str != null && sub != null)
		{
			int s, p, q;
			int slen = sub.length();
			StringBuffer sb = new StringBuffer();
	
			s = 0;
			p = str.indexOf(sub);
			q = p + slen;
			while (p != -1)
			{
				sb.append(str.substring(s, p));
				sb.append(rep);
				s = q;
				p = str.indexOf(sub, s);
				if (p != -1)
				{
					q = p + slen;
				}
			}
			sb.append(str.substring(s));
			return sb.toString();
		}
		return str;
	}
	
	/**
	 * <p>
	 * The web pages for this application use a lot of selection boxes to let the user make
	 * choices. The values for some of these options is a pairing of a primary key with its
	 * more user friendly "id" which represents a readable name (e.g. bottler name) or number
	 * (e.g. store ID). For example, an option value might be "1243233|CCE West".
	 * </p>
	 * <p>
	 * This method retrieves the primary key.
	 * </p>
	 * @param pkID the primary key + ID pair
	 * @return the primary key
	 */
	public static String pk(String pkID)
	{
		if (pkID == null || pkID.equals("all")) return pkID;
		
		return pkID.substring(0, pkID.indexOf("|"));
	}

	/**
	 * <p>
	 * The web pages for this application use a lot of selection boxes to let the user make
	 * choices. The values for some of these options is a pairing of a primary key with its
	 * more user friendly "id" which represents a readable name (e.g. bottler name) or number
	 * (e.g. store ID). For example, an option value might be "1243233|CCE West".
	 * </p>
	 * <p>
	 * This method retrieves the ID.
	 * </p>
	 * @param pkID the primary key + ID pair
	 * @return the ID
	 */
	public static String id(String pkID)
	{
		if (pkID == null || pkID.equals("all")) return "ALL";
		
		return pkID.substring(pkID.indexOf("|")+1, pkID.length());
	}
	
	/**
	 * Extracts the primary keys from an array of primary key + ID values.
	 * 
	 * @param pkID an array of primary key + ID values
	 * @return an array of primary keys
	 */
	public static String[] pk(String[] pkID)
	{
		if (pkID != null)
		{
			String[] pk = new String[pkID.length];
			for (int i=0; i<pkID.length; i++)
				pk[i] = pk(pkID[i]);
			return pk;
		}
		return null;
	}

	/**
	 * Extracts the ID's from an array of primary key + ID values.
	 * 
	 * @param pkID an array of primary key + ID values
	 * @return an array of ID's
	 */
	public static String[] id(String[] pkID)
	{
		if (pkID != null)
		{
			String[] id = new String[pkID.length];
			for (int i=0; i<pkID.length; i++)
				id[i] = id(pkID[i]);
			return id;
		}
		return null;
	}

	/**
	 * @param array an array of strings
	 * @return a single string composed of the passed in strings with a comma and
	 * a space separating the individual strings.
	 */
	public static String stringArrayToString(String[] array)
	{
		if (array == null)
			return null;
		
		StringBuffer sb = new StringBuffer("");
		sb.append(array[0]);
		for (int i=1; i<array.length; i++)
		{
			sb.append(", ");
			sb.append(array[i]);
		}
		return sb.toString();
	}
}
