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
import org.slf4j.LoggerFactory;
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
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SelectDestination.class);

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
        if (mux == null) {
            LOGGER.warn("MUX '{}' not found in NameRegistrar", muxName);
            return ABORTED;
        }

        ISOMsg m = ctx.get(requestName);
        if (m == null) {
            LOGGER.warn("Request '%s' is null in Context", requestName);
            return result.fail(CMF.INVALID_REQUEST, Caller.info(), "'%s' is null", requestName).FAIL();
        }

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
                    LOGGER.warn("MUX '{}' did not respond, but ignoring as per configuration", muxName);
                    ctx.log(String.format("MUX '%s' no response", muxName));
                } else {
                    LOGGER.warn("MUX '{}' did not respond", muxName);
                    return ABORTED;
                }
            } catch (ISOException e) {
                LOGGER.error("ISOException while querying MUX '{}': {}", muxName, e.getMessage(), e);
                return ABORTED;
            }
        } else if (ignoreUnreachable) {
            LOGGER.warn("MUX '{}' not connected, but ignoring as per configuration", muxName);
            ctx.log(String.format("MUX '%s' not connected", muxName));
        } else {
            LOGGER.warn("MUX '{}' is not connected", muxName);
            return ABORTED;
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
