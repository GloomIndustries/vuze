/*
 * Created on 17-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

import org.gudy.azureus2.core3.util.Debug;

public class 
TCPTransportHelperFilterCipherStream 
	implements TCPTransportHelperFilter
{
	private TCPTransportHelper		transport;
	private Cipher					read_cipher;
	private Cipher					write_cipher;
	
	private ByteBuffer	write_buffer_pending;
	private boolean		write_buffer_pending_byte_outstanding;
	private ByteBuffer	write_buffer_pending_byte;
	
	protected
	TCPTransportHelperFilterCipherStream(
		TCPTransportHelper		_transport,
		Cipher					_read_cipher,
		Cipher					_write_cipher )
	{
		transport	= _transport;
		
		read_cipher		= _read_cipher;
		write_cipher	= _write_cipher;
	}
	
	public long 
	write( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
			// deal with any outstanding cached crypted data first
		
		if  ( write_buffer_pending_byte_outstanding ){
			
			if ( transport.write( write_buffer_pending_byte ) == 0 ){
				
				return( 0 );
			}
			
			write_buffer_pending_byte_outstanding	= false;
		}
		
		long	total_written = 0;
		
		if ( write_buffer_pending != null ){
			
			int	max_writable = 0;
			
			for (int i=array_offset;i<length;i++){
				
				ByteBuffer	source_buffer = buffers[i];
				
				int	position 	= source_buffer.position();
				int	limit		= source_buffer.limit();
				
				int	size = limit - position;
				
				max_writable	+= size;
			}
			
			int	pending_position 	= write_buffer_pending.position();
			int pending_limit		= write_buffer_pending.limit();
			
			int	pending_size = pending_limit - pending_position;
			
			if ( pending_size > max_writable ){
								
				pending_size = max_writable;
				
				write_buffer_pending.limit( pending_position + pending_size );
			}
			
			int	written = transport.write( write_buffer_pending );
			
			write_buffer_pending.limit( pending_limit );
			
			total_written += written;
			
				// skip "written" bytes in the source
			
			int skip = written;
			
			for (int i=array_offset;i<length;i++){
				
				ByteBuffer	source_buffer = buffers[i];
				
				int	position 	= source_buffer.position();
				int	limit		= source_buffer.limit();
				
				int	size = limit - position;
				
				if ( size <= skip ){
					
					source_buffer.position( limit );
					
					skip	-= size;
					
				}else{
					
					source_buffer.position( position + skip );
					
					skip	= 0;
					
					break;
				}
			}
			
			if ( skip != 0 ){
				
				throw( new IOException( "skip inconsistent - " + skip ));
			}
			
			if ( written < pending_size || written == max_writable ){
				
				return( total_written );
			}
		}
		
			// problem - we must only crypt stuff once and when crypted it *has*
			// to be sent (else the stream will get out of sync).
			// so we have to turn this into single buffer operations
		
		for (int i=array_offset;i<length;i++){
			
			ByteBuffer	source_buffer = buffers[i];
			
			int	position 	= source_buffer.position();
			int	limit		= source_buffer.limit();
			
			int	size = limit - position;
			
			ByteBuffer	target_buffer = ByteBuffer.allocate( size );
		
			try{
				write_cipher.update( source_buffer, target_buffer );
				
			}catch( ShortBufferException e ){
				
				throw( new IOException( Debug.getNestedExceptionMessage( e )));
			}
			
			target_buffer.position( 0 );
			
			int	written = transport.write( target_buffer );
			
			total_written += written;
			
			if ( written < size ){
				
				source_buffer.position( position + written );
				
				write_buffer_pending	= target_buffer;
				
				if ( written == 0 ){
					
						// we gotta pretend at least 1 byte was written to
						// guarantee that the caller writes the rest
					
					write_buffer_pending_byte_outstanding = true;
					
					write_buffer_pending_byte = ByteBuffer.wrap(new byte[]{target_buffer.get()});
										
					source_buffer.get();
					
					total_written++;
				}
				
				break;
			}
		}
		
		return( total_written );
	}

	public long 
	read( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		throw( new IOException( "not imp" ));
	}
	
	public SocketChannel
	getSocketChannel()
	{
		return( transport.getSocketChannel());
	}
}
