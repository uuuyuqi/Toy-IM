package me.yq.support;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.LogInRequest;
import me.yq.biz.LogOutRequest;
import me.yq.biz.Message;
import me.yq.biz.Notice;
import me.yq.biz.domain.Friend;
import me.yq.biz.domain.User;
import me.yq.common.BaseRequest;
import me.yq.common.BaseResponse;
import me.yq.common.BizCode;
import me.yq.common.ResponseStatus;
import me.yq.common.exception.BusinessException;
import me.yq.common.exception.SystemException;
import me.yq.remoting.RemotingClient;
import me.yq.remoting.Stateful;
import me.yq.remoting.config.ClientConfigNames;
import me.yq.remoting.config.DefaultClientConfig;
import me.yq.remoting.processor.MessageReceivedProcessor;
import me.yq.remoting.processor.NoticeFromServerProcessor;
import me.yq.remoting.support.Config;
import me.yq.remoting.transport.Callback;
import me.yq.remoting.transport.Session;
import me.yq.remoting.transport.process.RequestProcessor;
import me.yq.remoting.transport.process.UserProcessor;
import me.yq.remoting.utils.NamedThreadFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 聊天客户端
 *
 * @author yq
 * @version v1.0 2023-02-12 10:00
 */
@Slf4j
public class ChatClient extends Stateful {

    private final Config config;

    private final boolean useDefaultProcessors;

    private final RemotingClient remotingClient;

    private final ThreadPoolExecutor bizThreadPool;

    private final UserProcessor userProcessor;

    /**
     * 一般使用该方式创建即可
     */
    public ChatClient() {
        this(true);
    }

    public ChatClient(boolean useDefaultProcessors) {
        this(useDefaultProcessors, new DefaultClientConfig());
    }

    /**
     * 构造 chat client，在创建时，可以选择是否使用默认的方式去构造。
     *
     * @param useDefaultProcessors 如果为 true，将会使用默认的业务处理器、通信处理器来处理请求。一般情况下为 true，
     *                             即无需手工配置并使用默认配置。为 false 手工配置，一般是 定制化、或者做测试测试的场景。
     */
    public ChatClient(boolean useDefaultProcessors,Config config) {
        this.useDefaultProcessors = useDefaultProcessors;
        this.config = Objects.requireNonNull(config);
        this.bizThreadPool = initBizThreadPool();
        this.userProcessor = initUserProcessor();
        this.remotingClient = initRemoting();
    }




    /**
     * 如果支持多用户登录，这里的 User 需要改成 List<User>
     */
    private User currentUser;

    private boolean onlineFlag = false;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 注册业务处理器，一般情况下，不需要手工注册，使用默认配置即可。当开启 useDefaultProcessors=false 时，才可以手工注册
     * @param code 业务码
     * @param processor 处理器
     */
    public void registerBizProcessor(byte code, RequestProcessor processor) {
        if (getCurrentStatus() != Status.NEW)
            throw new IllegalStateException("只有新建状态才可以注册处理器！");

        if (useDefaultProcessors)
            throw new RuntimeException("检测到使用默认配置(useDefaultProcessors=true)，不允许手工配置 processor!");

        userProcessor.registerBizProcessor(code, processor);
    }


    private ThreadPoolExecutor initBizThreadPool(){
        Integer coreNum = config.getInt(ClientConfigNames.BIZ_CORE_THREAD_NUM);
        Integer maxNum = config.getInt(ClientConfigNames.BIZ_MAX_THREAD_NUM);
        Integer maxAliveSec = config.getInt(ClientConfigNames.BIZ_EXTRA_T_ALIVE_SECONDS);
        boolean legal = (coreNum < maxNum) && (maxAliveSec > 0);
        if (!legal)
            throw new IllegalArgumentException("参数不合法，请重新检查 builder 参数。" +
                    "要求：核心线程<最大线程 && 多余线程存活时间>0 ");
        return new ThreadPoolExecutor(
                coreNum,
                maxNum,
                maxAliveSec,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("client-biz-thread"));
    }

    private UserProcessor initUserProcessor() {
        UserProcessor userProcessor = new UserProcessor(bizThreadPool);
        if (useDefaultProcessors){
            userProcessor.registerBizProcessor(BizCode.Messaging.code(), new MessageReceivedProcessor(this));
            userProcessor.registerBizProcessor(BizCode.Noticing.code(), new NoticeFromServerProcessor(this));
        }
        return userProcessor;
    }

    private RemotingClient initRemoting(){
        return new RemotingClient(this);
    }



    //************** biz method *************
    @Override
    protected void doStart() {
        this.remotingClient.start();
        log.info("客户端已启动......");
    }

    @Override
    protected void doShutdown() {
        // 关闭 channel （和服务端相反）
        remotingClient.shutdown(config.getLong(ClientConfigNames.SHUTDOWN_TIMEOUT_MILLIS));

        // 关闭 biz pool
        bizThreadPool.shutdown();

        log.info("客户端已关闭");
    }


    public User getCurrentUser() {
        return currentUser;
    }

    private void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Config getConfig(){
        return config;
    }

    public UserProcessor getUserProcessor(){
        return userProcessor;
    }

    //================== biz method ==================

    /**
     * 登录
     *
     * @param userId 用户账号
     * @param passwd 用户密码
     */
    public synchronized void logIn(long userId, String passwd) {
        try {
            if (this.onlineFlag)
                throw new BusinessException("当前用户已登录，请勿重复登录: " + getCurrentUser());

            LogInRequest logInRequest = new LogInRequest();
            logInRequest.setUser(new User(userId, passwd));

            BaseRequest request = new BaseRequest(BizCode.LogInRequest.code(), logInRequest);
            BaseResponse response = remotingClient.sendRequestSync(request);

            if (response.getStatus() != ResponseStatus.SUCCESS)
                throw new BusinessException("登陆失败！原因: " + response.getReturnMsg(), (Throwable) response.getAppResponse());


            this.setCurrentUser((User) response.getAppResponse());
            this.onlineFlag = true;

            log.info("登陆成功！服务端信息：{}", response.getReturnMsg());

        } catch (Exception e) {
            // todo 提取到一个 helper 或者 delegate 中
            if (e instanceof BusinessException)
                throw (BusinessException)e;
            else if (e instanceof SystemException)
                throw (SystemException)e;
            else
                throw new RuntimeException(e);
        }
    }

    /**
     * 登出
     *
     * @param userId 用户账号
     */
    public synchronized void logOut(long userId) {
        checkState();
        LogOutRequest logOutRequest = new LogOutRequest();
        logOutRequest.setUser(new User(userId));

        BaseRequest request = new BaseRequest(BizCode.LogOutRequest.code(), logOutRequest);
        remotingClient.sendRequestOneway(request);

        this.setOnlineFlag(false);
        this.setCurrentUser(null);


        log.info("已下线，现在请切换账号...");
    }

    /**
     * 向好友发送信息
     *
     * @param targetUserId 好友 id
     * @param msg          消息内容
     */
    public synchronized void sendMsg(long targetUserId, String msg) {
        checkState();

        User from = new User(getCurrentUser().getUserId());
        User to = new User(targetUserId);
        Message message = new Message(from, to, msg);
        messageMap.put(message.getMessageId(), message);
        BaseRequest request = new BaseRequest(BizCode.Messaging.code(), message);


        this.remotingClient.sendRequestCallback(request, new Callback() {

            private final int messageId = message.getMessageId();

            @Override
            public void onResponse(BaseResponse response) {
                // 对方成功回应，但是处理出现了问题
                if (response.getStatus() != ResponseStatus.SUCCESS) {
                    failedToSendMsg(messageId,"对方网络不佳，请稍后再试～");
                }
                log.debug("信息已发送！收到的反馈：{}", response.getReturnMsg());
                messageMap.remove(messageId);
            }

            @Override
            public void onException(Throwable cause) {
                log.error("发送信息失败！原因：{}", cause.getMessage());
                failedToSendMsg(messageId,"消息发送失败，请检查网络......");
                messageMap.remove(messageId);
            }

            @Override
            public void onTimeout() {
                failedToSendMsg(messageId,"对方网络不佳，请稍后再试～");
                messageMap.remove(messageId);
            }

            @Override
            public Executor getExecutor() {
                return ChatClient.this.bizThreadPool;
            }
        });
    }

    /**
     * 已经发送的消息，但是对方还未回应，已经回应的会被移除
     */
    private final Map<Integer,Message> messageMap = new ConcurrentHashMap<>();

    private void failedToSendMsg(int messageId, String possibleReason) {
        checkState();
        // todo 重发机制
        System.err.println("消息[" + messageMap.get(messageId).getMsg() + "]发送失败: " + possibleReason);
    }

    /**
     * 接收来自好友的消息
     *
     * @param from 好友
     * @param msg  消息内容
     */
    public void acceptMsg(User from, String msg) {
        checkState();
        Friend friend = getCurrentUser().queryFriend(from.getUserId());
        String newMsg = String.format("%-5s-- %s\n\"%s\"\n", friend.getName(), LocalDateTime.now().format(timeFormatter),msg);
        System.out.println(newMsg);
    }

    /**
     * 接受并向用户展示通知
     * @param notice 通知类型消息，一般具备标题和通知内容
     */
    public void acceptNotice(Notice notice) {
        checkState();
        System.out.printf("收到新的通知：%s\n", notice.getNoticeTitle());
        System.out.println(notice.getNoticeContent());
    }

    /**
     * 和服务端丢失连接
     */
    public void loseConnection() {
        //todo 直接关闭 or 重试？
        log.error("检测到和服务端的连接丢失，现在尝试重新连接......");

        this.onlineFlag = false;
    }

    private void checkState(){
        if (getCurrentStatus() != Status.RUNNING)
            throw new IllegalStateException("请先启动客户端");
        else if (!isOnline())
            throw new IllegalStateException("当前用户未登录，请先登录");
    }

    /**
     * 获取和服务端的 session
     * @return 服务端的 session
     */
    public Session getServerSession() {
        return this.remotingClient.getServerSession();
    }

    /**
     * 注意，判断是否在线时，不仅仅通过在线状态来判断，还要检测一下当前的连接状态，是否健康
     * @return 如果在线标志为 false，则直接返回 false，如果在线，则会二次确认当前连接是否真的健康
     */
    public boolean isOnline() {
        if (!onlineFlag)
            return false;
        return this.remotingClient.hasConnected();
    }

    public void setOnlineFlag(boolean onlineFlag) {
        this.onlineFlag = onlineFlag;
    }

    public ThreadPoolExecutor getBizThreadPool() {
        return bizThreadPool;
    }
}
