package io.openvidu.call.java.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.JSONPropertyIgnore;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notifications {
    private String Response;
    private String sessionId;

    private Thread threadName;

    public Notifications() {}

    public Notifications(String Response) {
        this.Response = Response;
    }

    public String getResponse() {
        return Response;
    }

    public void setResponse(String response) {
        Response = response;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Thread getThreadName() {
        return threadName;
    }

    public void setThreadName(Thread threadName) {
        this.threadName = threadName;
    }

    @Override
    public String toString() {
        return "Notifications{" +
                "Response='" + Response + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", threadName=" + threadName +
                '}';
    }
}
