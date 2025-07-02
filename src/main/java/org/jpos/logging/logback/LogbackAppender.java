package org.jpos.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class LogbackAppender extends AppenderBase<ILoggingEvent> {
    Logger logger;

    @Override
    public void start() {
        logger = Logger.getLogger(getName());
        super.start();
    }

    @Override
    protected void append(final ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        // Build
        final Log source = new Log(logger, getName() + "/" + event.getLoggerName());
        final LogEvent ev = new LogEvent(source,
                event.getLevel().levelStr.toLowerCase(),
                event.getFormattedMessage());

        // Handle exceptions
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            ev.addMessage(ThrowableProxyUtil.asString(tp));
        }

        // Do the actual logging
        Logger.log(ev);
    }
}
