package org.jpos.channel.tcb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.jpos.channel.base.BaseChannel;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFilter;
import org.jpos.iso.ISOFilter.VetoException;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.jfr.ChannelEvent;
import org.jpos.log.evt.Disconnect;
import org.jpos.util.Caller;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import io.micrometer.core.instrument.Counter;

/**
 * [4 BYTE ASCII LENGTH]
 * [MTI (4 bytes)]
 * [BITMAP (8 or 16 bytes)]
 * [DATA ELEMENTS...]
 */
public class TCBChannel extends BaseChannel {
    /**
     * Public constructor
     */
    boolean tpduSwap = true;
    private Socket socket;
    private boolean expectKeepAlive;
    private Counter msgOutCounter;
    private Counter msgInCounter;

    public TCBChannel() {
        super();
        setHost(null, 0);
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

    /**
     * sends an ISOMsg over the TCP/IP session
     *
     * @param m the Message to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException;
     */
    public void send(ISOMsg m)
            throws IOException, ISOException {
        ChannelEvent jfr = new ChannelEvent.Send();
        jfr.begin();
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected())
                throw new IOException("unconnected ISOChannel");
            m.setDirection(ISOMsg.OUTGOING);
            GenericPackager p = new GenericPackager("cfg/napas.xml");
            m.setPackager(p);
            m = applyOutgoingFilters(m, evt);
            evt.addMessage(m);
            m.setDirection(ISOMsg.OUTGOING); // filter may have dropped this info
            m.setPackager(p); // and could have dropped packager as well
            byte[] b = pack(m);
            serverOutLock.lock();
            try {
                sendMessageLength(b.length + getHeaderLength(m));
                sendMessageHeader(m, b.length);
                sendMessage(b, 0, b.length);
                sendMessageTrailer(m, b);
                serverOut.flush();
            } finally {
                serverOutLock.unlock();
            }
            cnt[TX]++;
            if (msgOutCounter != null)
                msgOutCounter.increment();
            setChanged();
            notifyObservers(m);
            jfr.setDetail(m.toString());
        } catch (VetoException e) {
            // if a filter vets the message it was not added to the event
            evt.addMessage(m);
            evt.addMessage(e);
            jfr.append(e.getMessage());
            throw e;
        } catch (ISOException | IOException e) {
            evt.addMessage(e);
            jfr = new ChannelEvent.SendException(e.getMessage());
            throw e;
        } catch (Exception e) {
            evt.addMessage(e);
            jfr = new ChannelEvent.SendException(e.getMessage());
            throw new IOException("unexpected exception", e);
        } finally {
            Logger.log(evt);
            jfr.commit();
        }
    }

    /**
     * Waits and receive an ISOMsg over the TCP/IP session
     *
     * @return the Message received
     * @throws IOException
     * @throws ISOException
     */
    public ISOMsg receive() throws IOException, ISOException {
        var jfr = new ChannelEvent.Receive();
        jfr.begin();

        byte[] b = null;
        byte[] header = null;
        LogEvent evt = new LogEvent(this, "receive");
        ISOMsg m = createMsg(); // call createMsg instead of createISOMsg for
                                // backward compatibility
        m.setSource(this);
        try {
            if (!isConnected())
                throw new IOException("unconnected ISOChannel");

            serverInLock.lock();
            try {
                int len = getMessageLength();
                if (expectKeepAlive) {
                    while (len == 0) {
                        // If zero length, this is a keep alive msg
                        len = getMessageLength();
                    }
                }
                int hLen = 5;

                if (len == -1) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                    }
                    b = streamReceive();
                } else if (len > 0 && len <= getMaxPacketLength()) {
                    if (hLen > 0) {
                        // ignore message header (TPDU)
                        // Note header length is not necessarily equal to hLen (see VAPChannel)
                        header = readHeader(hLen);
                        len -= header.length;
                    }
                    b = new byte[len];
                    getMessage(b, 0, len);
                    getMessageTrailer(m);
                } else
                    throw new ISOException(
                            "receive length " + len + " seems strange - maxPacketLength = " + getMaxPacketLength());
            } finally {
                serverInLock.unlock();
            }
            GenericPackager p = new GenericPackager("cfg/napas.xml");
            m.setPackager(p);
            m.setHeader(header);
            if (b.length > 0 && !shouldIgnore(header))
                unpack(m, b);
            m.setDirection(ISOMsg.INCOMING);
            evt.addMessage(m);
            m = applyIncomingFilters(m, header, b, evt);
            m.setDirection(ISOMsg.INCOMING);
            cnt[RX]++;
            if (msgInCounter != null) {
                msgInCounter.increment();
            }
            setChanged();
            notifyObservers(m);
        } catch (ISOException e) {
            evt.addMessage(e);
            if (header != null) {
                evt.addMessage("--- header ---");
                evt.addMessage(ISOUtil.hexdump(header));
            }
            if (b != null) {
                evt.addMessage("--- data ---");
                evt.addMessage(ISOUtil.hexdump(b));
            }
            throw e;
        } catch (IOException e) {
            evt.addMessage(
                    new Disconnect(socket.getInetAddress().getHostAddress(), socket.getPort(), socket.getLocalPort(),
                            "%s (%s)".formatted(Caller.shortClassName(e.getClass().getName()), Caller.info()),
                            e.getMessage()));
            closeSocket();
            throw e;
        } catch (Exception e) {
            closeSocket();
            evt.addMessage(m);
            evt.addMessage(e);
            throw new IOException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
        jfr.setDetail(m.toString());
        jfr.commit();
        return m;
    }

    @Override
    protected void sendMessageLength(int len) throws IOException {
        String lenStr = String.format("%04d", len);
        serverOut.write(lenStr.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    protected int getMessageLength() throws IOException {
        byte[] b = new byte[4];
        serverIn.readFully(b);
        return Integer.parseInt(new String(b, StandardCharsets.US_ASCII));
    }

    protected byte[] readHeader(int hLen) throws IOException {
        byte[] header = new byte[hLen];
        serverIn.read(header, 0, hLen);
        return header;
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
