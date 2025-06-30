package org.jpos.iso;

import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceSource;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.Log;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.transaction.Context;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class IncomingListener extends Log implements ISORequestListener, Configurable {
    long timeout;
    private Space<String, Context> sp;
    private String queue;
    private String source;
    private String request;
    private String timestamp;
    private Map<String, String> additionalContextEntries = null;
    private boolean remote = false;

    @SuppressWarnings("unchecked")
    public void setConfiguration(Configuration cfg)
            throws ConfigurationException {
        timeout = cfg.getLong("timeout", 15000L);
        sp = (Space<String, Context>) SpaceFactory.getSpace(cfg.get("space"));
        queue = cfg.get("queue", null);
        if (queue == null)
            throw new ConfigurationException("queue property not specified");
        source = cfg.get("source", ContextConstants.SOURCE.toString());
        request = cfg.get("request", ContextConstants.REQUEST.toString());
        timestamp = cfg.get("timestamp", ContextConstants.TIMESTAMP.toString());
        remote = cfg.getBoolean("remote") || cfg.get("space").startsWith("rspace:");
        Map<String, String> m = new HashMap<>();
        cfg.keySet()
                .stream()
                .filter(s -> s.startsWith("ctx."))
                .forEach(s -> m.put(s.substring(4), cfg.get(s)));
        if (m.size() > 0)
            additionalContextEntries = m;
    }

    public boolean process(ISOSource src, ISOMsg m) {
        final Context ctx = new Context();
        if (remote)
            src = new SpaceSource((LocalSpace) sp, src, timeout);
        ctx.put(timestamp, new Date(), remote);
        ctx.put(source, src, remote);
        ctx.put(request, m, remote);
        if (additionalContextEntries != null) {
            additionalContextEntries.entrySet().forEach(
                    e -> ctx.put(e.getKey(), e.getValue(), remote));
        }
        sp.out(queue, ctx, timeout);
        return true;
    }
}
