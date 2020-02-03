package com.maxk.sampleproject.db;

import com.maxk.sampleproject.model.MoneyTransfer;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

public class MoneyTransferDAO extends AbstractDAO<MoneyTransfer> {
    public MoneyTransferDAO(SessionFactory factory) {
        super(factory);
    }

    public MoneyTransfer create(MoneyTransfer moneyTransfer) {
        return persist(moneyTransfer);
    }

    public List<MoneyTransfer> getByAccountId(long accountId) {
        return list((Query<MoneyTransfer>) namedQuery("com.maxk.sampleproject.model.MoneyTransfer.getByAccountId")
            .setParameter("accountId", accountId));
    }
}
