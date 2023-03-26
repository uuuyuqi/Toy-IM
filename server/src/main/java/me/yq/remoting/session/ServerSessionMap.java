package me.yq.remoting.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import me.yq.remoting.command.DefaultRequestCommand;
import me.yq.remoting.config.ServerConfig;
import me.yq.remoting.support.ChannelAttributes;
import me.yq.remoting.support.RequestFutureMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.process.RequestProcessorManager;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在线用户 Session 集合。维护了所有连到本机的 IM 长连接。
 *
 * @author yq
 * @version v1.0 2023-02-15 4:06 PM
 */
@Slf4j
public enum ServerSessionMap {
    INSTANCE;

    public static ServerSessionMap getInstance() {
        return INSTANCE;
    }

    /**
     * session 底层数据结构 map
     * key： 用户 id
     * value：客户端 channel
     * 最大支持 200 人在线（200 个 session）
     */
    private final Map<Long/*在线用户*/, Session/*他们的通道信息*/> sessionMap = new ConcurrentHashMap<>(200);


    /**
     * 检查某个用户是否已有 session 建连了
     *
     * @param uid 待查询的用户
     * @return 如果在线则返回 true，不在线则返回 false
     */
    public boolean checkExists(long uid) {
        return sessionMap.containsKey(uid);
    }


    /**
     * 将新来的用户连接，添加到 sessionMap 中。<b>如果已经存在来自该用户的 session，则会将该 session 挤掉，并将已存在的老 session 返回。</b><br/>
     * 调用本方法时，应该考虑如何处置被挤掉的 session，是否对其通知？
     * @param session 新来的用户session
     */
    public Session addSession(Session session) {
        Session oldSession = sessionMap.putIfAbsent(Objects.requireNonNull(session).getUid(), session);

        // 之前已经有 session 连接，其将被当前 session 挤掉线
        if (oldSession != null && oldSession.notEquals(session))
            // todo 如果是电脑手机多端在线，就不能直接这么挤掉了，得判断终端设备类型
            return oldSession;

        return null;
    }

    /**
     * 根据用户 id 将用户 session 移除（一般发生在手工注销的情况）。
     * 在清除时，会将 session 中的 requestFuture 对象一并清理掉
     * 参考：{@link RequestProcessorManager#acceptAndProcess(io.netty.channel.ChannelHandlerContext, DefaultRequestCommand)}
     */
    public void removeSessionSafe(long uid) {

        // todo 此处，同一个 session 反复发来 remove 请求其实是有问题的，但是这种情况基本不会存在
        Channel channel = sessionMap.get(uid).getChannel();

        // 1.先将 session 置为不可接收请求状态
        AtomicBoolean canRequest = channel.attr(ChannelAttributes.CAN_REQUEST).get();
        canRequest.set(false);

        // 2.优雅移除所有的调用任务
        RequestFutureMap requestFutureMap = channel.attr(ChannelAttributes.REQUEST_FUTURE_MAP).get();
        requestFutureMap.removeAllFuturesSafe(ServerConfig.REMOVE_TIMEOUT_MILLIS);

        // 3.从 session 中移除
        sessionMap.remove(uid);

        // 4.后续的 channel.close() 应该在调用处执行
    }

    /**
     * 根据 channel 将 session 移除（一般发生在用户进程直接关闭等非手工注销的情况）
     */
    public Long removeSessionUnSafe(Channel channel) {
        Set<Long> ids = sessionMap.keySet();
        for (Long id : ids) {
            if (sessionMap.get(id).getChannel() == channel) {
                sessionMap.remove(id);
                return id;
            }
        }
        return null;
    }

    /**
     * 禁止每个 session 发送新请求。
     * <b>该过程只允许发生在即将停机的时候。<b/>
     */
    public void stopAcceptingRequests(){
        for (Session session : sessionMap.values()) {
            AtomicBoolean canRequest = session.getChannel().attr(ChannelAttributes.CAN_REQUEST).get();
            canRequest.set(false);
        }
    }

    /**
     * <p>
     * 粗暴移除所有 session 服务，请确保调用此方法时：
     * <ul>
     *     <li>不再有新请求进入</li>
     *     <li>当前所有 session 的请求基本处理完毕</li>
     * </ul>
     * <b>该过程只允许发生在即将停机的时候。<b/>
     * </p>
     */
    public void removeAllUnSafe(long timeoutMillis) {
        sessionMap.clear();
    }



    /**
     * 根据用户 id 获取 用户 channel
     */
    public Channel getUserChannel(long uid) {
        return sessionMap.get(uid).getChannel();
    }

}
