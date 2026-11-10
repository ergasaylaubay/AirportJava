package kz.airport;

import java.math.BigDecimal;

public class Booking {
    private String bookingCode;
    private Passenger passenger;
    private Flight flight;
    private Fare fare;
    private Seat seat;
    private int extraBaggagePieces;
    private BigDecimal totalPrice;
    private String status;

    public Booking(String bookingCode,
                   Passenger passenger,
                   Flight flight,
                   Fare fare,
                   int extraBaggagePieces,
                   BigDecimal totalPrice) {

        if (bookingCode == null || bookingCode.isBlank()) {
            throw new BookingException("Бронь коды бос болмауы керек.");
        }

        if (passenger == null || flight == null || fare == null) {
            throw new BookingException(
                    "Жолаушы, рейс және тариф көрсетілуі керек."
            );
        }

        if (extraBaggagePieces < 0
                || totalPrice == null
                || totalPrice.signum() < 0) {
            throw new BookingException(
                    "Багаж саны мен жалпы баға дұрыс болуы керек."
            );
        }

        this.bookingCode = bookingCode;
        this.passenger = passenger;
        this.flight = flight;
        this.fare = fare;
        this.extraBaggagePieces = extraBaggagePieces;
        this.totalPrice = totalPrice;
        this.status = "CREATED";
        this.seat = null;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Flight getFlight() {
        return flight;
    }

    public Fare getFare() {
        return fare;
    }

    public Seat getSeat() {
        return seat;
    }

    public int getExtraBaggagePieces() {
        return extraBaggagePieces;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void confirm() {
        if (!"CREATED".equals(status)) {
            throw new BookingException(
                    "Тек жаңа броньның төлемін растауға болады."
            );
        }

        status = "CONFIRMED";
    }

    public void cancel() {
        if (!"CREATED".equals(status)) {
            throw new BookingException(
                    "Тек төленбеген броннан бас тартуға болады."
            );
        }

        status = "CANCELLED";
    }

    public void checkIn(Seat seat) {
        if (!"CONFIRMED".equals(status)) {
            throw new BookingException(
                    "Тіркелу үшін бронь төлемі расталған болуы керек."
            );
        }

        if (seat == null) {
            throw new BookingException("Орын таңдалмады.");
        }

        if (!flight.getSeats().contains(seat)) {
            throw new BookingException(
                    "Бұл орын таңдалған рейске тиесілі емес."
            );
        }

        if (!seat.getCabinClass().equalsIgnoreCase(fare.getCabinClass())) {
            throw new BookingException(
                    "Орынның салон класы тарифке сәйкес келмейді."
            );
        }

        if (!seat.isAvailable()) {
            throw new BookingException("Бұл орын бос емес.");
        }

        seat.occupy();
        this.seat = seat;
        status = "CHECKED_IN";
    }
}