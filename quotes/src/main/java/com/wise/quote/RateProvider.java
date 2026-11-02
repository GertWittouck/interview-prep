package com.wise.quote;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

public interface RateProvider {

    Optional<BigDecimal> rate(Currency from, Currency to);
}
