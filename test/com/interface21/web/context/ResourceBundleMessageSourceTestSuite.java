package com.interface21.web.context;

import com.interface21.context.AbstractApplicationContextTests;
import com.interface21.context.ApplicationContext;
import com.interface21.context.NoSuchMessageException;

import com.interface21.web.context.WebApplicationContext;
import com.interface21.web.context.support.XmlWebApplicationContext;
import com.interface21.web.mock.MockServletContext;

import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletContext;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Creates a WebApplicationContext that points to a "web.xml" file that
 * contains the entry for what file to use for the applicationContext
 * (file "com/interface21/web/context/WEB-INF/applicationContext.xml").
 * That file then has an entry for a bean called "messageSource".
 * Whatever the basename property is set to for this bean is what the name of
 * a properties file in the classpath must be (in our case the name is
 * "messages" - note no package names).
 * Thus the catalog filename will be in the root of where the classes are compiled
 * to and will be called "messages_XX_YY.properties" where "XX" and "YY" are the
 * language and country codes known by the ResourceBundle class.
 *
 * NOTE:  The main method of this class is the "createContext(...)" method, and
 * it was copied from the com.interface21.web.context.WebApplicationContextTestSuite
 * class.
 *
 * @author  Rod Johnson / Tony Falabella
 * @version
 */
public class ResourceBundleMessageSourceTestSuite
    extends AbstractApplicationContextTests {
    //~ Static variables/initializers ------------------------------------------

    /** We use ticket WAR root for file structure.
     * We don't attempt to read web.xml.
     */
    public static final String WAR_ROOT = "/com/interface21/web/context";

    //~ Instance variables -----------------------------------------------------

    ServletContext servletContext;
    private WebApplicationContext root;

    //~ Constructors -----------------------------------------------------------

    public ResourceBundleMessageSourceTestSuite(String name) {
        super(name);
    }

    //~ Methods ----------------------------------------------------------------

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());

        //	junit.swingui.TestRunner.main(new String[] {PrototypeFactoryTests.class.getName() } );
    }

    public static Test suite() {
        return new TestSuite(ResourceBundleMessageSourceTestSuite.class);
    }

    /**
     * @see com.interface21.beans.factory.AbstractListableBeanFactoryTests#testCount()
     */
    public void testCount() {
        assertTrue("should have 15 beans, not " +
                   this.applicationContext.getBeanDefinitionCount(),
                   this.applicationContext.getBeanDefinitionCount() == 16);
    }

    /**
     * Overridden as we can't trust superclass method
     * @see com.interface21.context.AbstractApplicationContextTests#testEvents()
     */
    public void testEvents() throws Exception {
        // Do nothing
    }

    /**
     * @see com.interface21.context.support.AbstractNestingMessageSource for more details.
     * NOTE:  Messages are contained within the "test/com/interface21/web/context/WEB-INF/messagesXXX.properties" files.
     */
    public void testGetMessageWithDefaultPassedInAndFoundInMsgCatalog() {
        assertTrue("valid msg from resourcebundle with default msg passed in returned default msg.  Expected msg from catalog.",
                   root.getMessage("message.format.example2", null, "This is a default msg if not found in msg.cat.", Locale.US
                   )
                       .equals("This is a test message in the message catalog with no args."));
    }

    /**
     * @see com.interface21.context.support.AbstractNestingMessageSource for more details.
     * NOTE:  Messages are contained within the "test/com/interface21/web/context/WEB-INF/messagesXXX.properties" files.
     */
    public void testGetMessageWithDefaultPassedInAndNotFoundInMsgCatalog() {
        assertTrue("bogus msg from resourcebundle with default msg passed in returned default msg",
                   root.getMessage("bogus.message", null, "This is a default msg if not found in msg.cat.", Locale.UK
                   )
                       .equals("This is a default msg if not found in msg.cat."));
    }

    /**
     * The underlying implementation uses a hashMap to cache messageFormats
     * once a message has been asked for.  This test is an attempt to
     * make sure the cache is being used properly.
     * NOTE:  Messages are contained within the "test/com/interface21/web/context/WEB-INF/messagesXXX.properties" files.
     * @see com.interface21.context.support.AbstractNestingMessageSource for more details.
     */
    public void testGetMessageWithMessageAlreadyLookedFor()
                                                   throws Exception {
        Object[] arguments = {
            new Integer(7), new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
        };


        // The first time searching, we don't care about for this test
        root.getMessage("message.format.example1", arguments, Locale.US);


        // Now msg better be as expected
        assertTrue("2nd search within MsgFormat cache returned expected message for Locale.US",
                   root.getMessage("message.format.example1", arguments, Locale.US
                   )
                       .indexOf("there was \"a disturbance in the Force\" on planet 7.") != -1);

        Object[] newArguments = {
            new Integer(8), new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
        };


        // Now msg better be as expected even with different args
        assertTrue("2nd search within MsgFormat cache with different args returned expected message for Locale.US",
                   root.getMessage("message.format.example1", newArguments, Locale.US
                   )
                       .indexOf("there was \"a disturbance in the Force\" on planet 8.") != -1);
    }

    /**
     * @see com.interface21.context.support.AbstractNestingMessageSource for more details.
     * NOTE:  Messages are contained within the "test/com/interface21/web/context/WEB-INF/messagesXXX.properties" files.
     * Example taken from the javadocs for the java.text.MessageFormat class
     */
    public void testGetMessageWithNoDefaultPassedInAndFoundInMsgCatalog()
        throws Exception {
        Object[] arguments = {
            new Integer(7), new Date(System.currentTimeMillis()),
            "a disturbance in the Force"
        };


        /*
         Try with Locale.US
         Since the msg has a time value in it, we will use String.indexOf(...)
         to just look for a substring without the time.  This is because it is
         possible that by the time we store a time variable in this method
         and the time the ResourceBundleMessageSource resolves the msg the
         minutes of the time might not be the same.
         */
        assertTrue("msg from resourcebundle for Locale.US substituting args for placeholders is as expected",
                   root.getMessage("message.format.example1", arguments, Locale.US
                   )
                       .indexOf("there was \"a disturbance in the Force\" on planet 7.") != -1);


        // Try with Locale.UK
        assertTrue("msg from resourcebundle for Locale.UK substituting args for placeholders is as expected",
                   root.getMessage("message.format.example1", arguments, Locale.UK
                   )
                       .indexOf("there was \"a disturbance in the Force\" on station number 7.") != -1);


        // Try with Locale.US - different test msg that requires no args
        assertTrue("msg from resourcebundle that requires no args for Locale.US is as expected",
                   root.getMessage("message.format.example2", null, Locale.US)
                       .equals("This is a test message in the message catalog with no args."));
    }

    /**
     * @see com.interface21.context.support.AbstractNestingMessageSource for more details.
     * NOTE:  Messages are contained within the "test/com/interface21/web/context/WEB-INF/messagesXXX.properties" files.
     */
    public void testGetMessageWithNoDefaultPassedInAndNotFoundInMsgCatalog() {
        // Expecting an exception
        try {
            root.getMessage("bogus.message", null, Locale.UK);
            fail("bogus msg from resourcebundle without default msg should have thrown exception");
        } catch (NoSuchMessageException tExcept) {
            assertTrue("bogus msg from resourcebundle without default msg threw expected exception",
                       true);
        }
    }

    protected ApplicationContext createContext() throws Exception {
        root = new XmlWebApplicationContext();

        MockServletContext sc = new MockServletContext(WAR_ROOT);

        // This is required for the framework
        sc.addInitParameter("configUrl", "/WEB-INF/applicationContext.xml");

        this.servletContext = sc;

        root.setServletContext(sc);

        WebApplicationContext wac = new XmlWebApplicationContext(root,
                                                                 "test-servlet");

        wac.setServletContext(sc);

        // Add listeners expected by parent test case
        //wac.(this.listener);
        return wac;
    }
}