package kz.airport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class JsonStorage {
    private final Path filePath;
    private final Gson gson;

    public JsonStorage(String fileName) {
        this.filePath = Path.of(fileName).toAbsolutePath();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public void save(JsonObject data) {
        if (data == null) {
            throw new BookingException(
                    "Сақталатын деректер берілмеген."
            );
        }

        Path temporaryFile = null;

        try {
            Files.createDirectories(filePath.getParent());

            temporaryFile = Files.createTempFile(
                    filePath.getParent(),
                    "airport-",
                    ".tmp"
            );

            Files.writeString(
                    temporaryFile,
                    gson.toJson(data),
                    StandardCharsets.UTF_8
            );

            Files.move(
                    temporaryFile,
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new BookingException(
                    "Файлға сақтау қатесі: " + e.getMessage()
            );
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Уақытша файлды кейін өшіруге болады.
                }
            }
        }
    }

    public JsonObject load() {
        if (Files.notExists(filePath)) {
            return new JsonObject();
        }

        String json;

        try {
            json = Files.readString(
                    filePath,
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new BookingException(
                    "Файлды оқу қатесі: " + e.getMessage()
            );
        }

        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new BookingException(
                    "Файлдағы JSON деректерінің пішімі дұрыс емес."
            );
        }
    }
    public void saveState(List<Flight> flights,
                          List<Fare> fares,
                          BookingService service) {

        JsonObject root = new JsonObject();
        root.addProperty("version", 1);

        JsonArray flightArray = new JsonArray();

        for (Flight flight : flights) {
            JsonObject flightJson = new JsonObject();

            flightJson.addProperty("flightId", flight.getFlightId());
            flightJson.addProperty("flightNumber", flight.getFlightNumber());

            flightJson.add(
                    "departureAirport",
                    gson.toJsonTree(flight.getDepartureAirport())
            );

            flightJson.add(
                    "arrivalAirport",
                    gson.toJsonTree(flight.getArrivalAirport())
            );

            flightJson.addProperty(
                    "departureTime",
                    flight.getDepartureTime().toString()
            );

            JsonArray seatArray = new JsonArray();

            for (Seat seat : flight.getSeats()) {
                JsonObject seatJson = new JsonObject();

                seatJson.addProperty("seatNumber", seat.getSeatNumber());
                seatJson.addProperty("cabinClass", seat.getCabinClass());

                seatArray.add(seatJson);
            }

            flightJson.add("seats", seatArray);
            flightArray.add(flightJson);
        }

        root.add("flights", flightArray);
        root.add("fares", gson.toJsonTree(fares));

        JsonArray bookingArray = new JsonArray();

        for (Booking booking : service.getBookings()) {
            JsonObject bookingJson = new JsonObject();

            bookingJson.addProperty(
                    "bookingCode", booking.getBookingCode()
            );

            bookingJson.add(
                    "passenger", gson.toJsonTree(booking.getPassenger())
            );

            bookingJson.addProperty(
                    "flightId", booking.getFlight().getFlightId()
            );

            bookingJson.add(
                    "fare", gson.toJsonTree(booking.getFare())
            );

            bookingJson.addProperty(
                    "extraBaggagePieces", booking.getExtraBaggagePieces()
            );

            bookingJson.addProperty(
                    "totalPrice", booking.getTotalPrice()
            );

            bookingJson.addProperty("status", booking.getStatus());

            if (booking.getSeat() != null) {
                bookingJson.addProperty(
                        "seatNumber", booking.getSeat().getSeatNumber()
                );
            }

            for (Ticket ticket : service.getTickets()) {
                if (ticket.getBooking() == booking) {
                    bookingJson.addProperty(
                            "ticketNumber", ticket.getTicketNumber()
                    );

                    bookingJson.addProperty(
                            "issuedAt", ticket.getIssuedAt().toString()
                    );

                    break;
                }
            }

            bookingArray.add(bookingJson);
        }

        root.add("bookings", bookingArray);

        save(root);
    }
    public BookingService loadState(List<Flight> flights,
                                    List<Fare> fares) {

        if (Files.notExists(filePath)) {
            return null;
        }

        try {
            JsonObject root = load();

            if (root.get("version").getAsInt() != 1) {
                throw new BookingException(
                        "Файл нұсқасы бұл бағдарламаға сәйкес келмейді."
                );
            }

            List<Flight> loadedFlights = new ArrayList<>();
            List<Fare> loadedFares = new ArrayList<>();
            Map<String, Flight> flightById = new HashMap<>();

            BookingService loadedService = new BookingService(
                    new StandardFareCalculator()
            );

            for (JsonElement element : root.getAsJsonArray("flights")) {
                JsonObject item = element.getAsJsonObject();

                Airport departureAirport = readAirport(
                        item.getAsJsonObject("departureAirport")
                );

                Airport arrivalAirport = readAirport(
                        item.getAsJsonObject("arrivalAirport")
                );

                String flightId = item.get("flightId").getAsString();

                if (flightId.isBlank() || flightById.containsKey(flightId)) {
                    throw new BookingException(
                            "Файлдағы рейс идентификаторы бос немесе қайталанған."
                    );
                }

                Flight flight = new Flight(
                        flightId,
                        item.get("flightNumber").getAsString(),
                        departureAirport,
                        arrivalAirport,
                        LocalDateTime.parse(
                                item.get("departureTime").getAsString()
                        )
                );

                for (JsonElement seatElement : item.getAsJsonArray("seats")) {
                    JsonObject seatJson = seatElement.getAsJsonObject();

                    String seatNumber = seatJson.get("seatNumber").getAsString();
                    String cabinClass = seatJson.get("cabinClass").getAsString();

                    if (seatNumber.isBlank()
                            || (!"Economy".equalsIgnoreCase(cabinClass)
                            && !"Business".equalsIgnoreCase(cabinClass))) {
                        throw new BookingException(
                                "Файлдағы орын деректері дұрыс емес."
                        );
                    }

                    flight.addSeat(new Seat(seatNumber, cabinClass));
                }

                loadedFlights.add(flight);
                flightById.put(flightId, flight);
            }

            for (JsonElement element : root.getAsJsonArray("fares")) {
                loadedFares.add(readFare(element.getAsJsonObject()));
            }

            if (loadedFlights.isEmpty() || loadedFares.isEmpty()) {
                throw new BookingException(
                        "Файлда кемінде бір рейс және бір тариф болуы керек."
                );
            }

            for (JsonElement element : root.getAsJsonArray("bookings")) {
                JsonObject item = element.getAsJsonObject();

                String flightId = item.get("flightId").getAsString();
                Flight flight = flightById.get(flightId);

                if (flight == null) {
                    throw new BookingException(
                            "Броньға тиесілі рейс табылмады: " + flightId
                    );
                }

                JsonObject passengerJson = item.getAsJsonObject("passenger");

                Passenger passenger = new Passenger(
                        passengerJson.get("passengerId").getAsString(),
                        passengerJson.get("firstName").getAsString(),
                        passengerJson.get("lastName").getAsString(),
                        passengerJson.get("email").getAsString()
                );

                Fare fare = readFare(item.getAsJsonObject("fare"));

                String seatNumber = readOptionalString(item, "seatNumber");
                String ticketNumber = readOptionalString(item, "ticketNumber");
                String issuedAtText = readOptionalString(item, "issuedAt");

                LocalDateTime issuedAt = issuedAtText == null
                        ? null
                        : LocalDateTime.parse(issuedAtText);

                loadedService.restoreBooking(
                        item.get("bookingCode").getAsString(),
                        passenger,
                        flight,
                        fare,
                        item.get("extraBaggagePieces").getAsInt(),
                        item.get("totalPrice").getAsBigDecimal(),
                        item.get("status").getAsString(),
                        seatNumber,
                        ticketNumber,
                        issuedAt
                );
            }

            // Барлық деректер дұрыс оқылғаннан кейін ғана ауыстырамыз.
            flights.clear();
            flights.addAll(loadedFlights);

            fares.clear();
            fares.addAll(loadedFares);

            return loadedService;

        } catch (BookingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BookingException(
                    "Файл құрылымы немесе ішіндегі деректер дұрыс емес: "
                            + e.getMessage()
            );
        }
    }

    private Airport readAirport(JsonObject item) {
        return new Airport(
                item.get("code").getAsString(),
                item.get("name").getAsString(),
                item.get("city").getAsString()
        );
    }

    private Fare readFare(JsonObject item) {
        String cabinClass = item.get("cabinClass").getAsString();

        if (!"Economy".equalsIgnoreCase(cabinClass)
                && !"Business".equalsIgnoreCase(cabinClass)) {
            throw new BookingException(
                    "Тарифтің салон класы дұрыс емес."
            );
        }

        return new Fare(
                item.get("fareName").getAsString(),
                cabinClass,
                item.get("basePrice").getAsBigDecimal(),
                item.get("baggagePieces").getAsInt(),
                item.get("baggageWeightPerPiece").getAsInt(),
                item.get("extraBaggagePrice").getAsBigDecimal()
        );
    }

    private String readOptionalString(JsonObject item, String name) {
        if (!item.has(name) || item.get(name).isJsonNull()) {
            return null;
        }

        return item.get(name).getAsString();
    }
}