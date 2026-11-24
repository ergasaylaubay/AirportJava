package kz.airport;

/** Әуежайдың коды, атауы және қаласы. */
public class Airport {
    private final String code;
    private final String name;
    private final String city;

    public Airport(String code, String name, String city) {
        this.code = Validation.requiredText(code, "Әуежай коды");
        this.name = Validation.requiredText(name, "Әуежай атауы");
        this.city = Validation.requiredText(city, "Қала");
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
