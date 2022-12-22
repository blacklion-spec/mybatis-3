package com.marui.mapper;

import com.marui.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.Optional;

/**
 * @author marui
 * @version 1.0.0
 * @ClassName UserMapper.java
 * @Description
 * @createTime 2022年01月03日 16:33:00
 */
public interface UserMapper {

    User selectUser(int id);


    Optional<User> selectUserOp(int id);


    boolean insert(@Param("user") User user);

  User selectUser(int id, ResultHandler<User> resultHandler);

}
