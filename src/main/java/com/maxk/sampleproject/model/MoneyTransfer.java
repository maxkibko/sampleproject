package com.maxk.sampleproject.model;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transfers")
@NamedQueries({
    @NamedQuery(name = "com.maxk.sampleproject.model.MoneyTransfer.getByAccountId",
        query= "SELECT t FROM MoneyTransfer t WHERE " +
            "t.accountId = :accountId OR t.destinationAccountId = :accountId " +
            "ORDER BY t.dateTime")
})
public class MoneyTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "accountId", nullable = false)
    @NotNull
    private long accountId;

    @Column(name = "destinationAccountId")
    private Long destinationAccountId;

    @Column(name = "moneyAmount", nullable = false)
    @NotNull
    @Min(0)
    private double moneyAmount;

    @Column(name = "dateTime", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "transferType", nullable = false)
    @NotNull
    private TransferType transferType;

    public MoneyTransfer() {
    }

    public MoneyTransfer(long accountId, Long destinationAccountId,
                         double moneyAmount, TransferType transferType) {
        this.accountId = accountId;
        this.destinationAccountId = destinationAccountId;
        this.moneyAmount = moneyAmount;
        this.transferType = transferType;
        this.dateTime = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public double getMoneyAmount() {
        return moneyAmount;
    }

    public void setMoneyAmount(double moneyAmount) {
        this.moneyAmount = moneyAmount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }

    @Override
    public int hashCode() {return Objects.hash(id);}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {return true;}
        if (obj == null || getClass() != obj.getClass()) {return false;}
        final MoneyTransfer other = (MoneyTransfer) obj;
        return Objects.equals(this.id, other.id)
            && Objects.equals(this.accountId, other.accountId)
            && Objects.equals(this.destinationAccountId, other.destinationAccountId)
            && Objects.equals(this.moneyAmount, other.moneyAmount)
            && Objects.equals(this.dateTime, other.dateTime)
            && Objects.equals(this.transferType, other.transferType);
    }
}
