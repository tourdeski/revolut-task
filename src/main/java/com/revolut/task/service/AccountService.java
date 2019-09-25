package com.revolut.task.service;

import com.revolut.task.caller.Arg;
import com.revolut.task.caller.RemoteMethod;
import com.revolut.task.dao.AccountsDao;
import com.revolut.task.data.Account;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Service for operations with account
 */
public class AccountService implements RemoteService {

    private final AccountsDao repo = new AccountsDao();

    @RemoteMethod
    public BigDecimal getBalance(@Arg("accountId") @Nullable Long accountId) {
        Account account = repo.getAccountNotNull(accountId);
        return account.getBalance();
    }

    @RemoteMethod
    public String transfer(@Arg("correlationId") @Nullable String correlationId,
                           @Arg("fromId") @Nullable Long fromId,
                           @Arg("toId") @Nullable Long toId,
                           @Arg("sum") @Nullable BigDecimal sum) {
        if (sum == null) {
            return "Sum not specified";
        }
        if (correlationId == null || correlationId.isEmpty()) {
            return "CorrelationId not specified";
        }
        if (sum.signum() == -1) {
            return "Negative sum is not allowed";
        }
        Account from = repo.getAccountNotNull(fromId);
        Account to = repo.getAccountNotNull(toId);
        return from.transfer(correlationId, to, sum);
    }

    @RemoteMethod
    public Account createAccount(@Arg("name") @Nullable String name, @Arg("sum") @Nullable BigDecimal sum) {
        Objects.requireNonNull(name, "Name not specified");
        Objects.requireNonNull(sum, "Sum not specified");
        Account result = new Account(name, sum);
        repo.addAccount(result);
        return result;
    }

    @Override
    public String getName() {
        return "AccountService";
    }
}
