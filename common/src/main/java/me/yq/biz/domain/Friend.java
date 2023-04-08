package me.yq.biz.domain;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return friendId == friend.friendId && Objects.equals(name, friend.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(friendId, name);
    }

    @Override
    public String toString() {
        return name + "(" + friendId +")";
    }

    public Friend copy() {
        return new Friend(friendId, name);
    }
}
