package kz.airport;

import java.math.BigDecimal;

/** Тариф бағасы, кіретін багаж және қосымша багаж бағасы. */
public class Fare {
    private final String fareName;
    private final String cabinClass;
    private final BigDecimal basePrice;
    private final int baggagePieces;
    private final int baggageWeightPerPiece;
    private final BigDecimal extraBaggagePrice;

    public Fare(String fareName,
                String cabinClass,
                BigDecimal basePrice,
                int baggagePieces,
                int baggageWeightPerPiece,
                BigDecimal extraBaggagePrice) {

        if (basePrice == null || extraBaggagePrice == null) {
            throw new IllegalArgumentException(
                    "Баға бос мән болмауы керек."
            );
        }

        if (basePrice.signum() < 0
                || extraBaggagePrice.signum() < 0
                || baggagePieces < 0
                || baggageWeightPerPiece < 0) {
            throw new IllegalArgumentException(
                    "Баға және багаж көрсеткіштері теріс болмауы керек."
            );
        }

        this.fareName = Validation.requiredText(fareName, "Тариф атауы");
        this.cabinClass = Validation.requiredText(cabinClass, "Салон класы");
        this.basePrice = basePrice;
        this.baggagePieces = baggagePieces;
        this.baggageWeightPerPiece = baggageWeightPerPiece;
        this.extraBaggagePrice = extraBaggagePrice;
    }

    public String getFareName() {
        return fareName;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public int getBaggagePieces() {
        return baggagePieces;
    }

    public int getBaggageWeightPerPiece() {
        return baggageWeightPerPiece;
    }

    public BigDecimal getExtraBaggagePrice() {
        return extraBaggagePrice;
    }
}
