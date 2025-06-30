package org.jpos.transaction.participant;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;

import java.io.Serializable;

public class Trace implements AbortParticipant, Configurable {
    String trace;

    public int prepare(long id, Serializable o) {
        Context ctx = (Context) o;
        ctx.checkPoint("prepare:" + trace);
        return PREPARED | READONLY;
    }

    public void commit(long id, Serializable o) {
        Context ctx = (Context) o;
        ctx.checkPoint("commit:" + trace);
    }

    public void abort(long id, Serializable o) {
        Context ctx = (Context) o;
        ctx.checkPoint("abort:" + trace);
    }

    public int prepareForAbort(long id, Serializable o) {
        Context ctx = (Context) o;
        ctx.checkPoint("prepareForAbort:" + trace);
        return PREPARED | READONLY;
    }

    public void setConfiguration(Configuration cfg) {
        this.trace = cfg.get("trace", this.getClass().getName());
    }
}
