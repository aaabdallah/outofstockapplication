package net.btlcpy.outofstock.spreadsheetparsers.exceptions;

/**
 * Represents different types of errors that ultimately return to parsing exceptions.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class SpreadsheetParsingException extends Exception
{
	// ----- Static members ---------------------------------------------------
	/**
	 * Serialization Version Number
	 */
	private static final long serialVersionUID = 1000L;

	public SpreadsheetParsingException()
	{
		super();
	}
	
	public SpreadsheetParsingException(String message)
	{
		super(message);
	}
}
