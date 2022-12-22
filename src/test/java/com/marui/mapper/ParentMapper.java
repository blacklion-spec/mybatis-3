package com.marui.mapper;

import com.marui.entity.Parent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @createTime 2022年12月21日 23:20:00
 */
public interface ParentMapper {

  List<Parent> selectAll();

  int insert(@Param("p") Parent parent);

}
