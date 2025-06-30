
package org.jpos.transaction.participant;

import java.io.Serializable;
import org.jpos.transaction.TransactionConstants;

public interface TransactionParticipant extends TransactionConstants {
    /**
     * Called by TransactionManager in preparation for a transaction
     *
     * @param id      the Transaction identifier
     * @param context transaction context
     * @return PREPARED or ABORTED (| NO_JOIN | READONLY)
     */
    int prepare(long id, Serializable context);

    /**
     * Called by TransactionManager upon transaction commit.
     * Warning: implementation should be able to handle multiple calls
     * with the same transaction id (rare crash recovery)
     *
     * @param id      the Transaction identifier
     * @param context transaction context
     */
    default void commit(long id, Serializable context) {
    }

    /**
     * Called by TransactionManager upon transaction commit.
     * Warning: implementation should be able to handle multiple calls
     * with the same transaction id (rare crash recovery)
     *
     * @param id      the Transaction identifier
     * @param context transaction context
     */
    default void abort(long id, Serializable context) {
    }
}
