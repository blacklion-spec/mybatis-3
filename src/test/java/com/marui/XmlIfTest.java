package com.marui;

import com.marui.condition.UserCondition;
import com.marui.config.MybatisUtil;
import com.marui.entity.User;
import com.marui.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;

import java.util.List;

/**
 * @author marui
 * @Description
 */
public class XmlIfTest {


  @Test
  public void test1() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
      final UserCondition userCondition = new UserCondition();
      userCondition.setId(1);
      final List<User> users = mapper.selectByCondition(userCondition);
      System.out.println(users);
    }
  }


  @Test
  public void test2() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
      final User user = new User();
      user.setId(2);
      user.setName("xa");
      user.setPwd("dasdasd");
      final boolean insert = mapper.insert(user);
      sqlSession.commit();
    }
  }
}
