package me.yq.remoting.config;

/**
 * @author yq
 * @version v1.0 2023-03-30 21:35
 */
public class DefaultClientConfig extends Config {

    {
        putConfig(ClientConfigNames.REMOTE_SERVER_HOST,"localhost");
        putConfig(ClientConfigNames.REMOTE_SERVER_PORT,"9088");
        putConfig(ClientConfigNames.DEFAULT_SERIALIZATION,"1");
        putConfig(ClientConfigNames.DEFAULT_PROTOCOL_VERSION,"1");
        putConfig(ClientConfigNames.HEARTBEAT_IDLE_SECONDS,"5");
        putConfig(ClientConfigNames.HEARTBEAT_MAX_FAIL_COUNT,"3");
        putConfig(ClientConfigNames.SHUTDOWN_TIMEOUT_MILLIS,"5000");
        putConfig(ClientConfigNames.BIZ_CORE_THREAD_NUM,"20");
        putConfig(ClientConfigNames.BIZ_MAX_THREAD_NUM,"200");
        putConfig(ClientConfigNames.BIZ_EXTRA_T_ALIVE_SECONDS,"30");
        putConfig(ClientConfigNames.HEARTBEAT_ENABLE,"false");
        putConfig(ClientConfigNames.WAIT_RESPONSE_MILLIS,"3000");
        putConfig(ClientConfigNames.SEND_ONEWAY_CONFIRM_MILLIS,"3000");
    }
}
