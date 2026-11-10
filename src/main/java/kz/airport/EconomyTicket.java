package kz.airport;

public class EconomyTicket extends Ticket {

    public EconomyTicket(String ticketNumber, Booking booking) {
        super(ticketNumber, booking);

        if (!"Economy".equalsIgnoreCase(
                booking.getFare().getCabinClass())) {
            throw new BookingException(
                    "Эконом билет үшін Economy тарифі қажет."
            );
        }
    }

    @Override
    public String getCabinClass() {
        return "Economy";
    }
}