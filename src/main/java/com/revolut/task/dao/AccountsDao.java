package com.revolut.task.dao;

import com.revolut.task.data.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO for Accounts
 */
public class AccountsDao {

    /**
     * Accounts storage
     */
    @NotNull
    private final Map<Long, Account> accountMap = new ConcurrentHashMap<>();

    /**
     * Find account by id strictly
     *
     * @param accountId account id
     * @throws RuntimeException if account not found.
     */
    @NotNull
    public Account getAccountNotNull(@Nullable Long accountId) {
        Account result = accountMap.get(accountId);
        if (result == null) {
            throw new RuntimeException(String.format("Account not found: %s", accountId));
        } else {
            return result;
        }
    }

    public void addAccount(@NotNull Account result) {
        accountMap.put(result.getId(), result);
    }
}
