package com.maxk.sampleproject.db;

import com.maxk.sampleproject.model.Account;
import io.dropwizard.hibernate.AbstractDAO;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class AccountDAO extends AbstractDAO<Account> {
    public AccountDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<Account> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Account save(Account account) {
        return persist(account);
    }

    public List<Account> findAll() {
        return list((Query<Account>) namedQuery("com.maxk.sampleproject.model.Account.findAll"));
    }
}
