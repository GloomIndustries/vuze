/*
 * File    : PRUDPPacketReplyAnnounce.java
 * Created : 20-Jan-2004
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

package org.gudy.azureus2.core3.tracker.protocol.udp;

/**
 * @author parg
 *
 */

import java.io.*;

public class 
PRUDPPacketReplyAnnounce
extends PRUDPPacketReply
{
	public
	PRUDPPacketReplyAnnounce(
		int			trans_id )
	{
		super( ACT_REPLY_ANNOUNCE, trans_id );
	}
	
	protected
	PRUDPPacketReplyAnnounce(
		DataInputStream		is,
		int					trans_id )
	
		throws IOException
	{
		super( ACT_REPLY_ANNOUNCE, trans_id );
	}
	
	
	public void
	serialise(
		DataOutputStream	os )
	
	throws IOException
	{
		super.serialise(os);
	}
}

