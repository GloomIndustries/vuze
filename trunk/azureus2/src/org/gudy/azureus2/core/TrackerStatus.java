/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author Olivier
 * 
 */
public class TrackerStatus {
  private String scrapeURL = null;
  byte[] data;

  private HashMap hashes;
  private List hashList;

  public TrackerStatus(String trackerUrl) {    
    this.hashes = new HashMap();
    this.hashList = new Vector();
    try {
      trackerUrl = trackerUrl.replaceAll(" ", "");
      int position = trackerUrl.lastIndexOf('/');
      if(position >= 0 && trackerUrl.substring(position+1,position+9).equals("announce"))
        this.scrapeURL = trackerUrl.substring(0,position+1) + "scrape" + trackerUrl.substring(position+9);
    } catch (Exception e) {
      e.printStackTrace();
    } 
    data = new byte[1024];
  }

  public HashData getHashData(Hash hash) {
    return (HashData) hashes.get(hash);
  }

  public synchronized void asyncUpdate(final Hash hash) {
    hashes.put(hash,new HashData(0,0));
    Thread t = new Thread("Tracker Checker - Scrape interface") {
      /* (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      public void run() {
        update(hash);
      }
    };
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  public synchronized void update(Hash hash) {    
    hashes.put(hash,new HashData(0,0));
    if(! hashList.contains(hash))
        hashList.add(hash);
    if(scrapeURL == null)
      return;
    InputStream is = null;
    try {
      String info_hash = "?info_hash=";
      info_hash += URLEncoder.encode(new String(hash.getHash(), Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
      URL scrape = new URL(scrapeURL + info_hash);
      Logger.getLogger().log(0,0,Logger.INFORMATION,"Accessing scrape interface using url : " + scrape);
      HttpURLConnection con = (HttpURLConnection) scrape.openConnection();
      con.connect();
      is = con.getInputStream();
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      int nbRead = 0;
      while (nbRead >= 0) {
        try {
          nbRead = is.read(data);
          if (nbRead >= 0)
		  	message.write(data, 0, nbRead);
          Thread.sleep(20);
        } catch (Exception e) {
          // nbRead = -1;
          // message = null;
          // e.printStackTrace();
          return;
        }
      }
      //Logger.getLogger().log(0,0,Logger.INFORMATION,"Response from scrape interface : " + message);
      Map map = BDecoder.decode(message.toByteArray());
      map = (Map) map.get("files");
      Iterator iter = map.keySet().iterator();
      while(iter.hasNext()) {
        String strKey = (String)iter.next();
        byte[] key = (strKey).getBytes(Constants.BYTE_ENCODING);
        Map hashMap = (Map)map.get(strKey);
        //System.out.println(ByteFormater.nicePrint(key) + " :: " + hashMap);
        int seeds = ((Long)hashMap.get("complete")).intValue();
        int peers = ((Long)hashMap.get("incomplete")).intValue();
        hashes.put(new Hash(key),new HashData(seeds,peers));        
      }
    } catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
    } catch (Exception ignore) {
    } finally {
      if(is != null)
        try {
          is.close();
        } catch (IOException e1) {
        }
    }
  }
  
  public Iterator getHashesIterator() {
    return hashList.iterator();  
  }
  
  public void removeHash(Hash hash) {
    while(hashList.contains(hash))
      hashList.remove(hash);
  }  

}
