/**
 * Generic framework code included with 
 * <a href="http://www.amazon.com/exec/obidos/tg/detail/-/1861007841/">Expert One-On-One J2EE Design and Development</a>
 * by Rod Johnson (Wrox, 2002). 
 * This code is free to use and modify. However, please
 * acknowledge the source and include the above URL in each
 * class using or derived from this code. 
 * Please contact <a href="mailto:rod.johnson@interface21.com">rod.johnson@interface21.com</a>
 * for commercial support.
 */

package com.interface21.web.servlet.mvc;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.interface21.validation.DataBinder;
import com.interface21.validation.Validator;
import com.interface21.web.bind.BindUtils;

/**
 * Controller implementation that creates a Command object
 * on receipt of requests and attempts to populate the command's
 * JavaBean properties with request attribtes. 
 * Once created, commands can be validated using a Validator
 * associated with this class.
 * Type mismatches populating a command are treated as validation
 * errors, but caught by the framework, not application code.
 * @author Rod Johnson 
 */
public abstract class BaseCommandController extends AbstractController {

	public static final String DEFAULT_BEAN_NAME = "command";

	//-------------------------------------------------------------------------
	// Instance data
	//-------------------------------------------------------------------------
	private Class commandClass;

	private Validator validator;
	
	private String beanName = DEFAULT_BEAN_NAME;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------	
	public BaseCommandController() {
	}

	protected void setCommandClass(Class commandClass) {
		checkValidator(this.validator, commandClass);
		this.commandClass = commandClass;
	}

	protected Class getCommandClass() {
		return this.commandClass;
	}

	//-------------------------------------------------------------------------
	// JavaBean properties
	//-------------------------------------------------------------------------
	public final void setCommandClassName(String name) throws ClassNotFoundException {
		this.commandClass = Class.forName(name);
	}
	
	protected String getCommandClassName() {
		return (this.commandClass != null ? this.commandClass.getName() : null);
	}

	protected boolean checkCommandClass(Object command) {
		return (this.commandClass == null || this.commandClass.isInstance(command));
	}

	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}
	
	protected final String getBeanName() {
		return this.beanName;
	}

	public final void setValidator(Validator validator) {
		checkValidator(validator, this.commandClass);
		this.validator = validator;
	}

	private void checkValidator(Validator validator, Class commandClass) {
		if (validator != null && commandClass != null && !validator.supports(commandClass))
			throw new IllegalArgumentException(
				"Validator [" + validator + "] can't validate command class of type " + commandClass);
	}

	//-------------------------------------------------------------------------
	// Implementation methods
	//-------------------------------------------------------------------------

	protected Object createCommand(HttpServletRequest request) throws ServletException {
		logger.info("Must create new command of " + commandClass);
		try {
			return commandClass.newInstance();
		}	catch (InstantiationException ex) {
			throw new ServletException(
				"Cannot instantiate command " + commandClass + "; does it have a public no arg constructor?",
				ex);
		} catch (IllegalAccessException ex) {
			throw new ServletException(
				"Cannot instantiate command " + commandClass + "; cannot access constructor",
				ex);
		}
	}

	/**
	 * Subclasses can override this
	 * @return object to bind onto
	 */
	protected Object userObject(HttpServletRequest request) throws ServletException {
		return createCommand(request);
	}

	// *TODO: must be able to parameterize the binding process.
	// without depending on DataBinder
	
	protected final DataBinder bindAndValidate(HttpServletRequest request, Object command) throws ServletException {
		return BindUtils.bindAndValidate(request, command, this.beanName, this.validator);
	}
}