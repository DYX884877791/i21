/**
 * Generic framework code included with 
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/1861007841/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002). 
 * This code is free to use and modify. 
 * Please contact <a href="mailto:rod.johnson@interface21.com">rod.johnson@interface21.com</a>
 * for commercial support.
 */

package com.interface21.jdbc.core;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.interface21.beans.factory.BeanDefinitionStoreException;
import com.interface21.beans.factory.ListableBeanFactory;
import com.interface21.beans.factory.xml.XmlBeanFactory;
import com.interface21.jdbc.datasource.DataSourceUtils;

/**
 * Factory for creating SQLExceptionTranslator based on the
 * DatabaseProductName taken from the DatabaseMetaData.
 * Returns a SQLExceptionTranslator populated with vendor 
 * codes defined in a configuration file named "sql-error-codes.xml".
 * @author Thomas Risberg
   @version $Id: SQLExceptionTranslaterFactory.java,v 1.6 2003/06/06 16:13:22 jhoeller Exp $
 */
public class SQLExceptionTranslaterFactory {
	
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Name of SQL error code file, loading on the classpath. Will look
	 * in current directory (no leading /).
	 */
	public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "/sql-error-codes.xml";
	public static final String SQL_ERROR_CODE_DEFAULT_PATH = "sql-error-codes.xml";
	
	/**
	* Keep track of this instance so we can return it to classes that request it.
	*/
	private static final SQLExceptionTranslaterFactory instance;
	
	/**
	* Create a HashMap to hold error codes for all databases defined in the
	* config file.
	*/
	private Map rdbmsErrorCodes;
	
	/**
	 * Not public to enforce Singleton design pattern
	 */
	SQLExceptionTranslaterFactory() {
		try {
			java.io.InputStream is = SQLExceptionTranslaterFactory.class.getResourceAsStream(SQL_ERROR_CODE_OVERRIDE_PATH);
			if (is == null) {
				is = SQLExceptionTranslaterFactory.class.getResourceAsStream(SQL_ERROR_CODE_DEFAULT_PATH);
				if (is == null) 
					throw new BeanDefinitionStoreException("Unable to locate file [" + SQL_ERROR_CODE_DEFAULT_PATH +"]",null);
			}
			ListableBeanFactory bf = new XmlBeanFactory(is);
			String[] rdbmsNames = bf.getBeanDefinitionNames(com.interface21.jdbc.core.SQLErrorCodes.class);			
			rdbmsErrorCodes = new HashMap(rdbmsNames.length);
			for (int i = 0; i < rdbmsNames.length; i++) {
				SQLErrorCodes ec = (SQLErrorCodes) bf.getBean(rdbmsNames[i]);
				if (ec.getBadSqlGrammarCodes() == null)
					ec.setBadSqlGrammarCodes(new String[0]);
				else
					java.util.Arrays.sort(ec.getBadSqlGrammarCodes());
				if (ec.getDataIntegrityViolationCodes() == null)
					ec.setDataIntegrityViolationCodes(new String[0]);
				else
					java.util.Arrays.sort(ec.getDataIntegrityViolationCodes());
				rdbmsErrorCodes.put(rdbmsNames[i], ec);
			}
		}
		catch (BeanDefinitionStoreException be) {
			logger.warn("Error loading error codes from config file.  Message = " + be.getMessage());
			rdbmsErrorCodes = new HashMap(0);
		}
	}
		
	static {
		instance = new SQLExceptionTranslaterFactory();
	}

	/**
	 * Factory method
	 */
	public static SQLExceptionTranslaterFactory getInstance() {
		return instance;
	}

	/**
	 * 
	 */
	public SQLExceptionTranslater getDefaultTranslater(DataSource ds) {
		String dbName = null;
		Connection con = null;
		DatabaseMetaData dbmd = null;
		try {
			con = DataSourceUtils.getConnection(ds);
			if (con != null) 
				dbmd = con.getMetaData();
			if (dbmd != null)
				dbName = dbmd.getDatabaseProductName();
			if (dbName != null && dbName.startsWith("DB2/"))
				dbName = "DB2";
		}
		catch (SQLException se) {
			// this is bad - we probably lost the connection
			return new SQLStateSQLExceptionTranslater();
		}
		finally {
			DataSourceUtils.closeConnectionIfNecessary(con, ds);
		}
		SQLErrorCodes sec = null;
		if (dbName != null)
			sec = (SQLErrorCodes) rdbmsErrorCodes.get(dbName);
		
		// could not find the database among the defined ones
		if (sec == null) 
			return new SQLStateSQLExceptionTranslater();
			
		SQLErrorCodeSQLExceptionTranslater set = new SQLErrorCodeSQLExceptionTranslater(sec);
		return set;
	}

	/**
	 * 
	 */
	public SQLErrorCodes getErrorCodes(String dbName) {
		
		SQLErrorCodes sec = (SQLErrorCodes) rdbmsErrorCodes.get(dbName);
		
		// could not find the database among the defined ones
		if (sec == null) 
			sec = new SQLErrorCodes();

		return sec;
	}
}