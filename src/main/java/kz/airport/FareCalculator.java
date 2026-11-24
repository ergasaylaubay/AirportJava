package kz.airport;

import java.math.BigDecimal;

/** Тариф пен қосымша багаж бойынша баға есептеу келісімі. */
public interface FareCalculator {

    BigDecimal calculatePrice(Fare fare, int extraBaggagePieces);
}
