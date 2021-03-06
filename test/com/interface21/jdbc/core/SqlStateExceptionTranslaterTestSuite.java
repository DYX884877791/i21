package com.interface21.jdbc.core;

import java.sql.SQLException;

import com.interface21.jdbc.core.BadSqlGrammarException;
import com.interface21.jdbc.core.SQLStateSQLExceptionTranslater;
import com.interface21.jdbc.core.UncategorizedSQLException;

import junit.framework.*;

/**
 * 
 * @author Rod Johnson
 * @since 13-Jan-03
 */
public class SqlStateExceptionTranslaterTestSuite extends TestCase {
	
	private SQLStateSQLExceptionTranslater trans = new SQLStateSQLExceptionTranslater();


	/**
	 * Constructor for SqlStateExceptionTranslaterTestSuite.
	 * @param arg0
	 */
	public SqlStateExceptionTranslaterTestSuite(String arg0) {
		super(arg0);
	}

	// ALSO CHECK CHAIN of SQLExceptions!?
	
	// also allow chain of translaters? default if can't do specific?
	
	
	public void testBadSqlGrammar() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", "42001", 1);
		try {
			throw this.trans.translate("task", sql, sex);
		}
		catch (BadSqlGrammarException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}
	
	
	public void testInvalidSqlStateCode() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", "NO SUCH CODE", 1);
		try {
			throw this.trans.translate("task", sql, sex);
		}
		catch (UncategorizedSQLException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}
	
	public void testNullSqlStateCode() {
		String sql = "SELECT FOO FROM BAR";
		SQLException sex = new SQLException("Message", null, 1);
		try {
			throw this.trans.translate("task", sql, sex);
		}
		catch (UncategorizedSQLException ex) {
			// OK
			assertTrue("SQL is correct", sql.equals(ex.getSql()));
			assertTrue("Exception matches", sex.equals(ex.getSQLException()));
		}
	}

}
