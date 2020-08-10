package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.SQLException;

import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A bean to represent Bottling Company bottler business units. Note: the name
 * of this entity according to Bottling Company is "bottler region", however since
 * the original information from them (in their spreadsheets) referred to them
 * as "business units", the name lives on in the code though in the presentation
 * layer it is now referred to as a "region". Information sclerosis.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerBusinessUnit extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrbsnsunts";

	/**
	 * Finds bottler business units for a given bottler.
	 * @param connection connection to use, if null, creates a new connection
	 * @param bottlers primary keys of each bottler desired
	 * @return a list ordered map of bottler business units
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByBottler(Connection connection, Integer[] bottlers)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(bottlers[0]);
		for (int i=1; i<bottlers.length; i++)
		{
			sb.append(", " + bottlers[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT bttlrbsnsunts.* FROM bttlrbsnsunts, bttlrstobttlrbsnsunts WHERE " +
				"bttlrstobttlrbsnsunts.bottler IN (" + sb.toString() + ") AND " +
				"bttlrstobttlrbsnsunts.bottlerbusinessunit = bttlrbsnsunts.primarykey " +
				"ORDER BY name", 
				BottlerBusinessUnit.class, null);
	}

	// ----- Instance members -------------------------------------------------
	public BottlerBusinessUnit()
	{
	}
	
	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
