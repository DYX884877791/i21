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
 * @version $Id: DefaultTransactionAttribute.java,v 1.3 2003/07/31 18:09:48 jhoeller Exp $
 */
public class DefaultTransactionAttribute extends DefaultTransactionDefinition
    implements TransactionAttribute {

	public DefaultTransactionAttribute() {
	}

	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}

	/**
	 * Default behaviour is as with EJB: rollback on unchecked exception.
	 * Consistent with TransactionTemplate's behavior.
	 */
	public boolean rollbackOn(Throwable t) {
		return (t instanceof RuntimeException);
	}

}
