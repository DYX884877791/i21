package com.interface21.jdbc.core.support;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.core.DataFieldMaxValueIncrementer;

/**
 * Implementation of {@link com.interface21.jdbc.core.DataFieldMaxValueIncrementer}
 * Uses <b>Template Method</b> design pattern
 * Subclasses should provide implementations of protected abstract methods.
 * <br><br><b>History:</b> 
 * <li>17/04/2003 : donated to Spring by Dmitriy Kopylenko
 * <li>19/04/2003 : modified by Isabelle Muszynski, added nextDoubleValue
 * <li>09/05/2003 : modified by JPP, added nextLongValue
 * @author Dmitriy Kopylenko
 * @author Isabelle Muszynski
 * @author Jean-Pierre Pawlak
 * @version $Id: AbstractDataFieldMaxValueIncrementer.java,v 1.9 2003/06/10 17:51:03 dkopylenko Exp $
 *
 */
public abstract class AbstractDataFieldMaxValueIncrementer 
	implements DataFieldMaxValueIncrementer {

	/**
	 * Template method
	 * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextIntValue
	 */
	public final int nextIntValue() throws DataAccessException {
	return incrementIntValue();
	}
	
	/**
	 * Template method
	 * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextLongValue
	 */
	public final long nextLongValue() throws DataAccessException {
	return incrementLongValue();
	}

	/**
	 * Template method
	 * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextDoubleValue
	 */
	public final double nextDoubleValue() throws DataAccessException {
	return incrementDoubleValue();
	}
	
	/**
	 * Template method
	 * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextStringValue()
	 */
	public final String nextStringValue() throws DataAccessException {
	return incrementStringValue();
	}

	/**
	 * Template method
	 * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextValue(java.lang.Class)
	 */
	public final Object nextValue(Class keyClass) throws DataAccessException {
	if (int.class.getName().equals(keyClass.getName()) || 
		Integer.class.getName().equals(keyClass.getName()))
		return new Integer(incrementIntValue());
	else if (long.class.getName().equals(keyClass.getName()) || 
		Long.class.getName().equals(keyClass.getName()))
		return new Long(incrementLongValue());
	else if (double.class.getName().equals(keyClass.getName()) || 
		Double.class.getName().equals(keyClass.getName()))
		return new Double(incrementDoubleValue());
	else if (String.class.getName().equals(keyClass.getName()))
		return incrementStringValue();
	else
		throw new IllegalArgumentException("Invalid key class");
	}

	/**
	 * Template method implementation to be provided by concrete subclasses
	 * @see #nextIntValue
	 */
	protected abstract int incrementIntValue() throws DataAccessException;
	
	/**
	 * Template method implementation to be provided by concrete subclasses
	 * @see #nextLongValue
	 */
	protected abstract long incrementLongValue() throws DataAccessException;

	/**
	 * Template method implementation to be provided by concrete subclasses
	 * @see #nextDoubleValue
	 */
	protected abstract double incrementDoubleValue() throws DataAccessException;
	
	/**
	 * Template method implementation to be provided by concrete subclasses
	 * @see #nextStringValue
	 */
	protected abstract String incrementStringValue() throws DataAccessException;
	
}
	
