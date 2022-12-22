package com.marui.entity;

import java.io.Serializable;

/**
 * @createTime 2022年12月21日 23:13:00
 */
public class Son implements Serializable {

  private Integer id;

  private String name;

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

  @Override
  public String toString() {
    return "Son{" +
      "id=" + id +
      ", name='" + name + '\'' +
      '}';
  }
}
