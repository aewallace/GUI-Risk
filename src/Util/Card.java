package Util;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.Collection;

public class Card {
	private String country;
	private String type;
	
	public Card(String typeIn, String countryIn) throws IllegalArgumentException {
		if (typeIn == null) {
			throw new IllegalArgumentException("Card.Card: Type must not be null.");
		}
		//Wild Cards do not have a country
		else if (!typeIn.equals(RiskConstants.WILD_CARD)) {
			if (countryIn == null) {
				throw new IllegalArgumentException("Card.Card: Non-wild type cards require a country specification.");
			}
			this.country = countryIn;
		}
		this.type = typeIn;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getCountry() {
		return this.country;
	}
	
	@Override
	public boolean equals(Object obj) {
		Card other = (Card) obj;
		return (this.country != null && this.country.equals(other.getCountry()))
				|| (this.type.equals(RiskConstants.WILD_CARD) && this.type.equals(other.getType()));
	}
	
	public String toString() {
		return "[Type: " + this.type + "; Country: " + this.country + "]";
	}
}