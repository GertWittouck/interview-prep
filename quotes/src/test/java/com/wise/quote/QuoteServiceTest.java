package com.wise.quote;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuoteServiceTest {

    private final Map<Currency, FeeRate> feeRates = Map.of(
            Currency.getInstance("GBP"), new FeeRate(new BigDecimal("0.50"), new BigDecimal("0.35")),
            Currency.getInstance("EUR"), new FeeRate(new BigDecimal("0.45"), new BigDecimal("0.41")),
            Currency.getInstance("USD"), new FeeRate(new BigDecimal("1.00"), new BigDecimal("0.50"))
    );

    private QuoteService testable;

    @BeforeEach
    void setUp() {
        testable = new QuoteService(feeRates, new RateProviderImpl());
    }

    @Nested
    class calculateQuoteValidationTest {
        @Test
        void testCalculateQuoteWithMissingAmount() {
            assertThrows(MissingAmountException.class, () -> testable.calculateQuote(null, Currency.getInstance("USD"), Currency.getInstance("EUR")));
        }

        @Test
        void testCalculateQuoteWithNegativeAmount() {
            BigDecimal amount = new BigDecimal("-10.00");

            assertThrows(NegativeAmountException.class, () -> testable.calculateQuote(amount, Currency.getInstance("USD"), Currency.getInstance("EUR")));
        }

        @Test
        void testCalculateQuoteWithZeroAmount() {
            BigDecimal amount = BigDecimal.ZERO;

            assertThrows(ZeroAmountException.class, () -> testable.calculateQuote(amount, Currency.getInstance("USD"), Currency.getInstance("EUR")));
        }

        @Test
        void testCalculateQuoteWithInvalidPrecisionAmount() {
            assertThrows(InvalidFractionDigitsException.class, () -> testable.calculateQuote(new BigDecimal("10.005"), Currency.getInstance("USD"), Currency.getInstance("GBP")));
            assertThrows(InvalidFractionDigitsException.class, () -> testable.calculateQuote(new BigDecimal("10.005"), Currency.getInstance("USD"), Currency.getInstance("EUR")));
            assertThrows(InvalidFractionDigitsException.class, () -> testable.calculateQuote(new BigDecimal("10.005"), Currency.getInstance("GBP"), Currency.getInstance("USD")));
            assertThrows(InvalidFractionDigitsException.class, () -> testable.calculateQuote(new BigDecimal("10.005"), Currency.getInstance("GBP"), Currency.getInstance("JPY")));
        }

        @Test
        void testCalculateQuoteWithMissingSourceCurrency() {
            BigDecimal amount = new BigDecimal("10.00");

            assertThrows(MissingCurrencyException.class, () -> testable.calculateQuote(amount, null, Currency.getInstance("EUR")));
        }

        @Test
        void testCalculateQuoteWithMissingTargetCurrency() {
            BigDecimal amount = new BigDecimal("10.00");

            assertThrows(MissingCurrencyException.class, () -> testable.calculateQuote(amount, Currency.getInstance("EUR"), null));
        }

        @Test
        void testCalculateQuoteWithSameSourceAndTargetCurrency() {
            BigDecimal amount = new BigDecimal("10.00");

            assertThrows(SameCurrencyException.class, () -> testable.calculateQuote(amount, Currency.getInstance("USD"), Currency.getInstance("USD")));
        }

        @Test
        void testCalculateQuoteWithJPYasTargetCurrency() {
            BigDecimal amount = new BigDecimal("10");

            Quote result = testable.calculateQuote(amount, Currency.getInstance("GBP"), Currency.getInstance("JPY"));

            assertThat(result).isNotNull();
        }


    }

    @Nested
    class calculateQuoteTest {

        @Test
        void testCalculateQuoteWithUnSupportedSourceCurrency() {
            BigDecimal amount = new BigDecimal("10.00");
            assertThrows(UnsupportedCurrencyException.class, () -> testable.calculateQuote(amount, Currency.getInstance("DKK"), Currency.getInstance("USD")));
        }

        @Test
        void testCalculateQuoteWithFeeLargerThanAmount() {
            BigDecimal amount = new BigDecimal("0.40");

            assertThrows(FeeLargerThanAmountException.class, () -> testable.calculateQuote(amount, Currency.getInstance("GBP"), Currency.getInstance("USD")));
        }

        @ParameterizedTest
        @MethodSource("quoteRequests")
        void testCalculateQuote(BigDecimal amount, Currency sourceCurrency, Currency targetCurrency, Quote expectedQuote) {
            Quote result = testable.calculateQuote(amount, sourceCurrency, targetCurrency);
            assertThat(result).isEqualTo(expectedQuote);
        }

        private static Stream<Arguments> quoteRequests() {
            return Stream.of(
                    Arguments.of(new BigDecimal("1000.00"), Currency.getInstance("GBP"),
                            Currency.getInstance("EUR"),
                            new Quote(new BigDecimal("4.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("996.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("1145.40").setScale(2, RoundingMode.HALF_UP))),
                    Arguments.of(
                            new BigDecimal("1000.00"),
                            Currency.getInstance("EUR"),
                            Currency.getInstance("GBP"),
                            new Quote(new BigDecimal("4.55").setScale(2, RoundingMode.HALF_UP), new BigDecimal("995.45").setScale(2, RoundingMode.HALF_UP), new BigDecimal("866.04").setScale(2, RoundingMode.HALF_UP))),
                    Arguments.of(
                            new BigDecimal("1000.00"),
                            Currency.getInstance("USD"),
                            Currency.getInstance("GBP"),
                            new Quote(new BigDecimal("6.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("994.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("785.26").setScale(2, RoundingMode.HALF_UP))),
                    Arguments.of(
                            new BigDecimal("1000"),
                            Currency.getInstance("GBP"),
                            Currency.getInstance("JPY"),
                            new Quote(new BigDecimal("4.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("996.00").setScale(2, RoundingMode.HALF_UP), new BigDecimal("189738").setScale(0, RoundingMode.HALF_UP))),
                    Arguments.of(
                            new BigDecimal("333.33"),
                            Currency.getInstance("GBP"),
                            Currency.getInstance("EUR"),
                            new Quote(new BigDecimal("1.67").setScale(2, RoundingMode.HALF_UP), new BigDecimal("331.66").setScale(2, RoundingMode.HALF_UP), new BigDecimal("381.41").setScale(2, RoundingMode.HALF_UP)))
            );
        }

    }

}
