package me.yq.biz.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 用户实体
 * @author yq
 * @version v1.0 2023-02-12 9:53
 */
@Getter
public class User {
    private long userId;
    private String passwd;

    private String name;
    private int age;
    private String address;
    private String signature; // 个性签名

    private List<Friend> friendList;

    public User(){

    }

    public User(long userId) {
        this.userId = userId;
    }

    public User(long userId, String passwd) {
        this.userId = userId;
        this.passwd = passwd;
    }

    public User(long userId, String passwd, String name, int age, String address, String signature, List<Friend> friendList) {
        this.userId = userId;
        this.passwd = passwd;
        this.name = name;
        this.age = age;
        this.address = address;
        this.signature = signature;
        this.friendList = friendList;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<Friend> getFriendList() {
        return Collections.unmodifiableList(friendList);
    }

    public void setFriendList(List<Friend> friendList) {
        this.friendList = friendList;
    }

    public Friend queryFriend(long friendId){
        for (Friend friend : this.friendList) {
            if (friend.getFriendId() == friendId)
                return friend;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return name + "(" + userId +")";
    }


    public User copy(){
        User user = new User();
        user.setUserId(this.userId);
        user.setPasswd(this.passwd);
        user.setName(this.name);
        user.setAge(this.age);
        user.setAddress(this.address);
        user.setSignature(this.signature);
        List<Friend> friendList = new LinkedList<>();
        for (Friend friend : this.friendList) {
            friendList.add(friend.copy());
        }
        user.setFriendList(friendList);
        return user;
    }
}
