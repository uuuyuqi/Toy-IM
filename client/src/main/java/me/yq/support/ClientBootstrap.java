package me.yq.support;

import me.yq.remoting.RemotingClient;
import me.yq.remoting.processor.impl.MessageReceivedProcessor;
import me.yq.remoting.transport.deliver.CommandSendingDelegate;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.transport.support.constant.BizCode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yq
 * @version v1.0 2023-02-23 16:14
 */
public class ClientBootstrap {

    /**
     * 应用客户端
     */
    private ChatClient client;
    /**
     * 通信客户端
     */
    private RemotingClient remotingClient;
    /**
     * 请求处理器
     */
    private RequestProcessorManager processorManager;
    /**
     * 消息发送委派器
     */
    private CommandSendingDelegate sendingDelegate;

    /**
     * balk 模式防止重复创建
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    private void doBuild() {
        boolean startedAlready = started.compareAndSet(false, true);
        if (!startedAlready)
            throw new RuntimeException("请勿重复创建 client！");

        this.sendingDelegate = CommandSendingDelegate.getInstance();
        this.processorManager = new RequestProcessorManager();
        this.remotingClient = new RemotingClient(this);
        this.client = new ChatClient(remotingClient);
        this.processorManager.registerProcessor(BizCode.Messaging,new MessageReceivedProcessor(client));

    }


    public ChatClient buildClient(){
        doBuild();
        return this.client;
    }


    public CommandSendingDelegate getSendingDelegate() {
        return sendingDelegate;
    }

    public ChatClient getClient() {
        return client;
    }

    public RequestProcessorManager getProcessorManager() {
        return processorManager;
    }

    public RemotingClient getRemotingClient() {
        return remotingClient;
    }
}
