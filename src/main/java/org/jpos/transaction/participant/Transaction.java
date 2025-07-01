
package org.jpos.transaction.participant;

import java.io.Serializable;
import org.jpos.transaction.TransactionParticipant;

public class Transaction implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        return PREPARED | NO_JOIN | READONLY;
    }

    public void commit(long id, Serializable o) {
    }

    public void abort(long id, Serializable o) {
    }
}
