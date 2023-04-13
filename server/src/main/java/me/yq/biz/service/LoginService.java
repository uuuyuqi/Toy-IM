package me.yq.biz.service;

import lombok.extern.slf4j.Slf4j;
import me.yq.biz.domain.User;
import me.yq.biz.repository.UserDao;
import me.yq.common.exception.BusinessException;

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

    public User login(User user){

        User userFound = userDao.findUser(user);
        if (userFound == null){
            log.error("用户名[{}]不存在！登录失败！",user.getUserId());
            throw new BusinessException("用户名不存在！");
        }

        if (!userFound.getPasswd().equals(user.getPasswd())){
            log.error("用户名或密码错误！登录失败！");
            throw new BusinessException("用户名或密码错误！");
        }

        // 登录成功后，将密码到的用户密码置空
        userFound.setPasswd(null);
        return userFound;
    }

}
