package com.wise.account;

import com.wise.account.exceptions.InvalidAmountException;
import com.wise.account.exceptions.MissingAccountException;
import com.wise.account.exceptions.MissingAmountException;
import com.wise.account.exceptions.MissingCurrencyException;
import com.wise.account.exceptions.NegativeAmountException;
import com.wise.account.exceptions.SameAccountTransferException;
import com.wise.account.exceptions.WithDrawRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wise.account.ConcurrencyTestSupport.runConcurrently;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTransferServiceTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency EUR = Currency.getInstance("EUR");

    private AccountTransferService transferService;
    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        transferService = new AccountTransferService();
        alice = new Account();
        bob = new Account();
    }

    @Nested
    class TransferTest {

        @Test
        void testTransferMovesMoney() {
            alice.deposit(GBP, new BigDecimal("100.00"));

            transferService.transfer(alice, bob, GBP, new BigDecimal("30.25"));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("69.75"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("30.25"));
        }

        @Test
        void testTransferFullBalance() {
            alice.deposit(GBP, new BigDecimal("100.00"));

            transferService.transfer(alice, bob, GBP, new BigDecimal("100.00"));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        void testTransferAddsToExistingBalance() {
            alice.deposit(GBP, new BigDecimal("100.00"));
            bob.deposit(GBP, new BigDecimal("50.00"));

            transferService.transfer(alice, bob, GBP, new BigDecimal("25.00"));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("75.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("75.00"));
        }

        @Test
        void testTransferLeavesOtherCurrenciesUntouched() {
            alice.deposit(GBP, new BigDecimal("100.00"));
            alice.deposit(EUR, new BigDecimal("40.00"));
            bob.deposit(EUR, new BigDecimal("10.00"));

            transferService.transfer(alice, bob, GBP, new BigDecimal("60.00"));

            assertThat(alice.getBalance(EUR)).isEqualTo(new BigDecimal("40.00"));
            assertThat(bob.getBalance(EUR)).isEqualTo(new BigDecimal("10.00"));
        }

        @Test
        void testTransferWithInsufficientFunds() {
            alice.deposit(GBP, new BigDecimal("100.00"));
            bob.deposit(GBP, new BigDecimal("5.00"));

            BigDecimal amount = new BigDecimal("100.01");
            assertThrows(WithDrawRejectedException.class, () -> transferService.transfer(alice, bob, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("5.00"));
        }

        @Test
        void testTransferInCurrencyWithoutBalance() {
            alice.deposit(GBP, new BigDecimal("100.00"));

            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(WithDrawRejectedException.class, () -> transferService.transfer(alice, bob, EUR, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(bob.getBalance(EUR)).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void testTransferIsRefundedWhenDepositFails() {
            alice.deposit(GBP, new BigDecimal("100.00"));
            Account brokenAccount = new Account() {
                @Override
                public BigDecimal deposit(Currency currency, BigDecimal amount) {
                    throw new IllegalStateException("Deposit failed");
                }
            };

            BigDecimal amount = new BigDecimal("40.00");
            assertThrows(IllegalStateException.class, () -> transferService.transfer(alice, brokenAccount, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(brokenAccount.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
        }
    }

    @Nested
    class TransferValidationTest {

        @BeforeEach
        void setUp() {
            alice.deposit(GBP, new BigDecimal("100.00"));
        }

        @Test
        void testTransferWithNullFromAccount() {
            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(MissingAccountException.class, () -> transferService.transfer(null, bob, GBP, amount));
        }

        @Test
        void testTransferWithNullToAccount() {
            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(MissingAccountException.class, () -> transferService.transfer(alice, null, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        void testTransferToSameAccount() {
            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(SameAccountTransferException.class, () -> transferService.transfer(alice, alice, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        void testTransferWithNullCurrency() {
            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(MissingCurrencyException.class, () -> transferService.transfer(alice, bob, null, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        void testTransferWithNullAmount() {
            assertThrows(MissingAmountException.class, () -> transferService.transfer(alice, bob, GBP, null));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        void testTransferWithNegativeAmount() {
            BigDecimal amount = new BigDecimal("-10.00");
            assertThrows(NegativeAmountException.class, () -> transferService.transfer(alice, bob, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "10.001"})
        void testTransferWithInvalidAmount(String amountValue) {
            BigDecimal amount = new BigDecimal(amountValue);
            assertThrows(InvalidAmountException.class, () -> transferService.transfer(alice, bob, GBP, amount));

            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
        }
    }

    @Nested
    class TransferConcurrencyTest {

        // Without a global lock order these two would deadlock, which runConcurrently reports as a timeout.
        // A single transfer per thread rarely collides, so each thread loops to keep the race window open.
        @RepeatedTest(5)
        void testOppositeTransfersDoNotDeadlock() throws Exception {
            alice.deposit(GBP, new BigDecimal("100.00"));
            bob.deposit(GBP, new BigDecimal("100.00"));

            BigDecimal amount = new BigDecimal("0.01");
            AtomicInteger counter = new AtomicInteger();
            List<Throwable> failures = runConcurrently(2, () -> {
                boolean aliceToBob = counter.getAndIncrement() == 0;
                for (int i = 0; i < 10_000; i++) {
                    if (aliceToBob) {
                        transferService.transfer(alice, bob, GBP, amount);
                    } else {
                        transferService.transfer(bob, alice, GBP, amount);
                    }
                }
                return null;
            });

            assertThat(failures).isEmpty();
            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @RepeatedTest(20)
        void testConcurrentTransfersCannotOverdraw() throws Exception {
            alice.deposit(GBP, new BigDecimal("100.00"));

            List<Throwable> failures = runConcurrently(10, () -> {
                transferService.transfer(alice, bob, GBP, new BigDecimal("20.00"));
                return null;
            });

            assertThat(failures).hasSize(5).allMatch(WithDrawRejectedException.class::isInstance);
            assertThat(alice.getBalance(GBP)).isEqualTo(new BigDecimal("0.00"));
            assertThat(bob.getBalance(GBP)).isEqualTo(new BigDecimal("100.00"));
        }

        @RepeatedTest(20)
        void testRandomTransfersBetweenManyAccountsKeepTotalMoney() throws Exception {
            List<Account> accounts = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Account account = new Account();
                account.deposit(GBP, new BigDecimal("100.00"));
                accounts.add(account);
            }

            List<Throwable> failures = runConcurrently(200, () -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                int fromIndex = random.nextInt(accounts.size());
                int toIndex = (fromIndex + 1 + random.nextInt(accounts.size() - 1)) % accounts.size();
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(1, 5001), 2);

                transferService.transfer(accounts.get(fromIndex), accounts.get(toIndex), GBP, amount);
                return null;
            });

            // Some transfers may be rejected for insufficient funds, but nothing else may go wrong
            assertThat(failures).allMatch(WithDrawRejectedException.class::isInstance);

            BigDecimal total = accounts.stream()
                    .map(account -> account.getBalance(GBP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(total).isEqualTo(new BigDecimal("500.00"));
            assertThat(accounts).allMatch(account -> account.getBalance(GBP).signum() >= 0);
        }
    }
}
