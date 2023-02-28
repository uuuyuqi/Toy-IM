package me.yq.remoting.config;

import me.yq.remoting.transport.constant.DefaultConfig;

/**
 * 客户端所需配置
 * @author yq
 * @version v1.0 2023-02-12 10:55
 */
public class ClientConfig {

    // server to connect
    public static final String REMOTE_SERVER_HOST = "localhost";

    public static final int REMOTE_SERVER_PORT = 9088;



    // transport config
    public static final byte DEFAULT_SERIALIZATION = DefaultConfig.DEFAULT_SERIALIZATION; // means hessian2

    public static final byte DEFAULT_PROTOCOL_VERSION = DefaultConfig.DEFAULT_PROTOCOL_VERSION;




}
