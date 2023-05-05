package io.openvidu.call.java.services;

import io.openvidu.call.java.shortUrl.IDConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class URLConverterService {
  @Value("${base.url:-}")
  private String baseString;
  private static final Logger LOGGER = LoggerFactory.getLogger(URLConverterService.class);
  HashMap<Long,String> urlMap=new HashMap();


  public String shortenURL(String localURL, String longUrl) {
    LOGGER.info("Shortening {}", longUrl);
    Long id = getUniqueTimestamp();
    String uniqueID = IDConverter.INSTANCE.createUniqueID(Long.valueOf(id));
    urlMap.put(id, longUrl);
    LOGGER.info("Base String {}",baseString);
    String shortenedURL = baseString + uniqueID;
    return shortenedURL;
  }

  public String getLongURLFromID(String uniqueID) throws Exception {
    Long dictionaryKey = IDConverter.INSTANCE.getDictionaryKeyFromUniqueID(uniqueID);
    String longUrl = urlMap.get(dictionaryKey);
    LOGGER.info("Converting shortened URL back to {}", longUrl);
    LOGGER.info("URL Map {}",urlMap);
    urlMap.remove(dictionaryKey);
    return longUrl;
  }

  public String formatLocalURLFromShortener(String localURL) {
    String[] addressComponents = localURL.split("/");
    // remove the endpoint (last index)
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < addressComponents.length - 1; ++i) {
      sb.append(addressComponents[i]);
    }
    sb.append('/');
    return sb.toString();
  }
  private static final AtomicLong TS = new AtomicLong(System.currentTimeMillis() / 100);
  public static long getUniqueTimestamp() {
    return TS.incrementAndGet();
  }
}
