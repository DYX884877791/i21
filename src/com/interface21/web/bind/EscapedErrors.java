package com.interface21.web.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import com.interface21.validation.Errors;
import com.interface21.validation.FieldError;
import com.interface21.validation.ObjectError;
import com.interface21.web.util.HtmlUtils;

/**
 * Errors wrapper that adds automatic HTML escaping to the wrapped instance.
 * @author Juergen Hoeller
 * @since 01.03.2003
 */
public class EscapedErrors implements Errors {

	private Errors source = null;

	/**
	 * Create a new EscapedErrors instance for the given source instance.
	 */
	public EscapedErrors(Errors source) {
		this.source = source;
	}

	public String getObjectName() {
		return source.getObjectName();
	}

	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		source.reject(errorCode, errorArgs, defaultMessage);
	}

	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		source.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	public boolean hasErrors() {
		return source.hasErrors();
	}

	public int getErrorCount() {
		return source.getErrorCount();
	}

	public List getAllErrors() {
		return escapeObjectErrors(source.getAllErrors());
	}

	public boolean hasGlobalErrors() {
		return source.hasGlobalErrors();
	}

	public int getGlobalErrorCount() {
		return source.getGlobalErrorCount();
	}

	public List getGlobalErrors() {
		return escapeObjectErrors(source.getGlobalErrors());
	}

	public ObjectError getGlobalError() {
		return escapeObjectError(source.getGlobalError());
	}

	public boolean hasFieldErrors(String field) {
		return source.hasFieldErrors(field);
	}

	public int getFieldErrorCount(String field) {
		return source.getFieldErrorCount(field);
	}

	public List getFieldErrors(String field) {
		return escapeObjectErrors(source.getFieldErrors(field));
	}

	public FieldError getFieldError(String field) {
		return (FieldError) escapeObjectError(source.getFieldError(field));
	}

	public Object getPropertyValueOrRejectedUpdate(String field) {
		return source.getPropertyValueOrRejectedUpdate(field);
	}

	public void setNestedPath(String nestedPath) {
		source.setNestedPath(nestedPath);
	}

	private ObjectError escapeObjectError(ObjectError source) {
		if (source == null)
			return null;
		if (source instanceof FieldError) {
			FieldError fieldError = (FieldError) source;
			Object value = fieldError.getRejectedValue();
			if (value instanceof String) {
				value = HtmlUtils.htmlEscape((String) fieldError.getRejectedValue());
			}
			return new FieldError(fieldError.getObjectName(), fieldError.getField(), value, fieldError.getCode(), HtmlUtils.htmlEscape(fieldError.getDefaultMessage()));
		}
		return new ObjectError(source.getObjectName(), source.getCode(), HtmlUtils.htmlEscape(source.getDefaultMessage()));
	}

	private List escapeObjectErrors(List source) {
		List escaped = new ArrayList();
		for (Iterator it = source.iterator(); it.hasNext();) {
			ObjectError objectError = (ObjectError)it.next();
			escaped.add(escapeObjectError(objectError));
		}
		return escaped;
	}
}
