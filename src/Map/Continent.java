package Map;
import java.lang.String;
import java.lang.IllegalArgumentException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

public class Continent {
	private String name;
	private Map<String, Country> countries;
	
	public Continent(String nameIn) throws IllegalArgumentException {
		if (nameIn == null) {
			throw new IllegalArgumentException("Continent.Continent: Must provide a name.");
		}
		else {
			this.name = nameIn;
			this.countries = new HashMap<String, Country>();
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public void addCountry(Country country) {
		if (country != null) {
			this.countries.put(country.getName(), country);
		}
	}
	
	public Country getCountry(String countryName) {
		return this.countries.get(countryName);
	}
	
	public Collection<Country> getCountries() {
		return this.countries.values();
	}
	
	public String toString() {
		return "[Name: " + this.name + "]";
	}
}