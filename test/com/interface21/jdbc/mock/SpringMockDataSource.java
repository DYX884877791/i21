/*
 * SpringMockDataSource.java
 *
 * Copyright (C) 2002 by Interprise Software.  All rights reserved.
 */
package com.interface21.jdbc.mock;

import java.sql.Connection;

import com.interface21.jdbc.core.SmartDataSource;
import com.mockobjects.sql.MockDataSource;

/**
 * @task enter type comments
 * 
 * @author <a href="mailto:tcook@interprisesoftware.com">Trevor D. Cook</a>
 * @version $Id: SpringMockDataSource.java,v 1.1 2003/04/14 04:34:37 beanie42 Exp $
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
	 * @see com.interface21.jdbc.core.SmartDataSource#shouldClose(java.sql.Connection)
	 */
	public boolean shouldClose(Connection conn) {
		return false;
	}

}
