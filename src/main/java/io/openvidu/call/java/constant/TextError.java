package io.openvidu.call.java.constant;

public enum TextError {
  MISSING_MSISDN("MSISDN not found", 2058),MISSING_CALLURL("Call Url not found", 2059), THROTTELING_ERROR("Participant limit Exceeded", 2096),SESSION_ERROR("SessionId not found", 2097);

  private String text;
  private int code;

  TextError(String text, int code) {
    this.text = text;
    this.code = code;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getCode() {
    return String.valueOf(code);
  }

  public void setCode(int code) {
    this.code = code;
  }

}
