package org.jpos.constant;

public class ISOMsgRespCode {
    private ISOMsgRespCode() {
    }

    public static final String SUCCESS = "00";
    public static final String FAIL = "01";
    public static final String INVALID_AMOUNT = "02";
    public static final String INVALID_CARD = "03";
    public static final String INSUFFICIENT_FUNDS = "04";
    public static final String CARD_EXPIRED = "05";
    public static final String SYSTEM_ERROR = "06";
    public static final String DUPLICATE_TRANSACTION = "07";
    public static final String UNAUTHORIZED_ACCESS = "08";
    public static final String TRANSACTION_NOT_FOUND = "09";
}
