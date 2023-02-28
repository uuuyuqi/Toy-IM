package me.yq.support;

import me.yq.remoting.processor.impl.MessagingTransferProcessor;
import me.yq.remoting.processor.impl.SignInProcessor;
import me.yq.remoting.processor.impl.SignOutProcessor;
import me.yq.remoting.transport.deliver.CommandSendingDelegate;
import me.yq.remoting.transport.deliver.process.RequestProcessorManager;
import me.yq.remoting.utils.NamedThreadFactory;
import me.yq.remoting.transport.support.constant.BizCode;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务端主配置类
 * @author yq
 * @version v1.0 2023-02-17 2:09 PM
 */
public class ServerBootstrap {

    private final Executor bizThreadPool;
    private final RequestProcessorManager processorManager;
    private final CommandSendingDelegate sendingDelegate;

    public ServerBootstrap() {
        /*
         * 业务线程池<br/>
         * biz:20<br/>
         * max:200 (30s alive)<br/>
         * no task queue<br/>
         */
        bizThreadPool = new ThreadPoolExecutor(
                20,
                200,
                30,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("biz-thread"));

        processorManager = new RequestProcessorManager(bizThreadPool);
        processorManager.registerProcessor(BizCode.Messaging, new MessagingTransferProcessor(this));
        processorManager.registerProcessor(BizCode.SignInRequest, new SignInProcessor());
        processorManager.registerProcessor(BizCode.SignOutRequest, new SignOutProcessor());

        sendingDelegate = CommandSendingDelegate.getInstance();
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
}
