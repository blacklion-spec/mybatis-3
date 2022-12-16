package com.marui.entity;

import java.io.Serializable;

/**
 * @author marui
 * @version 1.0.0
 * @ClassName User.java
 * @Description
 * @createTime 2022年01月03日 16:33:00
 */
public class User implements Serializable {



    private Integer id;


    private String name;


    private String pwd;



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }
}
