/**
 * 
 */
package com.aelitis.azureus.ui.swt.views;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SizeItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.UpItem;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.swt.columns.torrent.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.views.list.ListRow;
import com.aelitis.azureus.ui.swt.views.list.ListView;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 */
public class TorrentListView
extends ListView
	implements GlobalManagerListener
{
	public final static int VIEW_DOWNLOADING = 0;

	public final static int VIEW_RECENT_DOWNLOADED = 1;

	public final static int VIEW_MY_MEDIA = 2;

	public static final String TABLE_MYMEDIA = "MyMedia";

	private final static String[] TABLE_IDS = {
		"Downloading",
		"Recent",
		"Media",
	};

	private final dowloadManagerListener dmListener;

	protected final GlobalManager globalManager;

	private final int viewMode;

	private TableColumnCore[] tableColumns;

	private final SWTSkinObjectText countArea;

	private final ArrayList listeners = new ArrayList();

	private final AEMonitor listeners_mon = new AEMonitor("3.TLV.listeners");

	private final Composite dataArea;

	private boolean bAllowScrolling;

	protected boolean bSkipUpdateCount = false;

	public TorrentListView(AzureusCore core, final SWTSkin skin,
			SWTSkinProperties skinProperties, Composite headerArea,
			SWTSkinObjectText countArea, final Composite dataArea, int viewMode,
			final boolean bMiniMode, final boolean bAllowScrolling) {

		super(TABLE_IDS[viewMode] + ((bMiniMode) ? "-Mini" : ""), skinProperties,
				dataArea, headerArea, bAllowScrolling ? SWT.V_SCROLL : SWT.NONE);
		this.countArea = countArea;
		this.dataArea = dataArea;
		this.viewMode = viewMode;
		this.bAllowScrolling = bAllowScrolling;
		dmListener = new dowloadManagerListener(this);

		TableColumnManager tcManager = TableColumnManager.getInstance();
		if (viewMode == VIEW_DOWNLOADING) {
			if (bMiniMode) {
				tableColumns = new TableColumnCore[] {
					new ColumnAzProduct(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnMediaThumb(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnTitle(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new SizeItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnQuality(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnSpeed(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnProgressETA(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnDateAdded2Liner(TableManager.TABLE_MYTORRENTS_INCOMPLETE,
							false),
				};
				setColumnList(tableColumns, "date_added", true);
				String[] autoHideOrder = new String[] {
					ColumnSpeed.COLUMN_ID,
					ColumnQuality.COLUMN_ID,
					ColumnAzProduct.COLUMN_ID,
					SizeItem.COLUMN_ID,
					ColumnMediaThumb.COLUMN_ID,
				};
				tcManager.setAutoHideOrder(getTableID(), autoHideOrder);
			} else {
				tableColumns = new TableColumnCore[] {
					new ColumnAzProduct(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnMediaThumb(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnTitle(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnRate(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new SizeItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnQuality(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnSpeed(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnProgressETA(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new ColumnDateAdded2Liner(TableManager.TABLE_MYTORRENTS_INCOMPLETE,
							false),
				};
				setColumnList(tableColumns, "date_added", true);
				String[] autoHideOrder = new String[] {
					ColumnQuality.COLUMN_ID,
					ColumnAzProduct.COLUMN_ID,
					SizeItem.COLUMN_ID,
					ColumnMediaThumb.COLUMN_ID,
					ColumnDateAdded2Liner.COLUMN_ID,
				};
				tcManager.setAutoHideOrder(getTableID(), autoHideOrder);
			}
		} else if (viewMode == VIEW_RECENT_DOWNLOADED) {
			if (bMiniMode) {
				tableColumns = new TableColumnCore[] {
					new ColumnAzProduct(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnMediaThumb(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnTitle(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnQuality(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
					new SizeItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnRateUpDown(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnRate(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnDateAdded2Liner(TableManager.TABLE_MYTORRENTS_COMPLETE, false),
				};

				setColumnList(tableColumns, "date_added", true);
				String[] autoHideOrder = new String[] {
					ColumnQuality.COLUMN_ID,
					ColumnAzProduct.COLUMN_ID,
					SizeItem.COLUMN_ID,
					ColumnMediaThumb.COLUMN_ID,
					ColumnRateUpDown.COLUMN_ID,
				};
				tcManager.setAutoHideOrder(getTableID(), autoHideOrder);
			} else {
				tableColumns = new TableColumnCore[] {
					new ColumnIsSeeding(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnAzProduct(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnMediaThumb(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnTitle(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnRate(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new SizeItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnSpeed(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new UpItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnRateUpDown(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnIsPrivate(TableManager.TABLE_MYTORRENTS_COMPLETE),
					new ColumnDateAdded2Liner(TableManager.TABLE_MYTORRENTS_COMPLETE, false),
				};
				
				setColumnList(tableColumns, "date_added", true);
				String[] autoHideOrder = new String[] {
					ColumnQuality.COLUMN_ID,
					ColumnAzProduct.COLUMN_ID,
					SizeItem.COLUMN_ID,
					ColumnMediaThumb.COLUMN_ID,
				};
				tcManager.setAutoHideOrder(getTableID(), autoHideOrder);
			}
		} else {
			tableColumns = new TableColumnCore[] {
				new ColumnComplete(TABLE_MYMEDIA),
				new ColumnAzProduct(TABLE_MYMEDIA),
				new ColumnMediaThumb(TABLE_MYMEDIA),
				new ColumnTitle(TABLE_MYMEDIA),
				new SizeItem(TABLE_MYMEDIA),
				new ColumnQuality(TABLE_MYMEDIA),
				new ColumnDateAdded2Liner(TABLE_MYMEDIA, true),
				new ColumnRateUpDown(TABLE_MYMEDIA),
				new ColumnRate(TABLE_MYMEDIA),
			};
			setColumnList(tableColumns, "date_added", true);
			String[] autoHideOrder = new String[] {
				ColumnQuality.COLUMN_ID,
				ColumnAzProduct.COLUMN_ID,
				SizeItem.COLUMN_ID,
				ColumnMediaThumb.COLUMN_ID,
				ColumnDateAdded2Liner.COLUMN_ID,
			};
			tcManager.setAutoHideOrder(getTableID(), autoHideOrder);
		}

		if (countArea != null) {
			countArea.setText("");
		}

		getComposite().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				if (bMiniMode) {
					fixupRowCount();
				}
				expandNameColumn();
			}
		});

		final Listener l = new Listener() {
			public void handleEvent(Event event) {
				if (event.button == 2) {
					TableRowCore row = getRow(event.x, event.y);
					if (row != null) {
						DownloadManager dm = (DownloadManager) row.getDataSource(true);
						if (dm != null) {
							TOTorrent torrent = dm.getTorrent();
							// TODO: Add callback listener and update row
							PlatformTorrentUtils.updateMetaData(torrent, 1);
							Utils.beep();
						}

						if ((event.stateMask & SWT.CONTROL) != 0) {
							TableCellSWT cell = ((ListRow)row).getTableCellSWT(event.x, event.y);
							if (cell != null) {
								((TableCellImpl) cell).bDebug = !((TableCellImpl) cell).bDebug;
							}
						}
					}
				}
			}
		};

		getTableComposite().addListener(SWT.MouseUp, l);

		addSelectionListener(new TableSelectionAdapter() {
			public void defaultSelected(TableRowCore[] rows) {
				TableRowCore[] selectedRows = getSelectedRows();
				if (selectedRows.length > 0) {
					//TorrentListViewsUtils.viewDetails(skin, selectedRows[0]);
				}
			}
		}, false);

		this.globalManager = core.getGlobalManager();
		globalManager.addListener(this, false);

		// Needed or Java borks!
		dataArea.getDisplay().asyncExec(new AERunnable() {
			public void runSupport() {
				DownloadManager[] managers = sortDMList(globalManager.getDownloadManagers());
				bSkipUpdateCount = true;

				int max = (dataArea.getClientArea().height - 8) / ListRow.ROW_HEIGHT;
				for (int i = 0; i < managers.length; i++) {
					DownloadManager dm = managers[i];
					downloadManagerAdded(dm);

					if (max == i) {
						processDataSourceQueue();
						bSkipUpdateCount = false;
						updateCount();
						bSkipUpdateCount = true;

						for (int j = 0; j <= i; j++) {
							TableRowCore row = getRow(j);
							if (row != null) {
								row.redraw();
							}
						}
					}
				}
				bSkipUpdateCount = false;
				processDataSourceQueue();
				if (!bAllowScrolling) {
					regetDownloads();
				}
			}
		});

		dataArea.layout();
		_expandNameColumn();
	}

	// XXX Please get rid of me!  I suck and I am slow
	public void regetDownloads() {
		TableRowCore[] selectedRows = getSelectedRows();
		final int[] rowIndexes = new int[selectedRows.length];
		int selectedIndex = -1;
		if (selectedRows.length > 0) {
			for (int i = 0; i < selectedRows.length; i++) {
				rowIndexes[i] = selectedRows[i].getIndex();
			}
		}
		//System.out.println("SelectedIndex" + selectedIndex);

		//		globalManager.removeListener(this);
		removeAllTableRows();

		System.out.println("reget");
		//		globalManager.addListener(this, false);

		fixupRowCount();

		if (selectedIndex >= 0) {
			dataArea.getDisplay().asyncExec(new AERunnable() {
				public void runSupport() {
					for (int i = 0; i < rowIndexes.length; i++) {
						TableRowCore row = getRow(rowIndexes[i]);
						if (row != null) {
							row.setSelected(true);
						}
					}
				}
			});
		}
	}

	protected void expandNameColumn() {
		//		Utils.execSWTThread(new AERunnable() {
		//			public void runSupport() {
		//				_expandNameColumn();
		//			}
		//		});
	}

	protected void _expandNameColumn() {
		int viewWidth = getTableComposite().getClientArea().width;
		int columnWidthTotal = 0;
		int nameColumnIdx = -1;

		TableColumnCore[] columns = getVisibleColumns();
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getName().equals("name")) {
				nameColumnIdx = i;
			} else {
				columnWidthTotal += columns[i].getWidth() + (ListView.COLUMN_MARGIN_WIDTH * 2);
			}
		}

		if (nameColumnIdx >= 0) {
			columns[nameColumnIdx].setWidth(viewWidth - columnWidthTotal
					- (ListView.COLUMN_MARGIN_WIDTH * 2));
		}
	}

	protected void fixupRowCount() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_fixupRowCount();
			}
		});
	}

	private void _fixupRowCount() {
		//System.out.println("fixupRowCount");
		if (dataArea.isDisposed() || bAllowScrolling) {
			return;
		}

		int changeCount = 0;
		int curRowCount = size(true);

		int maxRows = bAllowScrolling ? 100000
				: (dataArea.getClientArea().height - 8) / ListRow.ROW_HEIGHT;

		long totalPossible = getTotalPossible();
		if (curRowCount < maxRows && totalPossible > curRowCount) {
			DownloadManager[] managers = sortDMList(globalManager.getDownloadManagers());

			int pos = 0;
			for (int i = 0; i < totalPossible && curRowCount < maxRows; i++) {
				DownloadManager dm = managers[i];
				if (isOurDownload(dm)) {
					if (!dataSourceExists(dm)) {
						addDataSource(dm, false);
						changeCount++;
						curRowCount++;
						pos++;
					}
				}
			}
			processDataSourceQueue();
		} else {
			while (curRowCount > maxRows) {
				TableRowCore row = getRow(--curRowCount);
				if (row != null) {
					removeDataSource(row.getDataSource(true), true);
				}
				changeCount++;
			}
		}

		updateCount();
	}

	protected DownloadManager[] sortDMList(List dms) {
		DownloadManager[] dmsArray = (DownloadManager[]) dms.toArray(new DownloadManager[0]);
		Arrays.sort(dmsArray, new Comparator() {
			public int compare(Object o1, Object o2) {
				DownloadManager dm1 = (DownloadManager) o1;
				DownloadManager dm2 = (DownloadManager) o2;

				boolean bOurDL1 = isOurDownload(dm1);
				boolean bOurDL2 = isOurDownload(dm2);
				if (bOurDL1 != bOurDL2) {
					return bOurDL1 ? -1 : 1;
				}

				long l1 = dm1 == null ? 0 : dm1.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
				long l2 = dm2 == null ? 0 : dm2.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
				return l1 == l2 ? 0 : l1 > l2 ? -1 : 1;
			}
		});
		return dmsArray;
	}

	// GlobalManagerListener
	public void destroyInitiated() {
		// TODO Auto-generated method stub

	}

	// GlobalManagerListener
	public void destroyed() {
		// TODO Auto-generated method stub

	}

	// GlobalManagerListener
	public void downloadManagerAdded(DownloadManager dm) {
		//regetDownloads();
		dm.addListener(dmListener);
		if (isOurDownload(dm)) {
			if (bAllowScrolling
					|| size(true) < (dataArea.getClientArea().height - 8)
							/ ListRow.ROW_HEIGHT) {
				addDataSource(dm, false);
				if (!bAllowScrolling && !bSkipUpdateCount) {
					regetDownloads();
				}
				updateCount();
			}
		}
	}

	// GlobalManagerListener
	public void downloadManagerRemoved(DownloadManager dm) {
		removeDataSource(dm, true);
		if (!bAllowScrolling) {
			regetDownloads();
		}
	}

	// GlobalManagerListener
	public void seedingStatusChanged(boolean seeding_only_mode) {
		// TODO Auto-generated method stub

	}

	public boolean isOurDownload(DownloadManager dm) {
		boolean bDownloadComplete = dm.isDownloadComplete(false);

		switch (viewMode) {
			/*
			 case VIEW_DOWNLOADING:
			 if (bDownloadComplete) {
			 return false;
			 }

			 int state = dm.getState();
			 return state != DownloadManager.STATE_STOPPED
			 && state != DownloadManager.STATE_ERROR
			 && state != DownloadManager.STATE_QUEUED;
			 */
			case VIEW_DOWNLOADING:
				return !bDownloadComplete;

			case VIEW_RECENT_DOWNLOADED:
				return bDownloadComplete;

			case VIEW_MY_MEDIA:
				return true;
		}

		return false;
	}

	private static class dowloadManagerListener
		implements DownloadManagerListener
	{
		private final TorrentListView view;

		/**
		 * @param view
		 */
		public dowloadManagerListener(TorrentListView view) {
			this.view = view;
		}

		public void completionChanged(DownloadManager manager, boolean bCompleted) {
			if (view.isOurDownload(manager)) {
				view.addDataSource(manager, true);
			} else {
				view.removeDataSource(manager, true);
			}
			if (!view.getAllowScrolling()) {
				view.regetDownloads();
			}
		}

		public void downloadComplete(DownloadManager manager) {
			if (view.isOurDownload(manager)) {
				view.addDataSource(manager, true);
			} else {
				view.removeDataSource(manager, true);
			}
			if (!view.getAllowScrolling()) {
				view.regetDownloads();
			}
		}

		public void filePriorityChanged(DownloadManager download,
				DiskManagerFileInfo file) {
		}

		public void positionChanged(DownloadManager download, int oldPosition,
				int newPosition) {

		}

		public void stateChanged(DownloadManager manager, int state) {
			try {
				view.listeners_mon.enter();
				for (Iterator iter = view.listeners.iterator(); iter.hasNext();) {
					TorrentListViewListener l = (TorrentListViewListener) iter.next();
					l.stateChanged(manager);
				}
			} finally {
				view.listeners_mon.exit();
			}
		}
	}

	protected void updateCount() {
		try {
			listeners_mon.enter();
			for (Iterator iter = listeners.iterator(); iter.hasNext();) {
				TorrentListViewListener l = (TorrentListViewListener) iter.next();
				l.countChanged();
			}
		} finally {
			listeners_mon.exit();
		}

		if (countArea != null && bSkipUpdateCount) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (countArea == null) {
						return;
					}
					long size1 = size(true);
					long size2 = getTotalPossible();

					if (size1 == size2) {
						countArea.setText(MessageText.getString("MainWindow.v3.count",
								new String[] { "" + size1
								}));
					} else {
						countArea.setText(MessageText.getString("MainWindow.v3.xofx",
								new String[] {
									"" + size1,
									"" + size2
								}));
					}
				}
			});
		}
	}

	private long getTotalPossible() {
		int count;
		if (viewMode == VIEW_DOWNLOADING) {
			count = globalManager.downloadManagerCount(false);
		} else if (viewMode == VIEW_RECENT_DOWNLOADED) {
			count = globalManager.downloadManagerCount(true);
		} else {
			count = globalManager.getDownloadManagers().size();
		}
		return count;
	}

	public void addListener(TorrentListViewListener l) {
		listeners.add(l);
		l.countChanged();
		l.stateChanged(null);
	}

	public void removeListener(TorrentListViewListener l) {
		listeners.remove(l);
	}

	// @see com.aelitis.azureus.ui.swt.views.list.ListView#fillMenu(org.eclipse.swt.widgets.Menu)
	public void fillMenu(Menu menu) {
		Object[] dms = getSelectedDataSources();
		boolean hasSelection = (dms.length > 0);

		// Explore
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu.explore");
		itemExplore.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				exploreTorrents();
			}
		});
		itemExplore.setEnabled(hasSelection);
	}

	private void exploreTorrents() {
		Object[] dataSources = getSelectedDataSources();
		for (int i = dataSources.length - 1; i >= 0; i--) {
			DownloadManager dm = (DownloadManager) dataSources[i];
			if (dm != null) {
				ManagerUtils.open(dm);
			}
		}
	}

	public boolean getAllowScrolling() {
		return bAllowScrolling;
	}
}
