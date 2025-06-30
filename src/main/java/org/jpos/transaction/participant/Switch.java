package org.jpos.transaction.participant;

import java.io.Serializable;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.transaction.Context;
import org.jpos.transaction.GroupSelector;
import static org.jpos.transaction.ContextConstants.TXNNAME;

@SuppressWarnings("unused")
public class Switch implements Configurable, GroupSelector {
    private Configuration cfg;
    private String txnNameEntry;

    public String select(long id, Serializable ser) {
        Context ctx = (Context) ser;
        String type = ctx.getString(txnNameEntry);
        String groups = null;
        if (type != null)
            groups = cfg.get(type, null);
        if (groups == null)
            groups = cfg.get("unknown", "");
        ctx.log("SWITCH " + type + " (" + groups + ")");

        return groups;
    }

    public int prepare(long id, Serializable o) {
        return PREPARED | READONLY | NO_JOIN;
    }

    public void setConfiguration(Configuration cfg) {
        this.cfg = cfg;
        txnNameEntry = cfg.get("txnname", TXNNAME.toString());
    }
}
