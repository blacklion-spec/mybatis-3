package com.marui.entity;

import java.io.Serializable;

/**
 * @createTime 2022年12月21日 23:12:00
 */
public class Parent implements Serializable {

  private String name;

  private Son son;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Son getSon() {
    return son;
  }

  public void setSon(Son son) {
    this.son = son;
  }

  @Override
  public String toString() {
    return "Parent{" +
      "name='" + name + '\'' +
      ", son=" + son +
      '}';
  }
}
