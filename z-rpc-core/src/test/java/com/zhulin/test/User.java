package com.zhulin.test;

import java.io.Serializable;

/**
 * @Author:ZHULIN
 * @Date: 2023/3/5
 * @Description:
 */
public class User implements Serializable {
    private int id;
    private String name;
    private int age;
    private String address;
    private String bankNo;
    private int sex;
    private String remark;

    public User(int id, String name, int age, String address, String bankNo, int sex, String remark) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
        this.bankNo = bankNo;
        this.sex = sex;
        this.remark = remark;
    }

    public User() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getBankNo() {
        return bankNo;
    }

    public void setBankNo(String bankNo) {
        this.bankNo = bankNo;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
