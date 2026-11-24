package kz.airport;

/** Бизнес салонға арналған билет; Ticket әдісін қайта анықтайды. */
public class BusinessTicket extends Ticket {

    public BusinessTicket(String ticketNumber, Booking booking) {
        super(ticketNumber, booking);

        if (!"Business".equalsIgnoreCase(
                booking.getFare().getCabinClass())) {
            throw new BookingException(
                    "Бизнес билет үшін Business тарифі қажет."
            );
        }
    }

    @Override
    public String getCabinClass() {
        return "Business";
    }
}
