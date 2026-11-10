package kz.airport;

public class Seat {
    private String seatNumber;
    private String cabinClass;
    private boolean occupied;

    public Seat(String seatNumber, String cabinClass) {
        this.seatNumber = seatNumber;
        this.cabinClass = cabinClass;
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