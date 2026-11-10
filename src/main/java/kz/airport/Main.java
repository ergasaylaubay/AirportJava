package kz.airport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final List<Flight> flights = new ArrayList<>();
    private static final List<Fare> fares = new ArrayList<>();
    private static final JsonStorage storage =
            new JsonStorage("data/airport.json");

    private static BookingService service =
            new BookingService(new StandardFareCalculator());

    public static void main(String[] args) {
        try {
            BookingService loaded = storage.loadState(flights, fares);

            if (loaded == null) {
                prepareData();
                System.out.println("Үлгі рейстер мен тарифтер дайындалды.");
            } else {
                service = loaded;
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
                String choice = readText("Таңдауыңыз: ");

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

    private static void prepareData() {
        Airport almaty = new Airport(
                "ALA", "Алматы әуежайы", "Алматы"
        );
        Airport astana = new Airport(
                "NQZ", "Астана әуежайы", "Астана"
        );

        LocalDateTime firstTime = LocalDateTime.now().plusDays(7)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        flights.add(new Flight(
                "F001", "KC123", almaty, astana, firstTime
        ));
        flights.add(new Flight(
                "F002", "KC456", astana, almaty,
                firstTime.plusDays(1).withHour(15).withMinute(30)
        ));

        for (Flight flight : flights) {
            flight.addSeat(new Seat("12A", "Economy"));
            flight.addSeat(new Seat("12B", "Economy"));
            flight.addSeat(new Seat("13A", "Economy"));
            flight.addSeat(new Seat("13B", "Economy"));
            flight.addSeat(new Seat("1A", "Business"));
            flight.addSeat(new Seat("1B", "Business"));
        }

        fares.add(new Fare(
                "Standard", "Economy",
                new BigDecimal("30000"),
                1, 23, new BigDecimal("8000")
        ));
        fares.add(new Fare(
                "Business", "Business",
                new BigDecimal("80000"),
                2, 32, new BigDecimal("10000")
        ));
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
                            + service.getRemainingCapacity(flight, "Economy")
                            + ", Business — "
                            + service.getRemainingCapacity(flight, "Business")
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
        showFlights();
        int flightIndex = readInt(
                "Рейстің тізімдегі нөмірі: ", 1, flights.size()
        ) - 1;

        Flight flight = flights.get(flightIndex);
        if (!flight.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BookingException(
                    "Ұшу уақыты өтіп кеткен рейске бронь жасауға болмайды."
            );
        }

        showFares();
        int fareIndex = readInt(
                "Тарифтің тізімдегі нөмірі: ", 1, fares.size()
        ) - 1;

        String firstName = readText("Атыңыз: ");
        String lastName = readText("Тегіңіз: ");
        String email = readText("Email: ");
        int extraBaggage = readInt(
                "Қосымша багаж саны (қажет болмаса 0): ",
                0, Integer.MAX_VALUE
        );

        Passenger passenger = new Passenger(
                UUID.randomUUID().toString(), firstName, lastName, email
        );
        Booking booking = service.createBooking(
                passenger, flight, fares.get(fareIndex), extraBaggage
        );

        System.out.println("Бронь сәтті жасалды!");
        showBooking(booking);
        System.out.println("Бронь кодын жазып алыңыз.");
    }

    private static Booking findBooking() {
        String code = readText("Бронь коды: ");
        String lastName = readText("Жолаушының тегі: ");
        return service.findBooking(code, lastName);
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

        for (Ticket ticket : service.getTickets()) {
            if (ticket.getBooking() == booking) {
                System.out.println("Билет нөмірі: "
                        + ticket.getTicketNumber());
                System.out.println("Салон класы: "
                        + ticket.getCabinClass());
                break;
            }
        }
    }

    private static void confirmPayment() {
        Booking booking = findBooking();
        showBooking(booking);

        String answer = readText(
                "Шартты төлемді растайсыз ба? (иә/жоқ): "
        );
        if (!"иә".equalsIgnoreCase(answer)) {
            System.out.println("Төлем расталмады.");
            return;
        }

        Ticket ticket = service.confirmPayment(booking);
        System.out.println("Шартты төлем расталды.");
        System.out.println("Билет нөмірі: " + ticket.getTicketNumber());
        System.out.println("Салон класы: " + ticket.getCabinClass());
    }

    private static void cancelBooking() {
        Booking booking = findBooking();
        String answer = readText(
                "Броннан бас тартасыз ба? (иә/жоқ): "
        );
        if (!"иә".equalsIgnoreCase(answer)) {
            System.out.println("Бронь өзгертілмеді.");
            return;
        }

        service.cancelBooking(booking);
        System.out.println("Броннан бас тартылды.");
    }

    private static void saveData() {
        storage.saveState(flights, fares, service);
        System.out.println("Деректер data/airport.json файлына сақталды.");
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Бос қалдырмай, мән енгізіңіз.");
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readText(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Қате енгізілсе, қайта сұраймыз.
            }
            System.out.println(
                    min + " мен " + max + " аралығындағы бүтін сан енгізіңіз."
            );
        }
    }
}