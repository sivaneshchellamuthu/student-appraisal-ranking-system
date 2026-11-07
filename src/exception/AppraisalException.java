package exception;

/**
 * OOP CONCEPT - CUSTOM EXCEPTION:
 *   All user-facing validation and database problems are wrapped in this
 *   checked exception so the UI can present clear, consistent messages.
 */
public class AppraisalException extends Exception {

    public AppraisalException(String message) {
        super(message);
    }

    public AppraisalException(String message, Throwable cause) {
        super(message, cause);
    }
}
