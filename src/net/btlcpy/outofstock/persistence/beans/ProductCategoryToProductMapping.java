package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A persistent bean to represent product category to product mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductCategoryToProductMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctctgrstoprdcts";

	// ----- Instance members -------------------------------------------------
	private Integer productCategory;
	private Integer product;

	public ProductCategoryToProductMapping()
	{
	}

	public Integer getProductCategory()
	{
		return productCategory;
	}

	public void setProductCategory(Integer productCategory)
	{
		this.productCategory = productCategory;
	}

	public Integer getProduct()
	{
		return product;
	}

	public void setProduct(Integer product)
	{
		this.product = product;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		productCategory = null;
		product = null;
	}

	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", productcategory, product"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + productCategory.intValue() + 
			", " + product.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", productcategory = " + productCategory.intValue() +
			", product = " + product.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setProductCategory(new Integer(resultSet.getInt("productcategory")));
		setProduct(new Integer(resultSet.getInt("product")));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, productCategory.intValue());
			preparedStatement.setInt(index++, product.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
