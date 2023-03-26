package me.yq.biz.service;

import io.netty.channel.Channel;
import me.yq.biz.Notice;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.remoting.session.ServerSessionMap;
import me.yq.remoting.support.session.Session;
import me.yq.remoting.transport.CommandSendingDelegate;

/**
 * 通知发送服务
 * @author yq
 * @version v1.0 2023-03-21 16:05
 */
public enum SendNoticeService {
    INSTANCE;

    public static SendNoticeService getInstance(){
        return INSTANCE;
    }


    private final ServerSessionMap serverSessionMap = ServerSessionMap.INSTANCE;

    public void sendNotice(String title, String content, long targetUid){
        Notice notice = new Notice(targetUid, title, content);
        BaseRequest request = new BaseRequest(BizCode.Noticing, notice);
        Channel channel = serverSessionMap.getUserChannel(targetUid);
        CommandSendingDelegate.sendRequestSync(channel,request);
    }


    // todo 接收通知失败，重发？
    public void sendNotice(String title, String content, Session session){
        Notice notice = new Notice(session.getUid(), title, content);
        BaseRequest request = new BaseRequest(BizCode.Noticing, notice);
        Channel channel = session.getChannel();
        CommandSendingDelegate.sendRequestSync(channel,request);
    }
}
