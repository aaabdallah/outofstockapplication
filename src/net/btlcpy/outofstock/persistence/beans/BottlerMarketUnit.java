package net.btlcpy.outofstock.persistence.beans;

import java.sql.Connection;
import java.sql.SQLException;

import net.btlcpy.outofstock.persistence.PersistenceManager;

import org.apache.commons.collections.map.ListOrderedMap;

/**
 * A bean to represent Bottling Company bottler market units.
 * 
 * @author Ahmed Abd-Allah
 */
public class BottlerMarketUnit extends NamedUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "bttlrmktunts";

	/**
	 * Finds bottler market units for a given bottler business unit.
	 * @param connection connection to use, if null, creates a new connection
	 * @param bottlerBusinessUnits primary keys of each bottler business unit desired
	 * @return a list ordered map of bottler market units
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	public static ListOrderedMap findByBottlerBusinessUnit(Connection connection, Integer[] bottlerBusinessUnits)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sb = new StringBuffer("");
		sb.append(bottlerBusinessUnits[0]);
		for (int i=1; i<bottlerBusinessUnits.length; i++)
		{
			sb.append(", " + bottlerBusinessUnits[i]);
		}
		return PersistenceManager.getPersistenceManager().
			findBeans(connection, 
				"SELECT bttlrmktunts.* FROM bttlrmktunts, bttlrbsnsuntstobttlrmktunts WHERE " +
				"bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit IN (" + sb.toString() + ") AND " +
				"bttlrbsnsuntstobttlrmktunts.bottlermarketunit = bttlrmktunts.primarykey " +
				"ORDER BY name", 
				BottlerMarketUnit.class, null);
	}

	// ----- Instance members -------------------------------------------------
	public BottlerMarketUnit()
	{
	}

	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
