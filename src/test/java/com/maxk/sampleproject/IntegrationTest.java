package com.maxk.sampleproject;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.model.TransferType;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(DropwizardExtensionsSupport.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTest {

    private static final String TMP_FILE = createTempFile();
    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-sample.yml");
    private static final double INITIAL_BALANCE_1 = 1500d, INITIAL_BALANCE_2 = 2000d,
        DEPOSIT = 1000d, WITHDRAWN = 500d, TRANSFERRED = 2000d;
    private static Account firstAccount, secondAccount;

    public static final DropwizardAppExtension<SampleApplicationConfiguration> RULE = new DropwizardAppExtension<>(
            SampleApplication.class, CONFIG_PATH,
            ConfigOverride.config("database.url", "jdbc:h2:" + TMP_FILE));

    @BeforeAll
    public static void initialize() throws Exception {
        RULE.getApplication().run("db", "migrate", CONFIG_PATH);

        firstAccount = new Account("John", "Doe", INITIAL_BALANCE_1);
        secondAccount = new Account("Jane", "Doe", INITIAL_BALANCE_2);
    }

    private static String createTempFile() {
        try {
            return File.createTempFile("test-sample", null).getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @Order(1)
    public void testCreateAccounts() throws Exception {
        Account firstCreatedAccount = postAccount(firstAccount);
        assertAccount(firstCreatedAccount, firstAccount, 1);

        Account secondCreatedAccount = postAccount(secondAccount);
        assertAccount(secondCreatedAccount, secondAccount, 2);
    }

    private Account postAccount(Account account) {
        return RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/accounts")
            .request()
            .post(Entity.entity(account, MediaType.APPLICATION_JSON_TYPE))
            .readEntity(Account.class);
    }

    private void assertAccount(Account actual, Account expected, long expectedId) {
        assertThat(actual.getId()).isEqualTo(expectedId);
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSurname()).isEqualTo(expected.getSurname());
        assertThat(actual.getBalance()).isEqualTo(expected.getBalance());
    }

    @Test
    @Order(2)
    public void testGetAccount() {
        long accountId = 1;
        Account account = getAccount(accountId);

        assertAccount(account, firstAccount, 1);
    }

    private Account getAccount(long accountId) {
        return RULE.client()
            .target("http://localhost:" + RULE.getLocalPort() + "/accounts/" + accountId)
            .request()
            .get()
            .readEntity(Account.class);
    }

    @Test
    @Order(3)
    public void testListAccounts() {
        List<Account> accounts = RULE.client()
            .target("http://localhost:" + RULE.getLocalPort() + "/accounts")
            .request()
            .get()
            .readEntity(new GenericType<>() { });

        assertEquals(2, accounts.size());
    }

    @Test
    @Order(4)
    public void testDepositMoney() {
        MoneyTransfer deposit = new MoneyTransfer(
            1, null, DEPOSIT, TransferType.DEPOSIT);

        int status = postMoneyTransfer(deposit);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), status);

        Account account = getAccount(1);
        assertEquals(INITIAL_BALANCE_1 + DEPOSIT, account.getBalance());
    }

    @Test
    @Order(5)
    public void testWithdrawMoney() {
        MoneyTransfer withdraw = new MoneyTransfer(
            2, null, WITHDRAWN, TransferType.WITHDRAW);

        int status = postMoneyTransfer(withdraw);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), status);

        Account account = getAccount(2);
        assertEquals(INITIAL_BALANCE_2 - WITHDRAWN, account.getBalance());
    }

    @Test
    @Order(6)
    public void testTransferMoney() {
        MoneyTransfer transfer = new MoneyTransfer(
            1, 2L, TRANSFERRED, TransferType.ACCOUNT_TO_ACCOUNT);

        int status = postMoneyTransfer(transfer);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), status);

        Account account1 = getAccount(1);
        assertEquals(INITIAL_BALANCE_1 + DEPOSIT - TRANSFERRED, account1.getBalance());
        Account account2 = getAccount(2);
        assertEquals(INITIAL_BALANCE_2 - WITHDRAWN + TRANSFERRED, account2.getBalance());
    }

    private int postMoneyTransfer(MoneyTransfer transfer) {
        int status = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/accounts/transfer")
            .request()
            .post(Entity.entity(transfer, MediaType.APPLICATION_JSON_TYPE))
            .getStatus();
        return status;
    }

    @Test
    @Order(7)
    public void testGetTransfers() {
        List<MoneyTransfer> firstAccountTransfers = getMoneyTransfers(1);
        assertEquals(firstAccountTransfers.size(), 2);
        MoneyTransfer depositTransfer = firstAccountTransfers.get(0);
        assertTransfer(depositTransfer,
            1, null, DEPOSIT, TransferType.DEPOSIT);
        MoneyTransfer transferToSecondAccount = firstAccountTransfers.get(1);
        assertTransfer(transferToSecondAccount,
            1, 2L, TRANSFERRED, TransferType.ACCOUNT_TO_ACCOUNT);

        List<MoneyTransfer> secondAccountTransfers = getMoneyTransfers(2);
        assertEquals(secondAccountTransfers.size(), 2);
        MoneyTransfer withdrawTransfer = secondAccountTransfers.get(0);
        assertTransfer(withdrawTransfer,
            2, null, WITHDRAWN, TransferType.WITHDRAW);
        MoneyTransfer transferFromFirstAccount = secondAccountTransfers.get(1);
        assertTransfer(transferFromFirstAccount,
            1, 2L, TRANSFERRED, TransferType.ACCOUNT_TO_ACCOUNT);

        assertThat(depositTransfer.getDateTime()).isBeforeOrEqualTo(withdrawTransfer.getDateTime());
        assertThat(withdrawTransfer.getDateTime()).isBeforeOrEqualTo(transferToSecondAccount.getDateTime());
        assertThat(transferToSecondAccount.getDateTime()).isEqualTo(transferFromFirstAccount.getDateTime());
    }

    private void assertTransfer(MoneyTransfer actualTransfer, long accountId, Long destinationAccountId,
                                double moneyAmount, TransferType transferType) {
        assertThat(actualTransfer.getId()).isGreaterThan(0);
        assertEquals(actualTransfer.getAccountId(), accountId);
        assertEquals(actualTransfer.getDestinationAccountId(), destinationAccountId);
        assertEquals(actualTransfer.getMoneyAmount(), moneyAmount);
        assertEquals(actualTransfer.getTransferType(), transferType);
        assertNotNull(actualTransfer.getDateTime());
        assertThat(actualTransfer.getDateTime()).isBefore(LocalDateTime.now());
    }

    private List<MoneyTransfer> getMoneyTransfers(long accountId) {
        List<MoneyTransfer> transfers = RULE.client().target("http://localhost:" + RULE.getLocalPort() +
                "/accounts/" + accountId + "/transfers")
            .request()
            .get()
            .readEntity(new GenericType<>() { });
        return transfers;
    }
}
