package net.btlcpy.outofstock.spreadsheetparsers;

import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.BatchManager;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.persistence.beans.Bottler;
import net.btlcpy.outofstock.persistence.beans.BottlerBranch;
import net.btlcpy.outofstock.persistence.beans.BottlerBranchToStoreMapping;
import net.btlcpy.outofstock.persistence.beans.BottlerBusinessUnit;
import net.btlcpy.outofstock.persistence.beans.BottlerBusinessUnitToBottlerMarketUnitMapping;
import net.btlcpy.outofstock.persistence.beans.BottlerMarketUnit;
import net.btlcpy.outofstock.persistence.beans.BottlerMarketUnitToBottlerBranchMapping;
import net.btlcpy.outofstock.persistence.beans.BottlerSalesRoute;
import net.btlcpy.outofstock.persistence.beans.BottlerSalesRouteToStoreMapping;
import net.btlcpy.outofstock.persistence.beans.BottlerToBottlerBusinessUnitMapping;
import net.btlcpy.outofstock.persistence.beans.Product;
import net.btlcpy.outofstock.persistence.beans.ProductPackage;
import net.btlcpy.outofstock.persistence.beans.DistributorDistrict;
import net.btlcpy.outofstock.persistence.beans.DistributorDistrictToStoreMapping;
import net.btlcpy.outofstock.persistence.beans.DistributorDivision;
import net.btlcpy.outofstock.persistence.beans.DistributorDivisionToDistributorDistrictMapping;
import net.btlcpy.outofstock.persistence.beans.Store;

import org.apache.poi.hssf.eventusermodel.HSSFUserException;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;

/**
 * This class is for parsing a spreadsheet of active stores in the Out of Stock application.
 * The exact specification of the spreadsheet is important, and should be reviewed to
 * understand this class. The specification can be found in the "Functional Design" document
 * for the project.
 *  
 * @author Ahmed A. Abd-Allah
 */
public class ActiveStoresSpreadsheetParser extends SpreadsheetParser
{
	// ----- Instance members -------------------------------------------------
	/**
	 * Caches of particular database tables that might be affected by this parsing.
	 */
	private Map allBottlers = null, allBottlerBusinessUnits = null, allBottlerMarketUnits = null,
		allBottlerBranches = null, allBottlerSalesRoutes = null, allDistributorDivisions = null,
		allDistributorDistricts = null, allStores = null;
	/**
	 * Caches of particular database tables that might be affected by this parsing.
	 */
	private Map allBottlerToBottlerBusinessUnitMappings = null, 
		allBottlerBusinessUnitToBottlerMarketUnitMappings = null, 
		allBottlerMarketUnitToBottlerBranchMappings = null,
		allBottlerBranchToStoreMappings = null,
		allBottlerSalesRouteToStoreMappings = null,
		allDistributorDivisionToDistributorDistrictMappings = null,
		allDistributorDistrictToStoreMappings = null;

	/** 
	 * The next few objects represent the entities that will be read per line in the
	 * spreadsheet. These objects will be filled in from the information on that line,
	 * then persisted to the database if not already in there. When the next line is
	 * read, the objects' contents shall be reset to empty values. 
	 */
	private Bottler bottler = null;
	private BottlerBusinessUnit bottlerBusinessUnit = null;
	private BottlerMarketUnit bottlerMarketUnit = null;
	private BottlerBranch bottlerBranch = null;
	private BottlerSalesRoute bottlerSalesRoute = null;
	private DistributorDivision distributorDivision = null;
	private DistributorDistrict distributorDistrict = null;
	private Store store = null;

	/** 
	 * <p>
	 * The next few objects represent the mappings that will be deduced per line in the
	 * spreadsheet. These mappings will be filled in from the information on that line,
	 * then persisted to the database if not already in there. When the next line is
	 * read, the mappings' contents shall be reset to empty values.
	 * </p>
	 * <p>
	 * An example of a mapping: a bottler to a bottler business unit.
	 * </p> 
	 */
	private BottlerToBottlerBusinessUnitMapping bottlerToBottlerBusinessUnitMapping = null;
	private BottlerBusinessUnitToBottlerMarketUnitMapping bottlerBusinessUnitToBottlerMarketUnitMapping = null;
	private BottlerMarketUnitToBottlerBranchMapping bottlerMarketUnitToBottlerBranchMapping = null;
	private BottlerBranchToStoreMapping bottlerBranchToStoreMapping = null;
	private BottlerSalesRouteToStoreMapping bottlerSalesRouteToStoreMapping = null;
	private DistributorDivisionToDistributorDistrictMapping distributorDivisionToDistributorDistrictMapping = null;
	private DistributorDistrictToStoreMapping distributorDistrictToStoreMapping = null;
	
	/**
	 * A default "no date value" for cases where the date is an empty or meaningless value in the spreadsheet.
	 * Represents the date: December 31, 9999.
	 */ 
	private Date noDateIndicator = null;
	{
		Calendar calendar = Calendar.getInstance();
		calendar.set(9999, 11, 31);
		noDateIndicator = new Date(calendar.getTime().getTime());
	}
	
	/**
	 * The number of stores read.
	 */
	private int storeCount = 0;

	public ActiveStoresSpreadsheetParser(String spreadsheetFileName, String relevantSheetName,
		boolean debugging)
	{
		super(spreadsheetFileName, relevantSheetName, debugging);
	}

	public ActiveStoresSpreadsheetParser(InputStream spreadsheetInputStream, String spreadsheetInputStreamName,
		String relevantSheetName, boolean debugging)
	{
		super(spreadsheetInputStream, spreadsheetInputStreamName, relevantSheetName, debugging);
	}

	/**
	 * Initializes the set of persistable objects that will be used while parsing the
	 * spreadsheet. These objects will be filled in per line of the spreadsheet, and examined
	 * if they are new or not. If so, they will be persisted. As we move from line to line,
	 * the set of objects are cleared out (see the method <code>resetPersistentObjects()</code>).
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	private void initializePersistentObjects()
		throws IllegalAccessException, InstantiationException, SQLException
	{
		// set the keys that will be used to uniquely identify various objects
		String nameKey[] = { "name" };
		String idKey[] = { "id" };
		String b2BbuKey[] = { "bottler", "bottlerbusinessunit" };
		String bbu2BmuKey[] = { "bottlerbusinessunit", "bottlermarketunit" };
		String bmu2BbKey[] = { "bottlermarketunit", "bottlerbranch" };
		String bb2strKey[] = { "bottlerbranch", "store" };
		String bsr2strKey[] = { "bottlersalesroute", "store" };
		String sdv2SdtKey[] = { "distributordivision", "distributordistrict" };
		String sdt2strKey[] = { "distributordistrict", "store" };

		PersistenceManager manager = PersistenceManager.getPersistenceManager();
		batchManager = new BatchManager(connection);

		// For the different objects in the next section of code (bottlers, bottler business units, etc.)
		// we create a new object that will be reused per line in the spreadsheet, then create a
		// reusable parameterized insertion query that can be used in the batch manager, retrieve
		// a cache of all the objects from the database of the same type, and finally update all
		// objects of that type in the database to reflect a new upload time (this is because 99%
		// of the time, the same objects will be uploaded - rarely will an entity go out of business -
		// so this is an efficient way of updating them all quickly in the database.)
		
		bottler = new Bottler();
		batchManager.addQuery("BottlerCreate", bottler.makeParameterizedCreateQuery(), new Integer(100));
		allBottlers = manager.
			findBeans(connection, Bottler.tableName, null, Bottler.class, nameKey, null);
		manager.bulkUpdate(connection, Bottler.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		bottlerBusinessUnit = new BottlerBusinessUnit();
		batchManager.addQuery("BottlerBusinessUnitCreate", bottlerBusinessUnit.makeParameterizedCreateQuery(), new Integer(100));
		allBottlerBusinessUnits = manager.
			findBeans(connection, BottlerBusinessUnit.tableName, null, BottlerBusinessUnit.class, nameKey, null);
		manager.bulkUpdate(connection, BottlerBusinessUnit.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerMarketUnit = new BottlerMarketUnit();
		batchManager.addQuery("BottlerMarketUnitCreate", bottlerMarketUnit.makeParameterizedCreateQuery(), new Integer(100));
		allBottlerMarketUnits = manager.
			findBeans(connection, BottlerMarketUnit.tableName, null, BottlerMarketUnit.class, nameKey, null);
		manager.bulkUpdate(connection, BottlerMarketUnit.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerBranch = new BottlerBranch();
		batchManager.addQuery("BottlerBranchCreate", bottlerBranch.makeParameterizedCreateQuery(), new Integer(100));
		allBottlerBranches = manager.
			findBeans(connection, BottlerBranch.tableName, null, BottlerBranch.class, nameKey, null);
		manager.bulkUpdate(connection, BottlerBranch.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerSalesRoute = new BottlerSalesRoute();
		batchManager.addQuery("BottlerSalesRouteCreate", bottlerSalesRoute.makeParameterizedCreateQuery(), new Integer(100));
		allBottlerSalesRoutes = manager.
			findBeans(connection, BottlerSalesRoute.tableName, null, BottlerSalesRoute.class, nameKey, null);
		manager.bulkUpdate(connection, BottlerSalesRoute.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		distributorDivision = new DistributorDivision();
		batchManager.addQuery("DistributorDivisionCreate", distributorDivision.makeParameterizedCreateQuery(), new Integer(100));
		allDistributorDivisions = manager.
			findBeans(connection, DistributorDivision.tableName, null, DistributorDivision.class, nameKey, null);
		manager.bulkUpdate(connection, DistributorDivision.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		distributorDistrict = new DistributorDistrict();
		batchManager.addQuery("DistributorDistrictCreate", distributorDistrict.makeParameterizedCreateQuery(), new Integer(100));
		allDistributorDistricts = manager.
			findBeans(connection, DistributorDistrict.tableName, null, DistributorDistrict.class, nameKey, null);
		manager.bulkUpdate(connection, DistributorDistrict.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		store = new Store();
		batchManager.addQuery("StoreCreate", store.makeParameterizedCreateQuery(), new Integer(100));
		allStores = manager.
			findBeans(connection, Store.tableName, null, Store.class, idKey, null);
		manager.bulkUpdate(connection, Store.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		// In the next section of the code (all the mappings below), we create a new mapping object
		// that will be reused per line in the spreadsheet, as well as a parameterized insertion query.
		// This is followed by caching all the current mappings, and updating their upload times in
		// the database.
		bottlerToBottlerBusinessUnitMapping = new BottlerToBottlerBusinessUnitMapping();
		batchManager.addQuery("BottlerToBottlerBusinessUnitMappingCreate", 
			bottlerToBottlerBusinessUnitMapping.makeParameterizedCreateQuery(), new Integer(200));
		allBottlerToBottlerBusinessUnitMappings = manager.
			findBeans(connection, BottlerToBottlerBusinessUnitMapping.tableName, null, 
				BottlerToBottlerBusinessUnitMapping.class, b2BbuKey, null);
		manager.bulkUpdate(connection, BottlerToBottlerBusinessUnitMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerBusinessUnitToBottlerMarketUnitMapping = new BottlerBusinessUnitToBottlerMarketUnitMapping();
		batchManager.addQuery("BottlerBusinessUnitToBottlerMarketUnitMappingCreate", 
			bottlerBusinessUnitToBottlerMarketUnitMapping.makeParameterizedCreateQuery(), new Integer(200));
		allBottlerBusinessUnitToBottlerMarketUnitMappings = manager.
			findBeans(connection, BottlerBusinessUnitToBottlerMarketUnitMapping.tableName, null, 
				BottlerBusinessUnitToBottlerMarketUnitMapping.class, bbu2BmuKey, null);
		manager.bulkUpdate(connection, BottlerBusinessUnitToBottlerMarketUnitMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerMarketUnitToBottlerBranchMapping = new BottlerMarketUnitToBottlerBranchMapping();
		batchManager.addQuery("BottlerMarketUnitToBottlerBranchMappingCreate", 
			bottlerMarketUnitToBottlerBranchMapping.makeParameterizedCreateQuery(), new Integer(200));
		allBottlerMarketUnitToBottlerBranchMappings = manager.
			findBeans(connection, BottlerMarketUnitToBottlerBranchMapping.tableName, null, 
				BottlerMarketUnitToBottlerBranchMapping.class, bmu2BbKey, null);
		manager.bulkUpdate(connection, BottlerMarketUnitToBottlerBranchMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		bottlerBranchToStoreMapping = new BottlerBranchToStoreMapping();
		batchManager.addQuery("BottlerBranchToStoreMappingCreate", 
			bottlerBranchToStoreMapping.makeParameterizedCreateQuery(), new Integer(200));
		allBottlerBranchToStoreMappings = manager.
			findBeans(connection, BottlerBranchToStoreMapping.tableName, null, 
				BottlerBranchToStoreMapping.class, bb2strKey, null);
		manager.bulkUpdate(connection, BottlerBranchToStoreMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		bottlerSalesRouteToStoreMapping = new BottlerSalesRouteToStoreMapping();
		batchManager.addQuery("BottlerSalesRouteToStoreMappingCreate", 
			bottlerSalesRouteToStoreMapping.makeParameterizedCreateQuery(), new Integer(200));
		allBottlerSalesRouteToStoreMappings = manager.
			findBeans(connection, BottlerSalesRouteToStoreMapping.tableName, null, 
				BottlerSalesRouteToStoreMapping.class, bsr2strKey, null);
		manager.bulkUpdate(connection, BottlerSalesRouteToStoreMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		distributorDivisionToDistributorDistrictMapping = new DistributorDivisionToDistributorDistrictMapping();
		batchManager.addQuery("DistributorDivisionToDistributorDistrictMappingCreate", 
			distributorDivisionToDistributorDistrictMapping.makeParameterizedCreateQuery(), new Integer(200));
		allDistributorDivisionToDistributorDistrictMappings = manager.
			findBeans(connection, DistributorDivisionToDistributorDistrictMapping.tableName, null, 
				DistributorDivisionToDistributorDistrictMapping.class, sdv2SdtKey, null);
		manager.bulkUpdate(connection, DistributorDivisionToDistributorDistrictMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		distributorDistrictToStoreMapping = new DistributorDistrictToStoreMapping();
		batchManager.addQuery("DistributorDistrictToStoreMappingCreate", 
			distributorDistrictToStoreMapping.makeParameterizedCreateQuery(), new Integer(200));
		allDistributorDistrictToStoreMappings = manager.
			findBeans(connection, DistributorDistrictToStoreMapping.tableName, null, 
				DistributorDistrictToStoreMapping.class, sdt2strKey, null);
		manager.bulkUpdate(connection, DistributorDistrictToStoreMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
	}
	
	/**
	 * Resets all the fields of the reusable set of persistable objects to empty values. This should
	 * be called whenever we go from one line of the spreadsheet to another.
	 */
	protected void resetPersistentObjects()
	{
		super.resetPersistentObjects();
		bottler.resetFields();
		bottlerBusinessUnit.resetFields();
		bottlerMarketUnit.resetFields();
		bottlerBranch.resetFields();
		bottlerSalesRoute.resetFields();
		distributorDivision.resetFields();
		distributorDistrict.resetFields();
		store.resetFields();
		
		bottlerToBottlerBusinessUnitMapping.resetFields();
		bottlerBusinessUnitToBottlerMarketUnitMapping.resetFields();
		bottlerMarketUnitToBottlerBranchMapping.resetFields();
		bottlerBranchToStoreMapping.resetFields();
		bottlerSalesRouteToStoreMapping.resetFields();
		distributorDivisionToDistributorDistrictMapping.resetFields();
		distributorDistrictToStoreMapping.resetFields();
	}

	/**
	 * Persists all the new objects and mappings encountered on a particular line of the spreadsheet.
	 */
	protected void persistPersistentObjects()
		throws InstantiationException, IllegalAccessException, SQLException
	{
		super.persistPersistentObjects();
		
		persistNamedUploadable(bottler, allBottlers);
		bottlerToBottlerBusinessUnitMapping.setBottler(bottler.getPrimaryKey());
		
		persistNamedUploadable(bottlerBusinessUnit, allBottlerBusinessUnits);
		bottlerToBottlerBusinessUnitMapping.setBottlerBusinessUnit(bottlerBusinessUnit.getPrimaryKey());
		bottlerBusinessUnitToBottlerMarketUnitMapping.setBottlerBusinessUnit(bottlerBusinessUnit.getPrimaryKey());

		persistNamedUploadable(bottlerMarketUnit, allBottlerMarketUnits);
		bottlerBusinessUnitToBottlerMarketUnitMapping.setBottlerMarketUnit(bottlerMarketUnit.getPrimaryKey());
		bottlerMarketUnitToBottlerBranchMapping.setBottlerMarketUnit(bottlerMarketUnit.getPrimaryKey());
		
		persistNamedUploadable(bottlerBranch, allBottlerBranches);
		bottlerMarketUnitToBottlerBranchMapping.setBottlerBranch(bottlerBranch.getPrimaryKey());
		bottlerBranchToStoreMapping.setBottlerBranch(bottlerBranch.getPrimaryKey());
		
		persistNamedUploadable(bottlerSalesRoute, allBottlerSalesRoutes);
		bottlerSalesRouteToStoreMapping.setBottlerSalesRoute(bottlerSalesRoute.getPrimaryKey());

		persistNamedUploadable(distributorDivision, allDistributorDivisions);
		distributorDivisionToDistributorDistrictMapping.setDistributorDivision(distributorDivision.getPrimaryKey());
		
		persistNamedUploadable(distributorDistrict, allDistributorDistricts);
		distributorDivisionToDistributorDistrictMapping.setDistributorDistrict(distributorDistrict.getPrimaryKey());
		distributorDistrictToStoreMapping.setDistributorDistrict(distributorDistrict.getPrimaryKey());
		
		persistNamedWithIdUploadable(store, allStores);
		bottlerBranchToStoreMapping.setStore(store.getPrimaryKey());
		bottlerSalesRouteToStoreMapping.setStore(store.getPrimaryKey());
		distributorDistrictToStoreMapping.setStore(store.getPrimaryKey());
		
		persistUploadableToUploadableMapping(
			bottler, bottlerBusinessUnit, 
			bottlerToBottlerBusinessUnitMapping,
			allBottlerToBottlerBusinessUnitMappings);
		persistUploadableToUploadableMapping(
			bottlerBusinessUnit, bottlerMarketUnit,  
			bottlerBusinessUnitToBottlerMarketUnitMapping,
			allBottlerBusinessUnitToBottlerMarketUnitMappings);
		persistUploadableToUploadableMapping(
			bottlerMarketUnit, bottlerBranch, 
			bottlerMarketUnitToBottlerBranchMapping,
			allBottlerMarketUnitToBottlerBranchMappings);
		persistUploadableToUploadableMapping(
			bottlerBranch, store, 
			bottlerBranchToStoreMapping,
			allBottlerBranchToStoreMappings);
		persistUploadableToUploadableMapping(
			bottlerSalesRoute, store,  
			bottlerSalesRouteToStoreMapping,
			allBottlerSalesRouteToStoreMappings);
		persistUploadableToUploadableMapping(
			distributorDivision, distributorDistrict, 
			distributorDivisionToDistributorDistrictMapping,
			allDistributorDivisionToDistributorDistrictMappings);
		persistUploadableToUploadableMapping(
			distributorDistrict, store, 
			distributorDistrictToStoreMapping,
			allDistributorDistrictToStoreMappings);
	}

	/**
	 * See the superclass's regressMissingPersistentObjects(). It is called on each group of uploadable
	 * items here.
	 * 
	 * @throws SQLException
	 */
	private void regressMissingPersistentObjects()
		throws SQLException
	{
		if (allBottlers != null) regressMissingPersistentObjects(allBottlers.values());
		if (allBottlerBusinessUnits != null) regressMissingPersistentObjects(allBottlerBusinessUnits.values());
		if (allBottlerMarketUnits != null) regressMissingPersistentObjects(allBottlerMarketUnits.values());
		if (allBottlerBranches != null) regressMissingPersistentObjects(allBottlerBranches.values());
		if (allBottlerSalesRoutes != null) regressMissingPersistentObjects(allBottlerSalesRoutes.values());
		if (allDistributorDivisions != null) regressMissingPersistentObjects(allDistributorDivisions.values());
		if (allDistributorDistricts != null) regressMissingPersistentObjects(allDistributorDistricts.values());
		if (allStores != null) regressMissingPersistentObjects(allStores.values());
		if (allBottlerToBottlerBusinessUnitMappings != null) regressMissingPersistentObjects(allBottlerToBottlerBusinessUnitMappings.values());
		if (allBottlerBusinessUnitToBottlerMarketUnitMappings != null) regressMissingPersistentObjects(allBottlerBusinessUnitToBottlerMarketUnitMappings.values());
		if (allBottlerMarketUnitToBottlerBranchMappings != null) regressMissingPersistentObjects(allBottlerMarketUnitToBottlerBranchMappings.values());
		if (allBottlerBranchToStoreMappings != null) regressMissingPersistentObjects(allBottlerBranchToStoreMappings.values());
		if (allBottlerSalesRouteToStoreMappings != null) regressMissingPersistentObjects(allBottlerSalesRouteToStoreMappings.values());
		if (allDistributorDivisionToDistributorDistrictMappings != null) regressMissingPersistentObjects(allDistributorDivisionToDistributorDistrictMappings.values());
		if (allDistributorDistrictToStoreMappings != null) regressMissingPersistentObjects(allDistributorDistrictToStoreMappings.values());
	}

	/**
	 * The POI design requires that there be a method with this signature that is called by POI while a spreadsheet is
	 * being parsed.
	 * @param record the Excel record being read currently
	 */
	public short abortableProcessRecord(Record record)
		throws HSSFUserException
	{
		try
		{
			if (debugging)
				abortableProcessRecordForDebugging(record);

			switch (record.getSid())
			{
			case SSTRecord.sid:
				staticStringRecords = (SSTRecord) record;
				break;
			case BOFRecord.sid:
				if (((BOFRecord) record).getType() == BOFRecord.TYPE_WORKSHEET)
				{
					if (relevantSheetPosition >= 1)
					{
						relevantSheetPosition--;
						if (relevantSheetPosition > 0)
							break;

						// else we have reached the beginning of the relevant sheet
						processing = true;
						initializePersistentObjects();
					}
				}
				break;
			case BoundSheetRecord.sid:
				if (relevantSheetPosition <= 0) // still haven't identified relevant sheet
					relevantSheetPosition--;
				if (((BoundSheetRecord) record).getSheetname().equalsIgnoreCase(relevantSheetName))
				{
					relevantSheetPosition = (short) -relevantSheetPosition;
				}
				break;
			case EOFRecord.sid:
				// have we reached the end of the relevant sheet's data?
				if (processing)
				{
					// MainLog.getLog().debug("Reached end of rows");
					
					regressMissingPersistentObjects();

					batchManager.executeAll(true);
					
					// these three only because they always appear on the specify report parameters page
					DistributorDivision.getCache(connection).reload(connection);
					Bottler.getCache(connection).reload(connection);
					BottlerSalesRoute.getCache(connection).reload(connection);
					
					Store.updateTotals(connection);
					Bottler.updateTotalActive(connection);
					BottlerBranch.updateTotalActive(connection);
					BottlerSalesRoute.updateTotalActive(connection);
					DistributorDistrict.updateTotalActive(connection);
					Product.updateTotalActive(connection);
					ProductPackage.updateTotalActive(connection);

					processing = false;
					return 1; // STOP PROCESSING
				}
				break;
			case NumberRecord.sid:
				if (processing)
				{
					NumberRecord numberRecord = (NumberRecord) record;
					if (numberRecord.getRow() == 0)
						break;

					// process the number
					switch (numberRecord.getColumn())
					{
					case 0: // count
						// MainLog.getLog().debug("Row: " + numberRecord.getRow());
						resetPersistentObjects();
						storeCount++;
						break;
					case 1: // store_id
						store.setId(new Integer((int) numberRecord.getValue()));
						break;
					case 3: // district_id
						distributorDistrict.setId(new Integer((int) numberRecord.getValue()));
						break;
					case 4: // district_cd
						distributorDistrict.setCd(new Integer((int) numberRecord.getValue()).toString());
						break;
					case 6: // division_id
						distributorDivision.setId(new Integer((int) numberRecord.getValue()));
						break;
					case 10: // store_zip5_id
						store.setZip(new Long((long) numberRecord.getValue()).toString());
						break;
					case 12: // store_voice_phone_nbr
						store.setPhoneNumber(new Long((long) numberRecord.getValue()).toString());
						break;
					case 13: // last_remodel_dt
						store.setDateLastRemodeled(new Date(HSSFDateUtil.getJavaDate(numberRecord.getValue()).getTime()));
						break;
					case 14: // opened_dt
						store.setDateOpened(new Date(HSSFDateUtil.getJavaDate(numberRecord.getValue()).getTime()));
						break;
					case 15: // closed_dt
						store.setDateClosed(new Date(HSSFDateUtil.getJavaDate(numberRecord.getValue()).getTime()));
						break;
					case 18: // total_selling_area_amt
						store.setTotalSellingArea(new Integer((int) numberRecord.getValue()));
						break;
					case 19: //  total_FLM count
						store.setTotalFlmCount(new Integer((int) numberRecord.getValue()));
						break;
					default:
						break;
					}
				}
				break;
			case LabelSSTRecord.sid:
				if (processing)
				{
					LabelSSTRecord labelRecord = (LabelSSTRecord) record;
					if (labelRecord.getRow() == 0)
						break;

					// process the string
					switch (labelRecord.getColumn())
					{
					case 0: // count
						// MainLog.getLog().debug("Row: " + labelRecord.getRow());
						resetPersistentObjects();
						storeCount++;
						break;
					case 2: // store_nm
						store.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 4: // district_cd
						distributorDistrict.setCd(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 5: // district_nm
						distributorDistrict.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 7: // division_nm
						distributorDivision.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 8: // store_addr_line2_txt
						store.setAddress(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 9: // store_city_nm
						store.setCity(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 10: // store_zip5_id
						store.setZip(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 11: // store_state_id
						store.setState(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 12: // store_voice_phone_nbr
						store.setPhoneNumber(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 13: // last_remodel_dt
						// Excel stores dates as numbers, so strings mean no useful data here
						store.setDateLastRemodeled(noDateIndicator);
						break;
					case 14: // opened_dt
						// Excel stores dates as numbers, so strings mean no useful data here
						store.setDateOpened(noDateIndicator);
						break;
					case 15: // closed_dt
						// Excel stores dates as numbers, so strings mean no useful data here
						store.setDateClosed(noDateIndicator);
						break;
					case 16: // Lifestyle (1/26/07)
						store.setLifeStyle(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 17: // DISTRIBUTOR.COM
						store.setDistributorCom(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 18: // total_selling_area_amt
						store.setTotalSellingArea(new Integer(0));
						break;
					case 19: // total_FLM count
						store.setTotalFlmCount(new Integer(0));
						break;
					case 20: // Bottling Company Bottler
						bottler.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 21: // KO Bottler Region
						bottlerBusinessUnit.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 22: // Bottler Market Unit 
						bottlerMarketUnit.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 23: // KO Bottler Branch 
						bottlerBranch.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 24: // KO Sales Route
						bottlerSalesRoute.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());

						persistPersistentObjects();

						break;
					default:
						break;
					}
				}
				break;
			}

			return 0;
		}
		catch (Exception e)
		{
			MainLog.getLog().error("Parsing error", e);
			errorIndicator = "Underlying exception: " + e.getClass().getName() + 
				(e.getMessage() != null ? ": " + e.getMessage() : "") + "\n" +
				"Record:\n" + record.toString();
			return 1;
		}
	}

	public String getUserActionDescription()
	{
		return "Active stores spreadsheet: " + (spreadsheetFileName != null ? spreadsheetFileName :
			spreadsheetInputStreamName + ". " + storeCount + " stores read.");
	}

	// Leave this in here for the moment to allow for emergency uploads of data from the command line if the Web interface
	// is down for whatever reason.
	public static void main(String args[])
	{
		try
		{
			MainLog.init("data/loggerConfiguration.lcf");
			//MainLog.getLog().debug( System.getenv("oracle_sid") );
			//if (true) return;
			PersistenceManager.initializePersistenceManager(null);
			
			long time1 = System.currentTimeMillis();
			ActiveStoresSpreadsheetParser parser = new ActiveStoresSpreadsheetParser(
				//"data/Distributor_active_stores_by_Bottling Company_Bottler_April_2007.zip", "TTL", false);
				//"data/DistributorStoreList.xls", "TTL", false);
				//"data/DISTRIBUTOR STORE LIST 01082008.XLS", "TTL", false);
				"data/New-2.22.08 Distributor Store List.xls", "TTL", false);

			parser.parse("admin");
			
			System.out.println("Elapsed time: " + ((double) System.currentTimeMillis() - time1)/1000 + " seconds");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//throw new SpreadsheetParsingException("Underlying exception: " + e.getClass().getName() + 
			//	(e.getMessage() != null ? " " + e.getMessage() : ""));
		}
	}
}
