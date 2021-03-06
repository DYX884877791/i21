
package com.interface21.web.servlet.mvc;

import javax.servlet.http.HttpServletResponse;

import com.interface21.web.context.support.WebApplicationObjectSupport;

/**
 * Convenient superclass for any kind of
 * web content generator, like Controller or MultiAction or
 * different workflow. Supports HTTP cache control options.
 * @author Rod Johnson
 */
public class WebContentGenerator extends WebApplicationObjectSupport {
	
	//---------------------------------------------------------------------
	// Instance data
	//---------------------------------------------------------------------
	/** Use HTTP 1.0 expires header? */
	private boolean useExpiresHeader = true;
	
	
	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------
	public WebContentGenerator() {
	}
	
	
	//---------------------------------------------------------------------
	// Bean properties
	//---------------------------------------------------------------------
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}
	
	
	//---------------------------------------------------------------------
	// Convenience methods for subclasses
	//---------------------------------------------------------------------
	
	// See www.mnot.net.cache docs/
	
	
	/**
	 * Prevent the response being cached
	 */
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader("Pragma", "No-cache");
		// HTTP 1.1 header
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 1L);
	}
	
	
	/**
	 * Set HTTP headers to allow caching for the given number of seconds
	 * @param response HTTP response
	 * @param seconds number of seconds into the future that the response should
	 * be cacheable for.
	 */
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		String hval = "max-age=" + seconds;
		if (mustRevalidate)
			hval += ", must-revalidate";
        response.setHeader("Cache-Control", hval);
        // HTTP 1.1 header
        // Http 1.0 is Expires
        if (this.useExpiresHeader) {
        	// We do this only if it's set as System current time millis
	        response.setDateHeader("Expires", System.currentTimeMillis() + seconds * 1000L);
        }
	}

}	// class WebContentGenerator
