package io.openvidu.call.java.controllers;


import io.openvidu.call.java.constant.RequestMappings;
import io.openvidu.call.java.models.SubmitResponse;
import io.openvidu.call.java.services.MessagingService;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(RequestMappings.API)
public class MessageApiController {

  @Autowired
  MessagingService messagingService;
  private static final Logger logger= LoggerFactory.getLogger(MessageApiController.class);
  @PostMapping(value="/sendSms", produces = MediaType.APPLICATION_JSON_VALUE)
  public SubmitResponse sendSMS(@RequestBody(required = false) Map<String, ?> params, HttpServletResponse response, HttpServletRequest request) throws IOException, URISyntaxException {
    String msisdn= (String) params.get("msisdn");
    String callUrl= getHeaders(request).get("origin")+(String) params.get("callUrl");
    String sessionId =getHeaders(request).get("sessionid");
    logger.info("REST API: POST {} {} Request Headers={}", RequestMappings.API, params != null ? params.toString() : "{}",getHeaders(request));
    SubmitResponse responseSms=messagingService.sendSms(request,response,sessionId,msisdn,callUrl);
    responseSms.setCallUrl(callUrl);
    logger.info("Request response {}",responseSms);
    return responseSms;
  }
  @PostMapping ("/sendWhatsapp")
  public SubmitResponse sendWA(@RequestBody(required = false) Map<String, ?> params, HttpServletResponse response, HttpServletRequest request) throws IOException, URISyntaxException, OpenViduJavaClientException, OpenViduHttpException {
    String from= (String) params.get("from");
    String to= (String) params.get("msisdn");
    String type= (String) params.get("type");
    String templateid= (String) params.get("templateId");
    String placeHolder= getHeaders(request).get("origin")+(String) params.get("callUrl");
    String sessionId =getHeaders(request).get("sessionid");
    String callUrl= getHeaders(request).get("origin")+(String) params.get("callUrl");
    logger.info("REST API: POST {} {} Request Headers={}", RequestMappings.API, params != null ? params.toString() : "{}",getHeaders(request));
    SubmitResponse responseSms=messagingService.sendWA(request,response,sessionId,to,placeHolder,from,type,templateid);
    responseSms.setCallUrl(callUrl);
    logger.info("Request response {}",responseSms);
    return responseSms;
  }
  private Map<String, String> getHeaders(HttpServletRequest request) {
    Enumeration<String> headers = request.getHeaderNames();
    Map<String, String> headerMap = new HashMap<>();
    while (headers.hasMoreElements()) {
      String header = headers.nextElement();
      headerMap.put(header, request.getHeader(header));
    }
    return headerMap;
  }
}
