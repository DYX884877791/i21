/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.transaction;

/**
 * Exception thrown when a transaction can't be created using an
 * underlying transaction API such as JTA.
 * @author Rod Johnson
 * @since 17-Mar-2003
 * @version $Revision: 1.2 $
 */
public class CannotCreateTransactionException extends TransactionException {

	public CannotCreateTransactionException(String s) {
		super(s);
	}

	public CannotCreateTransactionException(String s, Throwable ex) {
		super(s, ex);
	}

}
