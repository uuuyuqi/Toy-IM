package me.yq.service;

import me.yq.biz.domain.User;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户列表，必须得是单例！
 * @author yq
 * @version v1.0 2023-02-15 4:06 PM
 */
@Slf4j
public enum OnlineUserService {
    INSTANCE;

    public static OnlineUserService getInstance(){
        return INSTANCE;
    }

    /**
     * 最大支持 200 人在线
     */
    private final Map<Long/*在线用户*/, Channel/*他们的通道信息*/> onlineUser = new ConcurrentHashMap<>(200);

    /**
     * 检查某个用户是否在线
     * @param user 带查询的用户
     * @return 如果在线则返回 true，不在线则返回 false
     */
    public boolean checkOnlineState(User user){
        return onlineUser.containsKey(user.getUserId());
    }

    /**
     * 记录用户上线
     * @param user 上线的用户
     * @param channel 上线的用户其连接来的 channel
     */
    public void registerOnline(User user,Channel channel){
        Channel previous = onlineUser.put(user.getUserId(), Objects.requireNonNull(channel));
        if(previous != null) // 可以更加精细化对比
            log.warn("用户[{}]先前已登录，无需重复登录！",user);
    }

    /**
     * 记录用户下线
     */
    public void registerOffline(User user){
        Channel remove = onlineUser.remove(user.getUserId());
        if (remove == null)
            log.warn("该用户之前没有登录！");
        log.info("用户已经成功下线！");
    }

    public Channel getUserChannel(User user){
        return onlineUser.get(user.getUserId());
    }

}
