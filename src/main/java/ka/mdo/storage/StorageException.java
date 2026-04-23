package ka.mdo.storage;

/**
 * Exceção não-checada lançada por falhas de I/O no backend de storage.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
