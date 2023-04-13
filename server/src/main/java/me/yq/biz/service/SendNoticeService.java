package me.yq.biz.service;

import io.netty.channel.Channel;
import me.yq.biz.Notice;
import me.yq.common.BaseRequest;
import me.yq.common.BizCode;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.transport.CommandSendingDelegate;
import me.yq.remoting.transport.Session;


// todo 接收通知失败，重发？
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


    private final SessionMap sessionMap = SessionMap.getInstanceOrCreate(null);

    public void sendNotice(String title, String content, long targetUid, long timeoutMillis){
        Notice notice = new Notice(targetUid, title, content);
        BaseRequest request = new BaseRequest(BizCode.Noticing.code(), notice);
        Channel channel = sessionMap.getUserChannel(targetUid);
        CommandSendingDelegate.sendRequestSync(channel,request,timeoutMillis);
    }


    public void sendNotice(Notice notice, Session session,long timeoutMillis){
        BaseRequest request = new BaseRequest(BizCode.Noticing.code(), notice);
        Channel channel = session.getChannel();
        CommandSendingDelegate.sendRequestSync(channel,request,timeoutMillis);
    }
}
