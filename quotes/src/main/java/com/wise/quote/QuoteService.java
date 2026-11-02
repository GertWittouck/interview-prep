package com.wise.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;

public class QuoteService {

    private final Map<Currency, FeeRate> feeRates;
    private final RateProvider rateProvider;

    public QuoteService(Map<Currency, FeeRate> feeRates, RateProvider rateProvider) {
        this.feeRates = feeRates;
        this.rateProvider = rateProvider;
    }

    public Quote calculateQuote(
            BigDecimal amount,
            Currency from,
            Currency to) {

        validateCurrencies(from, to);

        int sourceScale = from.getDefaultFractionDigits();
        int targetScale = to.getDefaultFractionDigits();

        validateAmount(amount, sourceScale);

        FeeRate feeRate = Optional.ofNullable(feeRates.get(from)).orElseThrow(() -> new UnsupportedCurrencyException(from));

        BigDecimal conversionRate = loadConversionRate(from, to);

        BigDecimal fee = feeRate.fixedRate().add(amount.multiply(feeRate.variableRate())).setScale(sourceScale, RoundingMode.HALF_UP);
        if (fee.compareTo(amount) >= 0) {
            throw new FeeLargerThanAmountException(fee, amount);
        }

        BigDecimal convertedAmount = amount.subtract(fee);
        BigDecimal receivedAmount = convertedAmount.multiply(conversionRate).setScale(targetScale, RoundingMode.HALF_UP);

        return new Quote(fee, convertedAmount, receivedAmount);
    }

    private BigDecimal loadConversionRate(Currency from, Currency to) {
        return rateProvider.rate(from, to).orElseThrow(() -> new NotSupportedConversionRate(from, to));
    }

    private void validateCurrencies(Currency from, Currency to) {
        if (from == null || to == null) {
            throw new MissingCurrencyException();
        }

        if (from.equals(to)) {
            throw new SameCurrencyException(from);
        }
    }

    private static void validateAmount(BigDecimal amount, int scale) {
        if (amount == null) {
            throw new MissingAmountException();
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeAmountException(amount);
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ZeroAmountException();
        }

        if (amount.stripTrailingZeros().scale() > scale) {
            throw new InvalidFractionDigitsException(amount, scale);
        }
    }

}
