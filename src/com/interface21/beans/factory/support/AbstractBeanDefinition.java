/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.beans.factory.support;

import org.apache.log4j.Logger;

import com.interface21.beans.PropertyValues;

/**
 * Internal BeanFactory implementation class. Use a
 * FactoryBean to customize behaviour when returning application
 * beans.
 * <br>
 * A BeanDefinition describes a bean instance,
 * which has property values and further information supplied
 * by concrete classes or subinterfaces.
 * <br/>Once configuration is complete, a BeanFactory will be able
 * to return direct references to objects defined by
 * BeanDefinitions.
 * <br>This abstract base class defines the BeanFactory type.
 * @author Rod Johnson
 * @version $Revision: 1.2 $
 */
public abstract class AbstractBeanDefinition {

	/**
	* Create a logging category that is available
	* to subclasses. 
	*/
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	/** Is this a singleton bean? */
	private boolean singleton;
	
	/** Property map */
	private PropertyValues pvs;

	
	/** 
	 * Creates new BeanDefinition
	 * @param map properties of the bean
	 */
	protected AbstractBeanDefinition(PropertyValues pvs, boolean singleton) {
		this.pvs = pvs;
		this.singleton = singleton;
	}
	
	protected AbstractBeanDefinition() {
		this.singleton = true;
	}
	
	/**
	 * Is this a <b>Singleton</b>, with a single, shared
	 * instance returned on all calls,
	 * or should the BeanFactory apply the <b>Prototype</b> design pattern,
	 * with each caller requesting an instance getting an independent
	 * instance? How this is defined will depend on the BeanFactory.
	 * "Singletons" are the commoner type.
	 * @return whether this is a Singleton
	 */
	public final boolean isSingleton() {
		return singleton;
	}
	
	public void setPropertyValues(PropertyValues pvs) {
		this.pvs = pvs;
	}

	/**
	 * Return the PropertyValues to be applied to a new instance
	 * of this bean.
	 * @eturn the PropertyValues to be applied to a new instance
	 * of this bean
	 */
	public PropertyValues getPropertyValues() {
		return pvs;
	}

	
	/**
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object other) {
		if (!(other instanceof AbstractBeanDefinition))
			return false;
		AbstractBeanDefinition obd = (AbstractBeanDefinition) other;
		return this.singleton = obd.singleton &&
			this.pvs.changesSince(obd.pvs).getPropertyValues().length == 0;
	}

} 	// class BeanDefinition