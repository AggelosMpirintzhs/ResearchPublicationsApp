package db;

public class DatabaseConnectionException extends RuntimeException {

    // Creates database error
    public DatabaseConnectionException(String message) {
        super(message);
    }

    // Creates database error
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}