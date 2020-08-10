package net.btlcpy.outofstock.persistence.exceptions;

/**
 * Thrown when the application is unable to find a particular row/bean
 * in the database that *should* be there.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class FindException extends Exception
{
	// ----- Static members ---------------------------------------------------
	/**
	 * Serialization Version Number
	 */
	private static final long serialVersionUID = 1000L;
	
	// ----- Instance members -------------------------------------------------
	public FindException()
	{
		super();
	}

	public FindException(String message)
	{
		super(message);
	}
}
