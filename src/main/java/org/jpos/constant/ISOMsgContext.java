package org.jpos.constant;

public enum ISOMsgContext {
    PROFILER, TIMESTAMP,
    SOURCE, REQUEST, RESPONSE,
    LOGEVT,
    DB, TX,
    IRC,
    TXNNAME,
    RESULT,
    MID,
    TID,
    PCODE,
    CARD,
    TRANSMISSION_TIMESTAMP,
    TRANSACTION_TIMESTAMP,
    CAPTURE_DATE,
    POS_DATA_CODE,
    AMOUNT,
    LOCAL_AMOUNT,
    ORIGINAL_MTI,
    ORIGINAL_STAN,
    ORIGINAL_TIMESTAMP,
    ORIGINAL_DATA_ELEMENTS,
    DESTINATION,
    PANIC, TXN_TYPE, MTI;

    private final String name;

    ISOMsgContext() {
        this.name = name();
    }

    ISOMsgContext(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
