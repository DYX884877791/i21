package com.interface21.jdbc.core.support;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.core.DataFieldMaxValueIncrementer;

/**
 * Implementation of {@link com.interface21.jdbc.core.DataFieldMaxValueIncrementer}
 * Uses <b>Template Method</b> design pattern
 * Subclasses should provide implementations of protected abstract methods
 * @author <a href="mailto:dkopylenko@acs.rutgers.edu>Dmitriy Kopylenko</a>
 * @author <a href="mailto:isabelle@meta-logix.com">Isabelle Muszynski</a>
 * @version $Id: AbstractDataFieldMaxValueIncrementer.java,v 1.1 2003/04/21 19:10:06 isabellem Exp $
 *
 * History 
 * 17/04/2003 : donated to Spring by Dmitriy Kopylenko
 * 19/04/2003 : modified by Isabelle Muszynski, added nextDoubleValue
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
     * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextDOubleValue
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
     * @see com.interface21.jdbc.core.DataFieldMaxValueIncrementer#nextValue()
     */
    public final Object nextValue(Class keyClass) throws DataAccessException {
	if (int.class.getName().equals(keyClass.getName()) || 
	    Integer.class.getName().equals(keyClass.getName()))
	    return new Integer(incrementIntValue());
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
     * @see #nextDoubleValue
     */
    protected abstract double incrementDoubleValue() throws DataAccessException;
	
    /**
     * Template method implementation to be provided by concrete subclasses
     * @see #nextStringValue
     */
    protected abstract String incrementStringValue() throws DataAccessException;
	
}
	
