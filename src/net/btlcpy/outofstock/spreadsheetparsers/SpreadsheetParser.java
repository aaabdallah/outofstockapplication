package net.btlcpy.outofstock.spreadsheetparsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipInputStream;

import net.btlcpy.outofstock.loggers.MainLog;
import net.btlcpy.outofstock.persistence.BatchManager;
import net.btlcpy.outofstock.persistence.PersistenceManager;
import net.btlcpy.outofstock.persistence.beans.BasePersistentBean;
import net.btlcpy.outofstock.persistence.beans.NamedUploadable;
import net.btlcpy.outofstock.persistence.beans.NamedWithIdUploadable;
import net.btlcpy.outofstock.persistence.beans.Uploadable;
import net.btlcpy.outofstock.persistence.beans.UserAction;
import net.btlcpy.outofstock.spreadsheetparsers.exceptions.SpreadsheetParsingException;

import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * <p>
 * There are two types of files that are uploaded to the Out of Stock application: active store
 * lists, and out of stock event data. Both files are in Microsoft Excel format. Parsing those
 * files and converting the read-in data to something which can be stored in the database is a
 * key part of the application. Note that ZIP files are also supported, that is, a ZIP file that
 * contains a single Microsoft Excel file in it. ZIP files must end in ".zip" to be recognized.
 * </p>
 * <p>
 * This class is an abstract base class that provides basic facilities for parsing those Excel
 * files. This class does NOT do the low-level reading and parsing itself; the Apache POI
 * library is used for that.
 * </p>
 *  
 * @author Ahmed A. Abd-Allah
 */
abstract public class SpreadsheetParser extends AbortableHSSFListener implements HSSFListener
{
	/** The filename of the spreadsheet being read */
	protected String spreadsheetFileName = null;
	
	/** Alternatively, an already open input stream can be used */
	protected InputStream spreadsheetInputStream = null;
	
	/** The name assigned to an already open input stream (for logging purposes) */
	protected String spreadsheetInputStreamName = null;
	
	/** The relevant sheet name, if any */
	protected String relevantSheetName = null;
	
	/** To lookup the static strings stored with that file (see POI documentation) */
	protected SSTRecord staticStringRecords = null;
	
	/** The position of the relevant sheet, if any */
	protected short relevantSheetPosition = 0;
	
	/** Indicates whether the file is currently being processed or not */
	protected boolean processing = false;
	
	/** The database connection to use */
	protected Connection connection = null;
	
	/** The time the file was uploaded */
	protected Timestamp uploadTime = null;
	
	/** A string to store error information during parsing */
	protected String errorIndicator = null;
	
	/**
	 * This contains all objects encountered in the spreadsheet keyed NOT by their
	 * primary keys (since these aren't found in the spreadsheet) but rather by some
	 * unique field peculiar to that object (e.g. the name for a bottler). This is
	 * useful for quickly skipping over objects we have encountered before during
	 * the parsing process - skipping in the sense that it should not be reinserted
	 * into the database.
	 * 
	 * Note that the actual objects are not stored in the HashMap, only the keys.
	 * A HashSet would suffice except that its backing implementation is a HashMap
	 * anyway. Hence, we use a HashMap directly that maps the object's key to a
	 * dummy value essentially.
	 */
	protected HashMap encounteredObjectCache = new HashMap();
	
	/**
	 * <p>
	 * Used to indicate if an encountered spreadsheet object has been newly persisted
	 * or not. The reason for this: while reading in rows of spreadsheet data, we
	 * encounter new objects occasionally. When these objects are encountered, there
	 * may be a need to add a new mapping between those objects in the database
	 * along with the objects themselves. A good example is if we encounter a new
	 * store: that store needs to be inserted into the database, as well as mappings
	 * between it and the appropriate Distributor district and bottler branch. See the
	 * method "persistUploadableToUploadableMapping" for more details on how this
	 * map is ultimately being used.
	 * </p>
	 * <p>
	 * This mapping is reset with every row read in from the spreadsheet. There is also
	 * a key assumption used in the keys of the map: they are object class names. In
	 * retrospect, this is a weak assumption that has lasted till now; that uploaded
	 * data will contain PER ROW only one object of any one type/class. In other
	 * words, we expect to find in any one row at most one store, one bottler, etc.
	 * </p>
	 */
	protected HashMap newPersistentObjects = new HashMap();

	/** Indicates whether to turn on debugging or not - leads to extra logging info */
	protected boolean debugging = false;
	
	/** 
	 * Debug information: total (Excel) records read - see POI documentation for 
	 * an explanation of what a record is.
	 */
	protected int totalRecordCounter = 0;
	
	/** Debug information: total bytes read */
	protected int totalRecordBytes = 0;
	
	/** Debug information: a hashtable of all record types encountered */
	protected Hashtable recordTypes = null;
	
	/** Debug information: the number of times a particular record has been encountered */ 
	protected Hashtable recordCounters = null;

	/** The batch manager used to optimize insertions, updates, and deletions from the database */
	protected BatchManager batchManager = null;
	
	/**
	 * A constructor to parse from a file.
	 * 
	 * @param spreadsheetFileName the file name
	 * @param relevantSheetName the relevant sheet name
	 * @param debugging turns on debugging if true
	 */
	public SpreadsheetParser(String spreadsheetFileName, String relevantSheetName,
		boolean debugging)
	{
		setSpreadsheetFileName(spreadsheetFileName);
		setRelevantSheetName(relevantSheetName);
		setDebugging(debugging);
	}

	/**
	 * A constructor to parse from an already open input stream (useful when the input file
	 * is actually a stream being uploaded over the web from the user to the server..
	 * 
	 * @param spreadsheetInputStream the input stream
	 * @param spreadsheetInputStreamName the input stream name
	 * @param relevantSheetName the relevant sheet name
	 * @param debugging turns on debugging if true
	 */
	public SpreadsheetParser(InputStream spreadsheetInputStream, String spreadsheetInputStreamName, 
		String relevantSheetName, boolean debugging)
	{
		setSpreadsheetInputStream(spreadsheetInputStream);
		setSpreadsheetInputStreamName(spreadsheetInputStreamName);
		setRelevantSheetName(relevantSheetName);
		setDebugging(debugging);
	}

	public String getSpreadsheetFileName()
	{
		return spreadsheetFileName;
	}

	public void setSpreadsheetFileName(String spreadsheetFileName)
	{
		this.spreadsheetFileName = spreadsheetFileName;
	}

	public InputStream getSpreadsheetInputStream()
	{
		return spreadsheetInputStream;
	}

	public void setSpreadsheetInputStream(InputStream spreadsheetInputStream)
	{
		this.spreadsheetInputStream = spreadsheetInputStream;
	}

	public String getSpreadsheetInputStreamName()
	{
		return spreadsheetInputStreamName;
	}

	public void setSpreadsheetInputStreamName(String spreadsheetInputStreamName)
	{
		this.spreadsheetInputStreamName = spreadsheetInputStreamName;
	}

	public String getRelevantSheetName()
	{
		return relevantSheetName;
	}

	public void setRelevantSheetName(String relevantSheetName)
	{
		this.relevantSheetName = relevantSheetName;
	}

	public boolean isDebugging()
	{
		return debugging;
	}

	public void setDebugging(boolean debugging)
	{
		this.debugging = debugging;
	}

	/**
	 * See POI documentation for the place "abortableProcessRecord" methods have in the
	 * overall way that POI is used to read Excel files. This particular method can be
	 * used to provide debugging information when the file is being read.
	 * 
	 * @param record the record read from the file/stream
	 */
	public void abortableProcessRecordForDebugging(Record record)
	{
		totalRecordCounter++;
		recordTypes.put(record.getClass().getName(), new Short(record.getSid()));
		if (recordCounters.containsKey(record.getClass().getName()))
		{
			int recordCounter = ((Integer) recordCounters.get(record.getClass().getName())).intValue();
			recordCounters.put(record.getClass().getName(), new Integer(recordCounter+1));
		}
		else
			recordCounters.put(record.getClass().getName(), new Integer(1));
	
		// This doesn't seem to work: the byte sizes don't mesh with what BOFRecords report.
		totalRecordBytes += record.getRecordSize();
	}

	protected void resetPersistentObjects()
	{
		newPersistentObjects.clear();
	}

	/**
	 * While parsing a spreadsheet, there are times when a new entity is encountered (e.g. a bottler,
	 * a store, a Distributor division). Each of those entities must be stored - persisted - into the
	 * database. This method is a general-purpose method which does just that for all entities that
	 * are NamedUploadables - entities that have names and upload times. See the class
	 * net.btlcpy.outofstock.persistence.beans.NamedUploadable.java for more details on what is a
	 * NamedUploadable in this context.
	 * 
	 * @param namedUploadable the named, uploadable entity/object to persist
	 * @param cachedTableMap a cache of all currently stored entities of that type in the database. 
	 * The objects are assumed to be keyed by their "name" attribute (the name is assumed to be 
	 * unique - see the database schema).
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	protected void persistNamedUploadable(NamedUploadable namedUploadable, Map cachedTableMap)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		String objectKey = namedUploadable.getClass().getName() + namedUploadable.getName();
		
		// Check if the object has already been encountered before in the parsing process
		if (!encounteredObjectCache.containsKey(objectKey)) // Not encountered before
		{
			//MainLog.getLog().debug("\tUncached: " + namedUploadable.getClass().getName() + " " + namedUploadable.getName());
			namedUploadable.setTimeLastUploaded(uploadTime);

			// Check if this object is in the database
			NamedUploadable namedUploadableCopyInDatabase = null;
			if (cachedTableMap != null)
				namedUploadableCopyInDatabase = (NamedUploadable) cachedTableMap.get(namedUploadable.getName()); 
	
			if (namedUploadableCopyInDatabase != null) // i.e. it IS in the database already
			{
				// Fill in the primary key of the passed in object to reflect the primary key
				// of what is already contained in the database
				namedUploadable.setPrimaryKey( namedUploadableCopyInDatabase.getPrimaryKey() );
				
				// Update the new uploaded time in the cache. Why?
				// 1. Subclasses should bulk update all tables touched by the spreadsheet to reflect
				// the new upload time since that is the logical default - most objects WILL be uploaded
				// again (bottlers, stores, etc. - these are all typically staying in business as opposed
				// to closing).
				// 2. When a table of objects is read in and cached, that reading should occur BEFORE
				// the bulk update. Thus: we have a cache that reflects the old upload times, and a
				// database which has the new bulk update upload time (the most recent upload time).
				// 3. As we encounter each object, we update the upload time in the cache. That is what
				// is going on in the next line of code.
				// 4. After the end of processing all objects in the spreadsheet, subclasses of this 
				// class should call "regressMissingPersistentObjects" to set all the UN-uploaded 
				// objects in the cache to their original upload times. This ensures that old upload 
				// times stay the same for objects that were NOT uploaded in the current spreadsheet.
				namedUploadableCopyInDatabase.setTimeLastUploaded(uploadTime);

				// OLD WAY, inefficient
				//namedUploadable.update(connection);
			}
			else // i.e. it IS NOT in the database
			{
				// Schedule the object for insertion into the database using a batch manager
				// IMPORTANT: Note that the call to 'addBeanParameters' results in a call to the
				// bean's 'addToBatch' method. Since all NamedUploadable's are derived from
				// the class Instrumented, this means that THE PRIMARY KEY WILL BE GENERATED
				// NOW AND FILLED IN, based on what Instrumented.addToBatch() does.
				String className = namedUploadable.getClass().getName();
				batchManager.addBeanParameters(className.substring(className.lastIndexOf('.') + 1) + "Create", 
					namedUploadable, BasePersistentBean.BT_CREATE);

				// OLD WAY, inefficient
				//namedUploadable.create(connection);

				// add this object to the map of new persistent objects (will be used when adding
				// any related new mappings - we will check this map if the object is in it, i.e.
				// has been flagged as a new object to persist. Since it is, a new mapping should
				// also be created.)
				newPersistentObjects.put(namedUploadable.getClass().getName(), namedUploadable);
			}
			
			// cache object so we don't have to worry about it again if we encounter it in
			// the process of reading more objects from the spreadsheet.
			encounteredObjectCache.put(objectKey, namedUploadable.getPrimaryKey());
		}
		else // encountered before
		{
			//MainLog.getLog().debug("\tCached: " + namedUploadable.getClass().getName() + " " + namedUploadable.getName());
			// Based on the previous long comment, we know that sometimes this is a useless
			// line because it sets a null primary key to null again. No matter: nothing depends on it.
			namedUploadable.setPrimaryKey((Integer) encounteredObjectCache.get(objectKey));
		}
	}	

	/**
	 * See persistNamedUploadable.
	 * 
	 * @param namedWithIdUploadable See persistNamedUploadable.
	 * @param cachedTableMap See persistNamedUploadable.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	protected void persistNamedWithIdUploadable(NamedWithIdUploadable namedWithIdUploadable, Map cachedTableMap)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		String objectKey = namedWithIdUploadable.getClass().getName() + namedWithIdUploadable.getName() + 
			namedWithIdUploadable.getId().intValue();
		if (!encounteredObjectCache.containsKey(objectKey))
		{
			//MainLog.getLog().debug("\tUncached: " + namedWithIdUploadable.getClass().getName() + " " + namedWithIdUploadable.getName());
			namedWithIdUploadable.setTimeLastUploaded(uploadTime);
	
			NamedWithIdUploadable namedWithIdUploadableCopyInDatabase = null;
			if (cachedTableMap != null)
				namedWithIdUploadableCopyInDatabase = 
					(NamedWithIdUploadable) cachedTableMap.get(namedWithIdUploadable.getId().toString()); 
	
			if (namedWithIdUploadableCopyInDatabase != null)
			{
				namedWithIdUploadable.setPrimaryKey( namedWithIdUploadableCopyInDatabase.getPrimaryKey() );
				namedWithIdUploadableCopyInDatabase.setTimeLastUploaded(uploadTime);
				//namedWithIdUploadable.update(connection);
			}
			else
			{
				String className = namedWithIdUploadable.getClass().getName();
				batchManager.addBeanParameters(className.substring(className.lastIndexOf('.') + 1) + "Create", 
					namedWithIdUploadable, BasePersistentBean.BT_CREATE);
				//namedWithIdUploadable.create(connection);
				
				newPersistentObjects.put(namedWithIdUploadable.getClass().getName(), namedWithIdUploadable);
			}
			
			// cache object so we don't have to hit the database for it anymore
			encounteredObjectCache.put(objectKey, namedWithIdUploadable.getPrimaryKey());
		}
		else
		{
			namedWithIdUploadable.setPrimaryKey((Integer) encounteredObjectCache.get(objectKey));
		}
	}

	/**
	 * This method is for persisting a possible mapping between two objects (e.g. a bottler
	 * branch mapped to a store, or a Distributor division mapped to a Distributor district). Note
	 * that the direction is from general to specific, i.e. uploadable1 is presumed to be
	 * the containing entity for uploadable2 (again, division to district for example). Note
	 * also that the method checks the two uploadables first to see if either one is newly
	 * encountered before inserting a new mapping. If they are both previously encountered
	 * in the spreadsheet, then no new mapping is generated.
	 * 
	 * @param uploadable1 the containing object
	 * @param uploadable2 the contained object
	 * @param mapping the mapping (also an Uploadable)
	 * @param cachedTableMap the database table cache of all existing mappings
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 */
	protected void persistUploadableToUploadableMapping(
		Uploadable uploadable1, Uploadable uploadable2,
		// boolean checkForDuplicate1, boolean checkForDuplicate2,
		Uploadable mapping, Map cachedTableMap)
		throws InstantiationException, IllegalAccessException, SQLException
	{
		// first check if either one of the endpoints of the proposed mapping is newly persisted
		boolean newData1 = newPersistentObjects.containsKey(uploadable1.getClass().getName());
		boolean newData2 = newPersistentObjects.containsKey(uploadable2.getClass().getName());
		String objectKey = mapping.getClass().getName() + uploadable1.getPrimaryKey().intValue() + uploadable2.getPrimaryKey().intValue();

		mapping.setTimeLastUploaded(uploadTime);
		
		if (!newData1 && !newData2) // NEITHER one of the objects are new, hence the mapping may already be in the database
		{
			// If we have NOT encountered this mapping before, check if it is in the database first
			// before creating a whole new one.
			if (!encounteredObjectCache.containsKey(objectKey)) 
			{
				Uploadable mappingInDatabase = null;
				if (cachedTableMap != null)
				{
					// try to retrieve the mapping either way... forwards or backwards. Trying both ways is
					// based on a few key assumptions:
					// 1. Primary keys are unique globally across all tables
					// 2. This method will be called by callers who understand that they are
					// searching for mappings that can go either way, or by callers who know that
					// they can only go one way. It will NOT be called by callers who expect one
					// direction only, with the possibility that the other direction could exist.
					// In the current Out of Stock application (2008-02-10), 100% of the time, the 
					// first attempt to get a mapping will work. In other words, all the tables that
					// map bottlers to bottler business units, divisions to districts and so on,
					// they all arrange the columns from general to specific.
					mappingInDatabase = 
						(Uploadable) cachedTableMap.get(
							uploadable1.getPrimaryKey().toString() + uploadable2.getPrimaryKey().toString());
					if (mappingInDatabase == null)
						mappingInDatabase =
							(Uploadable) cachedTableMap.get(
								uploadable2.getPrimaryKey().toString() + uploadable1.getPrimaryKey().toString());
				}

				if (mappingInDatabase != null) // the mapping IS in the database
				{
					mapping.setPrimaryKey( mappingInDatabase.getPrimaryKey() );
					mappingInDatabase.setTimeLastUploaded(uploadTime);
					
					// OLD WAY, inefficient
					//mapping.update(connection);
				}
				else // the mapping IS NOT in the database
				{
					String className = mapping.getClass().getName();
					batchManager.addBeanParameters(className.substring(className.lastIndexOf('.') + 1) + "Create", 
						mapping, BasePersistentBean.BT_CREATE);
					
					// OLD WAY, inefficient
					//mapping.create(connection);
				}
				
				encounteredObjectCache.put(objectKey, mapping);
			}
		}
		else if (newData1 || newData2) // At least ONE of the objects is new, so we need to persist a new mapping
		{
			String className = mapping.getClass().getName();
			batchManager.addBeanParameters(className.substring(className.lastIndexOf('.') + 1) + "Create", 
				mapping, BasePersistentBean.BT_CREATE);

			// OLD WAY, inefficient
			//mapping.create(connection);

			encounteredObjectCache.put(objectKey, mapping);
		}
	}

	protected void persistPersistentObjects()
		throws InstantiationException, IllegalAccessException, SQLException
	{
		// the real work is done in the derived classes
	}

	/**
	 * Given a collection of Uploadable's, this method will go through the entire
	 * collection one by one, checking the uploadable if it matches the currently
	 * set upload time. If it is not equal to it, then regress the uploadable's
	 * row in the database to what is in the uploadable in the collection.
	 * 
	 * @param uploadables a collection of objects of type Uploadable
	 * @throws SQLException
	 */
	protected void regressMissingPersistentObjects(Collection uploadables)
		throws SQLException
	{
		Uploadable uploadable = null;
		
		Iterator iterator = uploadables.iterator();
		while (iterator.hasNext())
		{
			uploadable = (Uploadable) iterator.next();
			if (!uploadable.getTimeLastUploaded().equals(uploadTime))
				uploadable.update(connection);
		}
	}

	// null means do NOT record in logs
	abstract public String getUserActionDescription();

	/**
	 * The coordinating method which parses the spreadsheet. It coordinates calls to various
	 * other methods, including those that are overridden in the subclasses.
	 * 
	 * @param username the username of the person requesting that this spreadsheet be
	 * parsed. This information will be used during logging.
	 * 
	 * @throws SpreadsheetParsingException
	 */
	public void parse(String username)
		throws SpreadsheetParsingException
	{
		InputStream fileInput = null;
		InputStream workbookInputStream = null;
		boolean committed = false;
		
		try
		{
			if (debugging)
			{
				recordCounters = new Hashtable();
				recordTypes = new Hashtable();
			}
	
			// Use the POI library to open either the input stream passed into the constructor
			// or the file passed into the other constructor. Note that the input stream is
			// takes precedence, just in case both the input stream and filename are set 
			// simultaneously for whatever reason.
			POIFSFileSystem poifsInput = null;
			if (spreadsheetInputStream == null)
			{
				// ZIP files are supported.
				if (spreadsheetFileName.toLowerCase().endsWith("zip"))
				{
					fileInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(spreadsheetFileName)));
					((ZipInputStream) fileInput).getNextEntry();
				}
				else
					fileInput = new FileInputStream(spreadsheetFileName);
				poifsInput = new POIFSFileSystem(fileInput);
			}
			else
			{
				poifsInput = new POIFSFileSystem(spreadsheetInputStream);
			}
			
			workbookInputStream = poifsInput.createDocumentInputStream("Workbook");
	
			HSSFRequest request = new HSSFRequest();
	
			// lazy listen for ALL records with the listener set to be this object
			request.addListenerForAllRecords(this);
	
			HSSFEventFactory factory = new HSSFEventFactory();
			
			// Set the upload time for all parsed objects to be what we read right now.
			uploadTime = new Timestamp(System.currentTimeMillis());
	
			connection = PersistenceManager.getPersistenceManager().getConnection(false);
			
			// Read and parse the spreadsheet.
			factory.abortableProcessEvents(request, workbookInputStream);
			
			if (errorIndicator == null && relevantSheetPosition != 0) // never reached the desired spreadsheet page
				errorIndicator = "File is missing the required spreadsheet page named: " + relevantSheetName;

			if (errorIndicator != null)
				throw new SpreadsheetParsingException(errorIndicator);
			
			if (getUserActionDescription() != null)
			{
				UserAction userAction = new UserAction();
				userAction.setCategory(UserAction.UPLOAD);
				userAction.setTimeLastUploaded(new Timestamp(System.currentTimeMillis()));
				userAction.setDescription(getUserActionDescription());
				userAction.setName(username);
				userAction.create(connection);
			}

			connection.commit();
			committed = true;
	
			try
			{
				if (debugging)
				{
					MainLog.getLog().debug("=====================================================================");
					MainLog.getLog().debug("=====================================================================");
					MainLog.getLog().debug("=====================================================================");
					MainLog.getLog().debug("Summary of record types/count:");
					Enumeration enumeration = recordTypes.keys();
					while (enumeration.hasMoreElements())
					{
						String type = (String) enumeration.nextElement();
						MainLog.getLog().debug("Key: " + type + 
							", Value: " + ((Short) recordTypes.get(type)).shortValue() +
							", Count: " + ((Integer) recordCounters.get(type)).intValue());
					}
				}
			}
			catch (Exception e) 
			{}
		}
		catch (SpreadsheetParsingException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			MainLog.getLog().error("Exception while parsing spreadsheet", e);
			throw new SpreadsheetParsingException("Underlying exception: " + e.getClass().getName() + 
				(e.getMessage() != null ? ": " + e.getMessage() : ""));
		}
		finally
		{
			// once all the events are processed close our file input stream
			try { if (fileInput != null) fileInput.close(); } catch (Exception e) {}
			try { if (workbookInputStream != null) workbookInputStream.close(); } catch (Exception e) {}			
			try 
			{ 
				if (connection != null)
				{
					if (!committed)
					{
						try { connection.rollback(); } 
						catch (Exception rbe) { MainLog.getLog().error("Exception rolling back", rbe); }
					}
					connection.close();
				}
			} 
			catch (Exception e) 
			{
				MainLog.getLog().error("Exception cleaning up connection", e);
			}		
		}
	}
}
