package me.yq.common.exception;

/**
 * @author yq
 * @version v1.0 2023-03-17 09:52
 */
public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }
}
