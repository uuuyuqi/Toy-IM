package me.yq.test.common.domain;

import java.util.LinkedHashMap;

/**
 * 测试实体
 * @author yq
 * @version v1.0 2023-03-01 19:23
 */
public class MyLinkedHashMap extends LinkedHashMap<String,String> {
    private String var1;

    private String var2;

    private String var3;

    public String getVar1() {
        return var1;
    }

    public void setVar1(String var1) {
        this.var1 = var1;
    }

    public String getVar2() {
        return var2;
    }

    public void setVar2(String var2) {
        this.var2 = var2;
    }

    public String getVar3() {
        return var3;
    }

    public void setVar3(String var3) {
        this.var3 = var3;
    }

    @Override
    public String toString() {
        return "MyLinkedHashMap{" +
                "var1='" + var1 + '\'' +
                ", var2='" + var2 + '\'' +
                ", var3='" + var3 + '\'' +
                '}' + "super{" + super.toString() +"}";
    }
}
