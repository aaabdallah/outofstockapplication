package net.btlcpy.outofstock.persistence.beans;

/**
 * A persistent bean to represent product groups.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductGroup extends NamedWithIdUploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctgrps";
	
	// ----- Instance members -------------------------------------------------
	public ProductGroup()
	{
	}
	
	// ----- Overridden abstract methods --------------------------------------
	public String getTable() { return tableName; }
}
