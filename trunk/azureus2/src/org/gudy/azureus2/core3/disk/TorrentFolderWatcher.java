/*
 * Created on Dec 14, 2003
 * Modified Apr 13, 2004 by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */
 
package org.gudy.azureus2.core3.disk;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * This class can watch folders for new torrents and imports them.
 * 
 * @author Rene Leonhardt
 */
public class TorrentFolderWatcher {
  private static TorrentFolderWatcher instance = new TorrentFolderWatcher();
  private static FolderWatcher folderWatcher = instance.new FolderWatcher();
  private static MainWindow mainWindow = MainWindow.getWindow();

  public static FolderWatcher getFolderWatcher() {
    if(null == folderWatcher)
      folderWatcher = instance.new FolderWatcher();
    return folderWatcher;
  }
  
  public class FolderWatcher extends Thread implements ParameterListener {
    private boolean finished = true;
    private int waitTime = 60000;
    private boolean startWatchedTorrentsStopped = true;
    private File watchFolder = null;
    private String watchFolderString = null;
    private FilenameFilter filterTorrents;
    private final ArrayList delList = new ArrayList();
    
    public FolderWatcher() {
      super("FolderWatcher"); //$NON-NLS-1$
      setPriority(Thread.MIN_PRIORITY);
      parameterChanged("");
      filterTorrents = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith(".torrent");
        }
      };
    }

    public boolean initialize(String folderToWatchPath) {
      if(folderToWatchPath != null) {
        File folderToWatch = new File(folderToWatchPath);
        if (folderToWatch.isDirectory() && !folderToWatch.equals(watchFolder)) {
          watchFolder = folderToWatch;
          watchFolderString = watchFolder.getAbsolutePath() + File.separator;
          return true;
        }
      }
      return false;
    }

    public void run() {
      while (!finished) {
        importAddedFiles();
        try {
          Thread.sleep(waitTime);
        } catch (Exception ignore) {}
      }
    }

    /**
		 * @param parameterName
		 *          the name of the parameter that has changed
		 * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
		 */
    public void parameterChanged(String parameterName) {
      waitTime = COConfigurationManager.getIntParameter("Watch Torrent Folder Interval", 1) * 60000;
      startWatchedTorrentsStopped = COConfigurationManager.getBooleanParameter("Start Watched Torrents Stopped", false);
    }

    private synchronized void importAddedFiles() {
      if (watchFolder == null)
        return;
      
      //delete torrents from the previous import run
      for (int i=0; i < delList.size(); i++) {
        ((File)delList.get( i )).delete();
      }
      delList.clear();
      
      String[] currentFileList = watchFolder.list(filterTorrents);

      for (int i = 0; i < currentFileList.length; i++) {
        File file = new File( watchFolderString, currentFileList[i] );
        
        boolean saved = COConfigurationManager.getBooleanParameter("Save Torrent Files");
        String path = COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory");
        
        if ( !saved || path.length() < 1) {
        	File imported = new File( watchFolderString, file.getName() + ".imported" );
        	file.renameTo( imported );
        	mainWindow.openTorrent(watchFolderString + imported.getName(), startWatchedTorrentsStopped, false);
        }
        else {
          mainWindow.openTorrent(watchFolderString + file.getName(), startWatchedTorrentsStopped, false);
          delList.add( file );  //add file for deletion
        }
        LGLogger.log(LGLogger.INFORMATION, "Imported " + watchFolderString + "/" + currentFileList[i]);
      }
    }

    public void stopIt() {
      watchFolder = null;
      finished = true;
      COConfigurationManager.removeParameterListener("Watch Torrent Folder Interval", this);
      COConfigurationManager.removeParameterListener("Start Watched Torrents Stopped", this);
      folderWatcher = null;
    }

    public void startIt() {
      if (COConfigurationManager.getBooleanParameter("Watch Torrent Folder", false)) { //$NON-NLS-1$
        if(folderWatcher.initialize(COConfigurationManager.getStringParameter("Watch Torrent Folder Path", "")) && finished) {
          finished = false;
          COConfigurationManager.addParameterListener("Watch Torrent Folder Interval", this);
          COConfigurationManager.addParameterListener("Start Watched Torrents Stopped", this);
          start();
        }
      }
    }

  }
}
