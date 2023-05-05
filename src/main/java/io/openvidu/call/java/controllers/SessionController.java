package io.openvidu.call.java.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openvidu.java.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import io.openvidu.call.java.models.RecordingData;
import io.openvidu.call.java.services.OpenViduService;
import org.springframework.web.client.RestTemplate;

@CrossOrigin(origins = "*")
@RestController
public class SessionController {

  @Value("${CALL_RECORDING}")
  private String CALL_RECORDING;

  @Value("${CALL_STREAMING}")
  private String CALL_STREAMING;
  @Value("${WAIT_TIME:500000}")
  private long WaitTime;
  @Value("${PRERECORDED_PATH:/opt/openvidu/kurento-logs/}")
  private String preRecordedPath;

  @Autowired
  private OpenViduService openviduService;
  @Autowired
  RestTemplate restTemplate;
  private static SimpMessageSendingOperations messagingTemplate;

  @Autowired
  public void SessionController(SimpMessageSendingOperations messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  HashMap<String, Object> ThreadMap = new HashMap<>();
  ArrayList<HashMap<String, String>> RequestSession = new ArrayList<>();

  public boolean isValid;

  private final int cookieAdminMaxAge = 24 * 60 * 60;

  DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

  private static final Logger log = LoggerFactory.getLogger(SessionController.class);
  Map<String, String> IP_CAMERAS = new HashMap<String, String>();


  @PostMapping("/sessions")
  public ResponseEntity<Map<String, Object>> createConnection(
          @RequestBody(required = false) Map<String, Object> params,
          @CookieValue(name = OpenViduService.RECORDING_TOKEN_NAME, defaultValue = "") String recordingTokenCookie,
          HttpServletResponse res) {

    Map<String, Object> response = new HashMap<String, Object>();
    try {
      long date = -1;
      String nickname = "";

      String sessionId = params.get("sessionId").toString();
      if (params.containsKey("nickname")) {
        nickname = params.get("nickname").toString();
      }

      Session sessionCreated = this.openviduService.createSession(sessionId);

      boolean IS_RECORDING_ENABLED = CALL_RECORDING.toUpperCase().equals("ENABLED");

      boolean hasValidToken = this.openviduService.isValidToken(sessionId, recordingTokenCookie);
      boolean isSessionCreator = hasValidToken || sessionCreated.getActiveConnections().size() == 0;

      OpenViduRole role = isSessionCreator && IS_RECORDING_ENABLED ? OpenViduRole.MODERATOR
              : OpenViduRole.PUBLISHER;

      response.put("recordingEnabled", IS_RECORDING_ENABLED);
      response.put("recordings", new ArrayList<Recording>());

      Connection cameraConnection = this.openviduService.createConnection(sessionCreated, nickname, role);
      Connection screenConnection = this.openviduService.createConnection(sessionCreated, nickname, role);

      response.put("cameraToken", cameraConnection.getToken());
      response.put("screenToken", screenConnection.getToken());

      if (IS_RECORDING_ENABLED && isSessionCreator && !hasValidToken) {
        /**
         * ! *********** WARN *********** !
         *
         * To identify who is able to manage session recording, the code sends a cookie
         * with a token to the session creator. The relation between cookies and
         * sessions are stored in backend memory.
         *
         * This authentication & authorization system is pretty basic and it is not for
         * production. We highly recommend IMPLEMENT YOUR OWN USER MANAGEMENT with
         * persistence for a properly and secure recording feature.
         *
         * ! *********** WARN *********** !
         **/

        String uuid = UUID.randomUUID().toString();
        date = System.currentTimeMillis();
        String recordingToken = cameraConnection.getToken() + "&" + OpenViduService.RECORDING_TOKEN_NAME + "="
                + uuid + "&createdAt=" + date;

        Cookie cookie = new Cookie(OpenViduService.RECORDING_TOKEN_NAME, recordingToken);
        res.addCookie(cookie);

        RecordingData recData = new RecordingData(recordingToken, "");
        this.openviduService.recordingMap.put(sessionId, recData);
      }

      if (IS_RECORDING_ENABLED) {
        if (date == -1) {
          date = openviduService.getDateFromCookie(recordingTokenCookie);
        }
        List<Recording> recordings = openviduService.listRecordingsBySessionIdAndDate(sessionId, date);
        response.put("recordings", recordings);
      }

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (OpenViduJavaClientException | OpenViduHttpException e) {

      if (e.getMessage() != null && Integer.parseInt(e.getMessage()) == 501) {
        System.err.println("OpenVidu Server recording module is disabled");
        response.put("recordingEnabled", false);
        return new ResponseEntity<>(response, HttpStatus.OK);
      } else if (e.getMessage() != null && Integer.parseInt(e.getMessage()) == 401) {
        System.err.println("OpenVidu credentials are wrong.");
        return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);

      } else {
        e.printStackTrace();
        System.err.println(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  @GetMapping("/sessionList")
  public ResponseEntity<?> sessionList(
    HttpServletRequest req,
    HttpServletResponse res) {
    try {
      final HashMap<String, String> sessionList = new HashMap();

      if (RequestSession.size() != 0 && RequestSession != null) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("SessionList", RequestSession);
        return new ResponseEntity<>(response, HttpStatus.OK);
      } else {
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>("Unexpected error stopping streaming", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @DeleteMapping("/sessionList")
  public ResponseEntity<?> deleteSessionList(@RequestParam String sessionId,
                                             HttpServletRequest req,
                                             HttpServletResponse res) {
    try {
      for (HashMap<String, String> session : RequestSession) {
        if (RequestSession.size() != 0 && RequestSession != null && session.containsValue(sessionId)) {
          System.out.println("Session"+session);
          RequestSession.remove(session);
          System.out.println("Request Session"+RequestSession);
          Map<String, Object> response = new HashMap<String, Object>();
          Object thread = ThreadMap.get(sessionId);
          System.out.println(ThreadMap+"thread"+thread);
          if (thread != null) {
            synchronized (thread) {
              thread.notify();
              System.out.println("ThreadMap"+ThreadMap);
              isValid=true;
              ThreadMap.remove(sessionId);
            }
          }
          messagingTemplate.convertAndSend("/topic/notification", "request");
          break;
        }
      }
    }catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>("Unexpected error stopping streaming", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/IPSession")
  public ResponseEntity<Map<String, Object>> IPSession(
    @RequestBody(required = false) Map<String, Object> params,
    @CookieValue(name = OpenViduService.MODERATOR_TOKEN_NAME, defaultValue = "") String moderatorToken,
    HttpServletResponse res) throws Exception {
    Map<String, Object> response = new HashMap<String, Object>();
    try {
      long date = -1;
      String nickname = "";
      String preRecorded = "";
      String file = "";
      String URL = "";

      String sessionId = params.get("sessionId").toString();
      if (params.containsKey("nickname")) {
        nickname = params.get("nickname").toString();
      }
      if (params.containsKey("preRecorder")) {
        preRecorded = params.get("preRecorder").toString();
      }
      if (params.containsKey("file")) {
        file = params.get("file").toString();
      }
      Session sessionCreated = this.openviduService.createSession(sessionId);
      System.out.println("Main"+sessionCreated.getActiveConnections().size());
      List<Connection> connections=sessionCreated.getActiveConnections();
      for(Connection connection : connections){
        System.out.println(connection.getConnectionId());
        if(connections.size()<=1 && connection.getConnectionId().contains("IPCAM")) {
          sessionCreated.forceDisconnect(connection);
          sessionCreated = this.openviduService.createSession(sessionId);
        }
      }
      boolean IS_RECORDING_ENABLED = CALL_RECORDING.toUpperCase().equals("ENABLED");
      boolean IS_STREAMING_ENABLED = CALL_STREAMING.toUpperCase().equals("ENABLED");
      boolean PRIVATE_FEATURES_ENABLED = IS_RECORDING_ENABLED || IS_STREAMING_ENABLED;

      boolean hasValidToken = this.openviduService.isValidToken(sessionId, moderatorToken);
      boolean isSessionCreator = hasValidToken || sessionCreated.getActiveConnections().size() == 0;

      OpenViduRole role = isSessionCreator ? OpenViduRole.MODERATOR : OpenViduRole.PUBLISHER;
      Connection cameraConnection = this.openviduService.createConnection(sessionCreated, nickname, role);
      Connection screenConnection = this.openviduService.createConnection(sessionCreated, nickname, role);
      response.put("cameraToken", cameraConnection.getToken());
      response.put("screenToken", screenConnection.getToken());
      response.put("recordingEnabled", IS_RECORDING_ENABLED);

      if (isSessionCreator && !hasValidToken && PRIVATE_FEATURES_ENABLED) {
        /**
         * ! *********** WARN *********** !
         *
         * To identify who is able to manage session recording and streaming, the code sends a cookie
         * with a token to the session creator. The relation between cookies and
         * sessions are stored in backend memory.
         *
         * This authentication & authorization system is pretty basic and it is not for
         * production. We highly recommend IMPLEMENT YOUR OWN USER MANAGEMENT with
         * persistence for a properly and secure recording feature.
         *
         * ! *********** WARN *********** !
         **/

        String uuid = UUID.randomUUID().toString();
        date = System.currentTimeMillis();
        String recordingToken = cameraConnection.getToken() + "&" + OpenViduService.MODERATOR_TOKEN_NAME + "="
          + uuid + "&createdAt=" + date;

        Cookie cookie = new Cookie(OpenViduService.MODERATOR_TOKEN_NAME, recordingToken);
        cookie.setMaxAge(cookieAdminMaxAge);
        res.addCookie(cookie);

        RecordingData recData = new RecordingData(recordingToken, "");
        this.openviduService.recordingMap.put(sessionId, recData);
      }

      if (IS_RECORDING_ENABLED) {
        if (date == -1) {
          date = openviduService.getDateFromCookie(moderatorToken);
        }
        List<Recording> recordings = openviduService.listRecordingsBySessionIdAndDate(sessionId, date);
        response.put("recordings", recordings);
      }
      // See if we have already published any of our cameras
      // We fetch our only session current status and search for connections with
      // platform "IPCAM". Finally we get their server data field with the camera name


      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (OpenViduJavaClientException | OpenViduHttpException e) {

      if (e.getMessage() != null && Integer.parseInt(e.getMessage()) == 501) {
        System.err.println("OpenVidu Server recording module is disabled");
        return new ResponseEntity<>(response, HttpStatus.OK);
      } else if (e.getMessage() != null && Integer.parseInt(e.getMessage()) == 401) {
        System.err.println("OpenVidu credentials are wrong.");
        return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);

      } else {
        e.printStackTrace();
        System.err.println(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }
  @DeleteMapping("/IPSession")
  public ResponseEntity<?> deleteSession(@RequestParam String sessionId, HttpServletRequest req,HttpServletResponse res) {
    try {
      Session session= this.openviduService.getSession(sessionId);
      List<Connection> connections=session.getActiveConnections();
      for(Connection connection : connections){
        System.out.println(connection.getConnectionId());
        if(connections.size()<=1 && connection.getConnectionId().contains("IPCAM")) {
          session.forceDisconnect(connection);
        }
        }

      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("No Active Session ", HttpStatus.NOT_FOUND);
    }
  }
  @PostMapping("/IPConnection")
  public ResponseEntity<Map<String, Object>> IPConnection(
    @RequestBody(required = false) Map<String, Object> params,
    @CookieValue(name = OpenViduService.MODERATOR_TOKEN_NAME, defaultValue = "") String moderatorToken,
    HttpServletResponse res) throws Exception {
    String nickname = "";
    String preRecorded = "";
    String file = "";
    String URL = "";
    File fileExist =new File(preRecordedPath);

    String sessionId = params.get("sessionId").toString();
    if (params.containsKey("nickname")) {
      file = params.get("nickname").toString();
    }
    if (params.containsKey("preRecorder")) {
      preRecorded = params.get("preRecorder").toString();
    }
    if (params.containsKey("file")) {
      file = params.get("file").toString();
    }
    System.out.println(file+fileExist.exists());
    if(file!=null) {
      URL = "file://" + preRecordedPath + file;
      System.out.println(URL);
      IP_CAMERAS.put(sessionId, URL);
    }
    Session sessionCreated=this.openviduService.getSession(sessionId);
    boolean isSessionCreator = sessionCreated.getActiveConnections().size() == 0;
    System.out.println("connection"+sessionCreated.getActiveConnections().size());

    if (URL != null && !isSessionCreator) {
      System.out.println("Session Created");
      sessionCreated.fetch();
      List<String> alreadyPublishedCameras = sessionCreated.getActiveConnections().stream()
        .filter(connection -> "IPCAM".equals(connection.getPlatform()))
        .map(connection -> connection.getServerData()).collect(Collectors.toList());

      for (Map.Entry<String, String> cameraMapEntry : IP_CAMERAS.entrySet()) {
        try {
          String cameraUri = cameraMapEntry.getValue();
          String cameraName = cameraMapEntry.getKey();
          // Publish the camera only if it is not already published
          if (!alreadyPublishedCameras.contains(cameraName)) {
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
              .type(ConnectionType.IPCAM)
              .data(cameraName)
              .rtspUri(cameraUri)
              .adaptativeBitrate(true)
              .onlyPlayWithSubscribers(true)
              .build();
            sessionCreated.createConnection(connectionProperties);
          }
        } catch (Exception e) {
          log.error("Error publishing camera {}", cameraMapEntry.getKey());
        }
      }
      IP_CAMERAS.remove(sessionId);
      System.out.println(" Total connection"+sessionCreated.getActiveConnections().size());
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
