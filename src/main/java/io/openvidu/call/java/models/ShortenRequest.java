package io.openvidu.call.java.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

@Component
public class ShortenRequest {
  private String url;

  @JsonCreator
  public ShortenRequest() {

  }

  @JsonCreator
  public ShortenRequest(@JsonProperty("url") String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
