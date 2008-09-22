/*
 * Created : 11 nov. 2004
 * By      : Alon Rohter
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;



public class SwarmAverageSpeed
       extends CoreTableColumn 
       implements TableCellRefreshListener
{

  public static final String COLUMN_ID = "swarm_average_speed";

	public SwarmAverageSpeed(String sTableID) {
    super(COLUMN_ID, ALIGN_TRAIL, POSITION_INVISIBLE, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
  }

  public void refresh(TableCell cell) {
    long speed = -1;

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    if( dm != null ) {
    	speed = dm.getStats().getTotalAveragePerPeer();
    }

    if( !cell.setSortValue( speed ) && cell.isValid() ) {
      return;
    }
    
    if( speed < 0 ) {
      cell.setText( "" );
    }
    else {
      cell.setText( DisplayFormatters.formatByteCountToKiBEtcPerSec( speed ) );
    }
  }

}
