/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.gudy.azureus2.core3.disk;



/**
 * @author Aaron Grunthal
 * @create 01.05.2008
 */
public interface DiskManagerFileInfoSet {
	public boolean[] setStorageTypes(boolean[] toChange, int newStroageType);
	public void setPriority(int[] toChange);
	public void setSkipped(boolean[] toChange, boolean setSkipped);
	public DiskManagerFileInfo[] getFiles();
	public int nbFiles();
}
