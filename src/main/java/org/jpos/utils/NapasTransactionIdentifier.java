package org.jpos.utils;

import org.jpos.constant.ISOMsgField;
import org.jpos.constant.ISOMsgMTI;
import org.jpos.constant.ISOMsgProcessingCode;
import org.jpos.constant.NapasTransactionType;
import org.jpos.iso.ISOMsg;

public class NapasTransactionIdentifier {
    public static String identifyTransactionType(ISOMsg m) throws Exception {
        String mti = m.getMTI();
        String processingCode = m.getString(ISOMsgField.F3_PROCESSING_CODE);

        if (mti.equals(ISOMsgMTI.BALANCE_INQUIRY_REQUEST)
                && processingCode.equals(ISOMsgProcessingCode.BALANCE_INQUIRY)) {
            return NapasTransactionType.BALANCE_INQUIRY;
        } else if (mti.equals(ISOMsgMTI.PURCHASE_REQUEST) && processingCode.equals(ISOMsgProcessingCode.PURCHASE)) {
            return NapasTransactionType.PURCHASE;
        } else if (mti.equals(ISOMsgMTI.TRANSIT_PURCHASE_REQUEST)
                && processingCode.equals(ISOMsgProcessingCode.PURCHASE_TRANSIT)) {
            return NapasTransactionType.PURCHASE_TRANSIT;
        } else if (mti.equals(ISOMsgMTI.REFUND_ONLINE_REQUEST)
                && processingCode.equals(ISOMsgProcessingCode.REFUND_ONLINE)) {
            return NapasTransactionType.REFUND_ONLINE;
        } else if (mti.equals(ISOMsgMTI.VOID_REQUEST) && processingCode.equals(ISOMsgProcessingCode.VOID)) {
            return NapasTransactionType.VOID;
        } else if (mti.equals(ISOMsgMTI.PIN_CHANGE_REQUEST) && processingCode.equals(ISOMsgProcessingCode.PIN_CHANGE)) {
            return NapasTransactionType.PIN_CHANGE;
        }
        return "UNKNOWN";
    }
}
