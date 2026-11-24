package kz.airport;

/** Рейстегі физикалық орын және тіркелуден кейінгі бос/бос емес күйі. */
public class Seat {
    private final String seatNumber;
    private final String cabinClass;
    private boolean occupied;

    public Seat(String seatNumber, String cabinClass) {
        this.seatNumber = Validation.requiredText(seatNumber, "Орын нөмірі");
        this.cabinClass = Validation.requiredText(cabinClass, "Салон класы");
        this.occupied = false;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public boolean isAvailable() {
        return !occupied;
    }

    public void occupy() {
        if (occupied) {
            throw new IllegalStateException("Бұл орын бос емес.");
        }
        occupied = true;
    }

    public void release() {
        occupied = false;
    }
}
