package com.interface21.jdbc.core.support;

import javax.sql.DataSource;

import com.interface21.beans.factory.InitializingBean;
import com.interface21.jdbc.core.DataSourceUtils;
import com.interface21.jdbc.object.SqlFunction;

/**
 * Class to inceremnet maximum value of a given Oracle SEQUENCE 
 * @author <a href="mailto:dkopylenko@acs.rutgers.edu>Dmitriy Kopylenko</a>
 * @author <a href="mailto:isabelle@meta-logix.com">Isabelle Muszynski</a>
 * @version $Id: OracleSequenceMaxValueIncrementer.java,v 1.1 2003/04/21 19:10:06 isabellem Exp $
 */
public class OracleSequenceMaxValueIncrementer
    extends AbstractDataFieldMaxValueIncrementer
    implements InitializingBean {

    //-----------------------------------------------------------------
    // Instance data
    //-----------------------------------------------------------------
    private DataSource ds;

    private String sequenceName;

    /** Should the string result pre pre-pended with zeroes */
    private boolean prefixWithZero;

    /** The length to which the string result should be pre-pended with zeroes */
    private int paddingLength;

    private NextMaxValueProvider nextMaxValueProvider;

    //-----------------------------------------------------------------
    // Constructors
    //-----------------------------------------------------------------
    /**
     * Default constructor
     **/
    public OracleSequenceMaxValueIncrementer() {
	this.nextMaxValueProvider = new NextMaxValueProvider();
    }

    /**
     * Constructor
     * @param ds the datasource to use
     * @param seqName the sequence name to use for fetching key values
     */
    public OracleSequenceMaxValueIncrementer(DataSource ds, String seqName) {
	this.nextMaxValueProvider = new NextMaxValueProvider();
	this.ds = ds;
	this.sequenceName = seqName;
    }

    /**
     * Constructor
     * @param ds the datasource to be used
     * @param seqName the sequence name to use for fetching key values
     * @param prefixWithZero in case of a String return value, should the string be prefixed with zeroes
     * @param padding the length to which the string return value should be padded with zeroes
     */
    public OracleSequenceMaxValueIncrementer(DataSource ds, String seqName, boolean prefixWithZero, int padding) {
	this.nextMaxValueProvider = new NextMaxValueProvider();
	this.ds = ds;
	this.sequenceName = seqName;
	this.prefixWithZero = prefixWithZero;
	this.paddingLength = padding;
    }

    /**
     * @see com.interface21.jdbc.core.support.AbstractDataFieldMaxValueIncrementer#incrementIntValue()
     */
    protected int incrementIntValue() {
	return nextMaxValueProvider.getNextIntValue();
    }

    /**
     * @see com.interface21.jdbc.core.support.AbstractDataFieldMaxValueIncrementer#incrementDoubleValue()
     */
    protected double incrementDoubleValue() {
	return nextMaxValueProvider.getNextDoubleValue();
    }

    /**
     * @see com.interface21.jdbc.core.support.AbstractDataFieldMaxValueIncrementer#incrementStringValue()
     */
    protected String incrementStringValue() {
	return nextMaxValueProvider.getNextStringValue();
    }

    // Private class that does the actual
    // job of getting the sequence.nextVal value
    private class NextMaxValueProvider {

	public int getNextIntValue() {
	    SqlFunction sqlf = new SqlFunction(ds, "SELECT " + sequenceName + ".NEXTVAL FROM DUAL");
	    sqlf.compile();
	    return sqlf.run();
	}

	private double getNextDoubleValue() {
	    SqlFunction sqlf = new SqlFunction(ds, "SELECT " + sequenceName + ".NEXTVAL FROM DUAL");
	    sqlf.compile();
	    return ((Double)sqlf.runGeneric()).doubleValue();
	}

	private String getNextStringValue() {
	    String s = new Integer(nextIntValue()).toString();
	    if (prefixWithZero) {
		int len = s.length();
		if (len < paddingLength + 1) {
		    StringBuffer buff = new StringBuffer(paddingLength);
		    for (int i = 0; i < paddingLength - len; i++)
			buff.append("0");
		    buff.append(s);
		    s = buff.toString();
		}
	    }

	    return s;
	}
    }

    /**
     * Sets the datasource.
     * @param ds The data source to set
     */
    public void setDataSource(DataSource ds) {
	this.ds = ds;
    }

    /**
     * @see com.interface21.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
	if (ds == null || sequenceName == null)
	    throw new Exception("ds, sequenceName properties must be set on " + getClass().getName());
    }

    /**
     * Sets the prefixWithZero.
     * @param prefixWithZero The prefixWithZero to set
     */
    public void setPrefixWithZero(boolean prefixWithZero, int length) {
	this.prefixWithZero = prefixWithZero;
	this.paddingLength = length;
    }

    /**
     * Sets the sequenceName.
     * @param sequenceName The sequenceName to set
     */
    public void setSequenceName(String sequenceName) {
	this.sequenceName = sequenceName;
    }
}
