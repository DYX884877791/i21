/*
 * SpringMockDataSource.java
 *
 * Copyright (C) 2002 by Interprise Software.  All rights reserved.
 */
package com.interface21.jdbc.datasource;

import java.sql.Connection;

import com.interface21.jdbc.datasource.SmartDataSource;
import com.mockobjects.sql.MockDataSource;

/**
 * @task enter type comments
 * 
 * @author <a href="mailto:tcook@interprisesoftware.com">Trevor D. Cook</a>
 * @version $Id: SpringMockDataSource.java,v 1.2 2003/05/06 12:30:11 jhoeller Exp $
 */
public class SpringMockDataSource
	extends MockDataSource
	implements SmartDataSource {

	/**
	 * Constructor for SpringMockDataSource.
	 */
	public SpringMockDataSource() {
		super();
	}

	/**
	 * @see com.interface21.jdbc.datasource.SmartDataSource#shouldClose(java.sql.Connection)
	 */
	public boolean shouldClose(Connection conn) {
		return false;
	}

}
