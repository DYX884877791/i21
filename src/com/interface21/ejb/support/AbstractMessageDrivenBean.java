/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */
 
package com.interface21.ejb.support;

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

/** 
 * Convenient superclass for MDBs.
 * Doesn't require JMS, as EJB 2.1 MDBs are no longer
 * JMS-specific: see the AbstractJmsMessageDrivenBean subclass.
 * <br>This class ensures that subclasses have access to the
 * MessageDrivenContext provided by the EJB container, and implement
 * a no argument ejbCreate() method as required by the EJB specification.
 * This ejbCreate() method loads a BeanFactory, before invoking
 * the onEjbCreate() method, which should contain subclass-specific
 * initialization.
 * <br>
 * NB: we cannot use final methods to implement EJB API methods,
 * as this violates the EJB specification. However, there should
 * be no need to override the setMessageDrivenContext() or
 * ejbCreate() methods.
 * @author Rod Johnson
 * @version $Id: AbstractMessageDrivenBean.java,v 1.2 2003/07/26 13:41:41 johnsonr Exp $
 */
public abstract class AbstractMessageDrivenBean 
				extends AbstractEnterpriseBean 
				implements MessageDrivenBean {
	
	//-------------------------------------------------------------------------
	// Instance data
	//-------------------------------------------------------------------------
	/** MessageDrivenContext passed to this object */
	private MessageDrivenContext	messageDrivenContext;
	
	
	//-------------------------------------------------------------------------
	// Lifecycle methods
	//-------------------------------------------------------------------------	
	/**
	 * Convenience method for subclasses to use
	 * @return the MessageDrivenContext passed to this EJB by the EJB container
	 */
	protected final MessageDrivenContext getMessageDrivenContext() {
		return messageDrivenContext;
	}
	
	/**
	 * Required lifecycle method. Sets the MessageDriven context.
	 * @param messageDrivenContext MessageDrivenContext
	 */
	public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
		logger.debug("setMessageContext");
		this.messageDrivenContext = messageDrivenContext;
	}
	
	/** 
	 * Lifecycle method required by the EJB specification but not
	 * the MessageDrivenBean interface.
	 * This implementation loads the BeanFactory.
	 * Don't override it (although it can't be made final):
	 * code your initialization in onEjbCreate(), which is
	 * called when the BeanFactory is available.
	 * Unfortunately we can't load the BeanFactory in setSessionContext(), as ResourceManager
	 * access isn't permitted and the BeanFactory may require it. 
	 */
	public void ejbCreate() throws CreateException {
		loadBeanFactory();
		onEjbCreate();
	}
	
	/**
	 * Subclasses must implement this method to do any initialization
	 * they would otherwise have done in an ejbCreate() method. 
	 * The BeanFactory will have been loaded.
	 * The same restrictions apply to the work of this method as
	 * to an ejbCreate() method.
	 * @throws CreateException
	 */
	protected abstract void onEjbCreate() throws CreateException;

} 	// class AbstractMessageDrivenBean
