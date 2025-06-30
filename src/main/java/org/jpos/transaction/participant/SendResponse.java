package org.jpos.transaction.participant;

import java.io.Serializable;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceSource;
import org.jpos.transaction.Context;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.TransactionManager;

import static org.jpos.transaction.ContextConstants.*;

@SuppressWarnings("rawtypes")
public class SendResponse implements AbortParticipant, Configurable {
    private String source;
    private String request;
    private String response;
    private LocalSpace isp;
    private long timeout = 70000L;
    private HeaderStrategy headerStrategy;

    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOSource source = (ISOSource) ctx.get(this.source);
        if (source == null || !source.isConnected())
            return ABORTED | READONLY | NO_JOIN;

        return PREPARED | READONLY;
    }

    public void commit(long id, Serializable context) {
        sendResponse(id, (Context) context);
    }

    public void abort(long id, Serializable context) {
        sendResponse(id, (Context) context);
    }

    private void sendResponse(long id, Context ctx) {
        ISOSource src = (ISOSource) ctx.get(source);
        ISOMsg m = (ISOMsg) ctx.get(request);
        ISOMsg resp = (ISOMsg) ctx.get(response);
        try {
            if (ctx.getResult().hasInhibit()) {
                ctx.log("*** RESPONSE INHIBITED ***");
            } else if (ctx.get(TX.toString()) != null) {
                ctx.log("*** PANIC - TX not null - RESPONSE OMITTED ***");
            } else if (resp == null) {
                ctx.log(response + " not present");
            } else if (src == null) {
                ctx.log(source + " not present");
            } else if (!src.isConnected())
                ctx.log(source + " is no longer connected");
            else {
                if (src instanceof SpaceSource)
                    ((SpaceSource) src).init(isp, timeout);
                if (src.isConnected() && resp != null) {
                    headerStrategy.handleHeader(m, resp);
                    src.send(resp);
                }
            }
        } catch (Throwable t) {
            ctx.log(t);
        }
    }

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        source = cfg.get("source", SOURCE.toString());
        request = cfg.get("request", REQUEST.toString());
        response = cfg.get("response", RESPONSE.toString());
        timeout = cfg.getLong("timeout", timeout);
        try {
            headerStrategy = HeaderStrategy.valueOf(
                    cfg.get("header-strategy", "PRESERVE_RESPONSE").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public void setTransactionManager(TransactionManager tm) {
        isp = (LocalSpace) tm.getInputSpace();
    }

    private interface HeaderHandler {
        void handleHeader(ISOMsg m, ISOMsg r);
    }

    public enum HeaderStrategy implements HeaderHandler {
        PRESERVE_ORIGINAL() {
            @Override
            public void handleHeader(ISOMsg m, ISOMsg r) {
                r.setHeader(m.getHeader());
            }
        },
        PRESERVE_RESPONSE() {
            @Override
            public void handleHeader(ISOMsg m, ISOMsg r) {
            }
        },
        SET_TO_NULL() {
            @Override
            public void handleHeader(ISOMsg m, ISOMsg r) {
                r.setHeader((byte[]) null);
            }
        }
    }
}
