
package com.interface21.aop.framework;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods used by the AOP framework.
 * @author Rod Johnson
 * @version $Id: AopUtils.java,v 1.4 2003/07/23 18:44:23 johnsonr Exp $
 */
public class AopUtils {

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
