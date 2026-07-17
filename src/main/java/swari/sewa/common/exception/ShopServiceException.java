package swari.sewa.common.exception;

public class ShopServiceException extends RuntimeException {
    
    public ShopServiceException(String message) {
        super(message);
    }
    
    public ShopServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
