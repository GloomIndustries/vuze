/*
 * Created on 22-Feb-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.ddb;

import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;

/**
 * @author parg
 *
 */

public class 
DDBaseContactImpl
	implements DistributedDatabaseContact
{
	private DDBaseImpl				ddb;
	private InetSocketAddress		address;
	
	protected
	DDBaseContactImpl(
		DDBaseImpl				_ddb,
		InetSocketAddress		_address )
	{
		ddb			= _ddb;
		address		= _address;
	}
	
	public String
	getName()
	{
		return( address.toString());
	}
	
	public void
	write(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			data )
	
		throws DistributedDatabaseException
	{
		throw( new DistributedDatabaseException( "not implemented" ));
	}
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key )
	
		throws DistributedDatabaseException
	{
		byte[]	data = ddb.getDHT().read( 
							address,
							DDBaseHelpers.getKey(type.getClass()).getHash(),
							((DDBaseKeyImpl)key).getBytes());
							
		if ( data == null ){
			
			return( null );
		}
		
		return( new DDBaseValueImpl( new DDBaseContactImpl( ddb, address ),data));
	}
}
