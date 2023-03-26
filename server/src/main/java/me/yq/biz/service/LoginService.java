package me.yq.biz.service;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.domain.User;
import me.yq.biz.repository.UserDao;

/**
 * 校验登录信息是否合法的服务
 *
 * @author yq
 * @version v1.0 2023-02-15 9:55 AM
 */
@Slf4j
public enum LoginService {
    INSTANCE;

    public static LoginService getInstance(){
        return INSTANCE;
    }


    //@Autowired
    private final UserDao userDao = new UserDao();

    public void login(User user){

        User userFound = userDao.findUser(user);
        if (userFound == null){
            log.error("用户名[{}]不存在！登录失败！",user.getUserId());
            throw new RuntimeException("用户名不存在！");
        }

        if (!userFound.getPasswd().equals(user.getPasswd())){
            log.error("用户名或密码错误！登录失败！");
            throw new RuntimeException("用户名或密码错误！");
        }
    }

}
