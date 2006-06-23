/*
 * Created on 22 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.TransportEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.TransportCryptoManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TransportImpl;

public class 
UDPTransport
	extends TransportImpl
{
	private static final LogIDs LOGID = LogIDs.NET;
	
	private ProtocolEndpointUDP		endpoint;
	private byte[]					shared_secret;
	
	private int transport_mode = TRANSPORT_MODE_NORMAL;
	
	private volatile boolean	closed;
	
	protected
	UDPTransport(
		ProtocolEndpointUDP		_endpoint,
		byte[]					_shared_secret )
	{
		endpoint		= _endpoint;
		shared_secret	= _shared_secret;
	}

	protected
	UDPTransport(
		ProtocolEndpointUDP		_endpoint,
		TransportHelperFilter	_filter )
	{
		endpoint		= _endpoint;
	
		setFilter( _filter );
	}
	
	public TransportEndpoint 
	getTransportEndpoint()
	{
		return( new TransportEndpointUDP( endpoint ));
	}
	  
	public int
	getMssSize()
	{
	  return( UDPNetworkManager.getUdpMssSize());
	}
	 
	public String 
	getDescription()
	{
		return( endpoint.getAddress().toString());
	}
	
	public void 
	setTransportMode( 
		int mode )
	{
		transport_mode	= mode;
	}
	 
	public int 
	getTransportMode()
	{
		return( transport_mode );
	}
	
	public void
	connectOutbound(
		final ConnectListener 	listener )
	{
		if ( closed ){
			
			listener.connectFailure( new Throwable( "Connection already closed" ));
			
			return;
		}
		    
		if( getFilter() != null ){
		     
			listener.connectFailure( new Throwable( "Already connected" ));
			
			return;
		}
		
		try{
			listener.connectAttemptStarted();

			final UDPTransportHelper	helper = 
	 			new UDPTransportHelper( UDPNetworkManager.getSingleton().getConnectionManager(), endpoint.getAddress());
	 		
	    	TransportCryptoManager.getSingleton().manageCrypto( 
	    			helper, 
	    			shared_secret, 
	    			false, 
	    			new TransportCryptoManager.HandshakeListener() 
	    			{
	    				public void 
	    				handshakeSuccess( 
	    					ProtocolDecoder	decoder )
	    				{
	    					TransportHelperFilter	filter = decoder.getFilter();
	    					
	    					try{
		    					setFilter( filter );
		    					
		    					if ( closed ){
		    						
		    						close();
		    						
		    						listener.connectFailure( new Exception( "Connection already closed" ));
		    						
		    					}else{
		    						
			    		   			if ( Logger.isEnabled()){
			    		    		
			    		   				Logger.log(new LogEvent(LOGID, "Outgoing UDP stream to " + endpoint.getAddress() + " established, type = " + filter.getName()));
			    		    		}
			    		   			
			    		   			connectedOutbound();
			    		   			
			    		   			listener.connectSuccess( UDPTransport.this );
		    					}
	    					}catch( Throwable e ){
	    						
	    						Debug.printStackTrace(e);
	    						
	    						close();
	    						
	    						listener.connectFailure( e );
	    					}
	    				}
	
	    				public void 
	    				handshakeFailure( 
	    					Throwable failure_msg )
	    				{
	    					helper.close();
	    					
	    					listener.connectFailure( failure_msg );
	    				}
	    				
	    				public void
	    				gotSecret(
							byte[]				session_secret )
	    				{
	    		   			helper.getConnection().setSecret( session_secret );
	    				}
	    				
	    				public int 
	    				getMaximumPlainHeaderLength()
	    				{
	    		   			throw( new RuntimeException());	// this is outgoing
	    				}
	    				
	    				public int 
	    				matchPlainHeader( 
	    					ByteBuffer buffer )
	    				{
	    					throw( new RuntimeException());	// this is outgoing
	    				}
	    			});
	    	
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			listener.connectFailure( e );
		}
	}
	   
	public void 
	close()
	{
		closed	= true;
		
		readyForRead( false );
		readyForWrite( false );

		TransportHelperFilter	filter = getFilter();
		
		if ( filter != null ){
			
			filter.getHelper().close();
			
			setFilter( null );
		}
	}
}
