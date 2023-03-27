package com.zhulin.test;

import org.junit.Test;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/14
 * @Description:
 */
public class TestString {

    @Test
    public void test1() {
        String s="abc";
        s="abcd";
    }

    @Test
    public void test2() {
        String s = new String("1");
        s = s.intern();
        String s2 = "1";
        System.out.println(s == s2);//jdk6：false   jdk7/8：false

        String s3 = new String("1") + new String("1");
        s3.intern();
        String s4 = "11";
        System.out.println(s3 == s4);//jdk6：false  jdk7/8：true

    }

    @Test
    public void test3(){
        byte b1=127;
        byte b2=127;
        b1+=b2;
        System.out.println(b1);
    }
}
