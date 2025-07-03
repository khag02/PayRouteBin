package org.jpos.util;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jpos.jfr.LogEventDump;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.jpos.transaction.TransactionManager;

/**
 * @author apr
 */
public class LogEvent {
    private LogSource source;
    private String tag;
    private final List<Object> payLoad;
    private Instant createdAt;
    private Instant dumpedAt;
    private boolean honorSourceLogger;
    private boolean noArmor;
    private boolean hasException;
    private String traceId;
    private boolean springStyleLogging = true;

    // ANSI color codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_FAINT = "\u001B[2m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    // Spring-style log level mapping
    private static final Map<String, String> LOG_LEVEL_MAP = new HashMap<>();
    static {
        LOG_LEVEL_MAP.put("debug", "DEBUG");
        LOG_LEVEL_MAP.put("info", "INFO");
        LOG_LEVEL_MAP.put("warn", "WARN");
        LOG_LEVEL_MAP.put("error", "ERROR");
        LOG_LEVEL_MAP.put("trace", "TRACE");
    }

    // Log level colors mapping
    private static final Map<String, String> LOG_LEVEL_COLORS = new HashMap<>();
    static {
        LOG_LEVEL_COLORS.put("error", ANSI_RED);
        LOG_LEVEL_COLORS.put("warn", ANSI_YELLOW);
        LOG_LEVEL_COLORS.put("info", ANSI_GREEN);
        LOG_LEVEL_COLORS.put("debug", ANSI_GREEN);
        LOG_LEVEL_COLORS.put("trace", ANSI_GREEN);
    }

    // Date formatter for UTC timezone
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));

    public LogEvent(String tag) {
        super();
        this.tag = tag;
        createdAt = Instant.now();
        this.payLoad = Collections.synchronizedList(new ArrayList<>());
    }

    public LogEvent() {
        this("info");
    }

    public LogEvent(String tag, Object msg) {
        this(tag);
        addMessage(msg);
    }

    public LogEvent(LogSource source, String tag) {
        this(tag);
        this.source = source;
        honorSourceLogger = true;
    }

    public LogEvent(LogSource source, String tag, Object msg) {
        this(tag);
        this.source = source;
        honorSourceLogger = true;
        addMessage(msg);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void addMessage(Object msg) {
        payLoad.add(msg);
        if (msg instanceof Throwable)
            hasException = true;
    }

    public void addMessage(String tagname, String message) {
        payLoad.add("<" + tagname + ">" + message + "</" + tagname + ">");
    }

    public LogSource getSource() {
        return source;
    }

    public void setSource(LogSource source) {
        this.source = source;
    }

    public void setNoArmor(boolean noArmor) {
        this.noArmor = noArmor;
    }

    public void setSpringStyleLogging(boolean springStyleLogging) {
        this.springStyleLogging = springStyleLogging;
    }

    public boolean isSpringStyleLogging() {
        return springStyleLogging;
    }

    protected String dumpHeader(PrintStream p, String indent) {
        if (springStyleLogging) {
            return dumpSpringStyleHeader(p, indent);
        } else {
            return dumpOriginalHeader(p, indent);
        }
    }

    private String dumpSpringStyleHeader(PrintStream p, String indent) {
        if (noArmor) {
            return indent;
        }

        dumpedAt = getDumpedAt();
        StringBuilder sb = new StringBuilder();

        String timestamp = UTC_FORMATTER.format(dumpedAt);
        sb.append(ANSI_FAINT)
                .append(timestamp)
                .append(ANSI_RESET)
                .append("  ");

        String logLevel = LOG_LEVEL_MAP.getOrDefault(tag.toLowerCase(), "INFO");
        String levelColor = LOG_LEVEL_COLORS.getOrDefault(tag.toLowerCase(), ANSI_GREEN);
        sb.append(levelColor)
                .append(String.format("%-5s", logLevel))
                .append(ANSI_RESET)
                .append(" ");

        // Thread name
        // sb.append(ANSI_MAGENTA)
        // .append(String.format("[%-15.15s]", Thread.currentThread().getName()))
        // .append(ANSI_RESET)
        // .append(" ");

        // Separator
        sb.append(ANSI_FAINT)
                .append("--- ")
                .append(ANSI_RESET);

        if (source != null) {
            String className = source.getClass().getSimpleName();
            if (className.isEmpty()) {
                className = source.getClass().getName();
                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    className = className.substring(lastDot + 1);
                }
            }

            int maxLength = 18;
            if (className.length() > maxLength) {
                className = className.substring(0, maxLength);
            } else {
                className = String.format("%-" + maxLength + "s", className);
            }

            sb.append(ANSI_CYAN)
                    .append(className)
                    .append(ANSI_RESET);
        } else {
            sb.append(ANSI_CYAN)
                    .append(String.format("%-20s", "LogEvent"))
                    .append(ANSI_RESET);
        }

        if (tag != null) {
            sb.append(ANSI_BLUE)
                    .append(" [")
                    .append(tag.toUpperCase())
                    .append("]")
                    .append(ANSI_RESET);
        }

        sb.append(ANSI_FAINT)
                .append(" : ")
                .append(ANSI_RESET);

        p.print(sb.toString());
        return "";
    }

    private String dumpOriginalHeader(PrintStream p, String indent) {
        if (noArmor) {
            p.println("");
            return indent;
        }

        dumpedAt = getDumpedAt();
        StringBuilder sb = new StringBuilder(indent);
        sb.append("<log realm=\"")
                .append(getRealm())
                .append("\" at=\"")
                .append(UTC_FORMATTER.format(dumpedAt))
                .append("\"");

        long elapsed = Duration.between(createdAt, dumpedAt).toMillis();
        if (elapsed > 0) {
            sb.append(" lifespan=\"")
                    .append(elapsed)
                    .append("ms\"");
        }

        // if (traceId != null) {
        // sb.append(String.format(" trace-id=\"%s\"", traceId));
        // }

        sb.append(">");
        p.println(sb.toString());
        return indent + "  ";
    }

    private void dumpSpringStyle(PrintStream p, String indent) {
        if (payLoad.isEmpty()) {
            if (tag != null && !tag.isEmpty()) {
                p.println(ANSI_WHITE + "No message" + ANSI_RESET);
            }
        } else {
            synchronized (payLoad) {
                boolean firstMessage = true;
                for (Object o : payLoad) {
                    if (!firstMessage) {
                        p.println();
                        dumpSpringStyleHeader(p, "");
                    }

                    if (o instanceof Loggeable) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream tempPs = new PrintStream(baos);
                        ((Loggeable) o).dump(tempPs, "");
                        p.print(ANSI_WHITE + baos.toString().trim() + ANSI_RESET);
                    } else if (o instanceof SQLException) {
                        SQLException e = (SQLException) o;
                        p.print(ANSI_RED + "SQLException: " + e.getMessage() +
                                " (SQLState: " + e.getSQLState() +
                                ", ErrorCode: " + e.getErrorCode() + ")" + ANSI_RESET);
                        if (hasException) {
                            p.println();
                            e.printStackTrace(p);
                        }
                    } else if (o instanceof Throwable) {
                        Throwable t = (Throwable) o;
                        p.print(ANSI_RED + "Exception: " + t.getMessage() + ANSI_RESET);
                        p.println();
                        t.printStackTrace(p);
                    } else if (o instanceof Object[]) {
                        Object[] oa = (Object[]) o;
                        p.print(ANSI_WHITE + "Array: [");
                        for (int j = 0; j < oa.length; j++) {
                            if (j > 0)
                                p.print(", ");
                            p.print(oa[j] != null ? oa[j].toString() : "null");
                        }
                        p.print("]" + ANSI_RESET);
                    } else if (o instanceof Element) {
                        p.print(ANSI_WHITE + "XML Element: ");
                        XMLOutputter out = new XMLOutputter(Format.getCompactFormat());
                        try {
                            out.output((Element) o, p);
                        } catch (IOException ex) {
                            p.print("Error outputting XML: " + ex.getMessage());
                        }
                        p.print(ANSI_RESET);
                    } else if (o != null) {
                        p.print(ANSI_WHITE + o.toString() + ANSI_RESET);
                    } else {
                        p.print(ANSI_WHITE + "null" + ANSI_RESET);
                    }

                    firstMessage = false;
                }
                p.println();
            }
        }
    }

    private void dumpOriginalStyle(PrintStream p, String indent) {
        if (payLoad.isEmpty()) {
            if (tag != null)
                p.println(indent + "<" + tag + "/>");
        } else {
            String newIndent;
            if (tag != null) {
                if (!tag.isEmpty())
                    p.println(indent + "<" + tag + ">");
                newIndent = indent + "  ";
            } else
                newIndent = "";
            synchronized (payLoad) {
                for (Object o : payLoad) {
                    if (o instanceof Loggeable)
                        ((Loggeable) o).dump(p, newIndent);
                    else if (o instanceof SQLException) {
                        SQLException e = (SQLException) o;
                        p.println(newIndent + "<SQLException>"
                                + e.getMessage() + "</SQLException>");
                        p.println(newIndent + "<SQLState>"
                                + e.getSQLState() + "</SQLState>");
                        p.println(newIndent + "<VendorError>"
                                + e.getErrorCode() + "</VendorError>");
                        ((Throwable) o).printStackTrace(p);
                    } else if (o instanceof Throwable) {
                        p.println(newIndent + "<exception name=\""
                                + ((Throwable) o).getMessage() + "\">");
                        p.print(newIndent);
                        ((Throwable) o).printStackTrace(p);
                        p.println(newIndent + "</exception>");
                    } else if (o instanceof Object[]) {
                        Object[] oa = (Object[]) o;
                        p.print(newIndent + "[");
                        for (int j = 0; j < oa.length; j++) {
                            if (j > 0)
                                p.print(",");
                            p.print(oa[j].toString());
                        }
                        p.println("]");
                    } else if (o instanceof Element) {
                        p.println("");
                        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
                        out.getFormat().setLineSeparator("\n");
                        try {
                            out.output((Element) o, p);
                        } catch (IOException ex) {
                            ex.printStackTrace(p);
                        }
                        p.println("");
                    } else if (o != null) {
                        p.println(newIndent + o.toString());
                    } else {
                        p.println(newIndent + "null");
                    }
                }
            }
            if (tag != null && !tag.isEmpty())
                p.println(indent + "</" + tag + ">");
        }
    }

    protected void dumpTrailer(PrintStream p, String indent) {
        if (!noArmor && !springStyleLogging)
            p.println(indent + "</log>");
    }

    public void dump(PrintStream p, String outer) {
        var jfr = new LogEventDump();
        jfr.begin();
        try {
            String indent = dumpHeader(p, outer);

            if (springStyleLogging) {
                dumpSpringStyle(p, indent);
            } else {
                dumpOriginalStyle(p, indent);
            }

        } catch (Throwable t) {
            t.printStackTrace(p);
        } finally {
            dumpTrailer(p, outer);
            jfr.commit();
        }
    }

    public String getRealm() {
        return source != null ? source.getRealm() : "";
    }

    public LogEvent withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public LogEvent withTraceId(UUID uuid) {
        this.traceId = uuid.toString().replace("-", "");
        return this;
    }

    public LogEvent withSource(LogSource source) {
        setSource(source);
        return this;
    }

    public LogEvent add(Object o) {
        addMessage(o);
        return this;
    }

    public LogEvent withTraceId() {
        getTraceId();
        return this;
    }

    public String getTraceId() {
        synchronized (getPayLoad()) {
            if (traceId == null)
                traceId = UUID.randomUUID().toString().replace("-", "");
            return traceId;
        }
    }

    public List<Object> getPayLoad() {
        return payLoad;
    }

    public String toString(String indent) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(baos);
        synchronized (getPayLoad()) {
            dump(p, indent);
        }
        return baos.toString();
    }

    @Override
    public String toString() {
        return toString("");
    }

    public boolean hasException() {
        return hasException;
    }

    public boolean isHonorSourceLogger() {
        return honorSourceLogger;
    }

    public synchronized Instant getDumpedAt() {
        if (dumpedAt == null)
            dumpedAt = Instant.now();
        return dumpedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private boolean supportsAnsiColors() {
        String term = System.getenv("TERM");
        return !noArmor && term != null && !term.equals("dumb");
    }
}
