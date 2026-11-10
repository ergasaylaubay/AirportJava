package kz.airport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

public class BookingService {
    private Map<String, Booking> bookings = new HashMap<>();
    private List<Ticket> tickets = new ArrayList<>();
    private FareCalculator fareCalculator;

    public BookingService(FareCalculator fareCalculator) {
        if (fareCalculator == null) {
            throw new BookingException(
                    "Бағаны есептейтін объект қажет."
            );
        }

        this.fareCalculator = fareCalculator;
    }

    public Booking createBooking(Passenger passenger,
                                 Flight flight,
                                 Fare fare,
                                 int extraBaggagePieces) {

        if (passenger == null || flight == null || fare == null) {
            throw new BookingException(
                    "Жолаушы, рейс және тариф көрсетілуі керек."
            );
        }

        String cabinClass = fare.getCabinClass();

        if (!"Economy".equalsIgnoreCase(cabinClass)
                && !"Business".equalsIgnoreCase(cabinClass)) {
            throw new BookingException(
                    "Салон класы Economy немесе Business болуы керек."
            );
        }

        if (extraBaggagePieces < 0) {
            throw new BookingException(
                    "Қосымша багаж саны теріс болмауы керек."
            );
        }

        if (getRemainingCapacity(flight, cabinClass) <= 0) {
            throw new BookingException(
                    "Бұл салон класында брондауға бос орын жоқ."
            );
        }

        BigDecimal totalPrice = fareCalculator.calculatePrice(
                fare, extraBaggagePieces
        );

        String bookingCode;

        do {
            bookingCode = UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();
        } while (bookings.containsKey(bookingCode));

        Booking booking = new Booking(
                bookingCode,
                passenger,
                flight,
                fare,
                extraBaggagePieces,
                totalPrice
        );

        bookings.put(bookingCode, booking);
        return booking;
    }

    public int getRemainingCapacity(Flight flight, String cabinClass) {
        if (flight == null || cabinClass == null) {
            throw new BookingException(
                    "Рейс және салон класы көрсетілуі керек."
            );
        }

        int totalSeats = 0;

        for (Seat seat : flight.getSeats()) {
            if (cabinClass.equalsIgnoreCase(seat.getCabinClass())) {
                totalSeats++;
            }
        }

        int activeBookings = 0;

        for (Booking booking : bookings.values()) {
            boolean sameFlight = flight.getFlightId()
                    .equals(booking.getFlight().getFlightId());

            boolean sameCabin = cabinClass.equalsIgnoreCase(
                    booking.getFare().getCabinClass()
            );

            boolean active = !"CANCELLED".equals(
                    booking.getStatus()
            );

            if (sameFlight && sameCabin && active) {
                activeBookings++;
            }
        }

        return Math.max(0, totalSeats - activeBookings);
    }

    public Ticket confirmPayment(Booking booking) {
        requireManagedBooking(booking);

        if (!"CREATED".equals(booking.getStatus())) {
            throw new BookingException(
                    "Тек жаңа броньның төлемін растауға болады."
            );
        }

        String cabinClass = booking.getFare().getCabinClass();

        if (!"Economy".equalsIgnoreCase(cabinClass)
                && !"Business".equalsIgnoreCase(cabinClass)) {
            throw new BookingException("Белгісіз салон класы.");
        }

        booking.confirm();

        String ticketNumber = "T-" + booking.getBookingCode();
        Ticket ticket;

        if ("Economy".equalsIgnoreCase(cabinClass)) {
            ticket = new EconomyTicket(ticketNumber, booking);
        } else {
            ticket = new BusinessTicket(ticketNumber, booking);
        }

        tickets.add(ticket);
        return ticket;
    }

    public Booking findBooking(String code, String lastName) {
        if (code == null || lastName == null) {
            throw new BookingException(
                    "Бронь коды мен жолаушының тегін енгізіңіз."
            );
        }

        Booking booking = bookings.get(code.trim().toUpperCase());

        if (booking == null
                || !lastName.trim().equalsIgnoreCase(
                booking.getPassenger().getLastName())) {
            throw new BookingException(
                    "Бронь табылмады немесе тегі сәйкес келмейді."
            );
        }

        return booking;
    }

    public void cancelBooking(Booking booking) {
        requireManagedBooking(booking);
        booking.cancel();
    }

    public void checkIn(Booking booking, Seat seat) {
        requireManagedBooking(booking);
        booking.checkIn(seat);
    }

    public List<Booking> getBookings() {
        return List.copyOf(bookings.values());
    }

    public List<Ticket> getTickets() {
        return List.copyOf(tickets);
    }

    private void requireManagedBooking(Booking booking) {
        if (booking == null
                || bookings.get(booking.getBookingCode()) != booking) {
            throw new BookingException(
                    "Бұл бронь осы қызметте тіркелмеген."
            );
        }
    }
    public void restoreBooking(String code,
                               Passenger passenger,
                               Flight flight,
                               Fare fare,
                               int extraBaggagePieces,
                               BigDecimal totalPrice,
                               String status,
                               String seatNumber,
                               String ticketNumber,
                               LocalDateTime issuedAt) {

        if (code == null || code.isBlank()) {
            throw new BookingException("Сақталған бронь коды бос.");
        }

        code = code.trim().toUpperCase();

        if (bookings.containsKey(code)) {
            throw new BookingException(
                    "Файлда қайталанған бронь коды бар: " + code
            );
        }

        boolean paid = "CONFIRMED".equals(status)
                || "CHECKED_IN".equals(status);

        if (!"CREATED".equals(status)
                && !"CANCELLED".equals(status)
                && !paid) {
            throw new BookingException(
                    "Файлда белгісіз бронь күйі бар: " + status
            );
        }

        Booking booking = new Booking(
                code, passenger, flight, fare,
                extraBaggagePieces, totalPrice
        );

        String cabinClass = fare.getCabinClass();

        if (!"Economy".equalsIgnoreCase(cabinClass)
                && !"Business".equalsIgnoreCase(cabinClass)) {
            throw new BookingException("Файлдағы салон класы дұрыс емес.");
        }

        if (!"CANCELLED".equals(status)
                && getRemainingCapacity(flight, cabinClass) <= 0) {
            throw new BookingException(
                    "Сақталған броньдар саны рейс сыйымдылығынан артық."
            );
        }

        if (!"CHECKED_IN".equals(status) && seatNumber != null) {
            throw new BookingException(
                    "Тіркелмеген броньға орын бекітілмеуі керек."
            );
        }

        if (!paid && (ticketNumber != null || issuedAt != null)) {
            throw new BookingException(
                    "Төленбеген броньда билет болмауы керек."
            );
        }

        Ticket ticket = null;

        if (paid) {
            if (ticketNumber == null || ticketNumber.isBlank()
                    || issuedAt == null) {
                throw new BookingException(
                        "Сақталған билеттің деректері толық емес."
                );
            }

            for (Ticket existing : tickets) {
                if (ticketNumber.equals(existing.getTicketNumber())) {
                    throw new BookingException(
                            "Файлда қайталанған билет нөмірі бар."
                    );
                }
            }

            booking.confirm();

            if ("Economy".equalsIgnoreCase(cabinClass)) {
                ticket = new EconomyTicket(ticketNumber, booking);
            } else {
                ticket = new BusinessTicket(ticketNumber, booking);
            }

            ticket.restoreIssuedAt(issuedAt);
        }

        if ("CANCELLED".equals(status)) {
            booking.cancel();
        }

        if ("CHECKED_IN".equals(status)) {
            Seat selectedSeat = null;

            for (Seat seat : flight.getSeats()) {
                if (seat.getSeatNumber().equalsIgnoreCase(seatNumber)) {
                    selectedSeat = seat;
                    break;
                }
            }

            if (selectedSeat == null) {
                throw new BookingException(
                        "Сақталған орын рейстен табылмады."
                );
            }

            booking.checkIn(selectedSeat);
        }

        bookings.put(code, booking);

        if (ticket != null) {
            tickets.add(ticket);
        }
    }
}