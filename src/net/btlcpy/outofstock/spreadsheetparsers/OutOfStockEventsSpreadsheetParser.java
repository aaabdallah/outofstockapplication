package net.btlcpy.outofstock.spreadsheetparsers;

import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.BatchManager;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.persistence.beans.OutOfStockEvent;
import net.btlcpy.outofstock.persistence.beans.Product;
import net.btlcpy.outofstock.persistence.beans.ProductCategory;
import net.btlcpy.outofstock.persistence.beans.ProductCategoryToProductMapping;
import net.btlcpy.outofstock.persistence.beans.ProductGroup;
import net.btlcpy.outofstock.persistence.beans.ProductGroupToProductCategoryMapping;
import net.btlcpy.outofstock.persistence.beans.ProductPackage;
import net.btlcpy.outofstock.persistence.beans.ProductToProductPackageMapping;
import net.btlcpy.outofstock.persistence.beans.Setting;
import net.btlcpy.outofstock.persistence.beans.Store;
import net.btlcpy.outofstock.settings.SettingsManager;
import net.btlcpy.outofstock.spreadsheetparsers.exceptions.SpreadsheetParsingException;

import org.apache.commons.collections.map.ListOrderedMap;
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
 * <p>
 * This class is for parsing a spreadsheet of "out of stock events" in the Out of Stock 
 * application. The exact specification of the spreadsheet is important, and should be 
 * reviewed to understand this class. The specification can be found in the "Functional 
 * Design" document for the project.
 * </p>
 * <p>
 * Note that a "side effect" of parsing these spreadsheets is to populate the tables of
 * product groups, product categories, and products. In other words, there is no separate
 * spreadsheet detailing all possible products; we are required to infer them from this
 * spreadsheet.
 * </p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class OutOfStockEventsSpreadsheetParser extends SpreadsheetParser
{
	// ----- Instance members -------------------------------------------------
	/**
	 * Caches of particular database tables that might be affected by this parsing.
	 */
	private Map allProductGroups = null, allProductCategories = null, allProducts = null, 
		allProductPackages = null, allStores = null;
	/**
	 * Caches of particular database tables that might be affected by this parsing.
	 */
	private Map allProductGroupToProductCategoryMappings = null,
		allProductCategoryToProductMappings = null,
		allProductToProductPackageMappings = null;
	
	/** 
	 * The next few objects represent the entities that will be read per line in the
	 * spreadsheet. These objects will be filled in from the information on that line,
	 * then persisted to the database if not already in there. When the next line is
	 * read, the objects' contents shall be reset to empty values. 
	 */
	private ProductGroup productGroup = null;
	private ProductCategory productCategory = null;
	private Product product = null;
	private ProductPackage productPackage = null;
	private OutOfStockEvent outOfStockEvent = null;
	
	/** 
	 * <p>
	 * The next few objects represent the mappings that will be deduced per line in the
	 * spreadsheet. These mappings will be filled in from the information on that line,
	 * then persisted to the database if not already in there. When the next line is
	 * read, the mappings' contents shall be reset to empty values.
	 * </p>
	 * <p>
	 * An example of a mapping: a product category to a product.
	 * </p> 
	 */
	private ProductGroupToProductCategoryMapping productGroupToProductCategoryMapping = null;
	private ProductCategoryToProductMapping productCategoryToProductMapping = null;
	private ProductToProductPackageMapping productToProductPackageMapping = null;

	/**
	 * The spreadsheet comes with both store information and Distributor division information.
	 * Just for verification purposes, we will construct a collection of all division to
	 * store mappings from the database, and check the contents of the spreadsheet (line
	 * by line) against that collection of mappings.
	 */
	private Collection allDistributorDivisionsToStores = null;
	private Integer distributorDivisionId = null;
	private Integer storeId = null;
	
	/**
	 * Some of the stores referenced in the spreadsheet might not be currently stored in
	 * the database. We keep track of those stores temporarily for reporting purposes.
	 */
	private List /*<Integer>*/ unrecognizedStoreIds = null;

	/**
	 * The package names that come in this spreadsheet are not standard, in the sense
	 * that multiple strings can be used to describe the same package. This map
	 * is used to "fold" all alternative spellings into one standard spelling for
	 * a particular package name. See the method <code>prepareFoldedProductPackageNames</code>
	 * for a list of those foldings.
	 */
	HashMap /*<String, String>*/ foldedProductPackageNames = null;

	/**
	 * The number of events read.
	 */
	private int eventCount = 0;

	public OutOfStockEventsSpreadsheetParser(String spreadsheetFileName, String relevantSheetName,
		boolean debugging)
	{
		super(spreadsheetFileName, relevantSheetName, debugging);
	}

	public OutOfStockEventsSpreadsheetParser(InputStream spreadsheetInputStream, String spreadsheetInputStreamName,
		String relevantSheetName, boolean debugging)
	{
		super(spreadsheetInputStream, spreadsheetInputStreamName, relevantSheetName, debugging);
	}

	public List getUnrecognizedStoreIds()
	{
		return unrecognizedStoreIds;
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
		String upcidKey[] = { "upcid" };
		String pg2pcKey[] = { "productgroup", "productcategory" };
		String pc2pKey[] = { "productcategory", "product" };
		String p2ppKey[] = { "product", "productpackage" };
		
		PersistenceManager manager = PersistenceManager.getPersistenceManager();
		batchManager = new BatchManager(connection);

		// For the different objects in the next section of code (product categories, products, etc.)
		// we create a new object that will be reused per line in the spreadsheet, then create a
		// reusable parameterized insertion query that can be used in the batch manager, and retrieve
		// a cache of all the objects from the database of the same type.
		// Like in ActiveStoresSpreadsheet, we do update the upload time of all products, but we do not
		// consider UN-uploaded products to be discontinued because there is a reasonable possibility
		// that a product may not be referenced in the new spreadsheet without necessarily meaning
		// that the product has been discontinued.

		productGroup = new ProductGroup();
		batchManager.addQuery("ProductGroupCreate", productGroup.makeParameterizedCreateQuery(), new Integer(100));
		allProductGroups = manager.
			findBeans(connection, ProductGroup.tableName, null, ProductGroup.class, idKey, null);
		manager.bulkUpdate(connection, ProductGroup.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		productCategory = new ProductCategory();
		batchManager.addQuery("ProductCategoryCreate", productCategory.makeParameterizedCreateQuery(), new Integer(100));
		allProductCategories = manager.
			findBeans(connection, ProductCategory.tableName, null, ProductCategory.class, idKey, null);
		manager.bulkUpdate(connection, ProductCategory.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		product = new Product();
		batchManager.addQuery("ProductCreate", product.makeParameterizedCreateQuery(), new Integer(100));
		allProducts = manager.
			findBeans(connection, Product.tableName, null, Product.class, upcidKey, null);
		manager.bulkUpdate(connection, Product.tableName, 
		 	"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		productPackage = new ProductPackage();
		batchManager.addQuery("ProductPackageCreate", productPackage.makeParameterizedCreateQuery(), new Integer(100));
		allProductPackages = manager.
			findBeans(connection, ProductPackage.tableName, null, ProductPackage.class, nameKey, null);
		manager.bulkUpdate(connection, ProductPackage.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
		
		allStores = manager.
			findBeans(connection, Store.tableName, null, Store.class, idKey, null);

		outOfStockEvent = new OutOfStockEvent();
		batchManager.addQuery("OutOfStockEventCreate", outOfStockEvent.makeParameterizedCreateQuery(), new Integer(200));
		
		// Create the mappings between Distributor divisions and stores
		Map tempMap = manager.findRows(connection, 
			"SELECT dstbdvsns.id as distributordivisionid, stores.id as storeid " + 
			"FROM dstbdvsns, dstbdvsnstodstbdstrcts, dstbdstrctstostores, stores " +
			"WHERE dstbdvsns.primarykey = dstbdvsnstodstbdstrcts.distributordivision AND " +
				"dstbdvsnstodstbdstrcts.distributordistrict = dstbdstrctstostores.distributordistrict AND " +
				"stores.primarykey = dstbdstrctstostores.store", null);
		if (tempMap != null && !tempMap.isEmpty())
		{
			Collection tempCollection = tempMap.values();
			if (!tempCollection.isEmpty())
			{
				allDistributorDivisionsToStores = new HashSet();
				Iterator iterator = tempCollection.iterator();
				while (iterator.hasNext())
				{
					ListOrderedMap map = (ListOrderedMap) iterator.next();
					allDistributorDivisionsToStores.add(map.get("DISTRIBUTORDIVISIONID").toString() + " " + map.get("STOREID").toString());
				}
			}
		}

		// In the next section of the code (all the mappings below), we create a new mapping object
		// that will be reused per line in the spreadsheet, as well as a parameterized insertion query.
		// This is followed by caching all the current mappings, and updating their upload times in
		// the database.
		productGroupToProductCategoryMapping = new ProductGroupToProductCategoryMapping();
		batchManager.addQuery("ProductGroupToProductCategoryMappingCreate", 
			productGroupToProductCategoryMapping.makeParameterizedCreateQuery(), new Integer(200));
		allProductGroupToProductCategoryMappings = manager.
			findBeans(connection, ProductGroupToProductCategoryMapping.tableName, null, 
				ProductGroupToProductCategoryMapping.class, pg2pcKey, null);
		manager.bulkUpdate(connection, ProductGroupToProductCategoryMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		productCategoryToProductMapping = new ProductCategoryToProductMapping();
		batchManager.addQuery("ProductCategoryToProductMappingCreate", 
			productCategoryToProductMapping.makeParameterizedCreateQuery(), new Integer(200));
		allProductCategoryToProductMappings = manager.
			findBeans(connection, ProductCategoryToProductMapping.tableName, null, 
				ProductCategoryToProductMapping.class, pc2pKey, null);
		manager.bulkUpdate(connection, ProductCategoryToProductMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);

		productToProductPackageMapping = new ProductToProductPackageMapping();
		batchManager.addQuery("ProductToProductPackageMappingCreate", 
			productToProductPackageMapping.makeParameterizedCreateQuery(), new Integer(200));
		allProductToProductPackageMappings = manager.
			findBeans(connection, ProductToProductPackageMapping.tableName, null, 
				ProductToProductPackageMapping.class, p2ppKey, null);
		manager.bulkUpdate(connection, ProductToProductPackageMapping.tableName, 
			"timelastuploaded = TIMESTAMP " + BasePersistentBean.BQ + uploadTime + BasePersistentBean.EQ, null);
	}
	
	/**
	 * Resets all the fields of the reusable set of persistable objects to empty values. This should
	 * be called whenever we go from one line of the spreadsheet to another.
	 */
	protected void resetPersistentObjects()
	{
		super.resetPersistentObjects();
		productGroup.resetFields();
		productCategory.resetFields();
		product.resetFields();
		productPackage.resetFields();
		outOfStockEvent.resetFields();
		
		productGroupToProductCategoryMapping.resetFields();
		productCategoryToProductMapping.resetFields();
		productToProductPackageMapping.resetFields();
	}
	
	/**
	 * This method is similar to persistNamedUploadable in the superclass, however there are three differences.
	 * Unfortunately, products have 'descriptions' instead of 'names', their UPC id's require longs
	 * instead of ints, and descriptions are NOT unique (this last one is a fundamental difference versus names 
	 * in the other entities).
	 */
	private void persistProduct(Product product, Map cachedTableMap)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		String objectKey = product.getClass().getName() + product.getUpcId().longValue();
		if (!encounteredObjectCache.containsKey(objectKey))
		{
			//MainLog.getLog().debug("\tUncached: " + product.getClass().getName() + " " + product.getName());
			product.setTimeLastUploaded(uploadTime);
	
			Product productCopyInDatabase = null;
			if (cachedTableMap != null)
				productCopyInDatabase = 
					(Product) cachedTableMap.get(product.getUpcId().toString()); 
	
			if (productCopyInDatabase != null)
			{
				product.setPrimaryKey( productCopyInDatabase.getPrimaryKey() );
				productCopyInDatabase.setTimeLastUploaded(uploadTime);
				//product.update(connection);
			}
			else
			{
				batchManager.addBeanParameters("ProductCreate", 
					product, BasePersistentBean.BT_CREATE);
				//product.create(connection);
				newPersistentObjects.put(product.getClass().getName(), product);
			}
			
			// cache object so we don't have to hit the database for it anymore
			encounteredObjectCache.put(objectKey, product.getPrimaryKey());
		}
		else
		{
			product.setPrimaryKey((Integer) encounteredObjectCache.get(objectKey));
		}
	}
	
	/**
	 * Persists all the new objects and mappings encountered on a particular line of the spreadsheet.
	 */
	protected void persistPersistentObjects()
		throws InstantiationException, IllegalAccessException, SQLException
	{
		super.persistPersistentObjects();

		persistNamedWithIdUploadable(productGroup, allProductGroups);
		productGroupToProductCategoryMapping.setProductGroup(productGroup.getPrimaryKey());

		persistNamedWithIdUploadable(productCategory, allProductCategories);
		productGroupToProductCategoryMapping.setProductCategory(productCategory.getPrimaryKey());
		productCategoryToProductMapping.setProductCategory(productCategory.getPrimaryKey());

		persistProduct(product, allProducts);
		productCategoryToProductMapping.setProduct(product.getPrimaryKey());
		productToProductPackageMapping.setProduct(product.getPrimaryKey());
		outOfStockEvent.setProduct(product.getPrimaryKey());
		
		persistNamedUploadable(productPackage, allProductPackages);
		productToProductPackageMapping.setProductPackage(productPackage.getPrimaryKey());

		batchManager.addBeanParameters("OutOfStockEventCreate", 
			outOfStockEvent, BasePersistentBean.BT_CREATE);
		//outOfStockEvent.create(connection);

		persistUploadableToUploadableMapping(
			productGroup, productCategory,  
			productGroupToProductCategoryMapping,
			allProductGroupToProductCategoryMappings);
		persistUploadableToUploadableMapping(
			productCategory, product, 
			productCategoryToProductMapping,
			allProductCategoryToProductMappings);
		persistUploadableToUploadableMapping(
			product, productPackage,
			productToProductPackageMapping,
			allProductToProductPackageMappings);
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
		if (allProductGroups != null) regressMissingPersistentObjects(allProductGroups.values());
		if (allProductCategories != null) regressMissingPersistentObjects(allProductCategories.values());
		if (allProducts != null) regressMissingPersistentObjects(allProducts.values());
		if (allProductPackages != null) regressMissingPersistentObjects(allProductPackages.values());
		if (allProductGroupToProductCategoryMappings != null) regressMissingPersistentObjects(allProductGroupToProductCategoryMappings.values());
		if (allProductCategoryToProductMappings != null) regressMissingPersistentObjects(allProductCategoryToProductMappings.values());
		if (allProductToProductPackageMappings != null) regressMissingPersistentObjects(allProductToProductPackageMappings.values());
	}

	/**
	 * Folds alternative spellings of different package names to standard names.
	 */
	private void prepareFoldedProductPackageNames()
	{
		foldedProductPackageNames = new HashMap();

		foldedProductPackageNames.put("6-.5LT", "6-.5 LT");

		foldedProductPackageNames.put("6-8 OZ", "6-8 FZ");

		foldedProductPackageNames.put("6/10 OZ", "6-10 FZ");

		foldedProductPackageNames.put("6/12 OZ", "6-12 FZ");

		foldedProductPackageNames.put("6-16.9", "6-16.9Z");
		
		foldedProductPackageNames.put("6-33.8", "6-33.8Z");

		foldedProductPackageNames.put("12-12 F", "12-12FZ");
		foldedProductPackageNames.put("12-12 O", "12-12FZ");
		
		foldedProductPackageNames.put("24-12 F", "24-12FZ");
		foldedProductPackageNames.put("24/12OZ", "24-12FZ");

		foldedProductPackageNames.put("20 OZ", "20 FZ");
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
						prepareFoldedProductPackageNames();
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
					regressMissingPersistentObjects();
					
					batchManager.executeAll(true);

					// these two only because they always appear on the specify report parameters page
					ProductCategory.getCache(connection).reload(connection);
					ProductPackage.getCache(connection).reload(connection);
					
					OutOfStockEvent.updateEarliestEventDate(connection);
					
					ListOrderedMap ignoredBeverageCategories = 
						Setting.findByCategoryNameValue(null, "IgnoredEvent", "ProductCategory", null);
					if (ignoredBeverageCategories != null && ignoredBeverageCategories.size() > 0)
					{
						for (int j=0; j<ignoredBeverageCategories.size(); j++)
						{
							String beverageCategory = ((Setting) ignoredBeverageCategories.getValue(j)).getValue();
							SettingsManager.applyIgnoredBeverageCategoryToEvents(connection, beverageCategory);
							SettingsManager.applyIgnoredBeverageCategoryToProductCategories(connection, beverageCategory);
						}
					}

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
					case 0: // division_id
						//MainLog.getLog().debug("Row: " + numberRecord.getRow());
						resetPersistentObjects();
						distributorDivisionId = new Integer((int) numberRecord.getValue());
						eventCount++;
						break;
					case 1: // store_id
						storeId = new Integer((int) numberRecord.getValue());
						if (allStores.get(storeId.toString()) != null)
						{
							outOfStockEvent.setStore( ((Store) allStores.get(storeId.toString())).getPrimaryKey() );
							if (allDistributorDivisionsToStores != null &&
								!allDistributorDivisionsToStores.contains(distributorDivisionId.toString() + " " + storeId.toString()))
								throw new SpreadsheetParsingException("Invalid division to store mapping: " +
									"Division = " + distributorDivisionId.toString() + ", Store = " + storeId.toString());
						}
						else
						{
							if (unrecognizedStoreIds == null)
								unrecognizedStoreIds = new ArrayList();
							if (!unrecognizedStoreIds.contains(storeId))
								unrecognizedStoreIds.add(storeId);
						}
						break;
					case 2: // group_id
						productGroup.setId(new Integer((int) numberRecord.getValue()));
						break;
					case 4: // category_id
						productCategory.setId(new Integer((int) numberRecord.getValue()));
						break;
					case 6: // UPC_ID
						product.setUpcId(new Long((long) numberRecord.getValue()));
						break;
					case 10: // scan_dt
						outOfStockEvent.setDateOccurred(new Date(HSSFDateUtil.getJavaDate(numberRecord.getValue()).getTime()));
						break;
					case 11: // OOS_COUNT
						outOfStockEvent.setCount(new Integer((int) numberRecord.getValue()));
						break;
					case 12: // LOST_SALES_QTY
						outOfStockEvent.setLostSalesQuantity(new Float((float) numberRecord.getValue()));
						break;
					case 13: // LOST_SALES_AMT
						outOfStockEvent.setLostSalesAmount(new Float((float) numberRecord.getValue()));
						break;
					case 14: // VEND_NBR
						outOfStockEvent.setVendorNumber(new Integer((int) numberRecord.getValue()));
						break;
					case 15: // VEND_SUB_NBR
						outOfStockEvent.setVendorSubnumber(new Integer((int) numberRecord.getValue()));
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
					case 0: // division_id
						//MainLog.getLog().debug("Row: " + labelRecord.getRow());
						resetPersistentObjects();
						eventCount++;
						break;
					case 3: // group_nm
						productGroup.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 5: // category_nm
						productCategory.setName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 7: // upc_dsc
						product.setDescription(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 8: // SIZE_DSC
						String size = staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim();
						if (foldedProductPackageNames.containsKey(size))
							size = (String) foldedProductPackageNames.get(size);
						productPackage.setName(size);
						break;
					case 9:
						outOfStockEvent.setReason(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 14: // VEND_NBR
						outOfStockEvent.setVendorNumber(Integer.valueOf((staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim())));
						break;
					case 15: // VEND_SUB_NBR
						outOfStockEvent.setVendorSubnumber(Integer.valueOf((staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim())));
						break;
					case 16: // DSD_WHS_FLAG
						outOfStockEvent.setDsdwhsFlag(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());
						break;
					case 17: // VEND_NAME
						outOfStockEvent.setVendorName(staticStringRecords.getString(labelRecord.getSSTIndex()).toString().trim());

						if (unrecognizedStoreIds == null || !unrecognizedStoreIds.contains(storeId))
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
		return "Out of stock events spreadsheet: " + (spreadsheetFileName != null ? spreadsheetFileName :
			spreadsheetInputStreamName + ". " + eventCount + " events read.");
	}

	// This can be used in a pinch to read spreadsheet files from the command line.
	public static void processFiles(String args[])
	{
		try
		{
			PersistenceManager.initializePersistenceManager(null);

			for (int i=0; i<args.length; i++)
			{
				MainLog.getLog().debug("Processing file: " + args[i]);
				long time1 = System.currentTimeMillis();

				OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
					args[i], "Sheet1", false);
				parser.parse("admin");

				if (parser.getUnrecognizedStoreIds() != null)
					MainLog.getLog().debug("Unrecognized stores: " + parser.getUnrecognizedStoreIds().toString());

				MainLog.getLog().debug("Elapsed time: " + ((double) System.currentTimeMillis() - time1)/1000 + " seconds");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//throw new SpreadsheetParsingException("Underlying exception: " + e.getClass().getName() + 
			//	(e.getMessage() != null ? " " + e.getMessage() : ""));
		}
	}

	public static void main(String args[])
	{
		try
		{
			MainLog.init("data/loggerConfiguration.lcf");
			PersistenceManager.initializePersistenceManager(null);
			// processFiles(args);
			// if (true) return;
			

			long time1 = System.currentTimeMillis();
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Bottling Company week 26 OOS.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Fake Bottling Company week 27 OOS.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Fake Bottling Company week 28 OOS.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Fake Bottling Company week 29 OOS.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Bottling Company week 37 OOS.zip", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Bottling Company OOS Wk 22 fake.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Bottling Company week 34 OOS.zip", "Sheet1", false);
			
			OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
				"data/Bottling Company w6-2.xls", "Sheet1", false);
			//OutOfStockEventsSpreadsheetParser parser = new OutOfStockEventsSpreadsheetParser(
			//	"data/Bottling Company W7-2.xls", "Sheet1", false);
			
			

			parser.parse("ahmed");
			
			if (parser.getUnrecognizedStoreIds() != null)
				MainLog.getLog().debug("Unrecognized stores: " + parser.getUnrecognizedStoreIds().toString());
			
			MainLog.getLog().debug("Elapsed time: " + ((double) System.currentTimeMillis() - time1)/1000 + " seconds");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//throw new SpreadsheetParsingException("Underlying exception: " + e.getClass().getName() + 
			//	(e.getMessage() != null ? " " + e.getMessage() : ""));
		}
	}
}
