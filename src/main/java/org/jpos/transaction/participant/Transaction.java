
package org.jpos.transaction.participant;

import java.io.Serializable;

import org.jpos.config.SpringContextHolder;
import org.jpos.constant.ISOMsgContext;
import org.jpos.constant.ISOMsgField;
import org.jpos.constant.ISOMsgRespCode;
import org.jpos.constant.NapasTransactionType;
import org.jpos.entity.TransactionEntity;
import org.jpos.iso.ISOMsg;
import org.jpos.service.TransactionService;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

public class Transaction implements TransactionParticipant {
    private TransactionService transactionService;

    public Transaction() {
        this.transactionService = SpringContextHolder.context.getBean(TransactionService.class);
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = (ISOMsg) ctx.get(ISOMsgContext.REQUEST.toString());
        ISOMsg response = (ISOMsg) ctx.get(ISOMsgContext.RESPONSE.toString());
        ctx.put(ISOMsgContext.RESPONSE_CODE.toString(), response.getString(ISOMsgField.F39_RESPONSE_CODE));
        String responseCode = ctx.get(ISOMsgContext.RESPONSE_CODE.toString());
        String requestType = ctx.get(ISOMsgContext.TXN_TYPE.toString());

        if (ISOMsgRespCode.SUCCESS.equals(responseCode)) {
            switch (requestType) {
                case NapasTransactionType.BALANCE_INQUIRY:
                    break;
                case NapasTransactionType.PURCHASE:
                case NapasTransactionType.PURCHASE_TRANSIT:
                    handlePurchase(request, response);
                    break;
                default:
                    break;
            }
        }
        return PREPARED | NO_JOIN | READONLY;
    }

    private void handlePurchase(ISOMsg request, ISOMsg response) {
        TransactionEntity transactionEntity = new TransactionEntity();
        // save data request
        transactionEntity.setMti(request.getString(ISOMsgField.F0_MTI));
        transactionEntity.setPan(request.getString(ISOMsgField.F2_PAN));
        transactionEntity.setProcessingCode(request.getString(ISOMsgField.F3_PROCESSING_CODE));
        transactionEntity.setAmount(request.getString(ISOMsgField.F4_AMOUNT_TRANSACTION));
        transactionEntity.setTransmissionDateTime(request.getString(ISOMsgField.F7_TRANSMISSION_DATE_TIME));
        transactionEntity.setStan(request.getString(ISOMsgField.F11_STAN));
        transactionEntity.setLocalTransactionTime(request.getString(ISOMsgField.F12_LOCAL_DATE_TIME));
        transactionEntity.setRetrievalReferenceNumber(request.getString(ISOMsgField.F37_RETRIEVAL_REF));
        transactionEntity.setTerminalId(request.getString(ISOMsgField.F41_TERMINAL_ID));
        transactionEntity.setMerchantId(request.getString(ISOMsgField.F42_MERCHANT_ID));
        transactionEntity.setAdditionalData(request.getString(ISOMsgField.F48_ADDITIONAL_DATA_PRIVATE));
        transactionEntity.setCurrencyCode(request.getString(ISOMsgField.F49_CURRENCY_TRANSACTION));

        // save data response
        transactionEntity.setResponseCode(response.getString(ISOMsgField.F39_RESPONSE_CODE));

        transactionService.save(transactionEntity);
    }

    public void commit(long id, Serializable o) {
    }

    public void abort(long id, Serializable o) {
    }
}
