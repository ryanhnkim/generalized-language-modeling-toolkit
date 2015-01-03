package de.glmtk;

/**
 * Exception used to trigger safe program exit.
 */
public class Termination extends RuntimeException {
    private static final long serialVersionUID = -6305395239947614194L;

    public Termination() {
        super();
    }

    /**
     * @param message
     *            Will be output to {@code stderr} on program termination.
     */
    public Termination(String message) {
        super(message);
    }
}
