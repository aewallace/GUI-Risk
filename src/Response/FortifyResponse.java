package Response;
import Map.Country;
import Map.RiskMap;
import Util.RiskUtils;

public class FortifyResponse {
	private Country fromCountry;
	private Country toCountry;
	private int numArmies;
	
	public FortifyResponse() {
		this.fromCountry = null;
		this.toCountry = null;
		this.numArmies = 0;
	}
	
	public FortifyResponse(Country from, Country to, int numIn) {
		this.fromCountry = from;
		this.toCountry = to;
		this.numArmies = numIn;
	}
	
	public Country getFromCountry() {
		return this.fromCountry;
	}
	
	public void setFromCountry(Country countryIn) {
		this.fromCountry = countryIn;
	}
	
	public Country getToCountry() {
		return this.toCountry;
	}
	
	public void setToCountry(Country countryIn) {
		this.toCountry = countryIn;
	}
	
	public int getNumArmies() {
		return this.numArmies;
	}
	
	public void setNumArmies(int numIn) {
		if (numIn >= 0) {
			this.numArmies = numIn;
		}
	}
	
	public static boolean isValidResponse(FortifyResponse rsp, RiskMap map, String playerName) {
		if (rsp != null && rsp.getNumArmies() < map.getCountryArmies(rsp.getFromCountry())) {
			Country from = rsp.getFromCountry();
			Country to = rsp.getToCountry();
			return map.getCountryOwner(from).equals(playerName)
					&& map.getCountryOwner(to).equals(playerName)
					&& RiskUtils.areConnected(map, from, to, playerName, true);
		}
		else {
			return false;
		}
	}
}