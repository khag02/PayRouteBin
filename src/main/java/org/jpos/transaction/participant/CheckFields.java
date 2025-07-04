package org.jpos.transaction.participant;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.jpos.constant.ISOMsgContext;
import org.jpos.constant.ISOMsgField;
import org.jpos.core.*;
import org.jpos.iso.*;
import org.jpos.rc.CMF;
import org.jpos.rc.Result;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Caller;
import org.jpos.utils.NapasTransactionIdentifier;

public class CheckFields implements TransactionParticipant, Configurable {
    private Configuration cfg;
    private String request;
    private Pattern PCODE_PATTERN = Pattern.compile("^[\\d|\\w]{6}$");
    private Pattern TID_PATTERN = Pattern.compile("^[\\w\\s]{1,16}");
    private Pattern MID_PATTERN = Pattern.compile("^[\\w\\s]{1,15}");
    private Pattern TRANSMISSION_TIMESTAMP_PATTERN = Pattern.compile("^\\d{10}");
    private Pattern LOCAL_TIMESTAMP_PATTERN = Pattern.compile("^\\d{14}");
    private boolean ignoreCardValidation = false;
    private Pattern MTI_PATTERN = Pattern.compile("^\\d{4}$");
    private Pattern track1Pattern = null;
    private Pattern track2Pattern = null;

    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        Result rc = ctx.getResult();
        try {
            ISOMsg m = ctx.get(request);
            if (m == null) {
                ctx.getResult().fail(CMF.INVALID_TRANSACTION, Caller.info(), "'%s' not available in Context", request);
                return ABORTED | NO_JOIN | READONLY;
            }
            Set<String> validFields = new HashSet<>();
            assertFields(ctx, m, cfg.get("mandatory", ""), true, validFields, rc);
            assertFields(ctx, m, cfg.get("optional", ""), false, validFields, rc);
        } catch (Throwable t) {
            rc.fail(CMF.SYSTEM_ERROR, Caller.info(), t.getMessage());
            ctx.log(t);
        }
        return (rc.hasFailures() ? ABORTED : PREPARED) | NO_JOIN | READONLY;
    }

    public void setConfiguration(Configuration cfg) {
        this.cfg = cfg;
        request = cfg.get("request", ISOMsgContext.REQUEST.toString());
        ignoreCardValidation = cfg.getBoolean("ignore-card-validation", false);
    }

    private void assertFields(Context ctx, ISOMsg m, String fields, boolean mandatory, Set<String> validFields,
            Result rc) {
        StringTokenizer st = new StringTokenizer(fields, ", ");
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            ISOMsgContext k = null;
            try {
                k = ISOMsgContext.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
            if (k != null) {
                switch (k) {
                    case MTI:
                        putMti(ctx, m, mandatory, validFields, rc);
                        break;
                    case PCODE:
                        putPCode(ctx, m, mandatory, validFields, rc);
                        break;
                    case TXN_TYPE:
                        putTxnType(ctx, m, mandatory, validFields, rc);
                        break;
                    case CARD:
                        putCard(ctx, m, mandatory, validFields, rc);
                        break;
                    case TID:
                        putTid(ctx, m, mandatory, validFields, rc);
                        break;
                    case MID:
                        putMid(ctx, m, mandatory, validFields, rc);
                        break;
                    case TRANSMISSION_TIMESTAMP:
                        putTimestamp(ctx, m, ISOMsgContext.TRANSMISSION_TIMESTAMP.toString(), 7,
                                TRANSMISSION_TIMESTAMP_PATTERN,
                                mandatory, validFields, rc);
                        break;
                    case TRANSACTION_TIMESTAMP:
                        putTimestamp(ctx, m, ISOMsgContext.TRANSACTION_TIMESTAMP.toString(), 12,
                                LOCAL_TIMESTAMP_PATTERN, mandatory,
                                validFields, rc);
                        break;
                    case AMOUNT:
                        putAmount(ctx, m, mandatory, validFields, rc);
                        break;
                    default:
                        k = null;
                }
            }
            if (k == null) {
                if (mandatory && !m.hasField(s))
                    rc.fail(CMF.MISSING_FIELD, Caller.info(), s);
                else
                    validFields.add(s);
            }
        }
    }

    private void putTxnType(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        String transactionType = null;
        try {
            transactionType = NapasTransactionIdentifier.identifyTransactionType(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (transactionType != null) {
            ctx.put(ISOMsgContext.TXN_TYPE.toString(), transactionType);
            validFields.add(ISOMsgContext.TXN_TYPE.toString());
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "TXN_TYPE");
        }
    }

    private void putMti(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        if (m.hasField(ISOMsgField.F0_MTI)) {
            String s = m.getString(ISOMsgField.F0_MTI);
            validFields.add(ISOMsgField.F0_MTI);
            if (MTI_PATTERN.matcher(s).matches()) {
                ctx.put(ISOMsgContext.MTI.toString(), s);
            } else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid MTI '%s'", s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "MTI");
        }
    }

    private void putCard(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        boolean hasCard = m.hasField(ISOMsgField.F2_PAN) || m.hasField(ISOMsgField.F14_DATE_EXPIRATION)
                || m.hasField(ISOMsgField.F35_TRACK2) || m.hasField(ISOMsgField.F45_TRACK1);
        if (!mandatory && !hasCard)
            return;

        try {
            Card.Builder cb = Card.builder();
            if (track1Pattern != null)
                cb.withTrack1Builder(Track1.builder().pattern(track1Pattern));
            if (track2Pattern != null)
                cb.withTrack2Builder(Track2.builder().pattern(track2Pattern));
            cb.isomsg(m);
            if (ignoreCardValidation)
                cb.validator(null);
            Card card = cb.build();
            ctx.put(ContextConstants.CARD.toString(), card);
            if (card.hasTrack1())
                validFields.add(ISOMsgField.F45_TRACK1);
            if (card.hasTrack2())
                validFields.add(ISOMsgField.F35_TRACK2);
            if (card.getPan() != null && m.hasField(ISOMsgField.F2_PAN))
                validFields.add(ISOMsgField.F2_PAN);
            if (card.getExp() != null && m.hasField(ISOMsgField.F14_DATE_EXPIRATION))
                validFields.add(ISOMsgField.F14_DATE_EXPIRATION);
        } catch (InvalidCardException e) {
            validFields
                    .addAll(Arrays.asList(ISOMsgField.F2_PAN, ISOMsgField.F14_DATE_EXPIRATION, ISOMsgField.F35_TRACK2,
                            ISOMsgField.F45_TRACK1));
            if (hasCard) {
                rc.fail(CMF.INVALID_CARD_NUMBER, Caller.info(), e.getMessage());
            } else if (mandatory) {
                rc.fail(CMF.MISSING_FIELD, Caller.info(), e.getMessage());
            }
        }
    }

    private void putPCode(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        if (m.hasField(ISOMsgField.F3_PROCESSING_CODE)) {
            String s = m.getString(ISOMsgField.F3_PROCESSING_CODE);
            validFields.add(ISOMsgField.F3_PROCESSING_CODE);
            if (PCODE_PATTERN.matcher(s).matches()) {
                ctx.put(ISOMsgContext.PCODE.toString(), m.getString(ISOMsgField.F3_PROCESSING_CODE));
            } else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid PCODE '%s'", s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "PCODE");
        }
    }

    private void putTid(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        if (m.hasField(ISOMsgField.F41_TERMINAL_ID)) {
            String s = m.getString(ISOMsgField.F41_TERMINAL_ID);
            validFields.add(ISOMsgField.F41_TERMINAL_ID);
            if (TID_PATTERN.matcher(s).matches()) {
                ctx.put(ISOMsgContext.TID.toString(), s);
            } else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid TID '%s'", s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "TID");
        }
    }

    private void putMid(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        if (m.hasField(ISOMsgField.F42_MERCHANT_ID)) {
            String s = m.getString(ISOMsgField.F42_MERCHANT_ID);
            validFields.add(ISOMsgField.F42_MERCHANT_ID);
            if (MID_PATTERN.matcher(s).matches()) {
                ctx.put(ISOMsgContext.MID.toString(), s);
            } else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid MID '%s'", s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "MID");
        }
    }

    private void putTimestamp(Context ctx, ISOMsg m, String key, int fieldNumber, Pattern ptrn, boolean mandatory,
            Set<String> validFields, Result rc) {
        if (m.hasField(fieldNumber)) {
            String s = m.getString(fieldNumber);
            validFields.add(Integer.toString(fieldNumber));
            if (ptrn.matcher(s).matches())
                ctx.put(key, ISODate.parseISODate(s));
            else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid %s '%s'", key, s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), ISOMsgContext.TRANSMISSION_TIMESTAMP.toString());
        }
    }

    private void putAmount(Context ctx, ISOMsg m, boolean mandatory, Set<String> validFields, Result rc) {
        if (m.hasField(ISOMsgField.F4_AMOUNT_TRANSACTION)) {
            String s = m.getString(ISOMsgField.F4_AMOUNT_TRANSACTION);
            validFields.add(ISOMsgField.F4_AMOUNT_TRANSACTION);
            if (MID_PATTERN.matcher(s).matches()) {
                ctx.put(ISOMsgContext.AMOUNT.toString(), s);
            } else
                rc.fail(CMF.INVALID_FIELD, Caller.info(), "Invalid AMOUNT '%s'", s);
        } else if (mandatory) {
            rc.fail(CMF.MISSING_FIELD, Caller.info(), "AMOUNT");
        }
    }
}
