package io.openvidu.call.java.services;

import io.openvidu.call.java.models.SubmitResponse;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

public interface MessagingService {
  SubmitResponse sendSms(HttpServletRequest request, HttpServletResponse response,Object... args) throws IOException, URISyntaxException;
  SubmitResponse sendWA(HttpServletRequest request, HttpServletResponse response,Object... args) throws IOException, URISyntaxException, OpenViduJavaClientException, OpenViduHttpException;
  public Optional<SubmitResponse> validateSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException;

}
