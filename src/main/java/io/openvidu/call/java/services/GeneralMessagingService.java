package io.openvidu.call.java.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openvidu.call.java.constant.TextError;
import io.openvidu.call.java.models.message;
import io.openvidu.call.java.models.SubmitResponse;
import io.openvidu.call.java.models.WhatsappSubmit;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static io.openvidu.call.java.constant.allConstants.SUBMISSION_STATE;

@Service
public class GeneralMessagingService implements MessagingService {
  @Autowired
  RestTemplate restTemplate;
  @Autowired
  OpenViduService openViduService;
  private static final Logger logger = LoggerFactory.getLogger(GeneralMessagingService.class);
  @Value("${SMS_TEXT:-}")
  String smsText;
  @Value("${SMS_URL:-}")
  String smsUrl;
  @Value("${WA_URL:-}")
  String waUrl;
  @Value("${WA_API_KEY:-}")
  String waApiKey;

  @Override
  public SubmitResponse sendSms(HttpServletRequest request, HttpServletResponse response,Object... args) throws IOException, URISyntaxException {
    SubmitResponse submitResponse = null;
    try {
      SubmitResponse failedResponse = validateSessionAndRequest(args);
      if (failedResponse != null)
        return failedResponse;
      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      HttpEntity<String> entity = new HttpEntity<String>(headers);
      URI uri = new URI(createURL(smsUrl, args[1], URLEncoder.encode(createURL(smsText, args[2]), "UTF-8")));
      submitResponse = restTemplate.exchange(uri, HttpMethod.GET, entity, SubmitResponse.class).getBody();
      return submitResponse;
    } catch (HttpClientErrorException ex) {
      ResponseEntity<String> errorResponse = null;
      if (ex.getStatusCode() != null) {
        errorResponse = ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
      } else {
        errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
      }
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        submitResponse = objectMapper.readValue(errorResponse.getBody(), SubmitResponse.class);
        return submitResponse;
      } catch (JsonProcessingException e) {
        logger.error("Error in Paring Bad Request {}",e.getMessage());
      }
    } catch (OpenViduJavaClientException e) {
      throw new RuntimeException(e);
    } catch (OpenViduHttpException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("Internal server error occurred.");
  }
  @Override
  public SubmitResponse sendWA(HttpServletRequest request,HttpServletResponse response,Object... args) throws OpenViduJavaClientException, OpenViduHttpException {
    SubmitResponse submitResponse = null;

    try {
      SubmitResponse failedResponse = validateSessionAndRequest(args);
      if (failedResponse != null)
        return failedResponse;
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.add("apikey", waApiKey);
      logger.info(Arrays.toString(args));
      WhatsappSubmit body = new WhatsappSubmit();
      body.setFrom(String.valueOf(args[3]));
      body.setTo(String.valueOf(args[1]));
      body.setType(String.valueOf(args[4]));
      ArrayList<String> placeHolders = new ArrayList<>();
      placeHolders.add((String) args[2]);
      message message = new message(String.valueOf(args[5]),placeHolders);
      body.setMessage(message);
      ObjectMapper jsonBody=new ObjectMapper();
      String waBody=jsonBody.writeValueAsString(body);
      HttpEntity<String> entity = new HttpEntity<String>(waBody, headers);
      logger.info(String.valueOf(entity.getBody()));
      submitResponse=restTemplate.exchange(waUrl, HttpMethod.POST, entity, SubmitResponse.class).getBody();
      return submitResponse;
    }catch (HttpClientErrorException ex) {
      ResponseEntity<String> errorResponse = null;
      if (ex.getStatusCode() != null) {
        errorResponse = ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
      } else {
        errorResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
      }
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        submitResponse = objectMapper.readValue(errorResponse.getBody(), SubmitResponse.class);
        return submitResponse;
      } catch (JsonProcessingException e) {
        logger.error("Error in Paring Bad Request {}",e.getMessage());
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("Internal server error occurred.");

  }
  private SubmitResponse validateSessionAndRequest(Object... args) throws OpenViduJavaClientException, OpenViduHttpException {
    logger.info(Arrays.toString(args));
    Optional<SubmitResponse> validationResponse = validateSession((String) args[0]);
    if (validationResponse.isPresent())
      return validationResponse.get();
    Optional<SubmitResponse> reqValidationResponse = validateRequest(String.valueOf(args[1]), (String) args[2]);
    return reqValidationResponse.orElse(null);
  }
  private Optional<SubmitResponse> validateRequest(String msisdn,String callUrl) {
    if (msisdn == null || msisdn.equalsIgnoreCase("")) {
      return Optional.of(getFailedSubmitResponse(TextError.MISSING_MSISDN, 0L));
    }
    if (callUrl == null || callUrl.equalsIgnoreCase("")) {
      return Optional.of(getFailedSubmitResponse(TextError.MISSING_CALLURL, 0L));
    }
    return Optional.empty();
  }
  public SubmitResponse getFailedSubmitResponse(TextError error, Object... args) {
    SubmitResponse submitResponse = new SubmitResponse();
    submitResponse.setStatusCode(error.getCode());
    submitResponse.setDescription(String.format(error.getText(), args));
    submitResponse.setState((String) SUBMISSION_STATE);
    submitResponse.setTransactionId("0");
    return submitResponse;
  }
  @Override
  public Optional<SubmitResponse> validateSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
    if(sessionId!=null) {
      logger.info("Validating Session by id {} ", sessionId);
      Session session = openViduService.getSession(sessionId);
      if(session!=null) {
        int allowedParticipant = 5;
        int activeParticipant = session.getActiveConnections().size();
        if (activeParticipant > allowedParticipant) {
          logger.info("Participant limit Exceeded for SessionID : {}", sessionId);
          return Optional.of(getFailedSubmitResponse(TextError.THROTTELING_ERROR, 0L));
        }
      }else{
        return Optional.empty();
      }
    }else {
      return Optional.of(getFailedSubmitResponse(TextError.SESSION_ERROR));
    }
    return Optional.empty();
  }
  public String createURL (String url, Object ... params) {
    return new MessageFormat(url).format(params);
  }
}
