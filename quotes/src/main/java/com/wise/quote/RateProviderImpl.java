package com.wise.quote;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RateProviderImpl implements RateProvider {

    private final Map<RateCurrency, BigDecimal> rates = Map.of(
        new RateCurrency(Currency.getInstance("GBP"), Currency.getInstance("EUR")), new BigDecimal("1.15"),
        new RateCurrency(Currency.getInstance("GBP"), Currency.getInstance("USD")), new BigDecimal("1.27"),
        new RateCurrency(Currency.getInstance("GBP"), Currency.getInstance("JPY")), new BigDecimal("190.50"),
        new RateCurrency(Currency.getInstance("EUR"), Currency.getInstance("GBP")), new BigDecimal("0.87"),
        new RateCurrency(Currency.getInstance("USD"), Currency.getInstance("GBP")), new BigDecimal("0.79"));

    @Override
    public Optional<BigDecimal> rate(Currency from, Currency to) {
        RateCurrency search = new RateCurrency(from, to);
        return Optional.ofNullable(rates.get(search));
    }

    private record RateCurrency(Currency from, Currency rate) { }
}
