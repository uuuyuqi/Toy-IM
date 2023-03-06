package me.yq.support;

import me.yq.remoting.RemotingClient;
import me.yq.remoting.processor.impl.MessageReceivedProcessor;
import me.yq.remoting.transport.deliver.CommandSendingDelegate;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.transport.support.constant.BizCode;
import me.yq.remoting.utils.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
     * 客户端业务处理线程池
     */
    private Executor bizThreadPool;
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

    private void doBuildClient() {
        boolean startedAlready = started.compareAndSet(false, true);
        if (!startedAlready)
            throw new RuntimeException("请勿重复创建 client！");

        this.sendingDelegate = CommandSendingDelegate.getInstance();

        this.bizThreadPool = new ThreadPoolExecutor(
                20,
                200,
                30,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("server-biz-thread"));;
        this.processorManager = new RequestProcessorManager(bizThreadPool);
        this.remotingClient = new RemotingClient(processorManager,sendingDelegate);
        this.client = new ChatClient(remotingClient);

        prepareProcessors();
    }

    private void prepareProcessors(){
        this.processorManager.registerProcessor(BizCode.Messaging,new MessageReceivedProcessor(client));
    }

    /**
     * 创建客户端的入口方法
     * @return 新创建的客户端，如果已经创建了，则直接抛异常
     * @throws RuntimeException 重复创建则抛出此异常
     */
    public ChatClient buildClient() throws RuntimeException{
        doBuildClient();
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

    public Executor getBizThreadPool() {
        return bizThreadPool;
    }
}
