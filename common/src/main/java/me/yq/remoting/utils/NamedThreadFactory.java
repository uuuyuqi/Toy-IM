package me.yq.remoting.utils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义名称的线程的工厂，构造时可以向工厂传递初始参数，作为线程的名称前缀
 * @author yq
 * @version v1.0 2023-02-12 11:21
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;

    private final AtomicInteger index = new AtomicInteger(0);

    /**
     * 默认都是 daemon 线程 (daemon is true)
     */
    private final boolean daemon;

    private final UncaughtExceptionHandler exceptionHandler;

    private final ThreadGroup threadGroup;


    public NamedThreadFactory(String namePrefix) {
        this(namePrefix,true);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon) {
        this(namePrefix,daemon,null);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon, UncaughtExceptionHandler exceptionHandler) {
        this(namePrefix,daemon,exceptionHandler,null);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon, UncaughtExceptionHandler exceptionHandler, ThreadGroup group) {
        this.namePrefix = Objects.requireNonNull(namePrefix);
        this.daemon = daemon;
        this.exceptionHandler = exceptionHandler;
        this.threadGroup = group;
    }

    @Override
    public Thread newThread(Runnable r) {
        ThreadGroup group = this.threadGroup == null ? Thread.currentThread().getThreadGroup() : this.threadGroup;
        String threadName = "IM-" + namePrefix + "-" + index.getAndIncrement();
        Thread newThread = new Thread(group, r, threadName);

        // 不管所处的 threadGroup 怎么设置，这里还是按照 daemon 参数来设置
        newThread.setDaemon(daemon);

        if (this.exceptionHandler != null)
            newThread.setUncaughtExceptionHandler(this.exceptionHandler);

        return newThread;
    }
}
