package com.maxk.sampleproject.service;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.MoneyTransfer;

import java.util.List;

public interface AccountService {
    Account getAccount(long accountId);

    Account createAccount(Account account);

    List<Account> listAccounts();

    void transferMoney(MoneyTransfer transfer);

    List<MoneyTransfer> getTransfers(long accountId);
}
