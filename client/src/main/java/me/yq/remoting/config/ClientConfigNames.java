package me.yq.remoting.config;

/**
 * @author yq
 * @version v1.0 2023-03-30 15:33
 */
public class ClientConfigNames {


    public static final String REMOTE_SERVER_HOST = "REMOTE_SERVER_HOST";

    public static final String REMOTE_SERVER_PORT = "REMOTE_SERVER_PORT";




    public static final String DEFAULT_SERIALIZATION = "DEFAULT_SERIALIZATION"; // means hessian2

    public static final String DEFAULT_PROTOCOL_VERSION = "DEFAULT_PROTOCOL_VERSION";

    public static final String HEARTBEAT_IDLE_SECONDS = "HEARTBEAT_INTERVAL_SECONDS";

    public static final String HEARTBEAT_MAX_FAIL_COUNT = "HEARTBEAT_MAX_FAIL_COUNT";

    public static final String SHUTDOWN_TIMEOUT_MILLIS = "SHUTDOWN_TIMEOUT_MILLIS";



    public static final String BIZ_CORE_THREAD_NUM = "BIZ_CORE_THREAD_NUM";
    public static final String BIZ_MAX_THREAD_NUM = "BIZ_MAX_THREAD_NUM";
    public static final String BIZ_EXTRA_T_ALIVE_SECONDS = "BIZ_EXTRA_T_ALIVE_SECONDS";


    public static final String HEARTBEAT_ENABLE = "HEARTBEAT_ENABLE";
}
