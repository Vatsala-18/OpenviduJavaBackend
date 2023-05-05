package io.openvidu.call.java.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openvidu.call.java.models.ShortenRequest;
import io.openvidu.call.java.services.URLConverterService;
import io.openvidu.call.java.shortUrl.URLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URISyntaxException;


@RestController
public class URLController {

  private static final Logger LOGGER = LoggerFactory.getLogger(URLController.class);
  private final URLConverterService urlConverterService;
  @Value("${OPENVIDU_URL}")
  public String OPENVIDU_URL;
  @Value("${tiny.url:-}")
  private String baseString;

  public URLController(URLConverterService urlConverterService) {
    this.urlConverterService = urlConverterService;
  }

//  @RequestMapping(value = "/shortener", method=RequestMethod.POST, consumes = {"application/json"})
//  public String shortenUrl(@RequestBody @Valid final ShortenRequest shortenRequest, HttpServletRequest request1) throws Exception {
//    LOGGER.info("Received url to shorten: " + shortenRequest.getUrl());
//    String longUrl = shortenRequest.getUrl();
////    if (URLValidator.INSTANCE.validateURL(longUrl)) {
//      String localURL = request1.getRequestURL().toString();
//      String shortenedUrl = urlConverterService.shortenURL(localURL, shortenRequest.getUrl());
//      LOGGER.info("Shortened url to: " + shortenedUrl);
//      return shortenedUrl;
////    }
////    throw new Exception("Please enter a valid URL");
//  }

  @RequestMapping(value = "/url/{id}", method=RequestMethod.GET)
  public RedirectView redirectUrl(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException, Exception {
    LOGGER.info("Received shortened url to redirect: " + id);
    String redirectUrlString = urlConverterService.getLongURLFromID(id);
    LOGGER.info("Original URL: " + redirectUrlString);
    RedirectView redirectView = new RedirectView();
    redirectView.setUrl( redirectUrlString);
    return redirectView;
  }
  public String shortenUrl(ShortenRequest shortenRequest,HttpServletRequest request) throws Exception {
    LOGGER.info("Received url to shorten: " + shortenRequest.getUrl());
    String longUrl = shortenRequest.getUrl();
//    if (URLValidator.INSTANCE.validateURL(longUrl)) {
    String localURL = request.getRequestURL().toString();
    LOGGER.info("Request Url {}",localURL);
    String shortenedUrl = urlConverterService.shortenURL(localURL, shortenRequest.getUrl());
    LOGGER.info("Shortened url to: " + shortenedUrl);
    return shortenedUrl;
//    }
//    throw new Exception("Please enter a valid URL");
  }
}

