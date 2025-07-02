package org.jpos.logger;

import org.jdom2.Element;
import org.jpos.core.ConfigurationException;
import org.jpos.q2.QBeanSupport;
import org.jpos.q2.QFactory;
import org.jpos.util.*;

import java.io.IOException;
import java.io.PrintStream;

public class LoggerAdaptor extends QBeanSupport {
    private Logger logger;
    private PrintStream originalOut = null;
    private PrintStream originalErr = null;

    protected void initService() {
        logger = Logger.getLogger(getName());
    }

    protected void startService() throws ConfigurationException, IOException {
        logger.removeAllListeners();
        for (Object o : getPersist().getChildren("log-listener"))
            addListener((Element) o);

        String redirect = cfg.get("redirect");
        long delay = cfg.getLong("delay", 500);

        if (redirect.contains("stdout")) {
            originalOut = System.out;
            System.setOut(new PrintStream(new LogEventOutputStream(logger, "stdout", delay)));
        }
        if (redirect.contains("stderr")) {
            originalErr = System.err;
            System.setErr(new PrintStream(new LogEventOutputStream(logger, "stderr", delay)));
        }
    }

    protected void stopService() {
        if (originalOut != null)
            System.setOut(originalOut);
        if (originalErr != null)
            System.setErr(originalErr);
        logger.removeAllListeners();
    }

    protected void destroyService() {
        // we don't destroy (that would unregister the logger from the
        // NameRegistrar) because other components might have references
        // to this logger.
        //
        // logger.destroy ();
    }

    private void addListener(Element e)
            throws ConfigurationException {
        QFactory factory = getServer().getFactory();
        if (QFactory.isEnabled(e)) {
            String clazz = e.getAttributeValue("class");
            LogListener listener = factory.newInstance(clazz);
            factory.setConfiguration(listener, e);
            attemptToAddWriter(e.getChild("writer"), listener);
            logger.addListener(listener);
        }
    }

    private void attemptToAddWriter(Element e, LogListener listener) throws ConfigurationException {
        if (e != null) {
            QFactory factory = getServer().getFactory();
            if (QFactory.isEnabled(e)) {
                String clazz = e.getAttributeValue("class");
                LogEventWriter writer = factory.newInstance(clazz);
                factory.setConfiguration(writer, e);
                listener.setLogEventWriter(writer);
            }
        }
    }
}
