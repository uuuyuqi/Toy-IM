package me.yq.remoting.config;

public class DefaultConfig {
    // todo 测试时改为 无限大
    public static final long DEFAULT_CONSUMER_WAIT_MILLIS = -1;

    public static final byte DEFAULT_SERIALIZATION = (byte) 1; // hessian2
    public static final byte DEFAULT_PROTOCOL_VERSION = (byte) 1;
}
