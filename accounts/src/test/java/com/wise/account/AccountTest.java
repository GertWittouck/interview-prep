package com.wise.account;

import com.wise.account.exceptions.InvalidAmountException;
import com.wise.account.exceptions.MissingAmountException;
import com.wise.account.exceptions.MissingCurrencyException;
import com.wise.account.exceptions.NegativeAmountException;
import com.wise.account.exceptions.WithDrawRejectedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wise.account.ConcurrencyTestSupport.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    @Nested
    class AccountBalanceTest {

        @ParameterizedTest
        @ValueSource(strings = {"EUR", "GBP"})
        void testNewAccountBalance(String currencyCode) {
            Account account = new Account();

            Currency currency = Currency.getInstance(currencyCode);
            BigDecimal currentBalance = account.getBalance(currency);

            assertThat(currentBalance).isEqualByComparingTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        }

        @ParameterizedTest
        @ValueSource(strings = {"EUR", "GBP"})
        void testExistingAccountBalanceWithCurrencies(String currencyCode)  {
            Account account = new Account();

            Currency currency = Currency.getInstance(currencyCode);
            BigDecimal amount = BigDecimal.valueOf(100.55);

            BigDecimal newBalance = account.deposit(currency, amount);

            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testExistingAccountBalanceWithUnknownCurrencies() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            BigDecimal amount = BigDecimal.valueOf(100.55);

            account.deposit(currency, amount);

            Currency missingCurrency = Currency.getInstance("GBP");
            assertThat(account.getBalance(missingCurrency)).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        }
    }

    @Nested
    class AccountDepositTest {

        @Test
        void testDepositToNewAccount() {
            BigDecimal amount = new BigDecimal("100.55");
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            BigDecimal newBalance = account.deposit(currency, amount);

            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testDepositToExistingAccount() {
            BigDecimal initialAmount = new BigDecimal("100.55");
            BigDecimal newDepositedAmount = BigDecimal.valueOf(50.55);

            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            BigDecimal currentBalance = account.deposit(currency, initialAmount);
            assertThat(account.getBalance(currency)).isEqualTo(currentBalance);

            BigDecimal newBalance = account.deposit(currency, newDepositedAmount);
            assertThat(newBalance).isEqualTo(BigDecimal.valueOf(151.10).setScale(2, RoundingMode.HALF_EVEN));
            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testDepositToNewAccountWithNegativeAmount() {
            BigDecimal amount = new BigDecimal("-100.55");
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(NegativeAmountException.class, () -> account.deposit(currency, amount));
        }

        @Test
        void testDepositToNewAccountWithNullAmount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(MissingAmountException.class, () -> account.deposit(currency, null));
        }

        @Test
        void testDepositToNewAccountWithZeroAmount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(InvalidAmountException.class, () -> account.deposit(currency, BigDecimal.ZERO));
        }

        @Test
        void testDepositToNewAccountWithInvalidPrecisionAmount() {
            Account account = new Account();

            BigDecimal amount = BigDecimal.valueOf(100.555);
            Currency currency = Currency.getInstance("EUR");
            assertThrows(InvalidAmountException.class, () -> account.deposit(currency, amount));
        }

        @ParameterizedTest
        @ValueSource(strings = {"100" , "100.5", "100.55"})
        void testDepositToNewAccountWithInvalidPrecisionAmount(String amountValue) {
            Account account = new Account();

            BigDecimal amount = new BigDecimal(amountValue);
            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, amount);

            BigDecimal expectedAmount = amount.setScale(2, RoundingMode.HALF_UP);
            assertThat(account.getBalance(currency)).isEqualTo(expectedAmount);
        }

        @Test
        void testDepositToNewAccountWithNullCurrency() {
            BigDecimal amount = new BigDecimal("100.55");
            Account account = new Account();

            assertThrows(MissingCurrencyException.class, () -> account.deposit(null, amount));
        }
    }

    @Nested
    class AccountWithdrawTest {

        @Test
        void testWithdrawFromNewAccount() {
            BigDecimal amount = new BigDecimal("50.25");
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(WithDrawRejectedException.class, () -> account.withdraw(currency, amount));
            assertThat(account.getBalance(currency)).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        void testWithdrawFromExistingAccount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, new BigDecimal("100.55"));

            BigDecimal newBalance = account.withdraw(currency, new BigDecimal("50.25"));
            assertThat(newBalance).isEqualTo(new BigDecimal("50.30"));
            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testWithdrawFullBalance() {
            BigDecimal amount = new BigDecimal("100.55");
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, amount);

            BigDecimal newBalance = account.withdraw(currency, amount);
            assertThat(newBalance).isEqualTo(new BigDecimal("0.00"));
            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testWithdrawMoreThanBalance() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            BigDecimal currentBalance = account.deposit(currency, new BigDecimal("100.55"));

            BigDecimal amount = new BigDecimal("100.56");
            assertThrows(WithDrawRejectedException.class, () -> account.withdraw(currency, amount));
            assertThat(account.getBalance(currency)).isEqualTo(currentBalance);
        }

        @Test
        void testWithdrawFromUnknownCurrency() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, new BigDecimal("100.55"));

            Currency missingCurrency = Currency.getInstance("GBP");
            BigDecimal amount = new BigDecimal("50.25");
            assertThrows(WithDrawRejectedException.class, () -> account.withdraw(missingCurrency, amount));
        }

        @Test
        void testWithdrawWithNegativeAmount() {
            BigDecimal amount = new BigDecimal("-100.55");
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, new BigDecimal("200.00"));

            assertThrows(NegativeAmountException.class, () -> account.withdraw(currency, amount));
        }

        @Test
        void testWithdrawWithNullAmount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(MissingAmountException.class, () -> account.withdraw(currency, null));
        }

        @Test
        void testWithdrawWithZeroAmount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            assertThrows(InvalidAmountException.class, () -> account.withdraw(currency, BigDecimal.ZERO));
        }

        @Test
        void testWithdrawWithInvalidPrecisionAmount() {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, new BigDecimal("200.00"));

            BigDecimal amount = BigDecimal.valueOf(100.555);
            assertThrows(InvalidAmountException.class, () -> account.withdraw(currency, amount));
        }

        @ParameterizedTest
        @ValueSource(strings = {"100", "100.5", "100.55"})
        void testWithdrawWithValidPrecisionAmount(String amountValue) {
            Account account = new Account();

            Currency currency = Currency.getInstance("EUR");
            account.deposit(currency, new BigDecimal("200.00"));

            BigDecimal amount = new BigDecimal(amountValue);
            account.withdraw(currency, amount);

            BigDecimal expectedAmount = new BigDecimal("200.00").subtract(amount).setScale(2, RoundingMode.HALF_UP);
            assertThat(account.getBalance(currency)).isEqualTo(expectedAmount);
        }

        @Test
        void testWithdrawWithNullCurrency() {
            BigDecimal amount = new BigDecimal("100.55");
            Account account = new Account();

            assertThrows(MissingCurrencyException.class, () -> account.withdraw(null, amount));
        }
    }

    @Nested
    class AccountCombinedOperationsTest {

        @Test
        void testDepositsAndWithdrawalsInSameCurrency() {
            Account account = new Account();
            Currency currency = Currency.getInstance("EUR");

            account.deposit(currency, new BigDecimal("100.00"));
            account.withdraw(currency, new BigDecimal("30.25"));
            account.deposit(currency, new BigDecimal("15.50"));
            BigDecimal newBalance = account.withdraw(currency, new BigDecimal("5.25"));

            assertThat(newBalance).isEqualTo(new BigDecimal("80.00"));
            assertThat(account.getBalance(currency)).isEqualTo(newBalance);
        }

        @Test
        void testBalancesPerCurrencyAreIndependent() {
            Account account = new Account();
            Currency eur = Currency.getInstance("EUR");
            Currency gbp = Currency.getInstance("GBP");
            Currency usd = Currency.getInstance("USD");

            account.deposit(eur, new BigDecimal("100.00"));
            account.deposit(gbp, new BigDecimal("50.00"));
            account.withdraw(eur, new BigDecimal("25.50"));
            account.deposit(gbp, new BigDecimal("10.10"));

            assertThat(account.getBalance(eur)).isEqualTo(new BigDecimal("74.50"));
            assertThat(account.getBalance(gbp)).isEqualTo(new BigDecimal("60.10"));
            assertThat(account.getBalance(usd)).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        void testRejectedWithdrawLeavesBalancesUntouched() {
            Account account = new Account();
            Currency eur = Currency.getInstance("EUR");
            Currency gbp = Currency.getInstance("GBP");

            account.deposit(eur, new BigDecimal("100.00"));
            account.deposit(gbp, new BigDecimal("20.00"));

            BigDecimal amount = new BigDecimal("50.00");
            assertThrows(WithDrawRejectedException.class, () -> account.withdraw(gbp, amount));

            assertThat(account.getBalance(eur)).isEqualTo(new BigDecimal("100.00"));
            assertThat(account.getBalance(gbp)).isEqualTo(new BigDecimal("20.00"));

            BigDecimal newBalance = account.withdraw(eur, amount);
            assertThat(newBalance).isEqualTo(new BigDecimal("50.00"));
        }

        @Test
        void testDepositAfterEmptyingBalance() {
            Account account = new Account();
            Currency currency = Currency.getInstance("EUR");

            account.deposit(currency, new BigDecimal("40.00"));
            account.withdraw(currency, new BigDecimal("40.00"));
            assertThat(account.getBalance(currency)).isEqualTo(new BigDecimal("0.00"));

            BigDecimal newBalance = account.deposit(currency, new BigDecimal("12.34"));
            assertThat(newBalance).isEqualTo(new BigDecimal("12.34"));
        }
    }

    @Nested
    class AccountConcurrencyTest {

        private static final Currency GBP = Currency.getInstance("GBP");

        // Repeated so the threads get many chances to interleave badly
        @RepeatedTest(100)
        void testTwoConcurrentWithdrawalsExceedingBalance() throws Exception {
            Account account = new Account();
            account.deposit(GBP, new BigDecimal("100.00"));

            BigDecimal amount = new BigDecimal("80.00");
            List<Throwable> failures = runConcurrently(2, () -> account.withdraw(GBP, amount));

            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst()).isInstanceOf(WithDrawRejectedException.class);
            assertThat(account.getBalance(GBP)).isEqualTo(new BigDecimal("20.00"));
        }

        @RepeatedTest(20)
        void testManyConcurrentWithdrawalsStopAtZero() throws Exception {
            Account account = new Account();
            account.deposit(GBP, new BigDecimal("100.00"));

            BigDecimal amount = new BigDecimal("10.00");
            List<Throwable> failures = runConcurrently(25, () -> account.withdraw(GBP, amount));

            assertThat(failures).hasSize(15).allMatch(WithDrawRejectedException.class::isInstance);
            assertThat(account.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
        }

        @RepeatedTest(20)
        void testConcurrentDepositsAreNotLost() throws Exception {
            Account account = new Account();

            BigDecimal amount = new BigDecimal("1.25");
            List<Throwable> failures = runConcurrently(200, () -> account.deposit(GBP, amount));

            assertThat(failures).isEmpty();
            assertThat(account.getBalance(GBP)).isEqualTo(new BigDecimal("250.00"));
        }

        @RepeatedTest(20)
        void testConcurrentDepositsAndWithdrawals() throws Exception {
            Account account = new Account();
            account.deposit(GBP, new BigDecimal("500.00"));

            BigDecimal amount = new BigDecimal("10.00");
            AtomicInteger counter = new AtomicInteger();
            List<Throwable> failures = runConcurrently(100, () -> counter.getAndIncrement() % 2 == 0
                    ? account.deposit(GBP, amount)
                    : account.withdraw(GBP, amount));

            assertThat(failures).isEmpty();
            assertThat(account.getBalance(GBP)).isEqualTo(new BigDecimal("500.00"));
        }
    }
}
