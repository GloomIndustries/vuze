/*
 * File    : TRHostImpl.java
 * Created : 24-Oct-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements TRHost 
{
	public static final int RETRY_DELAY = 60*1000;
	
	protected static TRHostImpl		singleton;
	
	protected Hashtable	server_map 	= new Hashtable();
	
	protected List	torrents	= new ArrayList();
	
	public static synchronized TRHost
	create()
	{
		if ( singleton == null ){
			
			singleton = new TRHostImpl();
		}
		
		return( singleton );
	}
	
	public synchronized void
	addTorrent(
		TOTorrent		torrent )
	{
		int	port = torrent.getAnnounceURL().getPort();
		
		TRTrackerServer	server = (TRTrackerServer)server_map.get( new Integer( port ));
		
		if ( server == null ){
			
			try{
			
				server = TRTrackerServerFactory.create( port, RETRY_DELAY );
			
				server_map.put( new Integer( port ), server );
				
			}catch( TRTrackerServerException e ){
				
				e.printStackTrace();
			}
		}
		
		torrents.add( new TRHostTorrentImpl( this, server, torrent ));
	}
	
	public TRHostTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	res = new TRHostTorrent[torrents.size()];
		
		torrents.toArray( res );
		
		return( res );
	}
}
