package me.yq.remoting;

import lombok.extern.slf4j.Slf4j;


// todo 可以考虑改名为：ConfigurableBizProcessEngine
/**
 * 有状态的/生命周期的，需要做状态管理的对象应该主动继承该模板类
 */
@Slf4j
public abstract class Stateful {

    private Status currentStatus = Status.NEW;

    /**
     * 只有处于新建状态的服务才可以启动
     */
    public synchronized void start(){
        switch (currentStatus){
            case NEW:
                try{
                    doStart();
                    currentStatus = Status.RUNNING;
                }catch (Exception e){
                    log.error("启动时出现报错: [{}]",e.getMessage());
                    currentStatus = Status.EXCEPTION_CLOSED;
                }
                break;
            case RUNNING:
                log.warn("当前服务已经在运行中，请勿重复启动！");
                break;
            default:
                throw new IllegalStateException("当前服务已关闭，请通过 kill 等方式彻底关闭进程，重启服务！");
        }
    }

    abstract protected void doStart();


    /**
     * 只有处于运行态才可以关闭
     */
    public synchronized void shutdown(){
        switch (currentStatus){
            case NEW:
                log.warn("服务尚未启动, 无法关闭");
                break;
            case RUNNING:
                doShutdown();
                currentStatus = Status.CLOSED;
                break;
            case CLOSED:
                log.warn("服务器已关闭，请勿重复关闭");
                break;
            case EXCEPTION_CLOSED:
                log.warn("服务器因异常已自动关闭，请检查日志......");
                break;
        }
    }

    abstract protected void doShutdown();


    public Status getCurrentStatus() {
        return currentStatus;
    }

    public enum Status{
        NEW,
        RUNNING,
        CLOSED,
        EXCEPTION_CLOSED
    }
}
