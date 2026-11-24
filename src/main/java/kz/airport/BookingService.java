package kz.airport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Броньдарды код бойынша сақтап, сыйымдылық пен төлем ережелерін орындайды. */
public class BookingService {
    private final Map<String, Booking> bookings = new HashMap<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private final FareCalculator fareCalculator;

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

        // Ереже мәзірден тыс шақырылғанда да орындалады. Ескі броньды оқу бөлек.
        if (!flight.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BookingException("Ұшу уақыты өтіп кеткен рейске бронь жасауға болмайды.");
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
                    .substring(0, 8).toUpperCase(Locale.ROOT);
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

    /** Белсенді бронь орынды резервтейді; CHECKED_IN орнын екінші рет шегермейміз. */
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

            boolean active = !Booking.CANCELLED.equals(
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

        if (!Booking.CREATED.equals(booking.getStatus())) {
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
        Ticket ticket = createTicket(ticketNumber, booking);

        tickets.add(ticket);
        return ticket;
    }

    public Booking findBooking(String code, String lastName) {
        if (code == null || lastName == null) {
            throw new BookingException(
                    "Бронь коды мен жолаушының тегін енгізіңіз."
            );
        }

        Booking booking = bookings.get(code.trim().toUpperCase(Locale.ROOT));

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

    /** Бір броньға сәйкес билет; жоқ болса null (төлем әлі расталмаған). */
    public Ticket findTicket(Booking booking) {
        requireManagedBooking(booking);
        for (Ticket ticket : tickets) {
            if (ticket.getBooking() == booking) {
                return ticket;
            }
        }
        return null;
    }

    private Ticket createTicket(String ticketNumber, Booking booking) {
        if ("Economy".equalsIgnoreCase(booking.getFare().getCabinClass())) {
            return new EconomyTicket(ticketNumber, booking);
        }
        return new BusinessTicket(ticketNumber, booking);
    }

    private void requireManagedBooking(Booking booking) {
        if (booking == null
                || bookings.get(booking.getBookingCode()) != booking) {
            throw new BookingException(
                    "Бұл бронь осы қызметте тіркелмеген."
            );
        }
    }

    /** JSON-нан бастапқы кодты, бағаны және билет уақытын сақтап қалпына келтіреді.
     * Өткен рейстердің тарихи броньдарын да оқуға рұқсат етіледі.
     */
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

        code = code.trim().toUpperCase(Locale.ROOT);

        if (bookings.containsKey(code)) {
            throw new BookingException(
                    "Файлда қайталанған бронь коды бар: " + code
            );
        }

        boolean paid = Booking.CONFIRMED.equals(status)
                || Booking.CHECKED_IN.equals(status);

        if (!Booking.CREATED.equals(status)
                && !Booking.CANCELLED.equals(status)
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

        if (!Booking.CANCELLED.equals(status)
                && getRemainingCapacity(flight, cabinClass) <= 0) {
            throw new BookingException(
                    "Сақталған броньдар саны рейс сыйымдылығынан артық."
            );
        }

        if (!Booking.CHECKED_IN.equals(status) && seatNumber != null) {
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

            ticket = createTicket(ticketNumber, booking);

            ticket.restoreIssuedAt(issuedAt);
        }

        if (Booking.CANCELLED.equals(status)) {
            booking.cancel();
        }

        if (Booking.CHECKED_IN.equals(status)) {
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
