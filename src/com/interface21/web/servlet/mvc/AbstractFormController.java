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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.interface21.validation.BindException;
import com.interface21.validation.Errors;
import com.interface21.web.bind.ServletRequestDataBinder;
import com.interface21.web.servlet.ModelAndView;

/**
 * Form controller that autopopulates a form bean from the request, using a new bean
 * bean instance per request. To achieve population of the same bean instance,
 * "session form" can be activated. This is the common base class for both framework
 * subclasses like AbstractWizardFormController and application controllers.
 *
 * <p>Subclasses need to override showForm to prepare the form view, and processSubmit
 * to handle submit requests. For the latter, binding errors like type mismatches will
 * be reported via the given "errors" binder. For additional custom form validation,
 * a validator (property inherited from BaseCommandController) can be used, reporting
 * via the same "errors" instance.
 *
 * <p>This approach is similar to the Struts approach, with the main difference that
 * you can use any beans, with no need to derive from a form base class. Type
 * mismatches are stored in Errors instances instead of forcing the form bean to have
 * mostly String properties. A further difference is the decoupling of validation,
 * not even coupling the validation logic to web usage.
 *
 * <p>Note that by default POST requests are treated as form submissions. This can be
 * customized by overriding isFormSubmission. Custom binding can be achieved either
 * by registering custom property editors before binding in an initBinder
 * implementation, or by custom bean population from request parameters after binding
 * in an onBindAndValidate implementation.
 * 
 * <p>Note that views should never retrieve form beans from the session but always
 * from the request, as prepared by the form controller. Remember that some view
 * technologies like Velocity cannot even access a HTTP session.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AbstractWizardFormController
 * @see #setSessionForm
 * @see #showForm
 * @see #processSubmit
 * @see #setValidator
 * @see #isFormSubmission
 * @see #initBinder
 * @see #onBindAndValidate
 */
public abstract class AbstractFormController extends BaseCommandController {

	private boolean sessionForm = false;

	/**
	 * Create a new AbstractFormController.
	 * <p>Subclasses should set the following properties, either in the
	 * constructor or via a BeanFactory: commandClass, beanName, sessionForm.
	 * Note that commandClass doesn't need to be set when overriding
	 * formBackingObject, as this determines the class anyway.
	 * @see #setCommandClass
	 * @see #setBeanName
	 * @see #setSessionForm
	 */
	public AbstractFormController() {
		super();
	}

	/**
	 * Activates resp. deactivates session form mode.
	 * In session form mode, the form is stored in the session to keep
	 * the form object instance between requests, instead of creating
	 * a new one on each request.
	 * <p>This is necessary for either wizard-style controllers that
	 * populate a single form object from multiple pages, or forms
	 * that populate a persistent object that needs to be identical
	 * to track changes.
	 */
	public final void setSessionForm(boolean sessionForm) {
		this.sessionForm = sessionForm;
	}

	/**
	 * Return if session form mode is activated.
	 */
	protected final boolean isSessionForm() {
		return sessionForm;
	}

	/**
	 * Return the name of the session attribute that holds
	 * the form object for this controller.
	 * @return the name of the form session attribute,
	 * or null if not in session form mode.
	 */
	protected final String getFormSessionAttributeName() {
		return isSessionForm() ? getClass() + ".form." + getBeanName() : null;
	}

	protected final ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (isFormSubmission(request)) {
			// process submit
			Object command = userObject(request);
			ServletRequestDataBinder errors = bindAndValidate(request, command);
			return processSubmit(request, response, command, errors);
		}
		else {
			// show form
			logger.debug("Displaying form");
			Object command = prepareFormBackingObject(request);
			// add empty errors object, for convenient error evaluation in views
			// (on both first attempt and resubmit)
			return showForm(request, response, createBinder(request, command));
		}
	}

	/**
	 * Determine if the given request represents a form submission.
	 * <p>Default implementation treats POST requests as form submissions.
	 * Subclasses can override this to use a custom strategy, e.g. a specific
	 * request parameter (assumably a hidden field or submit button name).
	 * @param request current HTTP request
	 * @return if the request represents a form submission
	 */
	protected boolean isFormSubmission(HttpServletRequest request) {
		return "POST".equals(request.getMethod());
	}

	/**
	 * Prepare a backing object for the current form and the given request,
	 * and check its validity.
	 * @param request current HTTP request
	 * @throws ServletException in case of an invalid form object
	 */
	private Object prepareFormBackingObject(HttpServletRequest request) throws ServletException {
		Object formObject = formBackingObject(request);
		if (formObject == null)
			throw new ServletException("Form object returned by formBackingObject() may not be null");
		if (!checkCommand(formObject))
			throw new ServletException("Form object returned by formBackingObject() must match commandClass");
		return formObject;
	}

	/**
	 * Retrieve a backing object for the current form from the given request.
	 * <p>Default implementation calls BaseCommandController.createCommand.
	 * Subclasses can override this to provide a preinitialized backing object.
	 * @param request current HTTP request
	 * @return the backing objact
	 * @throws ServletException in case of invalid state or arguments
	 * @see BaseCommandController#createCommand
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		return createCommand();
	}

	/**
	 * Prepare the form model and view, including reference and error data.
	 * Can show a configured form page, or to generate a custom (programmatic)
	 * form view.
	 * <p>A configuration-based implementation will typically call showForm to prepare
	 * the form view for a given view name.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param errors binder containing errors
	 * @return the prepared form view
	 * @throws ServletException in case of invalid state or arguments
	 * @see #showForm(HttpServletRequest, BindException, String)
	 */
	protected abstract ModelAndView showForm(HttpServletRequest request, HttpServletResponse response,
	                                         BindException errors) throws ServletException;

	/**
	 * Prepare model and view for the given form, including reference and errors.
	 * In session form mode: Re-puts the form object in the session when returning
	 * to the form, as it has been removed by userObject.
	 * Can be used in subclasses to redirect back to a specific form page.
	 * @param request current HTTP request
	 * @param errors binder containing errors
	 * @param viewName name of the form view
	 * @return the prepared form view
	 * @throws ServletException in case of invalid state or arguments
	 */
	protected final ModelAndView showForm(HttpServletRequest request, BindException errors, String viewName)
	    throws ServletException {
		return showForm(request, errors, viewName, null);
	}

	/**
	 * Prepare model and view for the given form, including reference and errors,
	 * adding a controller-specific control model.
	 * In session form mode: Re-puts the form object in the session when returning
	 * to the form, as it has been removed by userObject.
	 * Can be used in subclasses to redirect back to a specific form page.
	 * @param request current HTTP request
	 * @param errors binder containing errors
	 * @param viewName name of the form view
	 * @param controlModel model map containing controller-specific control data
	 * (e.g. current page in wizard-style controllers).
	 * @return the prepared form view
	 * @throws ServletException in case of invalid state or arguments
	 */
	protected final ModelAndView showForm(HttpServletRequest request, BindException errors, String viewName,
	                                      Map controlModel) throws ServletException {
		if (isSessionForm()) {
			request.getSession().setAttribute(getFormSessionAttributeName(), errors.getTarget());
		}
		Map model = referenceData(request, errors.getTarget(), errors);
		if (model == null) {
			model = new HashMap();
		}
		model.putAll(errors.getModel());
		if (controlModel != null) {
			model.putAll(controlModel);
		}
		return new ModelAndView(viewName, model);
	}

	/**
	 * Create a reference data map for the given request, consisting of
	 * bean name/bean instance pairs as expected by ModelAndView.
	 * <p>Default implementation returns null.
	 * Subclasses can override this to set reference data used in the view.
	 * @param request current HTTP request
	 * @param command form object with request parameters bound onto it
	 * @param errors binder containing current errors, if any
	 * @return a Map with reference data entries, or null if none
	 * @throws ServletException in case of invalid state or arguments
	 * @see ModelAndView
	 */
	protected Map referenceData(HttpServletRequest request, Object command, Errors errors)
	    throws ServletException {
		return null;
	}

	/**
	 * Return the form object for the given request.
	 * In session form mode: Retrieve the form object from the session.
	 * The form object gets removed from the session, but it will be
	 * re-added when showing the form for resubmission.
	 * @param request current HTTP request
	 * @return object form to bind onto
	 * @throws ServletException in case of invalid state or arguments
	 */
	protected final Object userObject(HttpServletRequest request) throws ServletException {
		if (!isSessionForm()) {
			return super.userObject(request);
		}
		HttpSession session = request.getSession(false);
		if (session == null)
			throw new ServletException("Must have session when trying to bind");
		Object formObject = session.getAttribute(getFormSessionAttributeName());
		session.removeAttribute(getFormSessionAttributeName());
		if (formObject == null)
			throw new ServletException("Form object not found in session");
		return formObject;
	}

	/**
	 * Process submit request. Called by handleRequestInternal in case of a
	 * form submission.
	 * <p>Subclasses can override this to provide custom submission handling
	 * like triggering a custom action. They can also provide custom validation
	 * and call showForm/super.onSubmit accordingly.
	 * @param request current servlet request
	 * @param response current servlet response
	 * @param command form object with request parameters bound onto it
	 * @param errors binder without errors (subclass can add errors if it wants to)
	 * @return the prepared model and view, or null
	 * @throws ServletException in case of invalid state or arguments
	 * @throws IOException in case of I/O errors
	 * @see #showForm
	 */
	protected abstract ModelAndView processSubmit(HttpServletRequest request,	HttpServletResponse response,
	                                              Object command, BindException errors)
			throws ServletException, IOException;

}
