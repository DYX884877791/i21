/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Simple implementation of TransactionAttributeSource that
 * allows attributes to be stored in a map.
 * @since 24-Apr-2003
 * @version $Id: MapTransactionAttributeSource.java,v 1.2 2003/07/23 18:45:29 johnsonr Exp $
 * @author Rod Johnson
 */
public class MapTransactionAttributeSource implements TransactionAttributeSource {
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	/** Map from method to TransactionAttribute */
	protected Map methodMap = new HashMap();
	
	/**
	 * Set an attribute for a transactional method
	 * @param m method
	 * @param ta attribute associated with the method
	 */
	public void addTransactionalMethod(Method m, TransactionAttribute ta) {
		logger.info("Adding transactional method " + m + " with attribute " + ta);
		this.methodMap.put(m, ta);
	}
	
	/**
	 * @see com.interface21.transaction.interceptor.TransactionAttributeSource#getTransactionAttribute(org.aopalliance.MethodInvocation)
	 */
	public TransactionAttribute getTransactionAttribute(MethodInvocation invocation) {
		TransactionAttribute ta = (TransactionAttribute) this.methodMap.get(invocation.getMethod());
		return ta;
	}

}