package com.marui;

import com.marui.config.MybatisUtil;
import com.marui.entity.Parent;
import com.marui.entity.User;
import com.marui.handler.SimpleReturnHandler;
import com.marui.mapper.ParentMapper;
import com.marui.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

/**
 * @author marui
 * @version 1.0.0
 * @ClassName UserMapperTest.java
 * @Description
 * @createTime 2022年01月03日 16:38:00
 */
public class UserMapperTest {


    @Test
    public void test1() {
        SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
        //DefaultSqlSession
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            User user = mapper.selectUser(5);
            System.out.println(user);
        }
    }

    @Test
    public void resultHandlerTest() {
      SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
      //DefaultSqlSession
      try (SqlSession sqlSession = sqlSessionFactory.openSession()){
        final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = mapper.selectUser(5,new SimpleReturnHandler()); //为啥没用呢
        System.out.println(user);
      }
    }

  /**
   * 一级缓存二级缓存测试
   * 1.一级缓存，同一个sqlSession的查询结果会被Executor缓存，关闭sqlSession的时候，清空缓存
   * 2。二级缓存是，不同的sqlSession，同一个查询，共享相同的缓存，关闭sqlSession不会影响缓存
   * 3.如果该Mapper启用了缓存，会先从二级缓存中获取，然后在是一级缓存
   *
   * 结论：一级缓存可能是导致SqlSession无法被共享，用完之后就要关闭，如果不关闭，一级缓存的存在就会导致数据的不一致。
   * 其他SqlSession已经修改了数据，该sqlSession就是过期数据了
   */
  @Test
  public void cacheTest() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
      User user = mapper.selectUser(5);
      System.out.println(user);
      //缓存测试
      User user0 = mapper.selectUser(5);
      System.out.println(user0);
    }
  }


  @Test
  public void optionalTest() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
      Optional<User> user = mapper.selectUserOp(5);
      System.out.println(user.get());
    }
  }


  @Test
  public void insertTest() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      final UserMapper mapper = sqlSession.getMapper(UserMapper.class);
      User user = new User();

    }
  }




}
