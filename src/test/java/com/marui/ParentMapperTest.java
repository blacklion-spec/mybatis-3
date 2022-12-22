package com.marui;

import com.marui.config.MybatisUtil;
import com.marui.entity.Parent;
import com.marui.entity.Son;
import com.marui.mapper.ParentMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;

import java.util.List;

/**
 * @createTime 2022年12月21日 23:43:00
 */
public class ParentMapperTest {

  /**
   * 延迟加载测试
   */
  @Test
  public void lazyLoadTest() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    //DefaultSqlSession
    List<Parent> parents = null;
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      ParentMapper mapper = sqlSession.getMapper(ParentMapper.class);
      parents  = mapper.selectAll();
    }
    System.out.println(parents.get(0).getSon());
  }

  /**
   * 可以同时执行多条sql 需要在连接属性中开启 allowMultiQueries=true （目前不知道这是驱动支持还是数据库支持）
   */
  @Test
  public void insertTest() {
    SqlSessionFactory sqlSessionFactory = MybatisUtil.getSqlSessionFactory();
    try (SqlSession sqlSession = sqlSessionFactory.openSession()){
      ParentMapper mapper = sqlSession.getMapper(ParentMapper.class);
      Parent parent = new Parent();
      parent.setName("sds");
      Son son = new Son();
      son.setId(2);
      son.setName("dssd");
      parent.setSon(son);
      int insert = mapper.insert(parent);
      System.out.println(insert);
    }
  }


}
