package Util;
public class RollOutcome {
	private int atkLosses;
	private int dfdLosses;
	
	public RollOutcome() {
		this.atkLosses = 0;
		this.dfdLosses = 0;
	}
	
	public RollOutcome(int atkLoss, int dfdLoss) {
		this.atkLosses = atkLoss;
		this.dfdLosses = dfdLoss;
	}
	
	public void addAtkLoss() {
		this.atkLosses++;
	}
	
	public void addDfdLoss() {
		this.dfdLosses++;
	}
	
	public int getAtkLosses() {
		return this.atkLosses;
	}
	
	public int getDfdLosses() {
		return this.dfdLosses;
	}
}