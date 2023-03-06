package me.yq.support;

import me.yq.remoting.RemotingServer;
import me.yq.remoting.processor.impl.MessagingTransferProcessor;
import me.yq.remoting.processor.impl.SignInProcessor;
import me.yq.remoting.processor.impl.SignOutProcessor;
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
 * 服务端主配置类
 * @author yq
 * @version v1.0 2023-02-17 2:09 PM
 */
public class ServerBootstrap {

    /**
     * 应用服务端
     */
    private ChatServer server;
    /**
     * 通信服务端
     */
    private RemotingServer remotingServer;
    /**
     * 服务端业务处理线程池
     */
    private Executor bizThreadPool;
    /**
     * 请求处理器
     */
    private RequestProcessorManager processorManager;
    /**
     * 请求发送委派器
     */
    private CommandSendingDelegate sendingDelegate;

    /**
     * balk 模式防止重复创建
     */
    private final AtomicBoolean started = new AtomicBoolean(false);


    private void doBuildServer() {
        boolean startedAlready = started.compareAndSet(false, true);
        if (!startedAlready)
            throw new RuntimeException("请勿重复创建 server！");

        this.sendingDelegate = CommandSendingDelegate.getInstance();

        /*
         * 业务线程池<br/>
         * biz:20<br/>
         * max:200 (30s alive)<br/>
         * no task queue<br/>
         */
        this.bizThreadPool = new ThreadPoolExecutor(
                20,
                200,
                30,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("server-biz-thread"));
        this.processorManager = new RequestProcessorManager(bizThreadPool);
        this.remotingServer = new RemotingServer(processorManager);
        this.server = new ChatServer(remotingServer);

        prepareProcessors();
    }

    private void prepareProcessors(){
        this.processorManager.registerProcessor(BizCode.Messaging, new MessagingTransferProcessor(sendingDelegate));
        this.processorManager.registerProcessor(BizCode.SignInRequest, new SignInProcessor());
        this.processorManager.registerProcessor(BizCode.SignOutRequest, new SignOutProcessor());
    }


    /**
     * 创建服务端的入口方法
     * @return 新创建的服务端，如果已经创建了，则直接抛异常
     * @throws RuntimeException 重复创建则抛出此异常
     */
    public ChatServer buildServer() throws RuntimeException{
        doBuildServer();
        return this.server;
    }



    public Executor getBizThreadPool() {
        return bizThreadPool;
    }

    public RequestProcessorManager getProcessorManager() {
        return processorManager;
    }

    public CommandSendingDelegate getSendingDelegate() {
        return sendingDelegate;
    }

    public ChatServer getServer() {
        return server;
    }

    public RemotingServer getRemotingServer() {
        return remotingServer;
    }
}
