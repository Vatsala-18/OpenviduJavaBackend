package io.openvidu.call.java.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class message {
  @JsonProperty("templateid")
  private String templateId;
  @JsonProperty("placeholders")
  private ArrayList<String> placeHolders;

  public message(String templateid, ArrayList<String> placeholders) {
    this.templateId = templateid;
    this.placeHolders = placeholders;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public ArrayList<String> getPlaceHolders() {
    return placeHolders;
  }

  public void setPlaceHolders(ArrayList<String> placeHolders) {
    this.placeHolders = placeHolders;
  }
}
