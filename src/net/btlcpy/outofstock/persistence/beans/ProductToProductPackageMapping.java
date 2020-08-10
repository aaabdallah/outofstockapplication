package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A persistent bean to represent product to product package mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductToProductPackageMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctstoprdctpkgs";
	
	// ----- Instance members -------------------------------------------------
	private Integer product;
	private Integer productPackage;

	public ProductToProductPackageMapping()
	{
	}

	public Integer getProduct()
	{
		return product;
	}

	public void setProduct(Integer product)
	{
		this.product = product;
	}

	public Integer getProductPackage()
	{
		return productPackage;
	}

	public void setProductPackage(Integer productPackage)
	{
		this.productPackage = productPackage;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		product = null;
		productPackage = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", product, productpackage"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + product.intValue() + 
			", " + productPackage.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", product = " + product.intValue() +
			", productpackage = " + productPackage.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setProduct(new Integer(resultSet.getInt("product")));
		setProductPackage(new Integer(resultSet.getInt("productpackage")));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, product.intValue());
			preparedStatement.setInt(index++, productPackage.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
