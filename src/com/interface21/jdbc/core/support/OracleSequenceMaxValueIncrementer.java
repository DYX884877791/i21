package com.interface21.jdbc.core.support;

import java.sql.Types;

import javax.sql.DataSource;

import com.interface21.beans.factory.InitializingBean;
import com.interface21.core.InternalErrorException;
import com.interface21.jdbc.object.SqlFunction;
import com.interface21.jdbc.util.JdbcUtils;

/**
 * Class to increment maximum value of a given Oracle SEQUENCE 
 * If the sequence is created with a INCREMENT_BY value, this class
 * serves the intermediate values without querying the database
 * <br>****TODO Bug: The class has to check if the key donate by Oracle is the INITIAL_VALUE when using INCREMENT_BY.
 * @author <a href="mailto:dkopylenko@acs.rutgers.edu>Dmitriy Kopylenko</a>
 * @author <a href="mailto:isabelle@meta-logix.com">Isabelle Muszynski</a>
 * @author <a href="mailto:jp.pawlak@tiscali.fr">Jean-Pierre Pawlak</a>
 * @version $Id: OracleSequenceMaxValueIncrementer.java,v 1.7 2003/05/10 16:09:11 pawlakjp Exp $
 */
public class OracleSequenceMaxValueIncrementer
	extends AbstractDataFieldMaxValueIncrementer
	implements InitializingBean {

	//-----------------------------------------------------------------
	// Instance data
	//-----------------------------------------------------------------
	private DataSource ds;

	private String sequenceName;

	/** The number of keys buffered in a bunch. */
	private int incrementBy = 1;

	/** Flag if dirty definition */
	private boolean dirty = true;

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
	this.nextMaxValueProvider.setPrefixWithZero(prefixWithZero, padding);
	}

	/**
	 * @see com.interface21.jdbc.core.support.AbstractDataFieldMaxValueIncrementer#incrementIntValue()
	 */
	protected int incrementIntValue() {
	return nextMaxValueProvider.getNextIntValue();
	}

	/**
	 * @see com.interface21.jdbc.core.support.AbstractDataFieldMaxValueIncrementer#incrementLongValue()
	 */
	protected long incrementLongValue() {
	return nextMaxValueProvider.getNextLongValue();
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
	private class NextMaxValueProvider extends AbstractNextMaxValueProvider {

		/** The Sql String preparing to obtain keys from database */
		private static final String PREPARE_SQL = "SELECT INCREMENT_BY FROM ALL_SEQUENCES WHERE SEQUENCE_NAME = ?";

		/** The next id to serve */
		private long nextId = 0;

		/** The max id to serve */
		private long maxId = 0;

		protected long getNextKey(int type) {
			if (dirty) { initPrepare(); }
			if(maxId == nextId) {
				SqlFunction sqlf = new SqlFunction(ds, "SELECT " + sequenceName + ".NEXTVAL FROM DUAL", type);
				sqlf.compile();
				// Even if it's an int, it can be casted to a long
				// old code: maxId =((Long)sqlf.runGeneric()).longValue();
				// Convert to long
				switch(JdbcUtils.translateType(type)) {
				case Types.BIGINT:
					maxId = ((Long)sqlf.runGeneric()).longValue();
					break;
				case Types.INTEGER:
					maxId = ((Integer)sqlf.runGeneric()).intValue();
					break;
				case Types.NUMERIC:
					maxId = (long)((Double)sqlf.runGeneric()).doubleValue();
					break;
				case Types.VARCHAR:
					try {
					maxId = Long.parseLong((String)sqlf.runGeneric());
					} catch (NumberFormatException ex) {
					throw new InternalErrorException("Key value could not be converted to long");
					}
					break;
				}
				nextId = maxId - incrementBy;
			}
			nextId++;
			return nextId;
		}
	
		private void initPrepare() {
			SqlFunction sqlf = new SqlFunction(ds, PREPARE_SQL,	new int[] {Types.VARCHAR} );
			sqlf.compile();
			incrementBy = sqlf.run(new Object[] {sequenceName});
			dirty = false; 			
		}
	
	}

	/**
	 * Sets the datasource.
	 * @param ds The data source to set
	 */
	public void setDataSource(DataSource ds) {
		this.ds = ds;
		dirty = true; 			
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
		this.nextMaxValueProvider.setPrefixWithZero(prefixWithZero, length);
	}

	/**
	 * Sets the sequenceName.
	 * @param sequenceName The sequenceName to set
	 */
	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
		dirty = true; 			
	}
}
