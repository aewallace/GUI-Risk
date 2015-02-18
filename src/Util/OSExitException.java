//FXUI Gamemaster-Player Custom Exception: "OS Exit"
//Current build Albert Wallace, Version 001, Stamp y2015.mdB17.hm1436.sFIN
//for Seth Denney's RISK, JavaFX UI-capable version

package Util;


//represents the exception to be thrown if the player
// attempts to exit the program, to allow proper steps to be taken
public class OSExitException extends Exception {

	/**
	 * Random serial...
	 */
	private static final long serialVersionUID = 5050585206155180982L;

	public OSExitException() {
		// TODO Auto-generated constructor stub
	}

	public OSExitException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public OSExitException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public OSExitException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public OSExitException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
