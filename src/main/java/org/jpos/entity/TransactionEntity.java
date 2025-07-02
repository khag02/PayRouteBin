package org.jpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.jpos.iso.ISOUtil;
import org.jpos.utils.TransactionUtils;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mti", length = 4)
    private String mti;

    @Column(name = "pan", length = 19) // Field 2
    private String pan;

    @Column(name = "processing_code", length = 6) // Field 3
    private String processingCode;

    @Column(name = "amount", length = 12) // Field 4
    private String amount;

    @Column(name = "transmission_datetime", length = 10) // Field 7
    private String transmissionDateTime;

    @Column(name = "stan", length = 6) // Field 11
    private String stan;

    @Column(name = "local_transaction_time", length = 12) // Field 12
    private String localTransactionTime;

    @Column(name = "rrn", length = 12) // Field 37
    private String retrievalReferenceNumber;

    @Column(name = "response_code", length = 3) // Field 39
    private String responseCode;

    @Column(name = "terminal_id", length = 8) // Field 41
    private String terminalId;

    @Column(name = "merchant_id", length = 15) // Field 42
    private String merchantId;

    @Column(name = "additional_data", length = 999) // Field 48
    private String additionalData;

    @Column(name = "currency_code", length = 3) // Field 49
    private String currencyCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.pan != null && !this.pan.isEmpty()) {
            this.pan = TransactionUtils.maskPAN(this.pan);
        }
    }
}
