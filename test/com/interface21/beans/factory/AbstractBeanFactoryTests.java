/*
 * BeanWrapperTestSuite.java
 *
 * Created on 1 September 2001, 19:35
 */

package com.interface21.beans.factory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import com.interface21.beans.BeansException;
import com.interface21.beans.ErrorCodedPropertyVetoException;
import com.interface21.beans.PropertyVetoExceptionsException;
import com.interface21.beans.TestBean;

/**
 * Subclasses must implement setUp() to initialize bean factory
 * and any other variables they need
 * @author Rod Johnson
 * @version $RevisionId$
 * REQUIRES THE FOLLOWING BEAN DEFINITIONS:
 * see lbiinit
 */
public abstract class AbstractBeanFactoryTests extends TestCase {


	/** Creates new SeatingPlanTest */
	public AbstractBeanFactoryTests(String name) {
		super(name);
	}


	protected abstract BeanFactory getBeanFactory();

	/**
	 * Roderick beans inherits from rod,
	 * overriding name only
	 */
	public void testInheritance() {
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean roderick = (TestBean) getBeanFactory().getBean("roderick");
		assertTrue("not == ", rod != roderick);
		assertTrue("rod.name is Rod", rod.getName().equals("Rod"));
		assertTrue("rod.age is 31", rod.getAge() == 31);
		assertTrue("roderick.name is Roderick", roderick.getName().equals("Roderick"));
		assertTrue("roderick.age was inherited", roderick.getAge() == rod.getAge());
	}
	
	public void testFindsValidInstance() {
		try {
			Object o = getBeanFactory().getBean("rod");
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
			TestBean rod = (TestBean) o;
			assertTrue("rod.name is Rod", rod.getName().equals("Rod"));
			assertTrue("rod.age is 31", rod.getAge() == 31);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}
	
	public void testGetInstanceByMatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", TestBean.class);
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance with matching class");
		}
	}
	
	
	
	public void testGetInstanceByNonmatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", BeanFactory.class);
			fail("Rod bean is not of type BeanFactory; getBeanInstance(rod, BeanFactory.class) should throw BeanNotOfRequiredTypeException");
		}
		catch (BeanNotOfRequiredTypeException ex) {
			// So far, so good
			assertTrue("Exception has correct bean name", ex.getBeanName().equals("rod"));
			assertTrue("Exception requiredType must be BeanFactory.class", ex.getRequiredType().equals(BeanFactory.class));
			assertTrue("Exception actualType as TestBean.class", ex.getActualType().equals(TestBean.class));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}
	
	public void testGetSharedInstanceByMatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", TestBean.class);
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance with matching class");
		}
	}
	
	public void testGetSharedInstanceByMatchingClassNoCatch() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
	}
	
	public void testGetSharedInstanceByNonmatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", BeanFactory.class);
			fail("Rod bean is not of type BeanFactory; getBeanInstance(rod, BeanFactory.class) should throw BeanNotOfRequiredTypeException");
		}
		catch (BeanNotOfRequiredTypeException ex) {
			// So far, so good
			assertTrue("Exception has correct bean name", ex.getBeanName().equals("rod"));
			assertTrue("Exception requiredType must be BeanFactory.class", ex.getRequiredType().equals(BeanFactory.class));
			assertTrue("Exception actualType as TestBean.class", ex.getActualType().equals(TestBean.class));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testSharedInstancesAreEqual() {
		try {
			Object o = getBeanFactory().getBean("rod");
			assertTrue("Rod bean1 is a TestBean", o instanceof TestBean);
			Object o1 = getBeanFactory().getBean("rod");
			assertTrue("Rod bean2 is a TestBean", o1 instanceof TestBean);
			assertTrue("Object equals applies", o == o1);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}
	
	/*
	public void testPrototypeInstancesAreIndependent() {
		TestBean tb1 = (TestBean) getBeanFactory().getBean("kathy");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("kathy");
		assertTrue("ref equal DOES NOT apply", tb1 != tb2);
		assertTrue("object equal true", tb1.equals(tb2));
		tb1.setAge(1);
		tb2.setAge(2);
		assertTrue("1 age independent = 1", tb1.getAge() == 1);
		assertTrue("2 age independent = 2", tb2.getAge() == 2);
		assertTrue("object equal now false", !tb1.equals(tb2));
	}
	*/

	public void testNotThere() {
		try {
			Object o = getBeanFactory().getBean("Mr Squiggle");
			fail("Can't find missing bean");
		}
		catch (BeansException ex) {
			//ex.printStackTrace();
			//fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testValidEmpty() {
		try {
			Object o = getBeanFactory().getBean("validEmpty");
			assertTrue("validEmpty bean is a TestBean", o instanceof TestBean);
			TestBean ve = (TestBean) o;
			assertTrue("Valid empty has defaults", ve.getName() == null && ve.getAge() == 0 && ve.getSpouse() == null);
		}
		catch (BeansException ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on valid empty");
		}
	}

	public void testTypeMismatch() {
		try {
			Object o = getBeanFactory().getBean("typeMismatch");
			fail("Shouldn't succeed with type mismatch");
		}
		catch (PropertyVetoExceptionsException ex) {
			// Further tests
			assertTrue("Has one error ", ex.getExceptionCount() == 1);
			assertTrue("Error is for field age", ex.getPropertyVetoException("age") != null);

			TestBean tb = (TestBean) ex.getBeanWrapper().getWrappedInstance();
			assertTrue("Age still has default", tb.getAge() == 0);
			assertTrue("We have rejected age in exception", ex.getPropertyVetoException("age").getPropertyChangeEvent().getNewValue().equals("34x"));
			assertTrue("valid name stuck", tb.getName().equals("typeMismatch"));
			assertTrue("valid spouse stuck", tb.getSpouse().getName().equals("Rod"));
		}
		catch (BeansException ex) {
			ex.printStackTrace();
			fail("Shouldn't throw generic BeanException on type mismatch");
		}
	}

/*
	public void testVeto() {
		try {
			Object o = beanFactory.getBeanInstance("listenerVeto");
			fail("Shouldn't survive veto");
		}
		catch (PropertyVetoExceptionsException ex) {
			// Further tests
			ex.getPropertyVetoExceptions()[0].printStackTrace();
			ex.printStackTrace();

			assertTrue("Has one error ", ex.getExceptionCount() == 1);
			assertTrue("Error is for field age", ex.getPropertyVetoException("age") != null);

			TestBean tb = (TestBean) ex.getBeanWrapper().getWrappedInstance();
			assertTrue("Age still has default", tb.getAge() == 0);
			assertTrue("We have rejected age in exception", ex.getPropertyVetoException("age").getPropertyChangeEvent().getNewValue().equals(new Integer(66)));
			assertTrue("valid name stuck", tb.getName().equals("listenerVeto"));
		}
		catch (ReflectionException ex) {
			ex.printStackTrace();
			fail("Shouldn't throw ReflectionXception on type mismatch");
		}
	}
	*/


	private static class ConsoleListener implements PropertyChangeListener {
		int events;

		public void propertyChange(PropertyChangeEvent e) {
			++events;
			//System.out.println("PropertyChangeEvent " + events + ": old value=[" + e.getOldValue() + "] new value=[" + e.getNewValue() + "]");
		}

		public int getEventCount() {
			return events;
		}
	}

	public static class AgistListener implements VetoableChangeListener {
		int events;

		public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {
			++events;
			//System.out.println("VetoableChangeEvent: old value=[" + e.getOldValue() + "] new value=[" + e.getNewValue() + "]");
			if ("age".equals(e.getPropertyName())) {
				//if (e.getPropertyName().equals("age")
				Integer newValue = (Integer) e.getNewValue();
				if (newValue.intValue() > 65) {
					//System.out.println("Yeah! got another old bugger and vetoed it");
					throw new ErrorCodedPropertyVetoException("You specified " + newValue.intValue() + "; that's too bloody old", e, "tooOld");
				}
			}
		}

		public int getEventCount() {
			return events;
		}
	}

	public static class TestBeanEditor extends PropertyEditorSupport {
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}
}
