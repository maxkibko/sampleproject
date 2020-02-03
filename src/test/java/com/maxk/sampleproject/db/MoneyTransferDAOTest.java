package com.maxk.sampleproject.db;

import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.model.TransferType;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(DropwizardExtensionsSupport.class)
public class MoneyTransferDAOTest {

    public DAOTestExtension daoTestRule = DAOTestExtension.newBuilder()
        .addEntityClass(MoneyTransfer.class)
        .build();

    private MoneyTransferDAO moneyTransferDAO;

    @BeforeEach
    public void setUp() {
        moneyTransferDAO = new MoneyTransferDAO(daoTestRule.getSessionFactory());
    }

    @Test
    public void testCreateMoneyTransfer() {
        MoneyTransfer transferToCreate = new MoneyTransfer(1, 2L,
            100d, TransferType.ACCOUNT_TO_ACCOUNT);
        MoneyTransfer transfer = daoTestRule.inTransaction(() -> moneyTransferDAO.create(transferToCreate));
        assertThat(transfer.getId()).isGreaterThan(0);
        assertThat(transfer.getAccountId()).isEqualTo(1);
        assertThat(transfer.getDestinationAccountId()).isEqualTo(2);
        assertThat(transfer.getMoneyAmount()).isEqualTo(100d);
        assertThat(transfer.getTransferType()).isEqualTo(TransferType.ACCOUNT_TO_ACCOUNT);
        assertThat(transfer.getDateTime()).isEqualTo(transferToCreate.getDateTime());
    }

    @Test
    public void testFindAll() {
        List<MoneyTransfer> expectedFirstAccountTransfers = new ArrayList<>();
        List<MoneyTransfer> expectedSecondAccountTransfers = new ArrayList<>();
        daoTestRule.inTransaction(() -> {
            MoneyTransfer transfer1 = moneyTransferDAO.create(new MoneyTransfer(1, null,
                100d, TransferType.DEPOSIT));
            MoneyTransfer transfer2 = moneyTransferDAO.create(new MoneyTransfer(2, null,
                200d, TransferType.WITHDRAW));
            MoneyTransfer transfer3 = moneyTransferDAO.create(new MoneyTransfer(1, 2L,
                300d, TransferType.ACCOUNT_TO_ACCOUNT));
            expectedFirstAccountTransfers.add(transfer1);
            expectedFirstAccountTransfers.add(transfer3);
            expectedSecondAccountTransfers.add(transfer2);
            expectedSecondAccountTransfers.add(transfer3);
        });

        List<MoneyTransfer> firstAccountTransfers = moneyTransferDAO.getByAccountId(1);
        assertThat(firstAccountTransfers).contains(
            expectedFirstAccountTransfers.toArray(new MoneyTransfer[2]));
        List<MoneyTransfer> secondAccountTransfers = moneyTransferDAO.getByAccountId(2);
        assertThat(secondAccountTransfers).contains(
            expectedSecondAccountTransfers.toArray(new MoneyTransfer[2]));
    }

    @Test
    public void testHandlesNullValues() {
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(()->
            daoTestRule.inTransaction(() -> moneyTransferDAO.create(
                new MoneyTransfer(1, 2L,
                    100d, null))));
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(()->
            daoTestRule.inTransaction(() -> {
                MoneyTransfer transfer = new MoneyTransfer(1, 2L,
                    100d, null);
                transfer.setDateTime(null);
                moneyTransferDAO.create(transfer);
            }));
    }
}
