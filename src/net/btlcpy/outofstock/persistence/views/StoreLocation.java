package net.btlcpy.outofstock.persistence.views;

import java.sql.Connection;
import java.util.ArrayList;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.utilities.StringUtils;

import org.apache.commons.collections.map.ListOrderedMap;

public class StoreLocation
{
	public static final int DISTRIBUTORDIVISION = 1;
	public static final int DISTRIBUTORDISTRICT = 2;
	public static final int STORE = 3;
	public static final int BOTTLER = 4;
	public static final int BOTTLERBUSINESSUNIT = 5;
	public static final int BOTTLERMARKETUNIT = 6;
	public static final int BOTTLERBRANCH = 7;
	public static final int BOTTLERSALESROUTE = 8;
	
	public static String[] find(Connection connection, int target,
		String[] distributorDivisionPKID, String[] distributorDistrictPKID, String[] storePKID,
		String[] bottlerPKID, String[] bottlerBusinessUnitPKID, String[] bottlerMarketUnitPKID,
		String[] bottlerBranchPKID, String[] bottlerSalesRoutePKID)
	{
		boolean useOwnConnection = (connection == null);
		
		String[] bottlerPK = StringUtils.pk(bottlerPKID);
		String[] bottlerBusinessUnitPK = StringUtils.pk(bottlerBusinessUnitPKID);
		String[] bottlerMarketUnitPK = StringUtils.pk(bottlerMarketUnitPKID);
		String[] bottlerBranchPK = StringUtils.pk(bottlerBranchPKID);
		String[] bottlerSalesRoutePK = StringUtils.pk(bottlerSalesRoutePKID);
		String[] distributorDivisionPK = StringUtils.pk(distributorDivisionPKID);
		if (distributorDivisionPK != null && distributorDivisionPK.length == 1 && distributorDivisionPK[0].equalsIgnoreCase("-1"))
			distributorDivisionPK = null;
		String[] distributorDistrictPK = StringUtils.pk(distributorDistrictPKID);
		String[] storePK = StringUtils.pk(storePKID);

		try
		{
			ArrayList conditions = new ArrayList();
			String condition = "";
			String targetColumns = null;

			switch (target)
			{
			case STORE:
				targetColumns = "store AS key, storeid AS id";
				break;
			case DISTRIBUTORDIVISION:
				targetColumns = "distributordivision AS key, distributordivisionname AS id";
				break;
			case DISTRIBUTORDISTRICT:
				targetColumns = "distributordistrict AS key, distributordistrictname AS id";
				break;
			case BOTTLER:
				targetColumns = "bottler AS key, bottlername AS id";
				break;
			case BOTTLERBUSINESSUNIT:
				targetColumns = "bottlerbusinessunit AS key, bottlerbusinessunitname AS id";
				break;
			case BOTTLERMARKETUNIT:
				targetColumns = "bottlermarketunit AS key, bottlermarketunitname AS id";
				break;
			case BOTTLERBRANCH:
				targetColumns = "bottlerbranch AS key, bottlerbranchname AS id";
				break;
			case BOTTLERSALESROUTE:
				targetColumns = "bottlersalesroute AS key, bottlersalesroutename AS id";
				break;
			default:
				throw new IllegalArgumentException("Unrecognized target for store location retrieval");
			}

			if (target == BOTTLER || target == BOTTLERBUSINESSUNIT || target == BOTTLERMARKETUNIT || target == BOTTLERBRANCH)
			{
				if (storePK != null)
					conditions.add(" store IN (" + StringUtils.stringArrayToString(storePK) + ") ");
				else if (distributorDistrictPK != null)
					conditions.add(" distributordistrict IN (" + StringUtils.stringArrayToString(distributorDistrictPK) + ") ");
				else if (distributorDivisionPK != null)
					conditions.add(" distributordivision IN (" + StringUtils.stringArrayToString(distributorDivisionPK) + ") ");
				
				if (bottlerSalesRoutePK != null)
					conditions.add(" bottlersalesroute IN (" + StringUtils.stringArrayToString(bottlerSalesRoutePK) + ") ");

				if (target == BOTTLERBUSINESSUNIT && bottlerPK != null)
					conditions.add(" bottler IN (" + StringUtils.stringArrayToString(bottlerPK) + ") ");
				else if (target == BOTTLERMARKETUNIT && bottlerBusinessUnitPK != null)
					conditions.add(" bottlerbusinessunit IN (" + StringUtils.stringArrayToString(bottlerBusinessUnitPK) + ") ");
				else if (target == BOTTLERBRANCH && bottlerMarketUnitPK != null)
					conditions.add(" bottlermarketunit IN (" + StringUtils.stringArrayToString(bottlerMarketUnitPK) + ") ");
			}
			else if (target == DISTRIBUTORDIVISION || target == DISTRIBUTORDISTRICT || target == STORE)
			{
				if (bottlerBranchPK != null)
					conditions.add(" bottlerbranch IN (" + StringUtils.stringArrayToString(bottlerBranchPK) + ") ");
				else if (bottlerMarketUnitPK != null)
					conditions.add(" bottlermarketunit IN (" + StringUtils.stringArrayToString(bottlerMarketUnitPK) + ") ");
				else if (bottlerBusinessUnitPK != null)
					conditions.add(" bottlerbusinessunit IN (" + StringUtils.stringArrayToString(bottlerBusinessUnitPK) + ") ");
				else if (bottlerPK != null)
					conditions.add(" bottler IN (" + StringUtils.stringArrayToString(bottlerPK) + ")");

				if (bottlerSalesRoutePK != null)
					conditions.add(" bottlersalesroute IN (" + StringUtils.stringArrayToString(bottlerSalesRoutePK) + ") ");

				if (target == DISTRIBUTORDISTRICT && distributorDivisionPK != null)
					conditions.add(" distributordivision IN (" + StringUtils.stringArrayToString(distributorDivisionPK) + ") ");
				else if (target == STORE && distributorDistrictPK != null)
					conditions.add(" distributordistrict IN (" + StringUtils.stringArrayToString(distributorDistrictPK) + ") ");
			}
			else if (target == BOTTLERSALESROUTE)
			{
				if (storePK != null)
					conditions.add(" store IN (" + StringUtils.stringArrayToString(storePK) + ") ");
				else if (distributorDistrictPK != null)
					conditions.add(" distributordistrict IN (" + StringUtils.stringArrayToString(distributorDistrictPK) + ") ");
				else if (distributorDivisionPK != null)
					conditions.add(" distributordivision IN (" + StringUtils.stringArrayToString(distributorDivisionPK) + ") ");

				if (bottlerBranchPK != null)
					conditions.add(" bottlerbranch IN (" + StringUtils.stringArrayToString(bottlerBranchPK) + ") ");
				else if (bottlerMarketUnitPK != null)
					conditions.add(" bottlermarketunit IN (" + StringUtils.stringArrayToString(bottlerMarketUnitPK) + ") ");
				else if (bottlerBusinessUnitPK != null)
					conditions.add(" bottlerbusinessunit IN (" + StringUtils.stringArrayToString(bottlerBusinessUnitPK) + ") ");
				else if (bottlerPK != null)
					conditions.add(" bottler IN (" + StringUtils.stringArrayToString(bottlerPK) + ")");
			}
			
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);
			
			for (int i=0; i<conditions.size(); i++)
			{
				if (i == 0)
					condition += (" WHERE " + conditions.get(i));
				else
					condition += (" AND " + conditions.get(i));
			}

			String query = "\n\nSELECT DISTINCT " + targetColumns + " FROM storelocations " + condition +
				" ORDER BY id";
			MainLog.getLog().debug(query);
			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, query, null);

			if (results != null && results.size() > 0)
			{
				String[] pkid = new String[results.size()];
				
				for (int i=0; i<results.size(); i++)
				{
					pkid[i] = ((ListOrderedMap) results.getValue(i)).get("KEY").toString() + "|" +
						((ListOrderedMap) results.getValue(i)).get("ID").toString();
				}
				return pkid;
			}
			return null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to find store locations details", e);
			return null;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}
	
	public static ListOrderedMap findAll(Connection connection)
	{
		boolean useOwnConnection = (connection == null);

		try
		{
			if (useOwnConnection)
				connection = PersistenceManager.getPersistenceManager().getConnection(true);

			String query = 
				"SELECT store, storeid, distributordistrict, distributordistrictname, distributordivision, distributordivisionname, " +
				"bottlerbranch, bottlerbranchname, bottlermarketunit, bottlermarketunitname, " +
				"bottlerbusinessunit, bottlerbusinessunitname, bottler, bottlername, " +
				"bottlersalesroute, bottlersalesroutename " +
				"FROM storelocations";
			MainLog.getLog().debug(query);
			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, query, null);
			
			if (results != null && results.size() > 0)
				return results;
			return null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to get all store locations", e);
			return null;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}
}
