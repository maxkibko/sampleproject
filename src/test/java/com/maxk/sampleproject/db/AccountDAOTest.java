package com.maxk.sampleproject.db;

import com.maxk.sampleproject.model.Account;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AccountDAOTest {

    public DAOTestExtension daoTestRule = DAOTestExtension.newBuilder()
        .addEntityClass(Account.class)
        .build();

    private AccountDAO accountDAO;

    @BeforeEach
    public void setUp() {
        accountDAO = new AccountDAO(daoTestRule.getSessionFactory());
    }

    @Test
    public void testCreateAndFindAccount() {
        Account johnDoe = daoTestRule.inTransaction(() -> accountDAO.save(new Account("John", "Doe", 100d)));
        assertAccount(johnDoe, "John", "Doe", 100d);
        assertThat(accountDAO.findById(johnDoe.getId())).isEqualTo(Optional.of(johnDoe));
    }

    @Test
    public void testUpdateAccount() {
        Account janeDoe = daoTestRule.inTransaction(() -> accountDAO.save(new Account("Jane", "Doe", 100d)));
        assertAccount(janeDoe, "Jane", "Doe", 100d);
        janeDoe.setBalance(500d);
        Account updatedAccount = daoTestRule.inTransaction(() -> accountDAO.save(janeDoe));
        assertEquals(janeDoe.getId(), updatedAccount.getId());
        assertAccount(updatedAccount, "Jane", "Doe", 500d);
    }

    private void assertAccount(Account account, String name, String surname, double balance) {
        assertThat(account.getId()).isGreaterThan(0);
        assertThat(account.getName()).isEqualTo(name);
        assertThat(account.getSurname()).isEqualTo(surname);
        assertThat(account.getBalance()).isEqualTo(balance);
    }

    @Test
    public void testNoAccountFound() {
        assertThat(!accountDAO.findById(Long.MAX_VALUE).isPresent());
    }

    @Test
    public void testFindAll() {
        daoTestRule.inTransaction(() -> {
            accountDAO.save(new Account("John", "Doe", 100d));
            accountDAO.save(new Account("Jane", "Doe", 200d));
            accountDAO.save(new Account("Bilbo", "Baggins", 300d));
        });

        List<Account> accounts = accountDAO.findAll();
        assertThat(accounts).extracting("name").containsOnly("John", "Jane", "Bilbo");
        assertThat(accounts).extracting("surname").containsOnly("Doe", "Doe", "Baggins");
        assertThat(accounts).extracting("balance").containsOnly(100d, 200d, 300d);
    }

    @Test
    public void testHandlesNullValues() {
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(()->
            daoTestRule.inTransaction(() -> accountDAO.save(new Account(null, "NotNull", 0d))));
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(()->
            daoTestRule.inTransaction(() -> accountDAO.save(new Account("NotNull", null, 0d))));
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(()->
            daoTestRule.inTransaction(() -> accountDAO.save(new Account("NotNull", "NotNull", null))));
    }
}
