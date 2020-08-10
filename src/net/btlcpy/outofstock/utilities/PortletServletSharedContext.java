package net.btlcpy.outofstock.utilities;

import java.util.Hashtable;

/**
 * This class is intended to act as a simple shared memory manager accessible 
 * from within servlets and portlets. It is a thin wrapper around a singleton
 * Map that is accessible to everyone.
 * 
 * @author Ahmed A. Abd-Allah
 */
public class PortletServletSharedContext extends Hashtable  
{
	// For Serializable purposes
	public static final long serialVersionUID = 1;

	private static PortletServletSharedContext sharedContext = null;
	
	synchronized public static PortletServletSharedContext getInstance()
	{
		if (sharedContext == null)
			sharedContext = new PortletServletSharedContext();
		
		return sharedContext;
	}
	
	// Protected access on purpose.
	protected PortletServletSharedContext()
	{
		
	}
}
