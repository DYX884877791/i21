package com.interface21.transaction.jta;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.interface21.beans.factory.InitializingBean;
import com.interface21.jndi.JndiTemplate;
import com.interface21.transaction.CannotCreateTransactionException;
import com.interface21.transaction.HeuristicCompletionException;
import com.interface21.transaction.InvalidIsolationException;
import com.interface21.transaction.NestedTransactionNotPermittedException;
import com.interface21.transaction.NoTransactionException;
import com.interface21.transaction.TransactionDefinition;
import com.interface21.transaction.TransactionStatus;
import com.interface21.transaction.TransactionSystemException;
import com.interface21.transaction.UnexpectedRollbackException;
import com.interface21.transaction.support.AbstractPlatformTransactionManager;

/**
 * PlatformTransactionManager implementation for JTA.
 *
 * <p>This implementation is appropriate for handling distributed transactions,
 * i.e. transactions that span multiple resources, and for managing transactions
 * on a J2EE Connector (e.g. a persistence toolkit registered as Connector).
 * For a single JDBC DataSource, DataSourceTransactionManager is perfectly sufficient,
 * and for accessing a single resource with Hibernate (including transactional cache),
 * HibernateTransactionManager is appropriate.
 *
 * <p>Note: This implementation does not handle isolation levels. This needs
 * to be done by server-specific subclasses. DataSourceTransactionManager and
 * HibernateTransactionManager do support custom isolation levels, though.
 *
 * @author Juergen Hoeller
 * @since 24.03.2003
 * @see com.interface21.transaction.datasource.DataSourceTransactionManager
 * @see com.interface21.orm.hibernate.HibernateTransactionManager
 */
public class JtaTransactionManager extends AbstractPlatformTransactionManager implements InitializingBean {

	public static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

	//Default
	private JndiTemplate jndiTemplate = new JndiTemplate();

	private String userTransactionName;

	/**
	 * Create a new JtaTransactionManager instance.
	 */
	public JtaTransactionManager() {
	}

	/**
	 * Set the JndiTemplate to use for JNDI lookup.
	 * A default one is used if not set.
	 */
	public final void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiTemplate = jndiTemplate;
	}

	/**
	 * Set the JNDI name of the UserTransaction.
	 * The default one is used if not set.
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 */
	public void setUserTransactionName(String userTransactionName) {
		this.userTransactionName = userTransactionName;
	}

	/**
	 * Use default JndiTemplate if not set.
	 */
	public void afterPropertiesSet() {
		if (this.jndiTemplate == null) {
			this.jndiTemplate = new JndiTemplate();
		}
	}

	protected Object doGetTransaction() {
		try {
			return (UserTransaction) this.jndiTemplate.lookup(
				this.userTransactionName != null ? this.userTransactionName : DEFAULT_USER_TRANSACTION_NAME);
		}
		catch (NamingException ex) {
			throw new CannotCreateTransactionException("JTA is not available", ex);
		}
	}

	protected boolean isExistingTransaction(Object transaction) {
		try {
			return (((UserTransaction) transaction).getStatus() != Status.STATUS_NO_TRANSACTION);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * This standard JTA implementation simply ignores the isolation level.
	 * To be overridden by server-specific subclasses that actually handle
	 * the isolation level.
	 */
	protected void doBegin(Object transaction, int isolationLevel, int timeout) {
		if (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationException("JtaTransactionManager does not support custom isolation levels");
		}
		logger.debug("Beginning JTA transaction");
		UserTransaction ut = (UserTransaction) transaction;
		try {
			if (timeout >= 0) {
				ut.setTransactionTimeout(timeout);
			}
			ut.begin();
		}
		catch (NotSupportedException ex) {
			// assume "nested transactions not supported"
			throw new NestedTransactionNotPermittedException(
				"JTA implementation does not support nested transactions",
				ex);
		}
		catch (UnsupportedOperationException ex) {
			// assume "nested transactions not supported"
			throw new NestedTransactionNotPermittedException(
				"JTA implementation does not support nested transactions",
				ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on begin", ex);
		}
	}

	protected void doCommit(TransactionStatus status) {
		logger.debug("Committing JTA transaction");
		try {
			((UserTransaction) status.getTransaction()).commit();
		}
		catch (RollbackException ex) {
			throw new UnexpectedRollbackException("JTA transaction rolled back", ex);
		}
		catch (HeuristicMixedException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, ex);
		}
		catch (HeuristicRollbackException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on commit", ex);
		}
	}

	protected void doRollback(TransactionStatus status) {
		logger.debug("Rolling back JTA transaction");
		try {
			((UserTransaction) status.getTransaction()).rollback();
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on rollback", ex);
		}
	}

	protected void doSetRollbackOnly(TransactionStatus status) {
		logger.debug("Setting JTA transaction rollback-only");
		try {
			((UserTransaction) status.getTransaction()).setRollbackOnly();
		}
		catch (IllegalStateException ex) {
			throw new NoTransactionException("No active JTA transaction");
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on setRollbackOnly", ex);
		}
	}

}
