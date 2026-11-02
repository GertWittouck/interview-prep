package com.wise.account;

import com.wise.account.exceptions.InvalidAmountException;
import com.wise.account.exceptions.MissingAmountException;
import com.wise.account.exceptions.MissingCurrencyException;
import com.wise.account.exceptions.NegativeAmountException;
import com.wise.account.exceptions.WithDrawRejectedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Account {

    private static final String ACTION_DEPOSIT = "deposit";
    private static final String ACTION_WITHDRAW = "withdraw";

    // Generated so every account has a unique id, which transfers use as a global lock order
    private static final AtomicLong ID_SEQUENCE = new AtomicLong();

    private final long accountId;

    private final Map<Currency, BigDecimal> balances;

    // Private lock so only the transfer service can hold it, unlike synchronising on the public Account object
    private final ReentrantLock lock;

    public Account() {
        this.accountId = ID_SEQUENCE.incrementAndGet();
        this.balances = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    public long getAccountId() {
        return accountId;
    }

    ReentrantLock getLock() {
        return lock;
    }

    public BigDecimal getBalance(Currency currency) {
        if (!balances.containsKey(currency)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return balances.get(currency);
    }

    public BigDecimal deposit(Currency currency, BigDecimal amount) {
        validateAmount(amount, ACTION_DEPOSIT);
        validateCurrency(currency, ACTION_DEPOSIT);

        return balances.compute(currency, (c, currentBalance) -> {
            BigDecimal balance = currentBalance == null ? BigDecimal.ZERO : currentBalance;
            return balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        });
    }

    public BigDecimal withdraw(Currency currency, BigDecimal amount) {
        validateAmount(amount, ACTION_WITHDRAW);

        validateCurrency(currency, ACTION_WITHDRAW);

        return balances.compute(currency, (c, currentBalance) -> {
            BigDecimal balance = currentBalance == null ? BigDecimal.ZERO : currentBalance;
            if (balance.compareTo(amount) < 0) {
                throw new WithDrawRejectedException(amount, balance, currency);
            }
            return balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        });
    }

    static void validateAmount(BigDecimal amount, String actionDeposit) {
        if (amount == null) {
            throw new MissingAmountException(actionDeposit);
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountException(amount, actionDeposit);
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidAmountException(amount, actionDeposit);
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException(amount, actionDeposit);
        }
    }

    static void validateCurrency(Currency currency, String actionDeposit) {
        if (currency == null) {
            throw new MissingCurrencyException(actionDeposit);
        }
    }
}
