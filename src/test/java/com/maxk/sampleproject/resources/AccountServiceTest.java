package com.maxk.sampleproject.resources;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.db.AccountDAO;
import com.maxk.sampleproject.db.MoneyTransferDAO;
import com.maxk.sampleproject.model.InsufficientBalanceForTransferException;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.model.TransferType;
import com.maxk.sampleproject.service.AccountService;
import com.maxk.sampleproject.service.AccountServiceImpl;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AccountServiceTest {
    private static final AccountDAO ACCOUNT_DAO = mock(AccountDAO.class);
    private static final MoneyTransferDAO TRANSFER_DAO = mock(MoneyTransferDAO.class);
    private static final AccountService ACCOUNT_SERVICE = new AccountServiceImpl(ACCOUNT_DAO, TRANSFER_DAO);
    private ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
    private Account account;
    private MoneyTransfer transfer;

    @BeforeEach
    public void setup() {
        account = new Account();
        account.setId(1L);
        transfer = new MoneyTransfer();
        transfer.setId(2);
    }

    @AfterEach
    public void tearDown() {
        reset(ACCOUNT_DAO, TRANSFER_DAO);
    }

    @Test
    public void testGetAccountSuccess() {
        when(ACCOUNT_DAO.findById(1L)).thenReturn(Optional.of(account));

        Account found = ACCOUNT_SERVICE.getAccount(1L);

        assertThat(found.getId()).isEqualTo(account.getId());
        verify(ACCOUNT_DAO).findById(1L);
    }

    @Test
    public void testGetAccountNotFound() {
        when(ACCOUNT_DAO.findById(2L)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> ACCOUNT_SERVICE.getAccount(2L));
    }

    @Test
    public void testCreateAccount() {
        when(ACCOUNT_DAO.save(any(Account.class))).thenReturn(account);

        Account result = ACCOUNT_SERVICE.createAccount(account);
        assertThat(result).isEqualTo(account);
        verify(ACCOUNT_DAO).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue()).isEqualTo(account);
    }

    @Test
    public void testListAccounts() {
        List<Account> accounts = Collections.singletonList(account);
        when(ACCOUNT_DAO.findAll()).thenReturn(accounts);

        List<Account> result = ACCOUNT_SERVICE.listAccounts();

        verify(ACCOUNT_DAO).findAll();
        assertThat(result).containsAll(accounts);
    }

    @Test
    public void testDepositMoney() {
        account.setBalance(100d);
        when(ACCOUNT_DAO.findById(account.getId())).thenReturn(Optional.of(account));
        transfer.setAccountId(account.getId());
        transfer.setMoneyAmount(100d);
        transfer.setTransferType(TransferType.DEPOSIT);

        ACCOUNT_SERVICE.transferMoney(transfer);

        verify(TRANSFER_DAO).create(transfer);
        verify(ACCOUNT_DAO).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualTo(200);
    }

    @Test
    public void testWithdrawMoney() {
        account.setBalance(100d);
        when(ACCOUNT_DAO.findById(account.getId())).thenReturn(Optional.of(account));
        transfer.setAccountId(account.getId());
        transfer.setMoneyAmount(100d);
        transfer.setTransferType(TransferType.WITHDRAW);

        ACCOUNT_SERVICE.transferMoney(transfer);

        verify(TRANSFER_DAO).create(transfer);
        verify(ACCOUNT_DAO).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualTo(0);
    }

    @Test
    public void testWithdrawMoneyMoreThanBalance() {
        account.setBalance(100d);
        when(ACCOUNT_DAO.findById(account.getId())).thenReturn(Optional.of(account));
        transfer.setAccountId(account.getId());
        transfer.setMoneyAmount(200d);
        transfer.setTransferType(TransferType.WITHDRAW);

        Assertions.assertThrows(InsufficientBalanceForTransferException.class, () -> ACCOUNT_SERVICE.transferMoney(transfer));
    }

    @Test
    public void testTransferMoney() {
        account.setBalance(100d);
        when(ACCOUNT_DAO.findById(account.getId())).thenReturn(Optional.of(account));
        Account secondAccount = new Account();
        secondAccount.setId(account.getId() + 1);
        secondAccount.setBalance(200d);
        when(ACCOUNT_DAO.findById(secondAccount.getId())).thenReturn(Optional.of(secondAccount));
        transfer.setAccountId(account.getId());
        transfer.setDestinationAccountId(secondAccount.getId());
        transfer.setMoneyAmount(100d);
        transfer.setTransferType(TransferType.ACCOUNT_TO_ACCOUNT);

        ACCOUNT_SERVICE.transferMoney(transfer);

        verify(TRANSFER_DAO).create(transfer);
        verify(ACCOUNT_DAO, times(2)).save(accountCaptor.capture());
        List<Account> savedAccounts = accountCaptor.getAllValues();
        assertThat(savedAccounts).extracting("id").containsExactly(2L, 1L);
        assertThat(savedAccounts).extracting("balance").containsExactly(300d, 0d);
    }

    @Test
    public void testTransferMoneyMoreThanBalance() {
        account.setBalance(100d);
        when(ACCOUNT_DAO.findById(account.getId())).thenReturn(Optional.of(account));
        Account secondAccount = new Account();
        secondAccount.setId(account.getId() + 1);
        secondAccount.setBalance(200d);
        when(ACCOUNT_DAO.findById(secondAccount.getId())).thenReturn(Optional.of(secondAccount));
        transfer.setAccountId(account.getId());
        transfer.setDestinationAccountId(secondAccount.getId());
        transfer.setMoneyAmount(400d);
        transfer.setTransferType(TransferType.ACCOUNT_TO_ACCOUNT);

        Assertions.assertThrows(InsufficientBalanceForTransferException.class, () -> ACCOUNT_SERVICE.transferMoney(transfer));
    }

    @Test
    public void testGetTransfers() {
        List<MoneyTransfer> transfers = Collections.singletonList(transfer);
        when(TRANSFER_DAO.getByAccountId(1)).thenReturn(transfers);

        List<MoneyTransfer> result = ACCOUNT_SERVICE.getTransfers(1);

        verify(TRANSFER_DAO).getByAccountId(1);
        assertThat(result).containsAll(transfers);
    }
}
