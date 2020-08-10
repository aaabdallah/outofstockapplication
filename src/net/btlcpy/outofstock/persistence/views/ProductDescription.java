package net.btlcpy.outofstock.persistence.views;

import java.sql.Connection;
import java.util.ArrayList;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.utilities.StringUtils;

import org.apache.commons.collections.map.ListOrderedMap;

public class ProductDescription
{
	public static final int PRODUCTCATEGORY = 1;
	public static final int PRODUCT = 2;
	public static final int PRODUCTPACKAGE = 3;
	
	public static String[] find(Connection connection, int target,
		String[] productCategoryPKID, String[] productPKID, String[] productPackagePKID)
	{
		boolean useOwnConnection = (connection == null);
		
		String[] productCategoryPK = StringUtils.pk(productCategoryPKID);
		String[] productPK = StringUtils.pk(productPKID);
		String[] productPackagePK = StringUtils.pk(productPackagePKID);

		try
		{
			ArrayList conditions = new ArrayList();
			String condition = "";
			String targetColumns = null;

			switch (target)
			{
			case PRODUCTCATEGORY:
				targetColumns = "productcategory AS key, productcategoryname AS id";
				break;
			case PRODUCT:
				targetColumns = "product AS key, productdescription AS id, productupcid AS subid";
				break;
			case PRODUCTPACKAGE:
				targetColumns = "productpackage AS key, productpackagename AS id";
				break;
			default:
				throw new IllegalArgumentException("Unrecognized target for product description retrieval");
			}

			if (target == PRODUCTCATEGORY || target == PRODUCT)
			{
				if (productPackagePK != null)
					conditions.add(" productpackage IN (" + StringUtils.stringArrayToString(productPackagePK) + ") ");

				if (target == PRODUCT && productCategoryPK != null)
					conditions.add(" productcategory IN (" + StringUtils.stringArrayToString(productCategoryPK) + ") ");
			}
			else if (target == PRODUCTPACKAGE)
			{
				if (productPK != null)
					conditions.add(" product IN (" + StringUtils.stringArrayToString(productPK) + ") ");
				else if (productCategoryPK != null)
					conditions.add(" productcategory IN (" + StringUtils.stringArrayToString(productCategoryPK) + ") ");
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

			String query = "\n\nSELECT DISTINCT " + targetColumns + " FROM productdescriptions " + condition +
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
						((ListOrderedMap) results.getValue(i)).get("ID").toString() +
						(target == PRODUCT ? " [" + ((ListOrderedMap) results.getValue(i)).get("SUBID").toString() + "]": "");
				}
				return pkid;
			}
			return null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to find product descriptions details", e);
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
				"SELECT product, productupcid, productdescription, " +
				"productcategory, productcategoryid, productcategoryname, " +
				"productpackage, productpackagename " +
				"FROM productdescriptions";
			MainLog.getLog().debug(query);
			ListOrderedMap results = PersistenceManager.getPersistenceManager().
				findRows(connection, query, null);
			
			if (results != null && results.size() > 0)
				return results;
			return null;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Unable to get all product descriptions", e);
			return null;
		}
		finally
		{
			if (useOwnConnection)
				try { if (connection != null) connection.close(); } catch (Exception e) {}
		}
	}
}
