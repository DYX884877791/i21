/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.beans.factory.support;

import com.interface21.beans.factory.BeanFactory;

/**
 * Interface to be implemented by objects that can 
 * load BeanFactories (usually on behalf of application 
 * components such as EJBs).
 * @see com.interface21.ejb.support.AbstractEnterpriseBean
 * @author Rod Johnson
 * @since 20-Jul-2003
 * @version $Id: BeanFactoryLoader.java,v 1.1 2003/07/26 13:19:15 johnsonr Exp $
 */
public interface BeanFactoryLoader {
	
	/**
	 * Load the BeanFactory
	 * @return BeanFactory loaded BeanFactory. 
	 * Never returns null. 
	 * @throws BootstrapException if a BeanFactory cannot be loaded
	 */
	BeanFactory loadBeanFactory() throws BootstrapException;

}
