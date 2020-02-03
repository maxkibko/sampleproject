package com.maxk.sampleproject.resources;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.service.AccountService;
import io.dropwizard.hibernate.UnitOfWork;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {

    private final AccountService accountService;

    public AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Path("/{accountId}")
    @UnitOfWork
    public Account getAccount(@PathParam("accountId") Long accountId) {
        return accountService.getAccount(accountId);
    }

    @POST
    @UnitOfWork
    public Account createAccount(@Valid Account account) {
        return accountService.createAccount(account);
    }

    @GET
    @UnitOfWork
    public List<Account> listAccounts() {
        return accountService.listAccounts();
    }

    @POST
    @Path("/transfer")
    @UnitOfWork
    public void transferMoney(@Valid MoneyTransfer transfer) {
        accountService.transferMoney(transfer);
    }

    @GET
    @Path("/{accountId}/transfers")
    @UnitOfWork
    public List<MoneyTransfer> getTransfers(@PathParam("accountId") Long accountId) {
        return accountService.getTransfers(accountId);
    }
}
