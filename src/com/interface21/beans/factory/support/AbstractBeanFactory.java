/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package com.interface21.beans.factory.support;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.interface21.beans.BeanWrapper;
import com.interface21.beans.BeanWrapperImpl;
import com.interface21.beans.BeansException;
import com.interface21.beans.FatalBeanException;
import com.interface21.beans.MutablePropertyValues;
import com.interface21.beans.PropertyValue;
import com.interface21.beans.PropertyValues;
import com.interface21.beans.factory.BeanDefinitionStoreException;
import com.interface21.beans.factory.BeanFactory;
import com.interface21.beans.factory.BeanIsNotAFactoryException;
import com.interface21.beans.factory.BeanNotOfRequiredTypeException;
import com.interface21.beans.factory.FactoryBean;
import com.interface21.beans.factory.HierarchicalBeanFactory;
import com.interface21.beans.factory.InitializingBean;
import com.interface21.beans.factory.BeanFactoryAware;
import com.interface21.beans.factory.NoSuchBeanDefinitionException;

/**
 * Abstract superclass that makes implementing a BeanFactory very easy.
 * This class uses the <b>Template Method</b> design pattern.
 * Subclasses must implement only the
 * <code>
 * getBeanDefinition(name)
 * </code>
 * method.
 * This class handles resolution of runtime bean references,
 * FactoryBean dereferencing, and management of collection properties.
 * It also allows for management of a bean factory hierarchy, 
 * implementing the HierarchicalBeanFactory method.
 * @author Rod Johnson
 * @since 15 April 2001
 * @version $Id: AbstractBeanFactory.java,v 1.27 2003/07/19 19:09:55 johnsonr Exp $
 */
public abstract class AbstractBeanFactory implements HierarchicalBeanFactory {

	/**
	 * Used to dereference a FactoryBean and distinguish it from
	 * beans <i>created</i> by the factory. For example,
	 * if the bean named <code>myEjb</code> is a factory, getting
	 * <code>&myEjb</code> will return the factory, not the instance
	 * returned by the factory.
	 */
	public static final String FACTORY_BEAN_PREFIX = "&";


	//---------------------------------------------------------------------
	// Instance data
	//---------------------------------------------------------------------

	/** parent bean factory, for bean inheritance support */
	private BeanFactory parentBeanFactory;

	/** Cache of shared instances. bean name --> bean instanced */
	private Map sharedInstanceCache = new HashMap();

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** name of default parent bean */
	protected String defaultParentBean;
	
	/** Map from alias to canonical bean name */
	private Map aliasMap = new HashMap();


	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------

	/**
	 * Creates a new AbstractBeanFactory
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Creates a new AbstractBeanFactory, with the given parent.
	 * @param parentBeanFactory  the parent bean factory, or null if none
	 * @see #getBean
	 */
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	/**
	 * @see com.interface21.beans.factory.HierarchicalBeanFactory#getParentBeanFactory()
	 */
	public BeanFactory getParentBeanFactory() {
		return parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------
	/**
	 * Return the bean name, stripping out the factory deference prefix if necessary,
	 * and resolving aliases to canonical names.
	 */
	private String transformedBeanName(String name) {
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			name = name.substring(FACTORY_BEAN_PREFIX.length());
		}
		// Handle aliasing
		String canonicalName = (String) this.aliasMap.get(name);
		return canonicalName != null ? canonicalName : name;
	}

	/**
	 * Return whether this name is a factory dereference (beginning
	 * with the factory dereference prefix)
	 */
	private boolean isFactoryDereference(String name) {
		return name.startsWith(FACTORY_BEAN_PREFIX);
	}

	/**
	 * Get a singleton instance of this bean name. Note that this method shouldn't
	 * be called too often: Callers should keep hold of instances. Hence, the whole
	 * method is synchronized here.
	 * TODO: There probably isn't any need for this to be synchronized,
	 * at least not if we pre-instantiate singletons.
	 * @param pname name that may include factory dereference prefix
	 * @param newlyCreatedBeans cache with newly created beans (name, instance)
	 * if triggered by the creation of another bean, or null else
	 * (necessary to resolve circular references)
	 */
	private final synchronized Object getSharedInstance(String pname, Map newlyCreatedBeans) throws BeansException {
		// Get rid of the dereference prefix if there is one
		String name = transformedBeanName(pname);

		Object beanInstance = this.sharedInstanceCache.get(name);
		if (beanInstance == null) {
			logger.info("Cached shared instance of Singleton bean '" + name + "'");
			if (newlyCreatedBeans == null) {
				newlyCreatedBeans = new HashMap();
			}
			beanInstance = createBean(name, newlyCreatedBeans);
			this.sharedInstanceCache.put(name, beanInstance);
		}
		else {
			if (logger.isDebugEnabled())
				logger.debug("Returning cached instance of Singleton bean '" + name + "'");
		}

		// Don't let calling code try to dereference the
		// bean factory if the bean isn't a factory
		if (isFactoryDereference(pname) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(name, beanInstance);
		}

		// Now we have the beanInstance, which may be a normal bean
		// or a FactoryBean. If it's a FactoryBean, we use it to
		// create a bean instance, unless the caller actually wants
		// a reference to the factory.
		if (beanInstance instanceof FactoryBean) {
			if (!isFactoryDereference(pname)) {
				// Configure and return new bean instance from factory
				FactoryBean factory = (FactoryBean) beanInstance;
				logger.debug("Bean with name '" + name + "' is a factory bean");
				beanInstance = factory.getObject();

				// Set pass-through properties
				if (factory.getPropertyValues() != null) {
					logger.debug("Applying pass-through properties to bean with name '" + name + "'");
					new BeanWrapperImpl(beanInstance).setPropertyValues(factory.getPropertyValues());
				}
				// Initialization is really up to factory
				//invokeInitializerIfNecessary(beanInstance);
			}
			else {
				// The user wants the factory itself
				logger.debug("Calling code asked for BeanFactory instance for name '" + name + "'");
			}
		}	// if we're dealing with a factory bean

		return beanInstance;
	}

	/**
	 * Return the bean with the given name,
	 * checking the parent bean factory if not found.
	 * @param name name of the bean to retrieve
	 */
	public final Object getBean(String name) {
		return getBeanInternal(name, null);
	}

	/**
	 * Return the bean with the given name,
	 * checking the parent bean factory if not found.
	 * @param name name of the bean to retrieve
	 * @param newlyCreatedBeans cache with newly created beans (name, instance)
	 * if triggered by the creation of another bean, or null else
	 * (necessary to resolve circular references)
	 */
	private Object getBeanInternal(String name, Map newlyCreatedBeans) {
		if (name == null)
			throw new NoSuchBeanDefinitionException(null, "Cannot get bean with null name");
		if (newlyCreatedBeans != null && newlyCreatedBeans.containsKey(name)) {
			return newlyCreatedBeans.get(name);
		}
		try {
			AbstractBeanDefinition bd = getBeanDefinition(transformedBeanName(name));
			return bd.isSingleton() ? getSharedInstance(name, newlyCreatedBeans) : createBean(name, newlyCreatedBeans);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// not found -> check parent
			if (this.parentBeanFactory != null)
				return this.parentBeanFactory.getBean(name);
			throw ex;
		}
	}

	/**
	 * Return a shared instance of the given bean. Analogous to getBeanInstance(name, requiredType).
	 * @param name name of the instance to return
	 * @param requiredType type the bean must match
	 * @return a shared instance of the given bean
	 * @throws BeanNotOfRequiredTypeException if the bean  not of the required type
	 * @throws NoSuchBeanDefinitionException if there's no such bean definition
	 */
	public final Object getBean(String name, Class requiredType) throws BeansException {
		Object bean = getBean(name);
		Class clazz = bean.getClass();
		if (!requiredType.isAssignableFrom(clazz)) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean);
		}
		return bean;
	}

	/**
	 * @see BeanFactory#isSingleton(String)
	 */
	public boolean isSingleton(String pname) throws NoSuchBeanDefinitionException {
		String name = transformedBeanName(pname);
		try {
			return getBeanDefinition(name).isSingleton();
		}
		catch (NoSuchBeanDefinitionException ex) {
			// not found -> check parent
			if (this.parentBeanFactory != null)
				return this.parentBeanFactory.isSingleton(name);
			throw ex;
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------
	/**
	 * All the other methods in this class invoke this method
	 * although beans may be cached after being instantiated by this method.
	 * All bean instantiation within this class is performed by this method.
	 * Return a BeanWrapper object for a new instance of this bean.
	 * First look up BeanDefinition for the given bean name.
	 * Uses recursion to support instance "inheritance".
	 * @param name name of the bean. Must be unique in the BeanFactory
	 * @param newlyCreatedBeans cache with newly created beans (name, instance)
	 * if triggered by the creation of another bean, or null else
	 * (necessary to resolve circular references)
	 * @return a new instance of this bean
	 */
	private Object createBean(String name, Map newlyCreatedBeans) throws BeansException {
		if (newlyCreatedBeans == null) {
			newlyCreatedBeans = new HashMap();
		}
		logger.debug("createBean (" + name + ")");
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition(name);
		logger.debug("Merged definition is: " + mergedBeanDefinition);
		BeanWrapper instanceWrapper = new BeanWrapperImpl(mergedBeanDefinition.getBeanClass());
		
		// cache new instance to be able resolve circular references
		newlyCreatedBeans.put(name, instanceWrapper.getWrappedInstance());
		PropertyValues pvs = mergedBeanDefinition.getPropertyValues();
		applyPropertyValues(instanceWrapper, pvs, name, newlyCreatedBeans);
		Object bean = instanceWrapper.getWrappedInstance();
		callLifecycleMethodsIfNecessary(bean, name, mergedBeanDefinition, instanceWrapper);
		return bean;
	}

	/**
	 * Apply the given property values, resolving any runtime references
	 * to other beans in this bean factory.
	 * Must use deep copy, so we don't permanently modify this property
	 * @param bw BeanWrapper wrapping the target object
	 * @param pvs new property values
	 * @param name bean name passed for better exception information
	 * @param newlyCreatedBeans cache with newly created beans (name, instance)
	 * if triggered by the creation of another bean, or null else
	 * (necessary to resolve circular references)
	 */
	private void applyPropertyValues(BeanWrapper bw, PropertyValues pvs, String name, Map newlyCreatedBeans) throws BeansException {
		if (pvs == null)
			return;

		MutablePropertyValues deepCopy = new MutablePropertyValues(pvs);
		PropertyValue[] pvals = deepCopy.getPropertyValues();
		
		for (int i = 0; i < pvals.length; i++) {
			PropertyValue pv = new PropertyValue(pvals[i].getName(), resolveValueIfNecessary(bw, newlyCreatedBeans, pvals[i]));
			// Update mutable copy
			deepCopy.setPropertyValueAt(pv, i);
		}
		
		// Set our (possibly massaged) deepCopy
		try {
			bw.setPropertyValues(deepCopy);
		}
		catch (FatalBeanException ex) {
			// Improve the message by showing the context
			throw new FatalBeanException("Error setting property on bean [" + name + "]", ex);
		}
	}

	/**
	 * Given a PropertyValue, return a value, resolving any references to other
	 * beans in the factory if necessary. The value could be:
	 * <li>An ordinary object or null, in which case it's left alone
	 * <li>A RuntimeBeanReference, which must be resolved
	 * <li>A ManagedList. This is a special collection that may contain
	 * RuntimeBeanReferences that will need to be resolved.
	 * <li>A ManagedMap. In this case the value may be a reference that
	 * must be resolved.
	 * If the value is a simple object, but the property takes a Collection type,
	 * the value must be placed in a list.
	 */
	private Object resolveValueIfNecessary(BeanWrapper bw, Map newlyCreatedBeans, PropertyValue pv)
		throws BeansException {
		Object val;
		
		// Now we must check each PropertyValue to see whether it
		 // requires a runtime reference to another bean to be resolved.
		 // If it does, we'll attempt to instantiate the bean and set the reference.
		if (pv.getValue() != null && (pv.getValue() instanceof RuntimeBeanReference)) {
			RuntimeBeanReference ref = (RuntimeBeanReference) pv.getValue();
			val = resolveReference(pv.getName(), ref, newlyCreatedBeans);
		}	
		else if (pv.getValue() != null && (pv.getValue() instanceof ManagedList)) {
			// Convert from managed list. This is a special container that
			// may contain runtime bean references.
			// May need to resolve references
			val = resolveManagedList(pv.getName(), (ManagedList) pv.getValue(), newlyCreatedBeans);
		}
		else if (pv.getValue() != null && (pv.getValue() instanceof ManagedMap)) {
			// Convert from managed map. This is a special container that
			// may contain runtime bean references as values.
			// May need to resolve references
			ManagedMap mm = (ManagedMap) pv.getValue();
			val = resolveManagedMap(pv.getName(), mm, newlyCreatedBeans);	
		}
		else {
			// It's an ordinary property. Just copy it.
			val = pv.getValue();
		}
		
		 // If it's an array type, we may have to massage type
		 // of collection. We'll start with ManagedList.
		 // We may also have to convert array elements from Strings
		 // TODO consider refactoring into BeanWrapperImpl?
		 if (val != null && val instanceof ManagedList && bw.getPropertyDescriptor(pv.getName()).getPropertyType().isArray()) {
			 // It's an array
			 Class arrayClass = bw.getPropertyDescriptor(pv.getName()).getPropertyType();
			 Class componentType = arrayClass.getComponentType();
			 List l = (List) val;
		
			val = managedListToArray(bw, pv, val, componentType, l);
		 }
		
		return val;
	}
	
	/**
	 * Resolve a reference to another bean in the factory
	 * @param name included for diagnostics
	 */
	private Object resolveReference(String name, RuntimeBeanReference ref, Map newlyCreatedBeans) {
		try {
			// Try to resolve bean reference
			logger.debug("Resolving reference from bean [" + name + "] to bean [" + ref.getBeanName() + "]");
			Object bean = getBeanInternal(ref.getBeanName(), newlyCreatedBeans);
			// Create a new PropertyValue object holding the bean reference
			return bean;
		}
		catch (BeansException ex) {
			throw new FatalBeanException("Can't resolve reference to bean [" + ref.getBeanName() + "] while setting properties on bean [" + name + "]", ex);
		}
	}



	/**
	 * For each element in the ManagedMap, resolve references if necessary.
	 * Allow ManagedLists as map entries.
	 */
	private ManagedMap resolveManagedMap(String name, ManagedMap mm, Map newlyCreatedBeans) {
		Iterator keys = mm.keySet().iterator();
		while (keys.hasNext()) {
			Object key = keys.next();
			Object value = mm.get(key);
			if (value instanceof RuntimeBeanReference) {
				mm.put(key, resolveReference(name, (RuntimeBeanReference) value, newlyCreatedBeans));
			}
			else if (value instanceof ManagedList) {
				// An entry may be a ManagedList, in which case we may need to
				// resolve references
				mm.put(key, resolveManagedList(name, (ManagedList) value, newlyCreatedBeans));
			}
		}	// for each key in the managed map
		return mm;
	}

	/**
	 * For each element in the ManagedList, resolve reference if necessary
	 */
	private ManagedList resolveManagedList(String name, ManagedList l, Map newlyCreatedBeans) {
		for (int j = 0; j < l.size(); j++) {
			if (l.get(j) instanceof RuntimeBeanReference) {
				l.set(j, resolveReference(name, (RuntimeBeanReference) l.get(j), newlyCreatedBeans));
			}
		}
		return l;
	}
	
	private Object managedListToArray(BeanWrapper bw, PropertyValue pv, Object val, Class componentType, List l)
		throws NegativeArraySizeException, BeansException, BeanDefinitionStoreException {
		try {
			Object[] arr = (Object[]) Array.newInstance(componentType, l.size());
			for (int i = 0; i < l.size(); i++) {
				// TODO hack: BWI cast
				Object newval = ((BeanWrapperImpl) bw).doTypeConversionIfNecessary(bw.getWrappedInstance(), pv.getName(), null, l.get(i), componentType);
				arr[i] = newval;
			}
			val = arr;
		}
		catch (ArrayStoreException ex) {
			throw new BeanDefinitionStoreException("Cannot convert array element from String to " + componentType, ex);
		}
		return val;
	}
	
	/**
	 * Give a bean a chance to react now all its properties are set,
	 * and a chance to know about its owning bean factory (this object).
	 * This means checking whether the bean implements InitializingBean
	 * and/or BeanFactoryAware, and invoking the necessary callback(s) if it does.
	 * @param bean new bean instance we may need to initialize
	 * @param name the bean has in the factory. Used for debug output.
	 */
	private void callLifecycleMethodsIfNecessary(Object bean, String name, RootBeanDefinition rbd, BeanWrapper bw) throws BeansException {
		if (bean instanceof InitializingBean) {
			logger.debug("configureBeanInstance calling afterPropertiesSet() on bean with name '" + name + "'");
			try {
				((InitializingBean) bean).afterPropertiesSet();
			}
			catch (Exception ex) {
				throw new FatalBeanException("afterPropertiesSet on with name '" + name + "' threw an exception", ex);
			}
		}
		
		if (rbd.getInitMethodName() != null) {
			logger.debug("configureBeanInstance calling custom init method '" + rbd.getInitMethodName() + "' on bean with name '" + name + "'");
			bw.invoke(rbd.getInitMethodName(), null);
			// Can throw MethodInvocationException
		}

		if (bean instanceof BeanFactoryAware) {
			logger.debug("configureBeanInstance calling setBeanFactory() on BeanFactoryAware bean with name '" + name + "'");
			try {
				((BeanFactoryAware) bean).setBeanFactory(this);
			}
			catch (BeansException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new FatalBeanException("BeanFactoryAware method on bean with name '" + name + "' threw an exception", ex);
			}
		}
	}	// callLifecycleMethodsIfNecessary

	/**
	 * Make a RootBeanDefinition, even by traversing parent if the parameter is a child definition.
	 * @return a merged RootBeanDefinition with overriden properties
	 */
	protected final RootBeanDefinition getMergedBeanDefinition(String name) throws NoSuchBeanDefinitionException {
		try {
			AbstractBeanDefinition bd = getBeanDefinition(name);
			if (bd instanceof RootBeanDefinition) {
				// Remember to take a deep copy
				return new RootBeanDefinition((RootBeanDefinition) bd);
			}
			else if (bd instanceof ChildBeanDefinition) {
				ChildBeanDefinition cbd = (ChildBeanDefinition) bd;
				// Deep copy
				RootBeanDefinition rbd = new RootBeanDefinition(getMergedBeanDefinition(cbd.getParentName()));
				// Override properties
				rbd.setPropertyValues(merge(rbd.getPropertyValues(), cbd.getPropertyValues()));
				return rbd;
			}			
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (this.parentBeanFactory != null) {
				if (!(this.parentBeanFactory instanceof AbstractBeanFactory))
					throw new BeanDefinitionStoreException("Parent bean factory must be of type AbstractBeanFactory to support inheritance from a parent bean definition: " +
							"offending bean name is '" + name + "'", null);
				return ((AbstractBeanFactory) this.parentBeanFactory).getMergedBeanDefinition(name);
			}
			else {
				throw ex;
			}
		}
		throw new FatalBeanException("Shouldn't happen: BeanDefinition for '" + name + "' is neither a RootBeanDefinition or ChildBeanDefinition");
	}
	
	/**
	 * Incorporate changes from overrides param into pv base param
	 */
	private PropertyValues merge(PropertyValues pv, PropertyValues overrides) {
		MutablePropertyValues parent = new MutablePropertyValues(pv);
		for (int i = 0; i < overrides.getPropertyValues().length; i++) {
			parent.addOrOverridePropertyValue(overrides.getPropertyValues()[i]);
		}
		return parent;
	}
	
	/**
	 * Given a bean name, create an alias. This must respect prototype/
	 * singleton behaviour. We typically use this method to support
	 * names that are illegal within XML ids (used for bean names).
	 * @param name name of the bean
	 * @param alias alias that will behave the same as the bean names
	 */
	public final void registerAlias(String name, String alias) {
		logger.debug("Creating alias '" + alias + "' for bean with name '" + name + "'");
		this.aliasMap.put(alias, name);
	}

	public final String[] getAliases(String pname) {
		String name = transformedBeanName(pname);
		List aliases = new ArrayList();
		for (Iterator it = this.aliasMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (entry.getValue().equals(name)) {
				aliases.add(entry.getKey());
			}
		}
		return (String[]) aliases.toArray(new String[aliases.size()]);
	}


	//---------------------------------------------------------------------
	// Abstract method to be implemented by concrete subclasses
	//---------------------------------------------------------------------
	/**
	 * This method must be defined by concrete subclasses to implement the
	 * <b>Template Method</b> GoF design pattern.
	 * <br>Subclasses should normally implement caching, as this method is invoked
	 * by this class every time a bean is requested.
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name. Must never return null.
	 * @throws NoSuchBeanDefinitionException if the bean definition cannot be resolved
	 */
	protected abstract AbstractBeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

}
