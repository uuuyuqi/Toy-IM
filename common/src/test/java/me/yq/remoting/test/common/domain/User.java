package me.yq.remoting.test.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 测试实体
 * @author yq
 * @version v1.0 2023-02-24 09:27
 */
@Data
@AllArgsConstructor
public class User implements Serializable {
    int id;
    Object friend;
}
