package me.yq.common.exception;

/**
 * @author yq
 * @version v1.0 2023-03-17 09:52
 */
public class SystemException extends RuntimeException {
    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemException(Throwable cause) {
        super(cause);
    }
}
