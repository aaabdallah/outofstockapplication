package net.btlcpy.outofstock.persistence.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A persistent bean to represent product group to product category mappings.
 * 
 * @author Ahmed Abd-Allah
 */
public class ProductGroupToProductCategoryMapping extends Uploadable
{
	// ----- Static members ---------------------------------------------------
	/** Database table name */
	public static final String tableName = "prdctgrpstoprdctctgrs";
	
	// ----- Instance members -------------------------------------------------
	private Integer productGroup;
	private Integer productCategory;

	public ProductGroupToProductCategoryMapping()
	{
	}

	public Integer getProductGroup()
	{
		return productGroup;
	}

	public void setProductGroup(Integer productGroup)
	{
		this.productGroup = productGroup;
	}

	public Integer getProductCategory()
	{
		return productCategory;
	}

	public void setProductCategory(Integer productCategory)
	{
		this.productCategory = productCategory;
	}

	// ----- Overridden abstract methods -------------------------------------- 
	public String getTable() { return tableName; }
	
	public void resetFields()
	{
		super.resetFields();
		productGroup = null;
		productCategory = null;
	}
	public String getFieldNames() 
	{ 
		return super.getFieldNames() + ", productgroup, productcategory"; 
	}
	
	public int getFieldNameCount()
	{
		return super.getFieldNameCount() + 2;
	}

	public String getFieldValuesAsString() 
	{
		return super.getFieldValuesAsString() + ", " + productGroup.intValue() + 
			", " + productCategory.intValue();
	}
	
	public String getFieldUpdatesAsString() 
	{
		return super.getFieldUpdatesAsString() + 
			", productgroup = " + productGroup.intValue() +
			", productcategory = " + productCategory.intValue();
	} 

	public void loadFields(ResultSet resultSet)
		throws SQLException
	{
		super.loadFields(resultSet);
		setProductGroup(new Integer(resultSet.getInt("productgroup")));
		setProductCategory(new Integer(resultSet.getInt("productcategory")));
	}
	public void addToBatch(PreparedStatement preparedStatement, int batchType, boolean batchNow)
		throws SQLException
	{
		super.addToBatch(preparedStatement, batchType, false);
		
		if (batchType == BT_CREATE)
		{
			int index = super.getFieldNameCount() + 1;
			preparedStatement.setInt(index++, productGroup.intValue());
			preparedStatement.setInt(index++, productCategory.intValue());
			if (batchNow) preparedStatement.addBatch();
		}
	}
}
