package com.interface21.validation;

import java.util.*;

import com.interface21.beans.BeanWrapper;
import com.interface21.beans.BeanWrapperImpl;

/**
 *
 * @author Rod Johnson
 */
public class BindException extends Exception implements Errors {

	public static final String ERROR_KEY_PREFIX = "com.interface21.web.bind.BIND_EXCEPTION.";

	//---------------------------------------------------------------------
	// Instance data
	//---------------------------------------------------------------------
	private List errors = new LinkedList();

	private BeanWrapper beanWrapper;

	private String objectName;

	private String nestedPath = "";

	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------
	public BindException(Object target, String name) {
		this.beanWrapper = new BeanWrapperImpl(target);
		this.objectName = name;
		this.nestedPath = "";
	}

	protected BeanWrapper getBeanWrapper() {
		return beanWrapper;
	}

	private String fixedField(String field) {
		// Add nested path, if present, allowing context changes
		field = nestedPath + field;
		//System.out.println("Fixed field = '" + field + "'");
		return field;
	}

	protected void addFieldError(FieldError fe) {
		errors.add(fe);
	}

	//---------------------------------------------------------------------
	// Implementation of Errors
	//---------------------------------------------------------------------
	public Object getTarget() {
		return beanWrapper.getWrappedInstance();
	}

	public String getObjectName() {
		return objectName;
	}

        public void reject(String resolvedMessage) {
                errors.add(new ObjectError(this.objectName, resolvedMessage));
        }

	public void reject(String errorCode, String defaultMessage) {
		errors.add(new ObjectError(this.objectName, errorCode, (Object[])null, defaultMessage));
	}

	public void rejectValue(String field, String errorCode, String defaultMessage) throws InvalidBinderUsageException {
		field = fixedField(field);
		Object newVal = getBeanWrapper().getPropertyValue(field);
		FieldError fe = new FieldError(this.objectName, field, newVal, errorCode, defaultMessage);
		errors.add(fe);
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public int getErrorCount() {
		return errors.size();
	}

	public List getAllErrors() {
		return Collections.unmodifiableList(errors);
	}

	public boolean hasGlobalErrors() {
		return (getGlobalErrorCount() > 0);
	}

	public int getGlobalErrorCount() {
		return getGlobalErrors().size();
	}

	public List getGlobalErrors() {
		List result = new ArrayList();
		for (Iterator it = errors.iterator(); it.hasNext();) {
			ObjectError fe = (ObjectError) it.next();
			if (!(fe instanceof FieldError))
				result.add(fe);
		}
		return Collections.unmodifiableList(result);
	}

	public ObjectError getGlobalError() {
		for (Iterator it = errors.iterator(); it.hasNext();) {
			ObjectError fe = (ObjectError) it.next();
			if (!(fe instanceof FieldError))
				return fe;
		}
		return null;
	}

	public boolean hasFieldErrors(String field) {
		return (getFieldErrorCount(field) > 0);
	}

	public int getFieldErrorCount(String field) {
		return getFieldErrors(field).size();
	}

	public List getFieldErrors(String field) {
		List result = new ArrayList();
		field = fixedField(field);
		for (Iterator it = errors.iterator(); it.hasNext();) {
			ObjectError fe = (ObjectError) it.next();
			if (fe instanceof FieldError && field.equals(((FieldError) fe).getField()))
				result.add(fe);
		}
		return Collections.unmodifiableList(result);
	}

	public FieldError getFieldError(String field) {
		field = fixedField(field);
		for (Iterator it = errors.iterator(); it.hasNext();) {
			ObjectError fe = (ObjectError) it.next();
			if (fe instanceof FieldError && field.equals(((FieldError) fe).getField()))
				return (FieldError) fe;
		}
		return null;
	}

	/**
	 * Return value held in error if error, else
	 */
	public Object getPropertyValueOrRejectedUpdate(String field) {
		field = fixedField(field);
		FieldError fe = getFieldError(field);
		if (fe == null)
			return getBeanWrapper().getPropertyValue(field);
		return fe.getRejectedValue();
	}

	public void setNestedPath(String nestedPath) {
		if (nestedPath == null)
			nestedPath = "";
		if (nestedPath.length() > 0)
			nestedPath += ".";
		this.nestedPath = nestedPath;
		//System.out.println("NESTEDPATH set to '" + this.nestedPath + "'");
	}

	/*
	 * Return model!?
	 */
	public final Map getModel() {
		Map m = new HashMap();
		// errors instance, even if no errors
		m.put(ERROR_KEY_PREFIX + objectName, this);
		// mapping from name to target object
		m.put(objectName, beanWrapper.getWrappedInstance());
		return m;
	}

	/**
	 * @return diagnostic information about the errors held in this object
	 */
	public String getMessage() {
		StringBuffer sb = new StringBuffer("BindException: " + getErrorCount() + " errors");
		//System.out.println("command is " + getTarget() + "; ");
		Iterator itr = errors.iterator();
		while (itr.hasNext()) {
			sb.append("; " + itr.next());
		}
		return sb.toString();
	}

}
