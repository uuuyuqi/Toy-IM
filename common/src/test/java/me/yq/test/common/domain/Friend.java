package me.yq.test.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测试实体
 * @author yq
 * @version v1.0 2023-02-24 09:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Friend implements Serializable {
    String name;
    int age;
}
