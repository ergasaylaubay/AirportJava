package kz.airport;

/** Жолаушы деректері; email форматы емес, тек бос еместігі тексеріледі. */
public class Passenger {
    private final String passengerId;
    private final String firstName;
    private final String lastName;
    private String email;

    public Passenger(String passengerId,
                     String firstName,
                     String lastName,
                     String email) {
        this.passengerId = Validation.requiredText(passengerId, "Жолаушы ID");
        this.firstName = Validation.requiredText(firstName, "Аты");
        this.lastName = Validation.requiredText(lastName, "Тегі");
        this.email = Validation.requiredText(email, "Email");
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
        this.email = Validation.requiredText(email, "Email");
    }

    public String getFirstName() {
        return firstName;
    }
}
