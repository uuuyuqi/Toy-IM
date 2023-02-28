package me.yq.remoting.utils;

import java.util.concurrent.Executor;

/**
 * 将当前线程包装成线程池
 * @author yq
 * @version v1.0 2023-02-28 11:53
 */
public class DirectThreadPool implements Executor {

    static private final DirectThreadPool INSTANCE = new DirectThreadPool();

    static public Executor getInstance(){
        return INSTANCE;
    }

    private DirectThreadPool() {
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
