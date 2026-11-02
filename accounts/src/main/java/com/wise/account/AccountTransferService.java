package com.wise.account;

import com.wise.account.exceptions.MissingAccountException;
import com.wise.account.exceptions.SameAccountTransferException;

import java.math.BigDecimal;
import java.util.Currency;

public class AccountTransferService {

    private static final String ACTION_TRANSFER = "transfer";

    public void transfer(Account from, Account to, Currency currency, BigDecimal amount) {
        if (from == null) {
            throw new MissingAccountException("from", ACTION_TRANSFER);
        }

        if (to == null) {
            throw new MissingAccountException("to", ACTION_TRANSFER);
        }

        if (from.getAccountId() == to.getAccountId()) {
            throw new SameAccountTransferException(from.getAccountId());
        }

        Account.validateAmount(amount, ACTION_TRANSFER);
        Account.validateCurrency(currency, ACTION_TRANSFER);

        // Always lock the lowest account id first, whatever the direction of the transfer,
        // so two opposite transfers can never wait on each other in a circle (deadlock)
        Account first = from.getAccountId() < to.getAccountId() ? from : to;
        Account second = first == from ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                // Withdraw first: if the money isn't there, nothing has changed yet
                from.withdraw(currency, amount);
                try {
                    to.deposit(currency, amount);
                } catch (RuntimeException e) {
                    // Compensate so the money never disappears between the two accounts
                    from.deposit(currency, amount);
                    throw e;
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }
}
