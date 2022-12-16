package com.marui.config;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author marui
 * @version 1.0.0
 * @ClassName MybatisUtil.java
 * @Description
 * @createTime 2022年01月03日 16:26:00
 */
public class MybatisUtil {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "marui/mybatis-config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }


    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

}
