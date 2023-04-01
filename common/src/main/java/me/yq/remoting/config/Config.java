package me.yq.remoting.config;

import me.yq.remoting.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置基类，可以提供基础的配置相关的操作<br/>
 * todo 新增 getOrDefault 方法
 * @author yq
 * @version v1.0 2023-03-30 14:54
 */
public abstract class Config {

    private final Map<String, String> configMap = new HashMap<>(128);

    public void putConfig(String configName, String value){

        if (StringUtils.hasNoLength(configName))
            throw new IllegalArgumentException("config 项的名称不合法: " + configName);
        if (value == null)
            throw new IllegalArgumentException("config[" + configName + "] 值必须时 non-non");

        configMap.put(configName,value);
    }


    public String getValue(String configName) {
        return getRawValue(configName);
    }


    public Boolean getBoolean(String configName) {
        String rawValue = getRawValue(configName);
        try{
            return Boolean.parseBoolean(rawValue);
        }catch (Exception e){
            throw new RuntimeException(String.format("[%s] 配置的值 [%s] 不是合法的 boolean",configName,rawValue));
        }
    }



    public Byte getByte (String configName) {
        String rawValue = getRawValue(configName);
        try{
            return Byte.parseByte(rawValue);
        }catch (Exception e){
            throw new RuntimeException(String.format("[%s] 配置的值 [%s] 不是合法的 byte",configName,rawValue));
        }
    }

    public Integer getInt(String configName) {
        String rawValue = getRawValue(configName);
        try{
            return Integer.parseInt(rawValue);
        }catch (Exception e){
            throw new RuntimeException(String.format("[%s] 配置的值 [%s] 不是合法的 int",configName,rawValue));
        }
    }

    public Long getLong(String configName) {
        String rawValue = getRawValue(configName);
        try{
            return Long.parseLong(rawValue);
        }catch (Exception e){
            throw new RuntimeException(String.format("[%s] 配置的值 [%s] 不是合法的 long",configName,rawValue));
        }
    }

    private String getRawValue(String configName){
        String value = configMap.get(configName);
        if (StringUtils.hasLength(value))
            return value;
        else
            throw new RuntimeException(String.format("配置的 [%s] 值 [%s] 不合法!!! 可能为空白字符串或者根本没有对 [%s] 进行配置！",configName,value,configName));
    }

}
