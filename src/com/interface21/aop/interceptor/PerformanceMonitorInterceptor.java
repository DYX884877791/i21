package com.interface21.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.interface21.util.StopWatch;

/**
 * Trivial performance monitor interceptor.
 * Could make this much more sophisticated, storing information.
 * Presently logs information using Log4j. This interceptor
 * has no effect on the intercepted method call.
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 * @version $Id: PerformanceMonitorInterceptor.java,v 1.2 2003/07/23 18:44:24 johnsonr Exp $
 */
public class PerformanceMonitorInterceptor implements MethodInterceptor {

	protected final Log logger = LogFactory.getLog(getClass());

	public Object invoke(MethodInvocation invocation) throws Throwable {
		logger.info("Begin...");

		StopWatch sw = new StopWatch();
		sw.start(invocation.getMethod().getName());
		Object rval = invocation.proceed();
		sw.stop();

		logger.info(sw.prettyPrint());

		logger.info("End.");

		return rval;
	}

}
