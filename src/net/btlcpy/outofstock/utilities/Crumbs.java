package net.btlcpy.outofstock.utilities;

/**
 * <p>A JavaBean to hold the properties for producing a set of "display crumbs"
 * for users of the web application on each JSP page, to make it easy for them
 * to see where they are as they navigate/browse through the site or tasks.</p>
 * 
 * @author Ahmed A. Abd-Allah
 */
public class Crumbs
{
	/** The main title to display. */
	private String headerTitle;
	/** The image beside the main title. */
	private String headerImageUrl;
	/** The display names of the crumbs, separated by bars <code>('|')</code>. */
	private String headerCrumbNames;
	/** The links for each one of the crumbs, separated by bars. The word "none"
	 *  means no link. */
	private String headerCrumbLinks;
	
	public String getHeaderTitle()
	{
		return headerTitle;
	}
	public void setHeaderTitle(String headerTitle)
	{
		this.headerTitle = headerTitle;
	}
	public String getHeaderImageUrl()
	{
		return headerImageUrl;
	}
	public void setHeaderImageUrl(String headerImageUrl)
	{
		this.headerImageUrl = headerImageUrl;
	}
	public String getHeaderCrumbNames()
	{
		return headerCrumbNames;
	}
	public void setHeaderCrumbNames(String headerCrumbNames)
	{
		this.headerCrumbNames = headerCrumbNames;
	}
	public String getHeaderCrumbLinks()
	{
		return headerCrumbLinks;
	}
	public void setHeaderCrumbLinks(String headerCrumbLinks)
	{
		this.headerCrumbLinks = headerCrumbLinks;
	}
}
