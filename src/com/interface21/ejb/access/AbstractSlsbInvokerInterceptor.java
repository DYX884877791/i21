/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.ejb.access;

import org.aopalliance.MethodInterceptor;

import com.interface21.beans.BeanWrapper;
import com.interface21.beans.BeanWrapperImpl;
import com.interface21.beans.factory.InitializingBean;
import com.interface21.jndi.AbstractJndiLocator;

/**
 * Superclass for all AOP interceptors invoking EJBs.
 * @author Rod Johnson
 * @version $Id: AbstractSlsbInvokerInterceptor.java,v 1.1 2003/06/13 13:40:37 jhoeller Exp $
 */
public abstract class AbstractSlsbInvokerInterceptor extends AbstractJndiLocator implements MethodInterceptor, InitializingBean {

	/** 
	 * Name of no arg create() method required on EJB homes,
	 * but not part of EJBLocalHome
	 */
	protected static final String CREATE_METHOD = "create";
	
	private BeanWrapper homeBeanWrapper;


	/**
	 * We can get actual home from the BeanWrapper, if we ever need it
	 * @return a BeanWrapper for the EJB home interface. This
	 * may be a local or remote home.
	 */
	protected BeanWrapper getHomeBeanWrapper() {
		return this.homeBeanWrapper;
	}

	
 	/**
 	 * Implementation of protected abstract method to cache the home wrapper.
	 * @see com.interface21.jndi.AbstractJndiLocator#located(java.lang.Object)
	 */
	protected void located(Object o) {
		this.homeBeanWrapper = new BeanWrapperImpl(o);
	}

}
