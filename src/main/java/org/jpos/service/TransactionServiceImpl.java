package org.jpos.service;

import org.jpos.entity.TransactionEntity;
import org.jpos.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {
    TransactionRepository transactionRepository;

    @Override
    public void save(TransactionEntity transactionEntity) {
        transactionRepository.save(transactionEntity);
    }

}
