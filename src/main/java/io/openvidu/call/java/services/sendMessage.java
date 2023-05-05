package io.openvidu.call.java.services;

import com.google.gson.Gson;
import io.openvidu.call.java.controllers.URLController;
import io.openvidu.call.java.models.ShortenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1")
public class sendMessage {
  @Autowired
  RestTemplate restTemplate;
  @Value("${sms.url:-}")
  String url;
  @Value("${whatsapp.url:https://api.pinbot.ai/v1/wamessage/send}")
  String whatsAppUrl;
  @Value("${sms.text:-}")
   String text;
  @Value("${whatsapp.text:}")
  String whatsappText;
  @Autowired
  URLController urlController;
  @Autowired
  ShortenRequest shortenRequest;
  @Value("${tiny.url:-}")
  private String baseString;

  @PostMapping("/notify1")
  public ResponseEntity<?> sessionList(@RequestBody(required = false) Map<String, Object> params, HttpServletRequest request,
                                       HttpServletResponse res) throws Exception {
    String msisdn = params.get("msisdn").toString();
    String CallUrl = params.get("callURl").toString();
    Gson gson = new Gson();
    String responseBody=sendSms(msisdn,CallUrl,request);
    System.out.println(responseBody);
    Map<Object,Object> attributes = gson.fromJson(responseBody,Map.class);
    Map<String, Object> response = new HashMap<String, Object>();
    System.out.println("Response url:- "+baseString+CallUrl);
    response.put("state",attributes.get("state"));
    response.put("description",attributes.get("description"));
    response.put("Url",baseString+CallUrl);

    return new ResponseEntity<>(response,HttpStatus.OK);
  }

  @PostMapping("/sendSms")
  public String sendSms(String msisdn,String CallUrl,HttpServletRequest request) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    shortenRequest.setUrl(CallUrl);
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity <String> entity = new HttpEntity<String>(headers);
    String finalUR=baseString+CallUrl;
    System.out.println(finalUR+baseString+CallUrl);
    String finalText=createURL(text,finalUR);
    finalText=URLEncoder.encode(finalText, "UTF-8");
    String finalUrl=createURL(url,msisdn,finalText);
    URI uri =new URI(finalUrl);

    return restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();

  }
  @PostMapping("/sendWhatsapp")
  public String sendMessage(String recipient, String templateId, String url, String salutation, String salutation1) {
      String endpoint = whatsAppUrl;
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Build the request body
      String requestBody = "{ \"from\": \"919811026184\", " +
              "\"to\": \"" + recipient + "\", " +
              "\"type\": \"template\", " +
              "\"message\": { " +
              "\"templateid\": \"" + templateId + "\", " +
              "\"url\": \"" + url + "\", " +
              "\"placeholders\": [ \"" + salutation + "\", \"" + salutation1 + "\"] " +
              "} " +
              "}";

      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);
      return response.getBody();
  }

  public String createURL (String url, Object ... params) {
    return new MessageFormat(url).format(params);
  }
  }
