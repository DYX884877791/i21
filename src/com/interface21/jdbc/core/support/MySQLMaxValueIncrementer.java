package com.interface21.jdbc.core.support;

import java.sql.Types;

import javax.sql.DataSource;

import com.interface21.beans.factory.InitializingBean;
import com.interface21.core.InternalErrorException;
import com.interface21.jdbc.object.SqlFunction;
import com.interface21.jdbc.object.SqlUpdate;
import com.interface21.jdbc.util.JdbcUtils;

/**
 * Class to increment maximum value of a given MySQL table with an auto-increment column
 * <br>The sequence is kept in a table; it is up to the user whether there is one sequence table
 * per table to be used, or whether multiple sequence values are kept in one table.
 * <p>
 * Thus you could have
 * <code>
 * create table sequences (
 *   seq1 int unsigned not null,
 *   seq2 int unsigned not null,
 * unique (seq1),
 * unique(seq2));
 * insert into sequences values(0, 0);
 * </code>
 * The table name in this case is "sequences", the column names for the auto-increment
 * fields "seq1" or "seq2".
 * Alternatively you could have
 * <code>
 * create table sequence1 (
 *    seq1 int unsigned not null primary key
 * );
 * insert into sequence1 values(0);
 * <br>
 * create table sequence2 (
 *   seq2 int unsigned not null primary key
 * );
 * insert into sequence2 values(0);
 * </code>
 * The table names in this case are "sequence1" and "sequence2", the column names
 * for the auto-increment fields respectively "seq1" or "seq2".
 * </p>
 * <p>If incrementBy is set, the intermediate values are served without querying the
 * database. If the server is stopped or crashes, the unused values will never be
 * served. The maximum hole size in numbering is consequently the value of incrementBy.
 * </p>
 * @author <a href="mailto:isabelle@meta-logix.com">Isabelle Muszynski</a>
 * @author <a href="mailto:jp.pawlak@tiscali.fr">Jean-Pierre Pawlak</a>
 * @version $Id: MySQLMaxValueIncrementer.java,v 1.8 2003/05/10 16:09:10 pawlakjp Exp $
 */

public class MySQLMaxValueIncrementer
	extends AbstractDataFieldMaxValueIncrementer
	implements InitializingBean {

	//-----------------------------------------------------------------
	// Instance data
	//-----------------------------------------------------------------
	private DataSource ds;

	/** The name of the table containing the sequence */
	private String tableName;

	/** The name of the column to use for this sequence */
	private String columnName;

	/** The number of keys buffered in a bunch. */
	private int incrementBy = 1;

	/** Flag if dirty definition */
	private boolean dirty = true;
	
	private NextMaxValueProvider nextMaxValueProvider;
    
	/**
	 * Default constructor 
	 **/
	public MySQLMaxValueIncrementer() {
	this.nextMaxValueProvider = new NextMaxValueProvider();
	}

	/**
	 * Constructor 
	 * @param ds the datasource to use
	 * @param tableName the name of the sequence table to use
	 * @param columnName the name of the column in the sequence table to use
	 **/
	public MySQLMaxValueIncrementer(DataSource ds, String tableName, String columnName) {
	this.ds = ds;
	this.tableName = tableName;
	this.columnName = columnName;
	this.nextMaxValueProvider = new NextMaxValueProvider();
	}

	/**
	 * Constructor 
	 * @param ds the datasource to use
	 * @param tableName the name of the sequence table to use
	 * @param columnName the name of the column in the sequence table to use
	 * @param incrementBy the number of buffered keys
	 **/
	public MySQLMaxValueIncrementer(DataSource ds, String tableName, String columnName, int incrementBy) {
	this.ds = ds;
	this.tableName = tableName;
	this.columnName = columnName;
	this.incrementBy = incrementBy;
	this.nextMaxValueProvider = new NextMaxValueProvider();
	}

	/**
	 * Constructor 
	 * @param ds the datasource to use
	 * @param tableName the name of the sequence table to use
	 * @param columnName the name of the column in the sequence table to use
	 * @param prefixWithZero in case of a String return value, should the string be prefixed with zeroes
	 * @param padding the length to which the string return value should be padded with zeroes
	 **/
	public MySQLMaxValueIncrementer(DataSource ds, String tableName, String columnName, boolean prefixWithZero, int padding) {
	this.ds = ds;
	this.tableName = tableName;
	this.columnName = columnName;
	this.nextMaxValueProvider = new NextMaxValueProvider();
	this.nextMaxValueProvider.setPrefixWithZero(prefixWithZero, padding);
	}

	/**
	 * Constructor 
	 * @param ds the datasource to use
	 * @param tableName the name of the sequence table to use
	 * @param columnName the name of the column in the sequence table to use
	 * @param prefixWithZero in case of a String return value, should the string be prefixed with zeroes
	 * @param padding the length to which the string return value should be padded with zeroes
	 * @param incrementBy the number of buffered keys
	 **/
	public MySQLMaxValueIncrementer(DataSource ds, String tableName, String columnName, boolean prefixWithZero, int padding, int incrementBy) {
	this.ds = ds;
	this.tableName = tableName;
	this.columnName = columnName;
	this.incrementBy = incrementBy;
	this.nextMaxValueProvider = new NextMaxValueProvider();
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
		private String prepareSql;
		
		/** The next id to serve */
		private long nextId = 0;

		/** The max id to serve */
		private long maxId = 0;
	
		protected long getNextKey(int type) {
			if (dirty) { initPrepare(); }
			if(maxId == nextId) {
				SqlUpdate sqlup = new SqlUpdate(ds, prepareSql);
				sqlup.compile();
				sqlup.update();
				SqlFunction sqlf = new SqlFunction(ds, "select last_insert_id()",type);
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
			StringBuffer buf = new StringBuffer();
			buf.append("update ");
			buf.append(tableName);
			buf.append(" set ");
			buf.append(columnName);
			buf.append(" = last_insert_id(");
			buf.append(columnName);
			buf.append(" + ");
			buf.append(incrementBy);
			buf.append(")");
			prepareSql = buf.toString();
			dirty = false; 			
		}
	}

	/**
	 * Sets the data source.
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
		if (ds == null || tableName == null || columnName == null)
			throw new Exception("dsName, sequenceName properties must be set on " + getClass().getName());
	}

	/**
	 * Sets the prefixWithZero.
	 * @param prefixWithZero The prefixWithZero to set
	 */
	public void setPrefixWithZero(boolean prefixWithZero, int length) {
		this.nextMaxValueProvider.setPrefixWithZero(prefixWithZero, length);
	}

	/**
	 * Sets the tableName.
	 * @param tableName The tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
		dirty = true; 			
	}

	/**
	 * Sets the columnName.
	 * @param columnName The columnName to set
	 */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
		dirty = true; 			
	}

	/**
	 * Sets the incrementBy.
	 * @param incrementBy The number of buffered keys
	 */
	public void setIncrementBy(int incrementBy) {
		this.incrementBy = incrementBy;
		dirty = true; 			
	}

}
 
