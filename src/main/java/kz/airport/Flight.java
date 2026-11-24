package kz.airport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Рейс кестесі мен орындар; броньға қолжетімді санды BookingService есептейді. */
public class Flight {
    private final String flightId;
    private String flightNumber;
    private final Airport departureAirport;
    private final Airport arrivalAirport;
    private LocalDateTime departureTime;
    private final List<Seat> seats;

    public Flight(String flightId,
                  String flightNumber,
                  Airport departureAirport,
                  Airport arrivalAirport,
                  LocalDateTime departureTime) {
        this.flightId = Validation.requiredText(flightId, "Рейс ID");
        this.flightNumber = Validation.requiredText(flightNumber, "Рейс нөмірі");
        this.departureAirport = java.util.Objects.requireNonNull(departureAirport, "departureAirport");
        this.arrivalAirport = java.util.Objects.requireNonNull(arrivalAirport, "arrivalAirport");
        this.departureTime = java.util.Objects.requireNonNull(departureTime, "departureTime");
        this.seats = new ArrayList<>();
    }

    public String getFlightId() {
        return flightId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Airport getDepartureAirport() {
        return departureAirport;
    }

    public Airport getArrivalAirport() {
        return arrivalAirport;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public List<Seat> getSeats() {
        return List.copyOf(seats);
    }

    public void addSeat(Seat seat) {
        if (seat == null) {
            throw new IllegalArgumentException("Орын бос мән болмауы керек.");
        }

        for (Seat existingSeat : seats) {
            if (existingSeat.getSeatNumber()
                    .equalsIgnoreCase(seat.getSeatNumber())) {
                throw new IllegalArgumentException(
                        "Бұл нөмірлі орын рейсте бар."
                );
            }
        }

        seats.add(seat);
    }

    public int getAvailableSeats(String cabinClass) {
        int count = 0;

        for (Seat seat : seats) {
            if (seat.getCabinClass().equalsIgnoreCase(cabinClass)
                    && seat.isAvailable()) {
                count++;
            }
        }

        return count;
    }

    public boolean hasAvailableSeats(String cabinClass) {
        return getAvailableSeats(cabinClass) > 0;
    }

    public void updateDetails(String flightNumber,
                              LocalDateTime departureTime) {
        if (flightNumber == null || flightNumber.isBlank()
                || departureTime == null) {
            throw new BookingException(
                    "Рейс нөмірі мен ұшу уақыты көрсетілуі керек."
            );
        }

        this.flightNumber = flightNumber.trim();
        this.departureTime = java.util.Objects.requireNonNull(departureTime, "departureTime");
    }
}
