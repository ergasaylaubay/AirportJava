package kz.airport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Файл жоқ кезде ғана бастапқы рейстер мен тарифтерді дайындайды. */
public final class DemoData {
    private DemoData() {
    }

    public static void populate(List<Flight> flights, List<Fare> fares) {
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

}
