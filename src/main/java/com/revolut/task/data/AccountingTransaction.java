package com.revolut.task.data;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Financial transaction between accounts
 */
public class AccountingTransaction {

    /**
     * CorrelationId of transaction
     */
    @NotNull
    private String correlationId;

    /**
     * From account id
     */
    private long from;

    /**
     * To account id
     */
    private long to;

    /**
     * Money amount
     */
    @NotNull
    private BigDecimal sum;

    public AccountingTransaction(@NotNull String correlationId, long from, long to, @NotNull BigDecimal sum) {
        this.correlationId = correlationId;
        this.from = from;
        this.to = to;
        this.sum = sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountingTransaction that = (AccountingTransaction) o;
        return Objects.equals(correlationId, that.correlationId) &&
                from == that.from &&
                to == that.to &&
                sum.compareTo(that.sum) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, from, to, sum);
    }
}
