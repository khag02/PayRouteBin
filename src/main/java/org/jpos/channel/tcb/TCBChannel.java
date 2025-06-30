package org.jpos.channel.tcb;

import java.io.IOException;
import java.net.ServerSocket;

import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.channel.NCCChannel;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class TCBChannel extends NCCChannel {
    /**
     * Public constructor
     */
    boolean tpduSwap = true;

    public TCBChannel() {
        super();
    }

    /**
     * Construct client ISOChannel
     *
     * @param host server TCP Address
     * @param port server port number
     * @param p    an ISOPackager
     * @param TPDU an optional raw header (i.e. TPDU)
     * @see ISOPackager
     */
    public TCBChannel(String host, int port, ISOPackager p, byte[] TPDU) {
        this.header = TPDU;
    }

    /**
     * Construct server ISOChannel
     *
     * @param p    an ISOPackager
     * @param TPDU an optional raw header (i.e. TPDU)
     * @exception IOException
     * @see ISOPackager
     */
    public TCBChannel(ISOPackager p, byte[] TPDU) throws IOException {
        this.header = TPDU;
    }

    /**
     * constructs server ISOChannel associated with a Server Socket
     *
     * @param p            an ISOPackager
     * @param TPDU         an optional raw header (i.e. TPDU)
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see ISOPackager
     */
    public TCBChannel(ISOPackager p, byte[] TPDU, ServerSocket serverSocket)
            throws IOException {
        this.header = TPDU;
    }

    protected void sendMessageLength(int len) throws IOException {
        try {
            serverOut.write(
                    ISOUtil.str2bcd(
                            ISOUtil.zeropad(Integer.toString(len % 10000), 4), true));
        } catch (ISOException e) {
            Logger.log(new LogEvent(this, "send-message-length", e));
        }
    }

    protected int getMessageLength() throws IOException, ISOException {
        byte[] b = new byte[2];
        serverIn.readFully(b, 0, 2);
        return Integer.parseInt(
                ISOUtil.bcd2str(b, 0, 4, true));
    }

    protected void sendMessageHeader(ISOMsg m, int len) throws IOException {
        byte[] h = m.getHeader();
        if (h != null) {
            if (tpduSwap && h.length == 5) {
                // swap src/dest address
                byte[] tmp = new byte[2];
                System.arraycopy(h, 1, tmp, 0, 2);
                System.arraycopy(h, 3, h, 1, 2);
                System.arraycopy(tmp, 0, h, 3, 2);
            }
        } else
            h = header;
        if (h != null)
            serverOut.write(h);
    }

    /**
     * New QSP compatible signature (see QSP's ConfigChannel)
     *
     * @param header String as seen by QSP
     */
    public void setHeader(String header) {
        super.setHeader(ISOUtil.str2bcd(header, false));
    }

    public void setConfiguration(Configuration cfg)
            throws ConfigurationException {
        super.setConfiguration(cfg);
        tpduSwap = cfg.getBoolean("tpdu-swap", true);
    }
}
