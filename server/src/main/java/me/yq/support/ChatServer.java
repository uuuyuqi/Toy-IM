package me.yq.support;

import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import me.yq.common.BizCode;
import me.yq.remoting.RemotingServer;
import me.yq.remoting.Stateful;
import me.yq.remoting.config.Config;
import me.yq.remoting.config.DefaultServerConfig;
import me.yq.remoting.config.ServerConfigNames;
import me.yq.remoting.processor.LogInProcessor;
import me.yq.remoting.processor.LogOutProcessor;
import me.yq.remoting.processor.MessagingTransferProcessor;
import me.yq.remoting.session.SessionMap;
import me.yq.remoting.transport.process.RequestProcessor;
import me.yq.remoting.transport.process.UserProcessor;
import me.yq.remoting.utils.NamedThreadFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 服务端<br/>
 * 这个只是单机服务端，在分布式场景下，每个客户端连接（长连接）的服务端都不一样，想要顺利地进行消息转发
 * 必须能够将用户的消息转发给确定的那个服务端
 * @author yq
 * @version v1.0 2023-02-14 5:33 PM
 */
@Slf4j
public class ChatServer extends Stateful {


    private final Config config;

    private final boolean useDefaultProcessors;

    private final RemotingServer remotingServer;

    private final ThreadPoolExecutor bizThreadPool;

    private final UserProcessor userProcessor;

    private final SessionMap sessionMap;


    /**
     * 自定义 handler，这些 handler 会放在一切 handler 之前
     */
    private final Map<String,Supplier<ChannelHandler>> customHandlersAhead = new LinkedHashMap<>();



    /**
     * 一般使用该方式创建即可
     */
    public ChatServer() {
        this(true);
    }

    public ChatServer(boolean useDefaultProcessors) {
        this(useDefaultProcessors,new DefaultServerConfig());
    }

    //
    // 这块各个字段到底要不要加 volatile，其实很值得研究
    // 首先：
    // 1.一条 java 语句会被前端 javac 编译器编译成 多条字节码指令
    // 2.一条 热点字节码 指令 会被解释器 或者 jit 编译器变异成多条机器指令
    // 3.当代 cpu 为了加速流水，大量进行机器指令的重排（防止流水线塞入过多的 NOP）
    // 4.一旦指令重排序，肯定会导致下面这段代码出现 npe 问题，因为发生了参数的逃逸（初始化没完成，就被其他语句使用了）
    // 5.为了解决这个问题，按理说应该加上 volatile 来禁止指令的重排序
    // 6.加上 volatile 之后，必定增加了访问主内存次数，带来性能的开销，而这些字段都非常常用，应避免性能的额外开销
    //
    // 但是：
    // 1.在解释执行单条字节码指令的情况下，基本不会有指令重排序的问题，即便重排序机器指令，本质上只有一条字节码指令得以运行
    // 2.而一般情况下，代构单例的构造方法，一般是单线程内创建，几乎不会被判定为热点代码，也不会被 jit 后端编译成机器码
    // 4.解释执行这个构造器，在一般的开发环境下，是一条条字节码指令的执行，所以可以粗略的认为不会产生重排序导致的 npe
    //
    // 然而：
    // 1.经验上，线上运行环境的 jre 会设置成 server 模式，以便于提高性能
    // 2.server 模式下， jvm 会大量采用 c2 这个激进的 jit 编译器（默认开发环境下 mixed 模式会混用 c1 和 c2）
    // 3.这就要考虑，我们这段代码会不会被 c2 编译成机器码，从而存在被 cpu 重排序执行引发参数逃逸，进而导致 npe
    //
    // 定论：
    // 1.由于即便开启了 server 模式，在程序启动时和运行初期，仍然采取解释执行，并采集热点信息和记录代码变更情况
    // 2.总之，这段代码最终还是解释执行，从而不需要加 volatile
    //
    /**
     * 构造 chat server，在创建时，可以选择是否使用默认的方式去构造。
     *
     * @param useDefaultProcessors 如果为 true，将会使用默认的业务处理器、通信处理器来处理请求。一般情况下为 true，
     *                             即无需手工配置并使用默认配置。为 false 手工配置，一般是 定制化、或者做测试测试的场景。
     * @param config               服务端初始配置
     */
    public ChatServer(boolean useDefaultProcessors,Config config) {
        this.useDefaultProcessors = useDefaultProcessors;
        this.config = Objects.requireNonNull(config);
        this.sessionMap = SessionMap.getInstanceOrCreate(config);
        this.bizThreadPool = initBizThreadPool();
        this.userProcessor = initUserProcessor();
        this.remotingServer = initRemoting();

    }


    /**
     * 注册业务处理器，只有在 useDefaultProcessors=false 时才可以使用，否则会抛出异常
     * @param code 业务码
     * @param processor 处理器，不可以为 null
     */
    public void registerBizProcessor(byte code, RequestProcessor processor) {
        if (getCurrentStatus() != Status.NEW)
            throw new IllegalStateException("只有新建状态才可以注册处理器！");

        if (useDefaultProcessors)
            throw new RuntimeException("检测到使用默认配置(useDefaultProcessors=true)，不允许手工配置 processor!");

        userProcessor.registerBizProcessors(code, processor);
    }


    private ThreadPoolExecutor initBizThreadPool(){
        int coreNum = config.getInt(ServerConfigNames.BIZ_CORE_THREAD_NUM);
        int maxNum = config.getInt(ServerConfigNames.BIZ_MAX_THREAD_NUM);
        int aliveSec = config.getInt(ServerConfigNames.BIZ_EXTRA_T_ALIVE_SECONDS);
        boolean legal = (coreNum < maxNum) && (aliveSec > 0);
        if (!legal)
            throw new IllegalArgumentException("参数不合法，请重新检查 builder 参数。" +
                    "要求：核心线程<最大线程 && 多余线程存活时间>0 ");

        return new ThreadPoolExecutor(
                coreNum,
                maxNum,
                aliveSec,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("server-biz-thread"));
    }

    private UserProcessor initUserProcessor() {
        if (useDefaultProcessors){
            registerBizProcessor(BizCode.Messaging.code(), new MessagingTransferProcessor(this.getSessionMap(),this.config));
            registerBizProcessor(BizCode.LogInRequest.code(), new LogInProcessor(this.getSessionMap(),this.config));
            registerBizProcessor(BizCode.LogOutRequest.code(), new LogOutProcessor());
        }
        return new UserProcessor(bizThreadPool);
    }

    private RemotingServer initRemoting(){
        return new RemotingServer(this);
    }




    @Override
    protected void doStart() {
        remotingServer.start();
        log.info("服务端已启动......");
    }

    @Override
    protected void doShutdown() {
        // 1.禁止所有 channel 再来新请求
        sessionMap.stopAcceptingRequests();

        // 2.等待未执行完的任务，再执行一会
        long start = System.currentTimeMillis();
        AtomicInteger currentRequestCounts = this.userProcessor.getCurrentRequestCounts();
        Long timeout = config.getLong(ServerConfigNames.SHUTDOWN_TIMEOUT_MILLIS);
        while(currentRequestCounts.get() > 0
                && System.currentTimeMillis() - start < timeout){
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                log.warn("准备强行关闭！");
                break;
            }
        }

        // 3.直接关闭 session 服务
        if (currentRequestCounts.get() > 0)
            log.warn("检测到当前仍存在未执行完的任务，准备强行关闭！");
        sessionMap.removeAllUnSafe();

        // 4.关闭 server remoting (IO threads)
        remotingServer.shutdown();

        // 5.关闭 biz pool
        bizThreadPool.shutdown();
        log.info("服务端已关闭!");
    }


    public Map<String, Supplier<ChannelHandler>> getCustomHandlersAhead() {
        return customHandlersAhead;
    }

    /**
     * 注册定制化的 handler，需要注意的是：
     * 1.这些 handler 会放在所有内置 handler 之前
     * 2.这些 handler 是否是 @Sharable 应该通过 supplier 的方式决定
     * （2-1）如果 supplier 提供的是固定的一个 handler，那就是 @Sharable
     * （2-2）如果 supplier 提供的是 new 出来的，那么就是非 @Sharable
     */
    public void registerCustomHandlersAhead(String name, Supplier<ChannelHandler> handlerSupplier) {
        this.customHandlersAhead.put(name, handlerSupplier);
    }


    public SessionMap getSessionMap(){
        return this.sessionMap;
    }


    public Config getConfig(){
        return config;
    }

    public UserProcessor getUserProcessor(){
        return userProcessor;
    }

}
