package com.interface21.orm.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;

import com.interface21.transaction.CannotCreateTransactionException;
import com.interface21.transaction.TransactionException;
import com.interface21.transaction.TransactionStatus;
import com.interface21.transaction.TransactionSystemException;
import com.interface21.transaction.support.AbstractPlatformTransactionManager;

/**
 * PlatformTransactionManager implementation for single Hibernate session
 * factories. Binds a Hibernate Session from the specified factory to the
 * thread, potentially allowing for one thread Session per factory.
 *
 * <p>SessionFactoryUtils.openSession and HibernateTemplate.execute are
 * aware of thread-bound Sessions and take part in such transactions
 * automatically. This is required for proper Hibernate access code
 * supporting this transaction handling mechanism.
 *
 * <p>This implementation is appropriate for applications that solely use
 * Hibernate for transactional data access. It allows for proper Hibernate
 * transactional cache handling, which is not the case when using
 * DataSourceTransactionManager with Hibernate. The advantage of the latter
 * is that it allows for direct data source access within a transaction too.
 *
 * <p>Note: JTA resp. JtaTransactionManager is preferable for accessing
 * multiple transactional resources, but it is significantly harder to
 * setup Hibernate including transactional caching for JTA than for
 * this transaction manager. Normally, Hibernate JTA setup is somewhat
 * container-specific due to the JTA TransactionManager lookup. Using
 * its J2EE Connector is advisable but involves classloading issues.
 *
 * Note: This class, like all of Spring's Hibernate support, requires
 * Hibernate 2.0 (initially developed with RC1).
 * 
 * @author Juergen Hoeller
 * @since 02.05.2003
 * @see SessionFactoryUtils#openSession
 * @see HibernateTemplate#execute
 * @see com.interface21.transaction.support.DataSourceTransactionManager
 */
public class HibernateTransactionManager extends AbstractPlatformTransactionManager {

	private SessionFactory sessionFactory;

	/**
	 * Create a new HibernateTransactionManager instance.
	 */
	public HibernateTransactionManager() {
	}

	/**
	 * Create a new HibernateTransactionManager instance.
	 * @param sessionFactoryName name of the SessionFactory to manage transactions for
	 */
	public HibernateTransactionManager(String sessionFactoryName) {
		setSessionFactoryName(sessionFactoryName);
	}

	/**
	 * Create a new HibernateTransactionManager instance.
	 * @param sessionFactory SessionFactory to manage transactions for
	 */
	public HibernateTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Set the JNDI name of the SessionFactory that this instance should manage
	 * transactions for.
	 */
	public void setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactory = SessionFactoryUtils.getSessionFactoryFromJndi(sessionFactoryName);
	}

	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the SessionFactory that this instance should manage transactions for.
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	protected Object doGetTransaction() throws CannotCreateTransactionException, TransactionException {
		if (SessionFactoryUtils.getThreadObjectManager().hasThreadObject(this.sessionFactory)) {
			SessionHolder sessionHolder = (SessionHolder) SessionFactoryUtils.getThreadObjectManager().getThreadObject(this.sessionFactory);
			return new HibernateTransactionObject(sessionHolder);
		}
		else {
			SessionHolder sessionHolder = new SessionHolder(SessionFactoryUtils.openSession(this.sessionFactory));
			return new HibernateTransactionObject(sessionHolder);
		}
	}

	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return SessionFactoryUtils.getThreadObjectManager().hasThreadObject(this.sessionFactory);
	}

	protected void doBegin(Object transaction, int isolationLevel) throws TransactionException {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		logger.debug("Beginning Hibernate transaction");
		try {
			Session session = txObject.getSessionHolder().getSession();
			if (isolationLevel != ISOLATION_DEFAULT) {
				logger.debug("Changing isolation level to " + isolationLevel);
				txObject.setPreviousIsolationLevel(new Integer(session.connection().getTransactionIsolation()));
				session.connection().setTransactionIsolation(isolationLevel);
			}
			txObject.getSessionHolder().setTransaction(session.beginTransaction());
		}
		catch (SQLException ex) {
			throw new CannotCreateTransactionException("Cannot set transaction isolation", ex);
		}
		catch (net.sf.hibernate.TransactionException ex) {
			throw new TransactionSystemException("Cannot create Hibernate transaction", ex.getCause());
		}
		catch (HibernateException ex) {
			throw new CannotCreateTransactionException("Cannot create Hibernate transaction", ex);
		}
		SessionFactoryUtils.getThreadObjectManager().bindThreadObject(this.sessionFactory, txObject.getSessionHolder());
	}

	protected void doCommit(TransactionStatus status) throws TransactionException {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (txObject.getSessionHolder().isRollbackOnly()) {
			// nested Hibernate transaction demanded rollback-only
			doRollback(status);
		}
		else {
			logger.debug("Committing Hibernate transaction");
			try {
				txObject.getSessionHolder().getTransaction().commit();
			}
			catch (net.sf.hibernate.TransactionException ex) {
				throw new TransactionSystemException("Cannot commit Hibernate transaction", ex.getCause());
			}
			catch (HibernateException ex) {
				throw new TransactionSystemException("Cannot commit Hibernate transaction", ex);
			}
			finally {
				closeSession(txObject);
			}
		}
	}

	protected void doRollback(TransactionStatus status) throws TransactionException {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		logger.debug("Rolling back Hibernate transaction");
		try {
			txObject.getSessionHolder().getTransaction().rollback();
		}
		catch (net.sf.hibernate.TransactionException ex) {
			throw new TransactionSystemException("Cannot rollback Hibernate transaction", ex.getCause());
		}
		catch (HibernateException ex) {
			throw new TransactionSystemException("Cannot rollback Hibernate transaction", ex);
		}
		finally {
			closeSession(txObject);
		}
	}

	protected void doSetRollbackOnly(TransactionStatus status) throws TransactionException {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		logger.debug("Setting Hibernate transaction rollback-only");
		txObject.getSessionHolder().setRollbackOnly();
	}

	private void closeSession(HibernateTransactionObject txObject) {
		SessionFactoryUtils.getThreadObjectManager().removeThreadObject(this.sessionFactory);
		try {
			// reset transaction isolation to previous value, if changed for the transaction
			if (txObject.getPreviousIsolationLevel() != null) {
				logger.debug("Resetting isolation level to " + txObject.getPreviousIsolationLevel());
				Connection con = txObject.getSessionHolder().getSession().connection();
				con.setTransactionIsolation(txObject.getPreviousIsolationLevel().intValue());
			}
		}
		catch (HibernateException ex) {
			logger.warn("Cannot reset transaction isolation", ex);
		}
		catch (SQLException ex) {
			logger.warn("Cannot reset transaction isolation", ex);
		}
		SessionFactoryUtils.closeSessionIfNecessary(txObject.getSessionHolder().getSession(), this.sessionFactory);
	}
	
}
