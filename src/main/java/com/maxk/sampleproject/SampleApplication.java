package com.maxk.sampleproject;

import com.maxk.sampleproject.model.Account;
import com.maxk.sampleproject.model.MoneyTransfer;
import com.maxk.sampleproject.db.AccountDAO;
import com.maxk.sampleproject.db.MoneyTransferDAO;
import com.maxk.sampleproject.resources.AccountResource;
import com.maxk.sampleproject.service.AccountService;
import com.maxk.sampleproject.service.AccountServiceImpl;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class SampleApplication extends Application<SampleApplicationConfiguration> {
    public static void main(String[] args) throws Exception {
        new SampleApplication().run(args);
    }

    private final HibernateBundle<SampleApplicationConfiguration> hibernateBundle =
        new HibernateBundle<>(Account.class, MoneyTransfer.class) {
            @Override
            public DataSourceFactory getDataSourceFactory(SampleApplicationConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        };

    @Override
    public String getName() {
        return "Sample app";
    }

    @Override
    public void initialize(Bootstrap<SampleApplicationConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(SampleApplicationConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(hibernateBundle);
    }

    @Override
    public void run(SampleApplicationConfiguration configuration, Environment environment) {
        final AccountDAO accountDAO = new AccountDAO(hibernateBundle.getSessionFactory());
        final MoneyTransferDAO moneyTransferDAO = new MoneyTransferDAO(hibernateBundle.getSessionFactory());
        final AccountService accountService = new AccountServiceImpl(accountDAO, moneyTransferDAO);

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AccountResource(accountService));
    }
}
