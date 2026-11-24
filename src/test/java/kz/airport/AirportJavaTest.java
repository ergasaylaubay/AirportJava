package kz.airport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/** Броньдау ережелері, енгізу және файлға сақтау үшін регрессиялық тесттер. */
class AirportJavaTest {
    @TempDir Path directory;
    private final List<Flight> flights = new ArrayList<>();
    private final List<Fare> fares = new ArrayList<>();
    private BookingService service;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        DemoData.populate(flights, fares);
        service = new BookingService(new StandardFareCalculator());
        passenger = new Passenger("P1", "Test", "Passenger", "test@example.com");
    }

    private Booking book(int fareIndex, int extraBaggage) {
        return service.createBooking(passenger, flights.get(0), fares.get(fareIndex), extraBaggage);
    }

    @Test
    void calculatesOnlyExtraBaggage() {
        assertEquals(new BigDecimal("38000"), book(0, 1).getTotalPrice());
        assertEquals(new BigDecimal("80000"), book(1, 0).getTotalPrice());
    }

    @Test
    void rejectsNegativeBaggageWithoutAddingBooking() {
        assertThrows(BookingException.class, () -> book(0, -1));
        assertTrue(service.getBookings().isEmpty());
    }

    @Test
    void serviceRejectsPastFlightButCanRestoreHistoricalBooking() {
        Flight flight = flights.get(0);
        flight.updateDetails("KC123", LocalDateTime.now().minusDays(1));
        assertThrows(BookingException.class, () -> book(0, 0));
        service.restoreBooking("OLD1", passenger, flight, fares.get(0), 0,
                new BigDecimal("30000"), Booking.CREATED, null, null, null);
        assertEquals("OLD1", service.findBooking("old1", "Passenger").getBookingCode());
    }

    @Test
    void capacityCountsReservationsAndCancellationReleasesOne() {
        Booking first = book(0, 0);
        for (int i = 0; i < 3; i++) book(0, 0);
        assertThrows(BookingException.class, () -> book(0, 0));
        service.cancelBooking(first);
        assertEquals(1, service.getRemainingCapacity(flights.get(0), "Economy"));
        assertNotNull(book(0, 0));
    }

    @Test
    void paymentCreatesPolymorphicTicketsAndCannotBeRepeated() {
        Booking economy = book(0, 0);
        Ticket ticket = service.confirmPayment(economy);
        assertInstanceOf(EconomyTicket.class, ticket);
        assertEquals("Economy", ticket.getCabinClass());
        assertSame(ticket, service.findTicket(economy));
        Ticket business = service.confirmPayment(book(1, 0));
        assertInstanceOf(BusinessTicket.class, business);
        assertEquals("Business", business.getCabinClass());
        assertThrows(BookingException.class, () -> service.confirmPayment(economy));
        assertEquals(2, service.getTickets().size());
    }

    @Test
    void paidBookingCannotBeCancelled() {
        Booking booking = book(0, 0);
        service.confirmPayment(booking);
        assertThrows(BookingException.class, () -> service.cancelBooking(booking));
        assertEquals(Booking.CONFIRMED, booking.getStatus());
    }

    @Test
    void lookupChecksLastNameAndNormalizesCode() {
        Booking booking = book(0, 0);
        assertSame(booking, service.findBooking(" " + booking.getBookingCode().toLowerCase() + " ", "passenger"));
        assertThrows(BookingException.class, () -> service.findBooking(booking.getBookingCode(), "Wrong"));
    }

    @Test
    void anotherServiceCannotChangeBooking() {
        Booking booking = book(0, 0);
        BookingService other = new BookingService(new StandardFareCalculator());
        assertThrows(BookingException.class, () -> other.confirmPayment(booking));
        assertEquals(Booking.CREATED, booking.getStatus());
    }

    @Test
    void jsonRoundTripKeepsAllStatusesTicketTimesAndSeatLinks() {
        Booking created = book(0, 1);
        Booking cancelled = book(0, 0);
        service.cancelBooking(cancelled);
        Booking paid = book(1, 0);
        Ticket ticket = service.confirmPayment(paid);
        Booking checkedIn = book(0, 0);
        service.confirmPayment(checkedIn);
        service.checkIn(checkedIn, flights.get(0).getSeats().get(0));
        JsonStorage storage = new JsonStorage(directory.resolve("airport.json").toString());
        storage.saveState(flights, fares, service);
        List<Flight> loadedFlights = new ArrayList<>();
        List<Fare> loadedFares = new ArrayList<>();
        BookingService loaded = storage.loadState(loadedFlights, loadedFares);
        assertEquals(4, loaded.getBookings().size());
        Booking restored = loaded.findBooking(paid.getBookingCode(), "Passenger");
        assertEquals(Booking.CONFIRMED, restored.getStatus());
        assertEquals(ticket.getIssuedAt(), loaded.findTicket(restored).getIssuedAt());
        assertEquals(ticket.getTicketNumber(), loaded.findTicket(restored).getTicketNumber());
        assertEquals(Booking.CANCELLED, loaded.findBooking(cancelled.getBookingCode(), "Passenger").getStatus());
        assertEquals(new BigDecimal("38000"), loaded.findBooking(created.getBookingCode(), "Passenger").getTotalPrice());
        Booking restoredCheckIn = loaded.findBooking(checkedIn.getBookingCode(), "Passenger");
        Flight restoredFlight = restoredCheckIn.getFlight();
        assertSame(restoredFlight.getSeats().get(0), restoredCheckIn.getSeat());
        assertFalse(restoredCheckIn.getSeat().isAvailable());
        assertEquals(2, loaded.getRemainingCapacity(restoredFlight, "Economy"));
    }

    @Test
    void malformedFileKeepsExistingListsAndFileBytes() throws Exception {
        Path file = directory.resolve("bad.json");
        Files.writeString(file, "{invalid");
        Flight original = flights.get(0);
        JsonStorage storage = new JsonStorage(file.toString());
        BookingException error = assertThrows(BookingException.class, () -> storage.loadState(flights, fares));
        assertNotNull(error.getCause());
        assertSame(original, flights.get(0));
        assertEquals(2, fares.size());
        assertEquals("{invalid", Files.readString(file));
    }

    @Test
    void missingFileReturnsNull() {
        assertNull(new JsonStorage(directory.resolve("missing.json").toString()).loadState(flights, fares));
    }

    @Test
    void rejectsBlankFieldsAndTrimsNames() {
        assertThrows(IllegalArgumentException.class, () -> new Passenger("P", " ", "Name", "x@y.kz"));
        Passenger person = new Passenger("P", " Test ", " Name ", "x@y.kz");
        assertEquals("Test Name", person.getFullName());
        assertThrows(IllegalArgumentException.class, () -> person.setEmail(" "));
        assertEquals("x@y.kz", person.getEmail());
    }

    @Test
    void inputRetriesInvalidNumbersAndRejectsEmptyRange() {
        ConsoleInput input = new ConsoleInput(new Scanner("abc\n0\n2\n"));
        assertEquals(2, input.readInt("", 1, 3));
        assertThrows(IllegalArgumentException.class, () -> input.readInt("", 1, 0));
    }
}
