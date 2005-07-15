/*
 * Created on Jul 3, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.peermanager.download.session;

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.peermanager.connection.*;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.session.auth.*;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;



public class TorrentSessionManager {
  
  private static final TorrentSessionManager instance = new TorrentSessionManager();
  
  private final HashMap hashes = new HashMap();
  private final AEMonitor hashes_mon = new AEMonitor( "TorrentSessionManager" );
  

  public static TorrentSessionManager getSingleton(){  return instance;  }

  
  private TorrentSessionManager() {
    /*nothing*/
  }
  
  
  public void init() {
    //register for new peer connection creation notification, so that we can catch torrent session syn messages
    PeerConnectionFactory.getSingleton().registerCreationListener( new PeerConnectionFactory.CreationListener() {
      public void connectionCreated( final AZPeerConnection connection ) {
        connection.getNetworkConnection().getIncomingMessageQueue().registerQueueListener( new IncomingMessageQueue.MessageQueueListener() {
          public boolean messageReceived( Message message ) {
            if( message.getID().equals( AZMessage.ID_AZ_TORRENT_SESSION_SYN ) ) {
              AZTorrentSessionSyn syn = (AZTorrentSessionSyn)message;

              String type = syn.getSessionType();
              byte[] hash = syn.getInfoHash();
              
              TorrentSessionAuthenticator auth = AuthenticatorFactory.createAuthenticator( type, hash );
              
              if( auth == null ) {
                Debug.out( "unknown session type: " +type );
                AZTorrentSessionEnd end = new AZTorrentSessionEnd( type, hash, "unknown session type id" );
                connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
              }
              else {
                //check for valid session infohash
                TorrentDownload download = null;
                
                try{ hashes_mon.enter();
                  download = (TorrentDownload)hashes.get( new HashWrapper( hash ) );
                }
                finally{ hashes_mon.exit();  }
                
                if( download == null ) {
                  System.out.println( "unknown session infohash " +ByteFormatter.nicePrint( hash, true ) );
                  AZTorrentSessionEnd end = new AZTorrentSessionEnd( type, hash, "unknown session infohash" );
                  connection.getNetworkConnection().getOutgoingMessageQueue().addMessage( end, false );
                }
                else { //success
                  TorrentSession session = TorrentSessionFactory.getSingleton().createIncomingSession( auth, connection, download, syn.getSessionID() );
                  session.authenticate( syn.getSessionInfo() );  //init processing
                }
              }
              
              syn.destroy();
              return true;
            }
            
            return false;
          }

          public void protocolBytesReceived( int byte_count ){}
          public void dataBytesReceived( int byte_count ){}
        });
      }
    });
  }
  
    
  
  /**
   * Register the given download for torrent session management.
   * @param download to add
   */
  public void registerForSessionManagement( TorrentDownload download ) {
    try{ hashes_mon.enter();
      hashes.put( new HashWrapper( download.getInfoHash() ), download );
    }
    finally{ hashes_mon.exit();  }
  }
  
  
  /**
   * Deregister the given download from torrent session management.
   * @param download to remove
   */
  public void deregisterForSessionManagement( TorrentDownload download ) {
    try{ hashes_mon.enter();
      hashes.remove( new HashWrapper( download.getInfoHash() ) );
    }
    finally{ hashes_mon.exit();  }
  }
  
  
  /**
   * Initiate a standard torrent session for the given download with the given peer connection.
   * @param download for session
   * @param connection to send request to
   */
  public void requestStandardTorrentSession( TorrentDownload download, AZPeerConnection connection ) {
    TorrentSessionAuthenticator auth = AuthenticatorFactory.createAuthenticator( TorrentSessionAuthenticator.AUTH_TYPE_STANDARD, download.getInfoHash() );
    TorrentSession session = TorrentSessionFactory.getSingleton().createOutgoingSession( auth, connection, download );
    session.authenticate( null );  //init processing
  }
  
  
  /**
   * Initiate a secure torrent session for the given download with the given peer connection.
   * @param download for session
   * @param connection to send request to
   */
  public void requestSecureTorrentSession( TorrentDownload download, AZPeerConnection connection ) {
    TorrentSessionAuthenticator auth = AuthenticatorFactory.createAuthenticator( TorrentSessionAuthenticator.AUTH_TYPE_SECURE, download.getInfoHash() );
    TorrentSession session = TorrentSessionFactory.getSingleton().createOutgoingSession( auth, connection, download );
    session.authenticate( null );  //init processing
  }
  
}
