package org.tron.core.services.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.alibaba.fastjson.JSONObject;
import java.io.UnsupportedEncodingException;
import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tron.common.BaseTest;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class GetAssetIssueListServletTest extends BaseTest {

  @Resource
  private GetAssetIssueListServlet getAssetIssueListServlet;

  static {
    dbPath = "db_GetNowBlockServlet_test";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
        }, Constant.TEST_CONF
    );
  }

  public MockHttpServletRequest createRequest(String contentType) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setContentType(contentType);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  public void testGetAssetIssueListByJson() {
    String jsonParam = "{\"visible\": true}";
    MockHttpServletRequest request = createRequest("application/json");
    request.setContent(jsonParam.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    getAssetIssueListServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject.parseObject(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAssetIssueListValue() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    try {
      String params = "visible=true";
      request.setContent(params.getBytes(UTF_8));
      MockHttpServletResponse response = new MockHttpServletResponse();
      getAssetIssueListServlet.doPost(request, response);
      String contentAsString = response.getContentAsString();
      JSONObject.parseObject(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAssetIssueListEmptyParam() {
    MockHttpServletRequest request = createRequest("application/x-www-form-urlencoded");
    String params = "visible=";
    request.setContent(params.getBytes(UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    getAssetIssueListServlet.doPost(request, response);
    try {
      String contentAsString = response.getContentAsString();
      JSONObject.parseObject(contentAsString);
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
  }

}
