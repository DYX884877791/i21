package com.interface21.transaction.support;

import org.apache.log4j.Logger;

import com.interface21.transaction.CannotCreateTransactionException;
import com.interface21.transaction.NoTransactionException;
import com.interface21.transaction.PlatformTransactionManager;
import com.interface21.transaction.TransactionException;
import com.interface21.transaction.TransactionStatus;

/**
 * Abstract class that allows for easy implementation of PlatformTransactionManager.
 * Provides the following case handling:
 * <ul>
 * <li>determines if there is an existing transaction;
 * <li>applies the appropriate propagation behavior;
 * <li>supports falling back to non-transactional execution;
 * <li>determines programmatic rollback on commit;
 * <li>applies the appropriate modification on rollback
 * (actual rollback or setting rollback only).
 * </ul>
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 */
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager {

	protected final Logger logger = Logger.getLogger(getClass());

	/**
	 * if transaction support needs to be available (else fallback behavior is enabled)
	 */
	private boolean allowNonTransactionalExecution = false;

	/**
	 * Set if transaction support needs to be available
	 * (else fallback behavior is enabled).
	 */
	public final void setAllowNonTransactionalExecution(boolean allowNonTransactionalExecution) {
		this.allowNonTransactionalExecution = allowNonTransactionalExecution;
	}

	/**
	 * Return if transaction support needs to be available
	 * (else fallback behavior is enabled).
	 */
	public final boolean getAllowNonTransactionalExecution() {
		return allowNonTransactionalExecution;
	}

	/**
	 * This implementation of getTransaction handles propagation behavior and
	 * checks non-transactional execution (on CannotCreateTransactionException).
	 * Delegates to doGetTransaction, isExistingTransaction, doBegin.
	 */
	public final TransactionStatus getTransaction(int propagationBehavior, int isolationLevel)
	    throws TransactionException {
		try {
			Object transaction = doGetTransaction();
			if (isExistingTransaction(transaction)) {
				logger.debug("Taking part in existing transaction");
				return new TransactionStatus(transaction, false);
			}
			if (propagationBehavior == PROPAGATION_MANDATORY) {
				throw new NoTransactionException("Transaction propagation mandatory but no existing transaction context");
			}
			if (propagationBehavior == PROPAGATION_REQUIRED) {
				// create new transaction
				doBegin(transaction, isolationLevel);
				return new TransactionStatus(transaction, true);
			}
		}
		catch (CannotCreateTransactionException ex) {
			// throw exception if transactional execution required
			if (!this.allowNonTransactionalExecution) {
				logger.error(ex.getMessage());
				throw ex;
			}
			// else non-transactional execution
			logger.warn("Transaction support is not available: falling back to non-transactional execution", ex);
		}
		catch (TransactionException ex) {
			logger.error(ex.getMessage());
			throw ex;
		}
		// empty (-> "no") transaction
		return new TransactionStatus(null, false);
	}

	/**
	 * This implementation of commit handles programmatic rollback requests,
	 * i.e. status.isRollbackOnly(), and non-transactional execution.
	 * Delegates to doCommit and rollback.
	 */
	public final void commit(TransactionStatus status) throws TransactionException {
		try {
			if (status.isRollbackOnly()) {
				logger.debug("Transactional code has requested rollback");
				rollback(status);
			}
			else if (status.isNewTransaction()) {
				doCommit(status);
			}
		}
		catch (TransactionException ex) {
			logger.error(ex.getMessage());
			throw ex;
		}
	}

	/**
	 * This implementation of rollback handles taking part in existing transactions
	 * and non-transactional execution. Delegates to doRollback and doSetRollbackOnly.
	 */
	public final void rollback(TransactionStatus status) throws TransactionException {
		try {
			if (status.isNewTransaction()) {
				doRollback(status);
			} else if (status.getTransaction() != null) {
				doSetRollbackOnly(status);
			} else {
				// no transaction support available
				logger.info("Should roll back transaction but cannot - no transaction support available");
			}
		}
		catch (TransactionException ex) {
			logger.error(ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Return a current transaction object, i.e. a JTA UserTransaction.
	 * @return the current transaction object
	 * @throws CannotCreateTransactionException if transaction support is
	 * not available (e.g. no JTA UserTransaction retrievable from JNDI)
	 * @throws TransactionException in case of lookup or system errors
	 */
	protected abstract Object doGetTransaction() throws CannotCreateTransactionException, TransactionException;

	/**
	 * Check if the given transaction object indicates an existing,
	 * i.e. already begun, transaction.
	 * @param transaction transaction object returned by doGetTransaction()
	 * @return if there is an existing transaction
	 * @throws TransactionException in case of system errors
	 */
	protected abstract boolean isExistingTransaction(Object transaction) throws TransactionException;

	/**
	 * Begin a new transaction with the given isolation level.
	 * @param transaction transaction object returned by doGetTransaction()
	 * @param isolationLevel desired isolation level
	 * @throws TransactionException in case of creation or system errors
	 */
	protected abstract void doBegin(Object transaction, int isolationLevel) throws TransactionException;

	/**
	 * Perform an actual commit on the given transaction.
	 * An implementation does not need to check the rollback-only flag.
	 * @param status status representation of the transaction
	 * @throws TransactionException in case of commit or system errors
	 */
	protected abstract void doCommit(TransactionStatus status) throws TransactionException;

	/**
	 * Perform an actual rollback on the given transaction.
	 * An implementation does not need to check the new transaction flag.
	 * @param status status representation of the transaction
	 * @throws TransactionException in case of system errors
	 */
	protected abstract void doRollback(TransactionStatus status) throws TransactionException;

	/**
	 * Set the given transaction rollback-only. Only called on rollback
	 * if the current transaction takes part in an existing one.
	 * @param status status representation of the transaction
	 * @throws TransactionException in case of system errors
	 */
	protected abstract void doSetRollbackOnly(TransactionStatus status) throws TransactionException;

}
