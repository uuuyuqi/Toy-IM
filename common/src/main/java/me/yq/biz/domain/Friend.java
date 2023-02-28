package me.yq.biz.domain;

/**
 * 好友实体
 * @author yq
 * @version v1.0 2023-02-12 9:55
 */
public class Friend {
    private final long friendId;
    private String name;

    public Friend(long friendId, String name) {
        this.friendId = friendId;
        this.name = name;
    }

    public long getFriendId() {
        return friendId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
