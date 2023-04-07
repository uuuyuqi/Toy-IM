package me.yq.remoting.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 存放一些通用性的 utils
 * @author yq
 * @version v1.0 2023-04-05 17:21
 */
public class CommonUtils {

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);

    /**
     * @return 返回 yyyy-MM-dd HH:mm:ss 格式的当前时间的字符串
     */
    public static String now(){
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

}
