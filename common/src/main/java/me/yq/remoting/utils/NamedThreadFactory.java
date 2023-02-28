package me.yq.remoting.utils;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义名称的线程的工厂，构造时可以向工厂传递初始参数，作为线程的名称前缀
 * @author yq
 * @version v1.0 2023-02-12 11:21
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger index = new AtomicInteger(0);

    private final String namePrefix;

    /**
     * 默认都是 daemon 线程 (daemon is true)
     */
    private final boolean daemon;

    public NamedThreadFactory(String namePrefix) {
        this(namePrefix,true);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon) {
        this.namePrefix = Objects.requireNonNull(namePrefix);
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = "IM-" + namePrefix + "-" + index.getAndIncrement();
        Thread newThread = new Thread(r,threadName);
        newThread.setDaemon(daemon);
        return newThread;
    }
}
