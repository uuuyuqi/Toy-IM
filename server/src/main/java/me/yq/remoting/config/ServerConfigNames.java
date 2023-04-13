package me.yq.remoting.config;

/**
 * @author yq
 * @version v1.0 2023-03-31 13:48
 */
public class ServerConfigNames {
    // 服务端端口
    public static final String SERVER_PORT = "SERVER_PORT";

    // 客户端请求建联 超时时间
    public static final String CONNECT_TIMEOUT_MILLIS = "CONNECT_TIMEOUT_MILLIS";
    // 客户端无响应 判定死亡的时间
    public static final String CLIENT_TIMEOUT_SECONDS = "CLIENT_TIMEOUT_SECONDS";

    // 停机超时时间
    public static final String SHUTDOWN_TIMEOUT_MILLIS = "SHUTDOWN_TIMEOUT_MILLIS";

    // 业务线程池核心数
    public static final String BIZ_CORE_THREAD_NUM = "BIZ_CORE_THREAD_NUM";
    // 业务线程池最大数
    public static final String BIZ_MAX_THREAD_NUM = "BIZ_MAX_THREAD_NUM";
    // 业务线程池存活时间
    public static final String BIZ_EXTRA_T_ALIVE_SECONDS = "BIZ_EXTRA_T_ALIVE_SECONDS";

    // 优雅移除单个 channel 超时时间
    public static final String REMOVE_TIMEOUT_MILLIS = "REMOVE_TIMEOUT_MILLIS";

    // 空闲检测开启
    public static final String IDLE_CHECK_ENABLE = "IDLE_CHECK_ENABLE";

    // 等待响应消息超时时间
    public static final String WAIT_RESPONSE_MILLIS = "WAIT_RESPONSE_MILLIS";

}
