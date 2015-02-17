//Current build Albert Wallace, Version 001, Stamp y2015.mdB17.hm1436.sFIN

package customException;

public class OSExitException extends Exception {

	/**
	 * Though I doubt I will serialize this, it doesn't hurt... --Albert
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
