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
 * Data access exception thrown when something unintended
 * appears to have happened with an update, but the
 * transaction hasn't already been rolled back.
 * Thrown, for example, when we wanted to update 1 row in an RDBMS
 * but actually updated 3
 * @author Rod Johnson
 * @version $Id: IncorrectUpdateSemanticsDataAccessException.java,v 1.1.1.1 2003/02/11 08:10:19 johnsonr Exp $
 */
public abstract class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * Constructor for IncorrectUpdateSemanticsDataAccessException.
	 * @param s message
	 */
	public IncorrectUpdateSemanticsDataAccessException(String s) {
		super(s);
	}

	/**
	 * Constructor for IncorrectUpdateSemanticsDataAccessException.
	 * @param s message
	 * @param ex root cause from the underlying API, such as JDBC.
	 */
	public IncorrectUpdateSemanticsDataAccessException(String s, Throwable ex) {
		super(s, ex);
	}
	
	/**
	 * Return whether data was updated
	 * @return whether data was updated (as opposed to being
	 * incorrectly updated). If this method returns true,
	 * there's nothing to roll back.
	 */
	public abstract boolean getDataWasUpdated();

}
