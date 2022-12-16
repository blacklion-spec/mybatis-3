package com.marui.handler;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @createTime 2022年12月13日 20:45:00
 */
public class SimpleReturnHandler implements ResultHandler {

  @Override
  public void handleResult(ResultContext resultContext) {
    int resultCount = resultContext.getResultCount();
    Object resultObject = resultContext.getResultObject();
    System.out.println(resultCount);
    System.out.println("XXXXXXXXXXXXXXXXXXXXXXX");
  }


}
