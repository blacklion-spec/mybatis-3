package com.marui.handler;

import com.marui.entity.User;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @createTime 2022年12月13日 20:45:00
 */
public class SimpleReturnHandler implements ResultHandler<User> {


  @Override
  public void handleResult(ResultContext<? extends User> resultContext) {
    int resultCount = resultContext.getResultCount();
    Object resultObject = resultContext.getResultObject();
    System.out.println(resultCount);
    System.out.println("XXXXXXXXXXXXXXXXXXXXXXX");
  }
}
