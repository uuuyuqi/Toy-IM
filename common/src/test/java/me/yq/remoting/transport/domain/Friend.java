package me.yq.remoting.transport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yq
 * @version v1.0 2023-02-24 09:27
 */
@Data
@AllArgsConstructor
public class Friend implements Serializable {
    String name;
    int age;
}
