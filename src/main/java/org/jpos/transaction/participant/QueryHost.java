package org.jpos.transaction.participant;

import java.io.Serializable;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.*;
import org.jpos.rc.CMF;
import org.jpos.rc.Result;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Caller;
import org.jpos.util.Chronometer;
import org.jpos.util.NameRegistrar;
import org.jpos.transaction.Context;

public class QueryHost implements TransactionParticipant, Configurable {
    public static final String TIMEOUT_NAME = "QUERYHOST_TIMEOUT";

    private static final long DEFAULT_TIMEOUT = 30000L;
    private static final long DEFAULT_WAIT_TIMEOUT = 1000L;

    private long timeout;
    private long waitTimeout;
    private String timeoutName = TIMEOUT_NAME; // default ctx name
    private String requestName;
    private String responseName;
    private String destination;
    private Configuration cfg;
    private boolean ignoreUnreachable;
    private boolean checkConnected = true;

    public QueryHost() {
        super();
    }

    public int prepare(long id, Serializable ser) {
        Context ctx = (Context) ser;

        Result result = ctx.getResult();
        String ds = ctx.getString(destination);
        if (ds == null) {
            return result.fail(
                    CMF.MISCONFIGURED_ENDPOINT, Caller.info(), "'%s' not present in Context", destination).FAIL();
        }
        String muxName = cfg.get("mux." + ds, "mux." + ds);
        MUX mux = NameRegistrar.getIfExists(muxName);
        if (mux == null)
            return result.fail(CMF.MISCONFIGURED_ENDPOINT, Caller.info(), "MUX '%s' not found", muxName).FAIL();

        ISOMsg m = ctx.get(requestName);
        if (m == null)
            return result.fail(CMF.INVALID_REQUEST, Caller.info(), "'%s' is null", requestName).FAIL();

        Chronometer chronometer = new Chronometer();
        if (isConnected(mux)) {
            long t = Math.max(resolveTimeout(ctx) - chronometer.elapsed(), 1000L); // give at least a second to catch a
                                                                                   // response
            try {
                ISOMsg resp = mux.request(m, t);
                if (resp != null) {
                    ctx.put(responseName, resp);
                    return PREPARED | READONLY | NO_JOIN;
                } else if (ignoreUnreachable) {
                    ctx.log(String.format("MUX '%s' no response", muxName));
                } else {
                    return result.fail(CMF.HOST_UNREACHABLE, Caller.info(), "'%s' does not respond", muxName).FAIL();
                }
            } catch (ISOException e) {
                return result.fail(CMF.SYSTEM_ERROR, Caller.info(), e.getMessage()).FAIL();
            }
        } else if (ignoreUnreachable) {
            ctx.log(String.format("MUX '%s' not connected", muxName));
        } else {
            return result.fail(CMF.HOST_UNREACHABLE, Caller.info(), "'%s' is not connected", muxName).FAIL();
        }
        return PREPARED | NO_JOIN | READONLY;
    }

    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
        timeout = cfg.getLong("timeout", DEFAULT_TIMEOUT);
        waitTimeout = cfg.getLong("wait-timeout", DEFAULT_WAIT_TIMEOUT);
        timeoutName = cfg.get("timeout-name", timeoutName);
        requestName = cfg.get("request", ContextConstants.REQUEST.toString());
        responseName = cfg.get("response", ContextConstants.RESPONSE.toString());
        destination = cfg.get("destination", ContextConstants.DESTINATION.toString());
        ignoreUnreachable = cfg.getBoolean("ignore-host-unreachable", false);
        checkConnected = cfg.getBoolean("check-connected", checkConnected);
    }

    protected long resolveTimeout(Context ctx) {
        Object o = ctx.get(timeoutName);
        if (o == null)
            return timeout;
        else if (o instanceof Number)
            return ((Number) o).longValue();
        else
            return Long.parseLong(o.toString());
    }

    protected boolean isConnected(MUX mux) {
        if (!checkConnected || mux.isConnected())
            return true;
        long timeout = System.currentTimeMillis() + waitTimeout;
        while (System.currentTimeMillis() < timeout) {
            if (mux.isConnected())
                return true;
            ISOUtil.sleep(500);
        }
        return false;
    }
}
