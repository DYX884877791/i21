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

import com.interface21.validation.Validator;
import com.interface21.web.bind.ServletRequestDataBinder;

/**
 * Controller implementation that creates a Command object
 * on receipt of requests and attempts to populate the command's
 * JavaBean properties with request attributes.
 * Once created, commands can be validated using a Validator
 * associated with this class.
 * Type mismatches populating a command are treated as validation
 * errors, but caught by the framework, not application code.
 *
 * <p>Note: This class is the base class for both FormController and
 * AbstractCommandController (which in turn is the base class for
 * custom controller implementations).
 *
 * @author Rod Johnson, Juergen Hoeller
 */
public abstract class BaseCommandController extends AbstractController {

	public static final String DEFAULT_BEAN_NAME = "command";

	private String beanName = DEFAULT_BEAN_NAME;

	private Class commandClass;

	private Validator validator;
	
	public BaseCommandController() {
	}

	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	protected final String getBeanName() {
		return this.beanName;
	}

	protected final void setCommandClass(Class commandClass) {
		checkValidator(this.validator, commandClass);
		this.commandClass = commandClass;
	}

	protected final Class getCommandClass() {
		return this.commandClass;
	}

	public final void setCommandClassName(String name) throws ClassNotFoundException {
		this.commandClass = Class.forName(name);
	}
	
	protected final String getCommandClassName() {
		return (this.commandClass != null ? this.commandClass.getName() : null);
	}

	/**
	 * Set the validator for this controller.
	 * @param validator validator for this controller.
	 */
	public final void setValidator(Validator validator) {
		checkValidator(validator, this.commandClass);
		this.validator = validator;
	}

	/**
	 * Return the validator for this controller.
	 * @return the validator for this controller.
	 */
	protected final Validator getValidator() {
		return validator;
	}

	/**
	 * Return the validator that gets applied when binding.
	 * Can be overridden to return null to allow for custom validator calls.
	 * @return the validator for the binding process
	 */
	protected Validator getValidatorForBinding() {
		return validator;
	}

	/**
	 * Check if the given validator and command class match.
	 * @param validator validator instance
	 * @param commandClass command class
	 */
	private void checkValidator(Validator validator, Class commandClass) throws IllegalArgumentException {
		if (validator != null && commandClass != null && !validator.supports(commandClass))
			throw new IllegalArgumentException(
				"Validator [" + validator + "] can't validate command class of type " + commandClass);
	}

	/**
	 * Check if the given command object is a valid for this controller,
	 * i.e. its command class.
	 * @param command command object to check
	 * @return if the command object is valid for this controller
	 */
	protected boolean checkCommand(Object command) {
		return (this.commandClass == null || this.commandClass.isInstance(command));
	}

	/**
	 * Create a new command instance for the command class of this controller.
	 * @return the new command instance
	 * @throws ServletException in case of instantiation errors
	 */
	protected final Object createCommand() throws ServletException {
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
	 * Retrieve a user's command object for the given request.
	 * <p>Default implementation calls createCommand. Subclasses can override this.
	 * @param request current HTTP request
	 * @return object command to bind onto
	 * @see #createCommand
	 */
	protected Object userObject(HttpServletRequest request) throws ServletException {
		return createCommand();
	}

	/**
	 * Bind the parameters of the given request to the given command object.
	 * @param request current HTTP request
	 * @param command command to bind onto
	 * @return the ServletRequestDataBinder instance for additional custom validation
	 * @throws ServletException in case of invalid state or arguments
	 */
	protected final ServletRequestDataBinder bindAndValidate(HttpServletRequest request, Object command) throws ServletException {
		ServletRequestDataBinder binder = createBinder(request, command);
		binder.bind(request);
		Validator validator = getValidatorForBinding();
		if (validator != null) {
			logger.debug("Invoking validator [" + validator + "]");
			if (!validator.supports(command.getClass()))
				throw new IllegalArgumentException("Validator " + validator.getClass() + " does not support " + command.getClass());
			validator.validate(command, binder);
			if (binder.hasErrors())
				logger.debug("Validator found " + binder.getErrorCount() + " errors");
			else
				logger.debug("Validator found no errors");
		}
		onBindAndValidate(request, command, binder);
		return binder;
	}

	/**
	 * Create a new binder instance for the given command and request.
	 * @param command command to bind onto
	 * @param request current request
	 * @return the new binder instance
	 * @see #bindAndValidate
	 */
	protected final ServletRequestDataBinder createBinder(HttpServletRequest request, Object command) {
		ServletRequestDataBinder binder = new ServletRequestDataBinder(command, getBeanName());
		initBinder(request, binder);
		return binder;
	}

	/**
	 * Initialize the given binder instance, e.g. with custom editors.
	 * Called by createBinder.
	 * @param request current request
	 * @param binder new binder instance
	 * @see #createBinder
	 */
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
	}

	/**
	 * Callback for custom postprocessing in terms of binding and validation.
	 * Called on each submit, after standard binding and validation,
	 * and before error evaluation.
	 * @param request current HTTP request
	 * @param command bound command
	 * @param errors ServletRequestDataBinder for additional custom validation
	 * @throws ServletException in case of invalid state or arguments
	 * @see #bindAndValidate
	 */
	protected void onBindAndValidate(HttpServletRequest request, Object command, ServletRequestDataBinder errors) throws ServletException {
	}

}
