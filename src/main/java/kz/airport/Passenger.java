package kz.airport;

public class Passenger {
    private String passengerId;
    private String firstName;
    private String lastName;
    private String email;

    public Passenger(String passengerId,
                     String firstName,
                     String lastName,
                     String email) {
        this.passengerId = passengerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getFirstName() {
        return firstName;
    }
}