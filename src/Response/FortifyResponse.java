package Response;
import Map.Country;
import Map.RiskMap;
import Util.RiskUtils;

public class FortifyResponse {
	private String fromCountry;
	private String toCountry;
	private int numArmies;
	
	public FortifyResponse() {
		this.fromCountry = null;
		this.toCountry = null;
		this.numArmies = 0;
	}
	
	public FortifyResponse(String from, String to, int numIn) {
		this.fromCountry = from;
		this.toCountry = to;
		this.numArmies = numIn;
	}
	
	public String getFromCountry() {
		return this.fromCountry;
	}
	
	public void setFromCountry(String countryIn) {
		this.fromCountry = countryIn;
	}
	
	public String getToCountry() {
		return this.toCountry;
	}
	
	public void setToCountry(String countryIn) {
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
		if (rsp != null && rsp.getNumArmies() < map.getCountries().get(rsp.getFromCountry()).getNumArmies()) {
			Country from = map.getCountries().get(rsp.getFromCountry());
			Country to = map.getCountries().get(rsp.getToCountry());
			return RiskUtils.areConnected(from, to, playerName);
		}
		else {
			return false;
		}
	}
}