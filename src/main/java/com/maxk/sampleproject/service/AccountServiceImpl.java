package com.maxk.sampleproject.service;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.InsufficientBalanceForTransferException;
import com.maxk.sampleproject.model.InvalidTransferTypeException;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.db.AccountDAO;
import com.maxk.sampleproject.db.MoneyTransferDAO;

import javax.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;

public class AccountServiceImpl implements AccountService {
    private AccountDAO accountDAO;
    private MoneyTransferDAO transferDAO;

    public AccountServiceImpl(AccountDAO accountDAO, MoneyTransferDAO transferDAO) {
        this.accountDAO = accountDAO;
        this.transferDAO = transferDAO;
    }

    public Account getAccount(long accountId) {
        return accountDAO.findById(accountId)
            .orElseThrow(() -> new NotFoundException("No such account."));
    }

    public Account createAccount(Account account) {
        return accountDAO.save(account);
    }

    public List<Account> listAccounts() {
        return accountDAO.findAll();
    }

    public void transferMoney(MoneyTransfer transfer) {
        transfer.setDateTime(LocalDateTime.now());
        transferDAO.create(transfer);

        Account sourceAccount = getAccount(transfer.getAccountId());
        double sourceAccountBalance;
        switch (transfer.getTransferType()) {
            case DEPOSIT:
                sourceAccountBalance = sourceAccount.getBalance() + transfer.getMoneyAmount();
                break;
            case WITHDRAW:
                if (transfer.getMoneyAmount() > sourceAccount.getBalance()) {
                    throw new InsufficientBalanceForTransferException("Can't withdraw " +
                        transfer.getMoneyAmount() + " from account " + sourceAccount.getId() +
                        " because the balance of the account is less than the withdrawn amount");
                }
                sourceAccountBalance = sourceAccount.getBalance() - transfer.getMoneyAmount();
                break;
            case ACCOUNT_TO_ACCOUNT:
                if (transfer.getMoneyAmount() > sourceAccount.getBalance()) {
                    throw new InsufficientBalanceForTransferException("Can't transfer " +
                        transfer.getMoneyAmount() + " from account " + sourceAccount.getId() +
                        " because the balance of the account is less than the transferred amount");
                }
                Account destinationAccount = getAccount(transfer.getDestinationAccountId());
                sourceAccountBalance = sourceAccount.getBalance() - transfer.getMoneyAmount();
                destinationAccount.setBalance(destinationAccount.getBalance() + transfer.getMoneyAmount());
                accountDAO.save(destinationAccount);
                break;
            default:
                throw new InvalidTransferTypeException("Invalid transfer type was specified");
        }
        sourceAccount.setBalance(sourceAccountBalance);
        accountDAO.save(sourceAccount);
    }

    public List<MoneyTransfer> getTransfers(long accountId) {
        return transferDAO.getByAccountId(accountId);
    }
}
