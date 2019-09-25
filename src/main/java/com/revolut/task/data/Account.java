package com.revolut.task.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple user account implementation
 */
public class Account {

    /**
     * Id
     */
    @NotNull
    private final Long id;

    /**
     * Name
     */
    @NotNull
    private final String name;

    /**
     * Account balance
     */
    @NotNull
    private volatile AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);

    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * List of successfully completed financial transactions
     */
    private transient Map<AccountingTransaction, Boolean> accountingTransactions = new ConcurrentHashMap<>();

    public Account(@NotNull String name, @NotNull BigDecimal sum) {
        this(ThreadLocalRandom.current().nextLong(1000000, Long.MAX_VALUE), name, sum);
    }

    public Account(@NotNull Long id, @NotNull String name, @NotNull BigDecimal sum) {
        this.id = id;
        this.name = name;
        this.balance.set(sum);
    }

    /**
     * Thread safe transfer money operation
     *
     * @param correlationId correlationId provide idempotency
     * @param to            account, where to transfer
     * @param sum           money amount
     * @return status of operation
     */
    @NotNull
    public String transfer(@NotNull String correlationId, @NotNull Account to, @NotNull BigDecimal sum) {
        AccountingTransaction transaction = new AccountingTransaction(correlationId, id, to.id, sum);
        if (accountingTransactions.containsKey(transaction)) {
            return "Duplicate operation was rejected";
        }
        if (balance.get().compareTo(sum) < 0) {
            return "Insufficient funds";
        }
        try {
            lock.writeLock().lock();
            if (accountingTransactions.containsKey(transaction)) {
                return "Duplicate operation was rejected";
            }
            if (balance.get().compareTo(sum) < 0) {
                return "Insufficient funds";
            }
            withdraw(sum);
            to.deposit(sum);
            accountingTransactions.put(transaction, true);
            return "Success";
        } finally {
            lock.writeLock().unlock();
        }
    }


    private void withdraw(@NotNull BigDecimal sum) {
        balance.getAndAccumulate(sum, BigDecimal::subtract);
    }

    private void deposit(@NotNull BigDecimal sum) {
        balance.getAndAccumulate(sum, BigDecimal::add);
    }

    @NotNull
    public Long getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public BigDecimal getBalance() {
        return balance.get();
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
