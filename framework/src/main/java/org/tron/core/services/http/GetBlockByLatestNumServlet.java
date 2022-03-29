package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetBlockByLatestNumServlet extends RateLimiterServlet {

  private static final long BLOCK_LIMIT_NUM = 100;
  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      fillResponse(Util.getVisible(request), Util.getDetail(request),
          Long.parseLong(request.getParameter("num")), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(), params.isDetail(), build.getNum(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, boolean detail, long num, HttpServletResponse response)
      throws IOException {
    if (num > 0 && num < BLOCK_LIMIT_NUM) {
      BlockList reply = wallet.getBlockByLatestNum(num, detail);
      if (reply != null) {
        response.getWriter().println(Util.printBlockList(reply, visible));
        return;
      }
    }
    response.getWriter().println("{}");
  }
}