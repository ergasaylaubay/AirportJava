package kz.airport;

import java.math.BigDecimal;

public interface FareCalculator {

    BigDecimal calculatePrice(Fare fare, int extraBaggagePieces);
}