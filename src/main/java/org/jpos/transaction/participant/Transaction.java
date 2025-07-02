
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
        transactionEntity.setAmount(request.getString(ISOMsgField.F4_AMOUNT_TRANSACTION));

        // save data response
        transactionEntity.setResponseCode(response.getString(ISOMsgField.F39_RESPONSE_CODE));
        transactionService.save(transactionEntity);
    }

    public void commit(long id, Serializable o) {
    }

    public void abort(long id, Serializable o) {
    }
}
