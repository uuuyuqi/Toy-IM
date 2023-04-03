package me.yq.remoting.config;

/**
 * @author yq
 * @version v1.0 2023-03-30 15:33
 */
public class ClientConfigNames {

    // 服务端地址
    public static final String REMOTE_SERVER_HOST = "REMOTE_SERVER_HOST";
    // 服务端端口
    public static final String REMOTE_SERVER_PORT = "REMOTE_SERVER_PORT";



    // 默认序列化方式
    public static final String DEFAULT_SERIALIZATION = "DEFAULT_SERIALIZATION"; // means hessian2
    // 默认协议版本
    public static final String DEFAULT_PROTOCOL_VERSION = "DEFAULT_PROTOCOL_VERSION";
    // 默认心跳间隔
    public static final String HEARTBEAT_IDLE_SECONDS = "HEARTBEAT_IDLE_SECONDS";
    // 默认心跳最大失败次数
    public static final String HEARTBEAT_MAX_FAIL_COUNT = "HEARTBEAT_MAX_FAIL_COUNT";


    // 业务线程池核心数
    public static final String BIZ_CORE_THREAD_NUM = "BIZ_CORE_THREAD_NUM";
    // 业务线程池最大数
    public static final String BIZ_MAX_THREAD_NUM = "BIZ_MAX_THREAD_NUM";
    // 业务线程池存活时间
    public static final String BIZ_EXTRA_T_ALIVE_SECONDS = "BIZ_EXTRA_T_ALIVE_SECONDS";
    // 心跳开启
    public static final String HEARTBEAT_ENABLE = "HEARTBEAT_ENABLE";


    // 停机超时时间
    public static final String SHUTDOWN_TIMEOUT_MILLIS = "SHUTDOWN_TIMEOUT_MILLIS";
    // 等待响应消息超时时间
    public static final String WAIT_RESPONSE_MILLIS = "WAIT_RESPONSE_MILLIS";
}
