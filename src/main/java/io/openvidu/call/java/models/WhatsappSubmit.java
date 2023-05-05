package io.openvidu.call.java.models;

public class WhatsappSubmit {
  private String from;
  private String to;
  private String type;
  private message message;


  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public message getMessage() {
    return message;
  }

  public void setMessage(message message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "{" +
      "from='" + from + '\'' +
      ", to='" + to + '\'' +
      ", type='" + type + '\'' +
      ", message=" + message +
      '}';
  }
}
