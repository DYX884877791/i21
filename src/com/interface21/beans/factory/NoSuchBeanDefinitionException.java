
package com.interface21.beans.factory;

import com.interface21.beans.BeansException;

/**
 * Exception thrown when a BeanFactory is asked for a bean 
 * instance name for which it cannot find a definition.
 * @author Rod Johnson
 * @version $Id: NoSuchBeanDefinitionException.java,v 1.2 2003/07/05 16:27:45 johnsonr Exp $
 */
public class NoSuchBeanDefinitionException extends BeansException {
	
	/** Name of the missing bean */
	private final String name;

	 /**
     * Creates new <code>NoSuchBeanDefinitionException</code>..
     * @param name the name of the missing bean
     * @param message further, detailed message describing the problem.
     */
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named [" + name + "] is defined {" + message + "}", null);
        this.name = name;
    }
    
    /**
     * Return the name of the missing bean
     * @return the name of the missing bean
     */
    public String getBeanName() {
        return name;
    }
    
}


