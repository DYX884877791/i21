/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.transaction.interceptor;

import com.interface21.transaction.support.DefaultTransactionDefinition;

/**
 * Transaction attribute that takes EJB approach to rolling
 * back on runtime, but not checked, exceptions.
 * @author Rod Johnson
 * @since 16-Mar-2003
 * @version $Id: DefaultTransactionAttribute.java,v 1.2 2003/07/05 09:46:51 johnsonr Exp $
 */
public class DefaultTransactionAttribute extends DefaultTransactionDefinition
    implements TransactionAttribute {

	/**
	 * Create a new transaction attribute with REQUIRED propagation
	 * and default transaction isolation.
	 */
	public DefaultTransactionAttribute() {
		super();
	}
	
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}
			
	public DefaultTransactionAttribute(int propagationBehavior, int isolationLevel) {
		super(propagationBehavior, isolationLevel);
	}
	
	/**
	 * Default behaviour is as with EJB: rollback on unchecked exception.
	 * Consistent with TransactionTemplate's behavior.
	 */
	public boolean rollBackOn(Throwable t) {
		return (t instanceof RuntimeException);
	}

}
