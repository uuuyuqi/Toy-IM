package me.yq.repository;

import me.yq.biz.domain.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户持久层，这里 mock 掉了后端信息
 *
 * @author yq
 * @version v1.0 2023-02-15 9:27 AM
 */
public class UserDao {
    public UserDao() {
    }

    public User findUser(User user) {
        return findFromMockDB(user);
    }

    private final List<User> userTable = new ArrayList<User>() {{
        add(new User(157146, "abcde", "yq", 24, "杭州市萧山区钱江世纪城", "每天保持训练！",null));
        add(new User(680712, "rm-rf/@12345","Jack", 35, "美国俄亥俄州", "让我们一起学习吧",null));
        add(new User(909900, "123456","Lydia", 18, "安徽省合肥市蜀山区", "健身让我年轻",null));
    }};

    private User findFromMockDB(User target) {
        for (User user : userTable) {
            if (user.getUserId() == target.getUserId()) {
                return user;
            }
        }
        // 用户不存在
        return null;
    }
}
