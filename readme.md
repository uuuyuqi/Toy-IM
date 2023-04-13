
你好

这一个基于 netty 的玩具 IM，似乎只支持**登录**、**注销**和**单聊**......

<br/>

<font size=5><b>那这项目写了个啥？</b></font>

至少它还有以下还算说得过去的功能：
- 使用自定义协议、配套的编解码器
  - 自定义了一套 YQ 协议，包含：魔数、版本、cmd、消息 id、header、body
  - 基于 ByteToMessageCodec 进行消息编解码
  - 基于长度+内容形式解决消息粘包半包
- 支持类似于 rpc 的多种调用方式进行通信
  - 包括多种调用方式：
    - 异步：future、callback
    - 同步：其实委托了 future 的实现...
    - 单向：oneway~
  - 支持多种序列化方式：
    - hessian2：相对折中的方案
    - kryo：你就说快不快吧
    - json：emmmmm
  - 可以额外添加扩展的 netty channel handler，支持添加 @Sharable 的扩展 handler
- 支持 biz 线程池处理业务，不是很多 demo 中纯靠 netty handler 处理请求 :D
  - io 只处理业务并分发请求和响应
  - biz 提交给 io 后，异步调用的情况下直接结束
  - 大量使用 NameThreadFactory，方便排查问题时看线程名字
  - 使用 netty attr 缓存 channel 上的调用，避免自己维护 map
- 支持高度可配置化的业务处理流程
  - 轻松注册业务处理器
  - 在业务处理器处理前后增加了小扩展点，方便做业务增强和单元测试
- 支持长连接的状态维护：
  - 客户端心跳保活
  - 服务端超时摘除
- 支持相对优雅停机
  - 停机前检测当前任务数
  - 停机前关闭 channel 再来新请求
  - 等待一定的超时时间
- 相对完备的单元测试
  - 使用 junit5 + mockito 进行多角度测试，甚至测试了编解码器的粘包半包 
  - 尽量考虑了一些边界条件
- 相对可维护、可拓展


很多地方参考了蚂蚁金服开源的 RPC 框架  <a href="https://github.com/sofastack/sofa-rpc">SOFA-RPC</a> 和 通信框架  <a href="https://github.com/sofastack/sofa-bolt">SOFA-Bolt</a>，
感谢蚂蚁金服的 SOFA 团队的开源精神，让我们有了优秀的学习资料~



