/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.aop.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aopalliance.AspectException;
import org.aopalliance.AttributeRegistry;
import org.aopalliance.Interceptor;
import org.apache.log4j.Logger;

import com.interface21.aop.attributes.Attrib4jAttributeRegistry;
import com.interface21.beans.BeansException;
import com.interface21.beans.PropertyValues;
import com.interface21.beans.factory.BeanFactory;
import com.interface21.beans.factory.FactoryBean;
import com.interface21.beans.factory.Lifecycle;
import com.interface21.beans.factory.ListableBeanFactory;
import com.interface21.util.StringUtils;

/** 
* FactoryBean implementation for use to source
* AOP proxies from a Spring BeanFactory. 
* <br>
* Interceptors are identified by a CSV list of bean names in the current 
* bean factory. These beans should be of type Interceptor or MethodPointcut.
* The last entry in the list can be the name of any bean in the factory.
* If it's neither an Interceptor or a MethodPointcut, a new InvokerInterceptor
* is added to wrap it.
* <br>
* Global pointcuts can be added at factory level. These are expanded in
* a CSV interceptor list where a * is included in the list.
* An interceptor name list may not conclude with a *, as global interceptors
* cannot invoke targets.
* <br>
* TODO there is presently no support for ordering global interceptors,
* although an ordering mechanism, as for UrlMappings, will probably be added in future.
* @author Rod Johnson
* @version $Id: ProxyFactoryBean.java,v 1.4 2003/05/19 13:06:20 johnsonr Exp $
*/
public class ProxyFactoryBean extends DefaultProxyConfig implements FactoryBean, Lifecycle {

	/**
	 * Bean name prefix identifying a global interceptor or pointcut.
	 * We need a way to identify globals to avoid interceptor/pointcut beans
	 * referenced specifically erroneously being treated as globals.
	 */
	public static final String GLOBAL_PREFIX = "g_";

	/**
	 * This value in an interceptor list indicates to expand globals.
	 */
	public static final String GLOBALS = "*";

	private AttributeRegistry attributeRegistry;

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private boolean singleton = true;
	
	/**
	 * Owning bean factory, which cannot be changed after this
	 * object is initialized.
	 */
	private BeanFactory beanFactory;
	
	/**
	 * Singleton instance if we're using a singleton
	 */
	private Object singletonInstance;
	
	/** 
	 * Map from PointCut or interceptor to bean name or null,
	 * depending on where it was sourced from. If it's sourced
	 * from a bean name, it will need to be refreshed each time a
	 * new prototype instance is created.
	 */
	private Map sourceMap = new HashMap();
	
	/** 
	 * Names of interceptor and pointcut beans in the factory.
	 * Default is for globals expansion only.
	 */
	private String[] interceptorNames = null;


	/**
	 * Set the name of the interface we're proxying
	 */
	public void setProxyInterfaces(String[] interfaceNames) throws AspectException, ClassNotFoundException {
		Class[] interfaces = new Class[interfaceNames.length];
		for (int i = 0; i < interfaceNames.length; i++) {
			interfaces[i] = Class.forName(interfaceNames[i]);
			// Check it's an interface
			if (!interfaces[i].isInterface())
				throw new AspectException("Can proxy only interfaces: " + interfaces[i] + " is a class");
		}
		setInterfaces(interfaces);
	}


	/**
	 * Set the CSV list of Interceptor/MethodPointcut bean names. This must
	 * always be set to use this factory bean in a bean factory.
	 * @param csv
	 */
	public void setInterceptorNames(String csv) {
		this.interceptorNames = StringUtils.commaDelimitedListToStringArray(csv);
	}
	
	/**
	 * @see com.interface21.beans.factory.Lifecycle#setBeanFactory(com.interface21.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {	
		
		this.beanFactory = beanFactory;
			
		// TODO
		// configure attribute registry from bean factory
		// if well-known bean...
		this.attributeRegistry = new Attrib4jAttributeRegistry();
		
		logger.info("Set BeanFactory. Will configure interceptor beans...");
		
		createInterceptorChain();
		
		// Eagerly create singleton proxy instance if necessary
		if (isSingleton()) {
			this.singletonInstance = createInstance();
		}
	}	// setBeanFactory


	/**
	 * Create the interceptor chain. The interceptors that
	 * are sourced from a BeanFactory will be refreshed each time
	 * a new prototype instance is added. Interceptors
	 * added programmatically through the factory API are
	 * unaffected by such changes.
	 */
	private void createInterceptorChain() throws AopConfigException, BeansException {
		
		if (this.interceptorNames == null || this.interceptorNames.length == 0)
			throw new AopConfigException("Interceptor names are required");
			
		// Globals can't be last
		if (this.interceptorNames[this.interceptorNames.length - 1].equals(GLOBALS))
			throw new AopConfigException("Target required after globals");
			
		// Materialize interceptor chain from bean names
		for (int i = 0; i < this.interceptorNames.length; i++) {
			logger.debug("Configuring interceptor '" + this.interceptorNames[i] + "'");
			
			if (this.interceptorNames[i].equals(GLOBALS)) {
				if (!(this.beanFactory instanceof ListableBeanFactory)) {
					throw new AopConfigException("Can only use global pointcuts or interceptors with a ListableBeanFactory");
				}
				else {
					addGlobalInterceptorsAndPointcuts((ListableBeanFactory) beanFactory);
				}
			}
			else {
				// Add a named interceptor
				addPointcutOrInterceptor(this.beanFactory.getBean(this.interceptorNames[i]), this.interceptorNames[i]);
			}
		}
	}	// setBeanFactory
	
	
	/**
	 * Refresh named beans from the interceptor chain.
	 * We need to do this every time a new prototype instance is
	 * returned, to return distinct instances of prototype interfaces
	 * and pointcuts.
	 */
	private void refreshInterceptorChain() {
		List pointcuts = getMethodPointcuts();
		for (Iterator iter = pointcuts.iterator(); iter.hasNext();) {
			MethodPointcut pc = (MethodPointcut) iter.next();
			String beanName = (String) this.sourceMap.get(pc);
			if (beanName != null) {
				logger.info("Refreshing bean named '" + beanName + "'");
			
				Object bean = this.beanFactory.getBean(beanName);
				MethodPointcut pc2 = null;
				// Bean may be a MethodPointcut or a target to wrap
				if (bean instanceof MethodPointcut) {
					pc2 = (MethodPointcut) bean;
				}
				else {
					// The special case when the object was a target
					// object, not an invoker or pointcut.
					// We need to create a fresh invoker interceptor wrapping
					// the new target.
					InvokerInterceptor ii = new InvokerInterceptor(bean);
					pc2 = new AlwaysInvoked(ii);
				}
				
				// What about aspect interfaces!? we're only updating
				replaceMethodPointcut(pc, pc2);
			}
		}
	}


	/**
	 * Add all global interceptors and pointcuts
	 */
	private void addGlobalInterceptorsAndPointcuts(ListableBeanFactory beanFactory) {
		String[] globalPointcutNames = beanFactory.getBeanDefinitionNames(MethodPointcut.class);
		String[] globalInterceptorNames = beanFactory.getBeanDefinitionNames(Interceptor.class);
		List l = new ArrayList(globalPointcutNames.length + globalInterceptorNames.length);
		l.addAll(Arrays.asList(globalPointcutNames));
		l.addAll(Arrays.asList(globalInterceptorNames));
		// TODO sort using Ordered
		
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			if (name.startsWith(GLOBAL_PREFIX)) {
				addPointcutOrInterceptor(beanFactory.getBean(name), name);
			}
		}
	}


	/**
	 * Add the given interceptor, pointcut or object to the interceptor list.
	 * Because of these three possibilities, we can't type the signature
	 * more strongly.
	 * @param next interceptor, pointcut or target object. 
	 * @param name bean name from which we obtained this object in our owning
	 * bean factory.
	 */
	private void addPointcutOrInterceptor(Object next, String name) {
		if (next instanceof MethodPointcut) {
			addMethodPointcut((MethodPointcut) next);
		}
		else if (next instanceof Interceptor) {
			addInterceptor((Interceptor) next);
		}
		else {
			// It's not a pointcut or interceptor.
			// It's a bean that needs an invoker around it.
			// TODO how do these get refreshed
			InvokerInterceptor ii = new InvokerInterceptor(next);
			addInterceptor(ii);
			//throw new AopConfigException("Illegal type: bean '" + name + "' must be of type MethodPointcut or Interceptor");
		}
		
		// Record the ultimate object as descended from the given bean name.
		// This tells us how to refresh the interceptor list, which we'll need to
		// do if we have to create a new prototype instance. Otherwise the new
		// prototype instance wouldn't be truly independent, because it might reference
		// the original instances of prototype interceptors.
		this.sourceMap.put(next, name);
	} 	// addPointcutOrInterceptor

	/**
	 * Return a proxy. Invoked when clients obtain beans
	 * from this factory bean.
	 * @see com.interface21.beans.factory.FactoryBean#getObject()
	 */
	public Object getObject() throws BeansException {
		if (this.singleton) {
			// Return singleton
			return this.singletonInstance;
		}
		else {
			// Create new interface
			return createInstance();
		}
	}
	
	
	/**
	 * Create an instance of the AOP proxy to be returned by this factory. 
	 * The instance will be cached for a singleton, and create on each call to 
	 * getObject() for a proxy.
	 * @return Object a fresh AOP proxy reflecting the current
	 * state of this factory
	 */
	private Object createInstance() {
		refreshInterceptorChain();
		AopProxy proxy = new AopProxy(this);
		return AopProxy.getProxy(getClass().getClassLoader(), proxy);
	}

	/**
	 * This factory doesn't support pass through properties.
	 * @see com.interface21.beans.factory.FactoryBean#getPropertyValues()
	 */
	public PropertyValues getPropertyValues() {
		return null;
	}

	/**
	 * @see com.interface21.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return this.singleton;
	}
	
	/**
	 * Set the value of the singleton property. Governs whether this factory
	 * should always return the same proxy instance (which implies the same target)
	 * or whether it should return a new prototype instance, which implies that
	 * the target and interceptors may be new instances also, if they are obtained
	 * from prototype bean definitions.
	 * This allows for fine control of independence/uniqueness in the object graph.
	 * @param singleton
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}
	
}	// ProxyFactoryBean