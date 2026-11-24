package kz.airport;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

/** Бағдарламаның кіру нүктесі: мәзір, әрекеттерді шақыру және қате хабарлау. */
public class Main {
    private static final ConsoleInput input = new ConsoleInput(new Scanner(System.in));
    private static final List<Flight> flights = new ArrayList<>();
    private static final List<Fare> fares = new ArrayList<>();
    private static final String DATA_FILE = "data/airport.json";
    private static final JsonStorage storage = new JsonStorage(DATA_FILE);

    private static BookingService bookingService =
            new BookingService(new StandardFareCalculator());

    public static void main(String[] args) {
        try {
            BookingService loaded = storage.loadState(flights, fares);

            if (loaded == null) {
                DemoData.populate(flights, fares);
                System.out.println("Үлгі рейстер мен тарифтер дайындалды.");
            } else {
                bookingService = loaded;
                System.out.println("Сақталған деректер жүктелді.");
            }
        } catch (BookingException e) {
            System.out.println("Деректерді оқу қатесі: " + e.getMessage());
            System.out.println("Файл өзгертілмеді. Бағдарлама тоқтатылды.");
            return;
        }

        while (true) {
            try {
                showMenu();
                String choice = input.readText("Таңдауыңыз: ");

                switch (choice) {
                    case "1" -> showFlights();
                    case "2" -> showFares();
                    case "3" -> createBooking();
                    case "4" -> confirmPayment();
                    case "5" -> showBooking(findBooking());
                    case "6" -> cancelBooking();
                    case "7" -> saveData();
                    case "0" -> {
                        saveData();
                        System.out.println("Сау болыңыз!");
                        return;
                    }
                    default -> System.out.println(
                            "0–7 аралығындағы санды таңдаңыз."
                    );
                }
            } catch (BookingException | IllegalArgumentException e) {
                System.out.println("Қате: " + e.getMessage());
            } catch (NoSuchElementException e) {
                System.out.println("\nЕнгізу аяқталды.");
                try {
                    saveData();
                } catch (BookingException saveError) {
                    System.out.println(saveError.getMessage());
                }
                return;
            }
        }
    }

    private static void showMenu() {
        System.out.println();
        System.out.println("=== ӘУЕ БИЛЕТТЕРІН БРОНДАУ ===");
        System.out.println("1. Рейстерді қарау");
        System.out.println("2. Тариф пен багаж шарттарын қарау");
        System.out.println("3. Билет брондау");
        System.out.println("4. Шартты төлемді растау");
        System.out.println("5. Өз бронын қарау");
        System.out.println("6. Төленбеген броннан бас тарту");
        System.out.println("7. Деректерді сақтау");
        System.out.println("0. Сақтап шығу");
    }

    private static void showFlights() {
        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            System.out.println(
                    (i + 1) + ". " + flight.getFlightNumber()
                            + " | " + flight.getDepartureAirport().getCity()
                            + " → " + flight.getArrivalAirport().getCity()
                            + " | " + flight.getDepartureTime()
            );
            System.out.println(
                    "   Брондауға бос орын: Economy — "
                            + bookingService.getRemainingCapacity(flight, "Economy")
                            + ", Business — "
                            + bookingService.getRemainingCapacity(flight, "Business")
            );
        }
    }

    private static void showFares() {
        for (int i = 0; i < fares.size(); i++) {
            Fare fare = fares.get(i);
            System.out.println(
                    (i + 1) + ". " + fare.getFareName()
                            + " | " + fare.getCabinClass()
                            + " | " + fare.getBasePrice() + " теңге"
            );
            System.out.println(
                    "   Багаж: " + fare.getBaggagePieces()
                            + " дана, әрқайсысы "
                            + fare.getBaggageWeightPerPiece() + " кг"
                            + " | Қосымша багаж: "
                            + fare.getExtraBaggagePrice() + " теңге/дана"
            );
        }
    }

    private static void createBooking() {
        if (flights.isEmpty() || fares.isEmpty()) {
            throw new BookingException("Бронь жасау үшін рейс пен тариф қажет.");
        }
        showFlights();
        int flightIndex = input.readInt(
                "Рейстің тізімдегі нөмірі: ", 1, flights.size()
        ) - 1;

        Flight flight = flights.get(flightIndex);
        showFares();
        int fareIndex = input.readInt(
                "Тарифтің тізімдегі нөмірі: ", 1, fares.size()
        ) - 1;

        String firstName = input.readText("Атыңыз: ");
        String lastName = input.readText("Тегіңіз: ");
        String email = input.readText("Email: ");
        int extraBaggage = input.readInt(
                "Қосымша багаж саны (қажет болмаса 0): ",
                0, Integer.MAX_VALUE
        );

        Passenger passenger = new Passenger(
                UUID.randomUUID().toString(), firstName, lastName, email
        );
        Booking booking = bookingService.createBooking(
                passenger, flight, fares.get(fareIndex), extraBaggage
        );

        System.out.println("Бронь сәтті жасалды!");
        showBooking(booking);
        System.out.println("Бронь кодын жазып алыңыз.");
    }

    private static Booking findBooking() {
        String code = input.readText("Бронь коды: ");
        String lastName = input.readText("Жолаушының тегі: ");
        return bookingService.findBooking(code, lastName);
    }

    private static void showBooking(Booking booking) {
        Flight flight = booking.getFlight();
        System.out.println("Бронь коды: " + booking.getBookingCode());
        System.out.println("Жолаушы: "
                + booking.getPassenger().getFullName());
        System.out.println("Рейс: " + flight.getFlightNumber());
        System.out.println("Бағыт: "
                + flight.getDepartureAirport().getCity()
                + " → " + flight.getArrivalAirport().getCity());
        System.out.println("Ұшу уақыты: " + flight.getDepartureTime());
        System.out.println("Тариф: " + booking.getFare().getFareName());
        System.out.println("Қосымша багаж: "
                + booking.getExtraBaggagePieces());
        System.out.println("Жалпы баға: "
                + booking.getTotalPrice() + " теңге");
        System.out.println("Күйі: " + booking.getStatus());

        Ticket ticket = bookingService.findTicket(booking);
        if (ticket != null) {
            System.out.println("Билет нөмірі: " + ticket.getTicketNumber());
            System.out.println("Салон класы: " + ticket.getCabinClass());
        }
    }

    private static void confirmPayment() {
        Booking booking = findBooking();
        showBooking(booking);

        String answer = input.readText(
                "Шартты төлемді растайсыз ба? (иә/жоқ): "
        );
        if (!"иә".equalsIgnoreCase(answer)) {
            System.out.println("Төлем расталмады.");
            return;
        }

        Ticket ticket = bookingService.confirmPayment(booking);
        System.out.println("Шартты төлем расталды.");
        System.out.println("Билет нөмірі: " + ticket.getTicketNumber());
        System.out.println("Салон класы: " + ticket.getCabinClass());
    }

    private static void cancelBooking() {
        Booking booking = findBooking();
        String answer = input.readText(
                "Броннан бас тартасыз ба? (иә/жоқ): "
        );
        if (!"иә".equalsIgnoreCase(answer)) {
            System.out.println("Бронь өзгертілмеді.");
            return;
        }

        bookingService.cancelBooking(booking);
        System.out.println("Броннан бас тартылды.");
    }

    private static void saveData() {
        storage.saveState(flights, fares, bookingService);
        System.out.println("Деректер " + DATA_FILE + " файлына сақталды.");
    }

}
