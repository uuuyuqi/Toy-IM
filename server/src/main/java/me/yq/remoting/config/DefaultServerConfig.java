package me.yq.remoting.config;

/**
 * @author yq
 * @version v1.0 2023-03-31 13:38
 */
public class DefaultServerConfig extends Config{
    {
        putConfig(ServerConfigNames.SERVER_PORT,"9088");
        putConfig(ServerConfigNames.CONNECT_TIMEOUT_MILLIS,"3000");
        putConfig(ServerConfigNames.CLIENT_TIMEOUT_SECONDS,"90000");
        putConfig(ServerConfigNames.SHUTDOWN_TIMEOUT_MILLIS,"5000");
        putConfig(ServerConfigNames.BIZ_CORE_THREAD_NUM,"20");
        putConfig(ServerConfigNames.BIZ_MAX_THREAD_NUM,"200");
        putConfig(ServerConfigNames.BIZ_EXTRA_T_ALIVE_SECONDS,"30");
        putConfig(ServerConfigNames.REMOVE_TIMEOUT_MILLIS,"3000");
        putConfig(ServerConfigNames.HEARTBEAT_ENABLE,"true");

    }
}
