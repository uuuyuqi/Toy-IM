package me.yq.remoting.utils;

/**
 * @author yq
 * @version v1.0 2023-03-30 17:27
 */
public class StringUtils {

    public static boolean hasLength(String str){
        return str != null && str.trim().length() > 0;
    }

    public static boolean hasNoLength(String str){
        return !hasLength(str);
    }
}
