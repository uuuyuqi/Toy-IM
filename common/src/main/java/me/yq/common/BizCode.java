package me.yq.common;

/**
 * 业务请求码
 *
 * @author yq
 * @version v1.0 2023-02-16 11:04 AM
 */
public enum BizCode {
    Messaging((byte) 11),

    Noticing ((byte) 15),

    LogInRequest((byte) 21),

    LogOutRequest((byte) 32);

    private final byte code;

    BizCode(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static BizCode lookup(byte code) {
        for (BizCode value : BizCode.values()) {
            if (value.code() == code)
                return value;
        }
        return null;
    }

    public static boolean isLegalBizCode(byte code) {
        return lookup(code) != null;
    }

}
