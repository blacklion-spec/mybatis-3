package com.marui.mapper;

import com.marui.entity.User;
import com.marui.handler.SimpleReturnHandler;
import org.apache.ibatis.annotations.Param;

/**
 * @author marui
 * @version 1.0.0
 * @ClassName UserMapper.java
 * @Description
 * @createTime 2022年01月03日 16:33:00
 */
public interface UserMapper {

    User selectUser(int id);


    boolean insert(@Param("user") User user);

}
