package org.jpos.mux;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.logging.formatLog;

public class MuxRequestListener implements ISORequestListener {
    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        LogEvent evt = new LogEvent();
        evt.addMessage("Message không khớp với MUX, gửi từ: " + source);
        try {
            evt.addMessage("MTI: " + m.getMTI());
        } catch (ISOException e) {
            e.printStackTrace();
        }
        formatLog.log(m, evt);
        Logger.log(evt);
        return false;
    }
}
