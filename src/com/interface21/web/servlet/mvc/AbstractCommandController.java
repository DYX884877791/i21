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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.interface21.validation.Errors;
import com.interface21.web.servlet.ModelAndView;

/**
 * Abstract base class for custom command controllers. Autopopulates a
 * command bean from the request. For command validation, a validator
 * (property inherited from BaseCommandController) can be used.
 *
 * @author Rod Johnson, Juergen Hoeller
 */
public abstract class AbstractCommandController extends BaseCommandController {

	public AbstractCommandController(Class commandClass, String beanName) {
		setCommandClass(commandClass);
		setBeanName(beanName);
	}
	
	public AbstractCommandController(Class commandClass) {
		setCommandClass(commandClass);
	}

	public AbstractCommandController() {
	}

	protected final ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Object command = userObject(request);
		Errors errors = bindAndValidate(request, command);
		return handle(request, response, command, errors);
	}

	/**
	 * Template method for request handling, providing a populated and
	 * validated instance of the command class, and an Errors object
	 * containing binding and validation errors.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param command the populated command object
	 * @param errors the binder containing errors
	 * @return a ModelAndView to render, or null
	 */
	protected abstract ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
	                                       Object command, Errors errors);

}
