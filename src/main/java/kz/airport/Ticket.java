package kz.airport;

import java.time.LocalDateTime;

public abstract class Ticket {
    private String ticketNumber;
    private Booking booking;
    private LocalDateTime issuedAt;

    public Ticket(String ticketNumber, Booking booking) {
        if (ticketNumber == null || ticketNumber.isBlank()) {
            throw new BookingException(
                    "Билет нөмірі бос болмауы керек."
            );
        }

        if (booking == null) {
            throw new BookingException(
                    "Билет үшін бронь көрсетілуі керек."
            );
        }

        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BookingException(
                    "Билет шығару үшін төлем расталуы керек."
            );
        }

        this.ticketNumber = ticketNumber;
        this.booking = booking;
        this.issuedAt = LocalDateTime.now();
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public Booking getBooking() {
        return booking;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public abstract String getCabinClass();

    void restoreIssuedAt(LocalDateTime issuedAt) {
        if (issuedAt == null) {
            throw new BookingException(
                    "Билеттің шығарылған уақыты көрсетілуі керек."
            );
        }

        this.issuedAt = issuedAt;
    }
}