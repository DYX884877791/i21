
package com.interface21.aop.framework;

import java.util.HashSet;
import java.util.Set;

import org.aopalliance.Invocation;

/**
 * Utility methods used by the AOP framework.
 * @author Rod Johnson
 * @version $Id: AopUtils.java,v 1.3 2003/05/15 17:16:55 johnsonr Exp $
 */
public class AopUtils {
	
	/**
	 * Return arguments
	 */
	public static Object[] getArguments(Invocation invocation) {
		// TODO make portable
		return ((MethodInvocationImpl) invocation).getArguments();
	}

	/**
	 * Get all implemented interfaces, even those implemented by superclasses.
	 * @param clazz
	 * @return Set
	 */
	public static Set findAllImplementedInterfaces(Class clazz) {
		Set s = new HashSet();
		Class[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			s.add(interfaces[i]);
		}
		Class superclass = clazz.getSuperclass();
		if (superclass != null) {
			s.addAll(findAllImplementedInterfaces(superclass));
		}
		return s;
	}

}
