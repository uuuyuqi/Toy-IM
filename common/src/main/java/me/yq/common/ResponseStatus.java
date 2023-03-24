package me.yq.common;

/**
 * 响应结果
 * @author yq
 * @version v1.0 2023-02-16 11:12 AM
 */
public enum ResponseStatus {
    SUCCESS((byte) 1),
    FAILED((byte) 2),

    CLIENT_ERROR((byte) 11),
    SERVER_ERROR((byte) 21);

    private final byte code;

    ResponseStatus(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static ResponseStatus lookup(byte code) {
        for (ResponseStatus value : ResponseStatus.values()) {
            if (value.code() == code)
                return value;
        }
        return null;
    }

    public static boolean isLegalBizCode(byte code) {
        return lookup(code) != null;
    }
}
