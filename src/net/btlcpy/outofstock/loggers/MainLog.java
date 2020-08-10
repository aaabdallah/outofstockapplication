package net.btlcpy.outofstock.loggers;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * <p>Purpose: a singleton class to manage logging from all portlets in the OOS web 
 * application.</p>
 * <p>The init() method should be called first to initialize the logger
 * with the passed in properties file. After that, the logger can always be
 * retrieved using getLog().</p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class MainLog
{
	private static Logger logger;
	private static boolean initComplete = false;

	/**
	 * <p>This method will only be effective when called the first time - thus even if
	 * it is called multiple times, the first time is the only time it will work
	 * (assuming no exception is thrown).</p>
	 * 
	 * <p>It should be called once with a legitimate properties file, otherwise
	 * subsequent calls to {@link MainLog#getLog()} will return a logger that is
	 * configured to default values only.</p>
	 * 
	 * @param propertiesFileLocation location of the logging configuration file
	 */
	synchronized public static void init(String propertiesFileLocation)
	{
		if (!initComplete)
		{
			logger = Logger.getLogger(MainLog.class.getName());
			PropertyConfigurator.configure(propertiesFileLocation);
			initComplete = true;
		}
	}

	/**
	 * Gets the main log.
	 * 
	 * @return the main log
	 */
	public static Logger getLog()
	{
		return logger;
	}
}
