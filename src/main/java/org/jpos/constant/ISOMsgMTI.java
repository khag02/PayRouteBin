package org.jpos.constant;

public class ISOMsgMTI {
    // Balance Inquiry
    public static final String BALANCE_INQUIRY_REQUEST = "0200";
    public static final String BALANCE_INQUIRY_RESPONSE = "0210";

    // Purchase
    public static final String PURCHASE_REQUEST = "0200";
    public static final String PURCHASE_RESPONSE = "0210";

    // Void
    public static final String VOID_REQUEST = "0420";
    public static final String VOID_RESPONSE = "0430";

    // PIN Change
    public static final String PIN_CHANGE_REQUEST = "0200";
    public static final String PIN_CHANGE_RESPONSE = "0210";

    // Purchase (Transit)
    public static final String TRANSIT_PURCHASE_REQUEST = "0200";
    public static final String TRANSIT_PURCHASE_RESPONSE = "0210";

    // Refund Online
    public static final String REFUND_ONLINE_REQUEST = "0200";
    public static final String REFUND_ONLINE_RESPONSE = "0210";

    // Preauthorization
    public static final String PREAUTH_REQUEST = "0100";
    public static final String PREAUTH_RESPONSE = "0110";

    // Preauthorization Cancellation
    public static final String PREAUTH_CANCEL_REQUEST = "0100";
    public static final String PREAUTH_CANCEL_RESPONSE = "0110";

    // Preauthorization Completion
    public static final String PREAUTH_COMPLETION_REQUEST = "0200";
    public static final String PREAUTH_COMPLETION_RESPONSE = "0210";

    // Preauthorization Completion Cancellation
    public static final String PREAUTH_COMPLETION_CANCEL_REQUEST = "0200";
    public static final String PREAUTH_COMPLETION_CANCEL_RESPONSE = "0210";
}
