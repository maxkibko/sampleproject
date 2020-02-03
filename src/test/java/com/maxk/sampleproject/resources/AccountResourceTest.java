package com.maxk.sampleproject.resources;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.service.AccountService;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AccountResourceTest {
    private static final AccountService ACCOUNT_SERVICE = mock(AccountService.class);
    private static final ResourceExtension RULE = ResourceExtension.builder()
            .addResource(new AccountResource(ACCOUNT_SERVICE))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();
    private ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
    private ArgumentCaptor<MoneyTransfer> transferCaptor = ArgumentCaptor.forClass(MoneyTransfer.class);
    private Account account;
    private MoneyTransfer transfer;

    @BeforeEach
    public void setup() {
        account = new Account();
        account.setId(1);
        transfer = new MoneyTransfer();
        transfer.setId(2);
    }

    @AfterEach
    public void tearDown() {
        reset(ACCOUNT_SERVICE);
    }

    @Test
    public void testGetAccountSuccess() {
        when(ACCOUNT_SERVICE.getAccount(1L)).thenReturn(account);

        Account found = RULE.target("/accounts/1").request().get(Account.class);

        assertThat(found.getId()).isEqualTo(account.getId());
        verify(ACCOUNT_SERVICE).getAccount(1L);
    }

    @Test
    public void testGetAccountNotFound() {
        when(ACCOUNT_SERVICE.getAccount(2L)).thenThrow(new NotFoundException());
        Response response = RULE.target("/accounts/2").request().get();

        assertThat(response.getStatusInfo().getStatusCode()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verify(ACCOUNT_SERVICE).getAccount(2L);
    }

    @Test
    public void testCreateAccount() {
        when(ACCOUNT_SERVICE.createAccount(any(Account.class))).thenReturn(account);
        Response response = RULE.target("/accounts")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(account, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        verify(ACCOUNT_SERVICE).createAccount(accountCaptor.capture());
        assertThat(accountCaptor.getValue()).isEqualTo(account);
    }

    @Test
    public void testCreateAccountFailureMinBalance() {
        account.setBalance(-1d);

        Response response = RULE.target("/accounts")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(account, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isNotEqualTo(Response.Status.OK);
        assertThat(response.readEntity(String.class)).contains("balance must be greater than or equal to 0");
    }

    @Test
    public void testListAccounts() {
        List<Account> accounts = Collections.singletonList(account);
        when(ACCOUNT_SERVICE.listAccounts()).thenReturn(accounts);

        List<Account> response = RULE.target("/accounts")
            .request().get(new GenericType<>() {
            });

        verify(ACCOUNT_SERVICE).listAccounts();
        assertThat(response).containsAll(accounts);
    }

    @Test
    public void testTransferMoneySuccess() {
        Response response = RULE.target("/accounts/transfer")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(transfer, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
        verify(ACCOUNT_SERVICE).transferMoney(transferCaptor.capture());
        assertThat(transferCaptor.getValue()).isEqualTo(transfer);
    }

    @Test
    public void testTransferMoneyFailureMinMoneyAmount() {
        transfer.setMoneyAmount(-1d);
        Response response = RULE.target("/accounts/transfer")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(transfer, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatusInfo()).isNotEqualTo(Response.Status.OK);
        assertThat(response.readEntity(String.class)).contains("moneyAmount must be greater than or equal to 0");
    }

    @Test
    public void testGetTransfers() {
        List<MoneyTransfer> transfers = Collections.singletonList(transfer);
        when(ACCOUNT_SERVICE.getTransfers(1)).thenReturn(transfers);

        List<MoneyTransfer> response = RULE.target("/accounts/1/transfers")
            .request().get(new GenericType<>() {
            });

        verify(ACCOUNT_SERVICE).getTransfers(1);
        assertThat(response).containsAll(transfers);
    }
}
