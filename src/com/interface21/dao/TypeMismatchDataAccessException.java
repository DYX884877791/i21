/**
 * Generic framework code included with 
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/1861007841/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002). 
 * This code is free to use and modify. 
 * Please contact <a href="mailto:rod.johnson@interface21.com">rod.johnson@interface21.com</a>
 * for commercial support.
 */

package com.interface21.dao;

/**
 * Exception thrown on mismatch between Java type and database type:
 * for example on an attempt to set an object of the wrong type
 * in an RDBMS column.
 * @author Rod Johnson
 * @version $Id: TypeMismatchDataAccessException.java,v 1.1.1.1 2003/02/11 08:10:19 johnsonr Exp $
 */
public class TypeMismatchDataAccessException extends DataAccessException {

	/**
	 * Constructor for TypeMismatchDataAccessException.
	 * @param s mesg
	 * @param ex root cause
	 */
	public TypeMismatchDataAccessException(String s, Throwable ex) {
		super(s, ex);
	}

}
