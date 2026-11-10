package kz.airport;

import java.math.BigDecimal;

public class StandardFareCalculator implements FareCalculator {

    @Override
    public BigDecimal calculatePrice(Fare fare, int extraBaggagePieces) {
        if (fare == null) {
            throw new IllegalArgumentException(
                    "Тариф көрсетілуі керек."
            );
        }

        if (extraBaggagePieces < 0) {
            throw new IllegalArgumentException(
                    "Қосымша багаж саны теріс болмауы керек."
            );
        }

        BigDecimal baggageCost = fare.getExtraBaggagePrice()
                .multiply(BigDecimal.valueOf(extraBaggagePieces));

        return fare.getBasePrice().add(baggageCost);
    }
}