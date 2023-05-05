package io.openvidu.call.java.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResponse {
  //SMS Submit Response
  private String transactionId;
  private String state;
  private String description;
  private String statusCode;
  //WhatsAppSubmit Response
  private String code;
  private String status;
  private String message;
  private Object data;
  private String callUrl;
  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public String getCallUrl() {
    return callUrl;
  }

  public void setCallUrl(String callUrl) {
    this.callUrl = callUrl;
  }

  @Override
  public String toString() {
    return "SubmitResponse{" +
      "transactionId='" + transactionId + '\'' +
      ", state='" + state + '\'' +
      ", description='" + description + '\'' +
      ", statusCode='" + statusCode + '\'' +
      ", code='" + code + '\'' +
      ", status='" + status + '\'' +
      ", message='" + message + '\'' +
      ", data='" + data + '\'' +
      ", callUrl='" + callUrl + '\'' +
      '}';
  }
}
