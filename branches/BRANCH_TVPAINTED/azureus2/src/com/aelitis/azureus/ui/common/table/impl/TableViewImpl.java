/**
 * 
 */
package com.aelitis.azureus.ui.common.table.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.common.table.*;

/**
 * @author TuxPaper
 * @created Feb 6, 2007
 */
public abstract class TableViewImpl<DATASOURCETYPE>
	implements TableView<DATASOURCETYPE>, TableStructureModificationListener<DATASOURCETYPE>
{
	private final static LogIDs LOGID = LogIDs.GUI;

	private static final boolean DEBUG_SORTER = false;

	/** Helpful output when trying to debug add/removal of rows */
	public final static boolean DEBUGADDREMOVE = true || System.getProperty(
			"debug.swt.table.addremove", "0").equals("1");

	public static final boolean DEBUG_SELECTION = false;

	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	// Shorter name for ConfigManager, easier to read code
	protected static final ConfigurationManager configMan = ConfigurationManager.getInstance();

	/** TableID (from {@link org.gudy.azureus2.plugins.ui.tables.TableManager}) 
	 * of the table this class is
	 * handling.  Config settings are stored with the prefix of 
	 * "Table.<i>TableID</i>"
	 */
	protected String tableID;

	/** Prefix for retrieving text from the properties file (MessageText)
	 * Typically <i>TableID</i> + "View"
	 */
	protected String propertiesPrefix;
	
	// What type of data is stored in this table
	private final Class<?> classPluginDataSourceType;


	private boolean bReallyAddingDataSources = false;

	private AEMonitor sortColumn_mon = new AEMonitor("TableView:sC");

	/** Sorting functions */
	private TableColumnCore sortColumn;

	/** TimeStamp of when last sorted all the rows was */
	private long lLastSortedOn;

	private AEMonitor listeners_mon = new AEMonitor("tablelisteners");

	private ArrayList<TableRowRefreshListener> rowRefreshListeners;

	// List of DataSourceChangedListener
	private CopyOnWriteList<TableDataSourceChangedListener> listenersDataSourceChanged = new CopyOnWriteList<TableDataSourceChangedListener>();

	private CopyOnWriteList<TableSelectionListener> listenersSelection = new CopyOnWriteList<TableSelectionListener>();

	private CopyOnWriteList<TableLifeCycleListener> listenersLifeCycle = new CopyOnWriteList<TableLifeCycleListener>();

	private CopyOnWriteList<TableRefreshListener> listenersRefresh = new CopyOnWriteList<TableRefreshListener>();

	private CopyOnWriteList<TableCountChangeListener> listenersCountChange = new CopyOnWriteList<TableCountChangeListener>(
			1);

	private Object parentDataSource;

	private AEMonitor sortedRows_mon = new AEMonitor("TableView:sR");

	/** Filtered rows in the table */
	private List<TableRowCore> sortedRows;

	private AEMonitor dataSourceToRow_mon = new AEMonitor("TableView:OTSI");

	/** Link DataSource to their row in the table.
	 * key = DataSource
	 * value = TableRowSWT
	 */
	private Map<DATASOURCETYPE, TableRowCore> mapDataSourceToRow;

	private AEMonitor listUnfilteredDatasources_mon = new AEMonitor(
			"TableView:uds");

	private Set<DATASOURCETYPE> listUnfilteredDataSources;

	/** Queue added datasources and add them on refresh */
	private LightHashSet dataSourcesToAdd = new LightHashSet(4);

	/** Queue removed datasources and add them on refresh */
	private LightHashSet dataSourcesToRemove = new LightHashSet(4);

	// class used to keep filter stuff in a nice readable parcel
	public static class filter<DATASOURCETYPE>
	{

		public TimerEvent eventUpdate;

		public String text = "";

		public long lastFilterTime;

		public boolean regex = false;

		public TableViewFilterCheck<DATASOURCETYPE> checker;

		public String nextText = "";
	};

	protected filter<DATASOURCETYPE> filter;

	private DataSourceCallBackUtil.addDataSourceCallback processDataSourceQueueCallback = new DataSourceCallBackUtil.addDataSourceCallback() {
		public void process() {
			processDataSourceQueue();
		}

		public void debug(String str) {
			TableViewImpl.this.debug(str);
		}
	};

	
	/** Basic (pre-defined) Column Definitions */
	private TableColumnCore[] basicItems;

	/** All Column Definitions.  The array is not necessarily in column order */
	private TableColumnCore[] tableColumns;

	/** We need to remember the order of the columns at the time we added them
	 * in case the user drags the columns around.
	 */
	private TableColumnCore[] columnsOrdered;

	/**
	 * Up to date list of selected rows, so we can access rows without being on SWT Thread.
	 * Guaranteed to have no nulls
	 */
	private List<TableRowCore> selectedRows = new ArrayList<TableRowCore>(1);

	private List<Object> listSelectedCoreDataSources;

	private boolean headerVisible = true;

	private boolean menuEnabled = true;



	public TableViewImpl(Class<?> pluginDataSourceType, String _sTableID,
			String _sPropertiesPrefix, TableColumnCore[] _basicItems) {
		classPluginDataSourceType = pluginDataSourceType;
		propertiesPrefix = _sPropertiesPrefix;
		tableID = _sTableID;
		basicItems = _basicItems;
		mapDataSourceToRow = new LightHashMap<DATASOURCETYPE, TableRowCore>();
		sortedRows = new ArrayList<TableRowCore>();
		listUnfilteredDataSources = new HashSet<DATASOURCETYPE>();
		initializeColumnDefs();
	}

	private void initializeColumnDefs() {
		// XXX Adding Columns only has to be done once per TableID.  
		// Doing it more than once won't harm anything, but it's a waste.
		TableColumnManager tcManager = TableColumnManager.getInstance();

		if (basicItems != null) {
			if (tcManager.getTableColumnCount(tableID) != basicItems.length) {
				tcManager.addColumns(basicItems);
			}
			basicItems = null;
		}

		tableColumns = tcManager.getAllTableColumnCoreAsArray(getDataSourceType(),
				tableID);

		// fixup order
		tcManager.ensureIntegrety(tableID);
	}


	public void addSelectionListener(TableSelectionListener listener,
			boolean bFireSelection) {
		listenersSelection.add(listener);
		if (bFireSelection) {
			TableRowCore[] rows = getSelectedRows();
			listener.selected(rows);
			listener.focusChanged(getFocusedRow());
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addTableDataSourceChangedListener(com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener, boolean)
	public void addTableDataSourceChangedListener(
			TableDataSourceChangedListener l, boolean trigger) {
		listenersDataSourceChanged.add(l);
		if (trigger) {
			l.tableDataSourceChanged(parentDataSource);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeTableDataSourceChangedListener(com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener)
	public void removeTableDataSourceChangedListener(
			TableDataSourceChangedListener l) {
		listenersDataSourceChanged.remove(l);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setParentDataSource(java.lang.Object)
	public void setParentDataSource(Object newDataSource) {
		parentDataSource = newDataSource;
		Object[] listeners = listenersDataSourceChanged.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableDataSourceChangedListener l = (TableDataSourceChangedListener) listeners[i];
			l.tableDataSourceChanged(newDataSource);
		}
	}

	public Object getParentDataSource() {
		return parentDataSource;
	}

	/**
	 * @param selectedRows
	 */
	public void triggerDefaultSelectedListeners(TableRowCore[] selectedRows,
			int keyMask) {
		for (Iterator iter = listenersSelection.iterator(); iter.hasNext();) {
			TableSelectionListener l = (TableSelectionListener) iter.next();
			l.defaultSelected(selectedRows, keyMask);
		}
	}

	/**
	 * @param eventType
	 */
	protected void triggerLifeCycleListener(int eventType) {
		Object[] listeners = listenersLifeCycle.toArray();
		if (eventType == TableLifeCycleListener.EVENT_INITIALIZED) {
			for (int i = 0; i < listeners.length; i++) {
				TableLifeCycleListener l = (TableLifeCycleListener) listeners[i];
				try {
					l.tableViewInitialized();
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		} else {
			for (int i = 0; i < listeners.length; i++) {
				TableLifeCycleListener l = (TableLifeCycleListener) listeners[i];
				try {
					l.tableViewDestroyed();
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void triggerSelectionListeners(TableRowCore[] rows) {
		if (rows == null || rows.length == 0) {
			return;
		}
		Object[] listeners = listenersSelection.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableSelectionListener l = (TableSelectionListener) listeners[i];
			l.selected(rows);
		}
	}

	protected void triggerDeselectionListeners(TableRowCore[] rows) {
		if (rows == null) {
			return;
		}
		Object[] listeners = listenersSelection.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableSelectionListener l = (TableSelectionListener) listeners[i];
			try {
				l.deselected(rows);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	protected void triggerMouseEnterExitRow(TableRowCore row, boolean enter) {
		if (row == null) {
			return;
		}
		Object[] listeners = listenersSelection.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableSelectionListener l = (TableSelectionListener) listeners[i];
			if (enter) {
				l.mouseEnter(row);
			} else {
				l.mouseExit(row);
			}
		}
	}

	protected void triggerFocusChangedListeners(TableRowCore row) {
		Object[] listeners = listenersSelection.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableSelectionListener l = (TableSelectionListener) listeners[i];
			l.focusChanged(row);
		}
	}

	/**
	 * 
	 */
	protected void triggerTableRefreshListeners() {
		Object[] listeners = listenersRefresh.toArray();
		for (int i = 0; i < listeners.length; i++) {
			TableRefreshListener l = (TableRefreshListener) listeners[i];
			l.tableRefresh();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addLifeCycleListener(com.aelitis.azureus.ui.common.table.TableLifeCycleListener)
	public void addLifeCycleListener(TableLifeCycleListener l) {
		listenersLifeCycle.add(l);
		if (!isDisposed()) {
			l.tableViewInitialized();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addRefreshListener(com.aelitis.azureus.ui.common.table.TableRefreshListener, boolean)
	public void addRefreshListener(TableRefreshListener l, boolean trigger) {
		listenersRefresh.add(l);
		if (trigger) {
			l.tableRefresh();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#addCountChangeListener(com.aelitis.azureus.ui.common.table.TableCountChangeListener)
	public void addCountChangeListener(TableCountChangeListener listener) {
		listenersCountChange.add(listener);
	}

	public void removeCountChangeListener(TableCountChangeListener listener) {
		listenersCountChange.remove(listener);
	}

	protected void triggerListenerRowAdded(final TableRowCore[] rows) {
		if (listenersCountChange.size() == 0) {
			return;
		}
		getOffUIThread(new AERunnable() {
			public void runSupport() {
				for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
					TableCountChangeListener l = (TableCountChangeListener) iter.next();
					for (TableRowCore row : rows) {
						l.rowAdded(row);
					}
				}
			}
		});
	}

	protected void triggerListenerRowRemoved(TableRowCore row) {
		for (Iterator iter = listenersCountChange.iterator(); iter.hasNext();) {
			TableCountChangeListener l = (TableCountChangeListener) iter.next();
			l.rowRemoved(row);
		}
	}
	
	public void addRefreshListener(TableRowRefreshListener listener) {
		try {
			listeners_mon.enter();

			if (rowRefreshListeners == null) {
				rowRefreshListeners = new ArrayList<TableRowRefreshListener>(1);
			}

			rowRefreshListeners.add(listener);

		} finally {
			listeners_mon.exit();
		}
	}

	public void removeRefreshListener(TableRowRefreshListener listener) {
		try {
			listeners_mon.enter();

			if (rowRefreshListeners == null) {
				return;
			}

			rowRefreshListeners.remove(listener);

		} finally {
			listeners_mon.exit();
		}
	}

	public void invokeRefreshListeners(TableRowCore row) {
		Object[] listeners;
		try {
			listeners_mon.enter();
			if (rowRefreshListeners == null) {
				return;
			}
			listeners = rowRefreshListeners.toArray();

		} finally {
			listeners_mon.exit();
		}

		for (int i = 0; i < listeners.length; i++) {
			try {
				TableRowRefreshListener l = (TableRowRefreshListener) listeners[i];

				l.rowRefresh(row);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}


	public void runForAllRows(TableGroupRowRunner runner) {
		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();
		if (runner.run(rows)) {
			return;
		}

		for (int i = 0; i < rows.length; i++) {
			runner.run(rows[i]);
		}
	}

	// see common.tableview
	public void runForAllRows(TableGroupRowVisibilityRunner runner) {
		if (isDisposed()) {
			return;
		}

		// put to array instead of synchronised iterator, so that runner can remove
		TableRowCore[] rows = getRows();

		for (int i = 0; i < rows.length; i++) {
			boolean isRowVisible = isRowVisible(rows[i]);
			runner.run(rows[i], isRowVisible);
			
			int numSubRows = rows[i].getSubItemCount();
			if (numSubRows > 0) {
				TableRowCore[] subRows = rows[i].getSubRowsWithNull();
				for (TableRowCore subRow : subRows) {
					if (subRow != null) {
						runner.run(subRow, isRowVisible(subRow));
					}
				}
			}
		}
	}


	/** For each row source that the user has selected, run the code
	 * provided by the specified parameter.
	 *
	 * @param runner Code to run for each selected row/datasource
	 */
	public void runForSelectedRows(TableGroupRowRunner runner) {
		if (isDisposed()) {
			return;
		}

		TableRowCore[] rows;
		synchronized (selectedRows) {
			rows = selectedRows.toArray(new TableRowCore[0]);
		}
		boolean ran = runner.run(rows);
		if (!ran) {
			for (int i = 0; i < rows.length; i++) {
				TableRowCore row = rows[i];
				runner.run(row);
			}
		}
	}

	public boolean isUnfilteredDataSourceAdded(Object ds) {
		return listUnfilteredDataSources.contains(ds);
	}

	@SuppressWarnings("unchecked")
	public void refilter() {
		if (filter == null) {
			return;
		}
		if (filter.eventUpdate != null) {
			filter.eventUpdate.cancel();
		}
		filter.eventUpdate = null;

		listUnfilteredDatasources_mon.enter();
		try {
			DATASOURCETYPE[] unfilteredArray = (DATASOURCETYPE[]) listUnfilteredDataSources.toArray();

			Set<DATASOURCETYPE> existing = new HashSet<DATASOURCETYPE>(
					getDataSources());
			List<DATASOURCETYPE> listRemoves = new ArrayList<DATASOURCETYPE>();
			List<DATASOURCETYPE> listAdds = new ArrayList<DATASOURCETYPE>();

			for (int i = 0; i < unfilteredArray.length; i++) {
				boolean bHave = existing.contains(unfilteredArray[i]);
				boolean isOurs = filter.checker.filterCheck(unfilteredArray[i],
						filter.text, filter.regex);
				if (!isOurs) {
					if (bHave) {
						listRemoves.add(unfilteredArray[i]);
					}
				} else {
					if (!bHave) {
						listAdds.add(unfilteredArray[i]);
					}
				}
			}
			removeDataSources((DATASOURCETYPE[]) listRemoves.toArray());
			addDataSources((DATASOURCETYPE[]) listAdds.toArray(), true);

			// add back the ones removeDataSources removed
			listUnfilteredDataSources.addAll(listRemoves);
		} finally {
			listUnfilteredDatasources_mon.exit();
			processDataSourceQueue();
		}
	}

	protected void debug(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("table");
		diag_logger.log(SystemTime.getCurrentTime() + ":" + getTableID() + ": " + s);

		System.out.println(Thread.currentThread().getName() + "] " + SystemTime.getCurrentTime() + ": " + getTableID() + ": "
				+ s);
	}

	private void _processDataSourceQueue() {
		Object[] dataSourcesAdd = null;
		Object[] dataSourcesRemove = null;

		try {
			dataSourceToRow_mon.enter();
			if (dataSourcesToAdd.size() > 0) {
				if (dataSourcesToAdd.removeAll(dataSourcesToRemove) && DEBUGADDREMOVE) {
					debug("Saved time by not adding a row that was removed");
				}
				dataSourcesAdd = dataSourcesToAdd.toArray();

				dataSourcesToAdd.clear();
			}

			if (dataSourcesToRemove.size() > 0) {
				dataSourcesRemove = dataSourcesToRemove.toArray();
				if (DEBUGADDREMOVE && dataSourcesRemove.length > 1) {
					debug("Streamlining removing " + dataSourcesRemove.length + " rows");
				}
				dataSourcesToRemove.clear();
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (dataSourcesAdd != null && dataSourcesAdd.length > 0) {
			reallyAddDataSources(dataSourcesAdd);
			if (DEBUGADDREMOVE && dataSourcesAdd.length > 1) {
				debug("Streamlined adding " + dataSourcesAdd.length + " rows");
			}
		}

		if (dataSourcesRemove != null && dataSourcesRemove.length > 0) {
			reallyRemoveDataSources(dataSourcesRemove);
		}
	}

	public void addDataSource(DATASOURCETYPE dataSource) {
		addDataSource(dataSource, false);
	}

	private void addDataSource(DATASOURCETYPE dataSource, boolean skipFilterCheck) {

		if (dataSource == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.add(dataSource);
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (!skipFilterCheck && filter != null
				&& !filter.checker.filterCheck(dataSource, filter.text, filter.regex)) {
			return;
		}

		if (DataSourceCallBackUtil.IMMEDIATE_ADDREMOVE_DELAY == 0 || sortedRows.size() < uiGuessMaxVisibleRows()) {
			reallyAddDataSources(new Object[] {
				dataSource
			});
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession

		try {
			dataSourceToRow_mon.enter();

			if (dataSourcesToRemove.remove(dataSource)) {
				// we're adding, override any pending removal
				if (DEBUGADDREMOVE) {
					debug("AddDS: Removed from toRemove.  Total Removals Queued: "
							+ dataSourcesToRemove.size());
				}
			}

			if (dataSourcesToAdd.contains(dataSource)) {
				// added twice.. ensure it's not in the remove list
				if (DEBUGADDREMOVE) {
					debug("AddDS: Already There.  Total Additions Queued: "
							+ dataSourcesToAdd.size());
				}
			} else {
				dataSourcesToAdd.add(dataSource);
				if (DEBUGADDREMOVE) {
					debug("Queued 1 dataSource to add.  Total Additions Queued: "
							+ dataSourcesToAdd.size() + "; already=" + sortedRows.size());
				}
				refreshenProcessDataSourcesTimer();
			}

		} finally {

			dataSourceToRow_mon.exit();
		}
	}

	// see common.TableView
	public void addDataSources(final DATASOURCETYPE dataSources[]) {
		addDataSources(dataSources, false);
	}

	public void addDataSources(final DATASOURCETYPE dataSources[],
			boolean skipFilterCheck) {

		if (dataSources == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.addAll(Arrays.asList(dataSources));
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (DataSourceCallBackUtil.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			if (!skipFilterCheck && filter != null) {
				for (int i = 0; i < dataSources.length; i++) {
					if (!filter.checker.filterCheck(dataSources[i], filter.text,
							filter.regex)) {
						dataSources[i] = null;
					}
				}
			}
			reallyAddDataSources(dataSources);
			return;
		}

		// In order to save time, we cache entries to be added and process them
		// in a refresh cycle.  This is a huge benefit to tables that have
		// many rows being added and removed in rapid succession

		try {
			dataSourceToRow_mon.enter();

			int count = 0;

			for (int i = 0; i < dataSources.length; i++) {
				DATASOURCETYPE dataSource = dataSources[i];
				if (dataSource == null) {
					continue;
				}
				if (!skipFilterCheck
						&& filter != null
						&& !filter.checker.filterCheck(dataSource, filter.text,
								filter.regex)) {
					continue;
				}
				dataSourcesToRemove.remove(dataSource); // may be pending removal, override

				if (dataSourcesToAdd.contains(dataSource)) {
				} else {
					count++;
					dataSourcesToAdd.add(dataSource);
				}
			}

			if (DEBUGADDREMOVE) {
				debug("Queued " + count + " of " + dataSources.length
						+ " dataSources to add.  Total Queued: " + dataSourcesToAdd.size());
			}

		} finally {

			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#dataSourceExists(java.lang.Object)
	public boolean dataSourceExists(DATASOURCETYPE dataSource) {
		return mapDataSourceToRow.containsKey(dataSource)
				|| dataSourcesToAdd.contains(dataSource);
	}

	public void processDataSourceQueue() {
		getOffUIThread(new AERunnable() {
			public void runSupport() {
				_processDataSourceQueue();
			}
		});
	}

	public abstract void getOffUIThread(AERunnable runnable);

	public void processDataSourceQueueSync() {
		_processDataSourceQueue();
	}

	/**
	 * @note bIncludeQueue can return an invalid number, such as a negative :(
	 */
	public int size(boolean bIncludeQueue) {
		int size = sortedRows.size();

		if (bIncludeQueue) {
			if (dataSourcesToAdd != null) {
				size += dataSourcesToAdd.size();
			}
			if (dataSourcesToRemove != null) {
				size -= dataSourcesToRemove.size();
			}
		}
		return size;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRows()
	public TableRowCore[] getRows() {
		try {
			sortedRows_mon.enter();

			return sortedRows.toArray(new TableRowCore[0]);

		} finally {
			sortedRows_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(java.lang.Object)
	public TableRowCore getRow(DATASOURCETYPE dataSource) {
		return mapDataSourceToRow.get(dataSource);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getRow(int)
	public TableRowCore getRow(int iPos) {
		try {
			sortedRows_mon.enter();

			if (iPos >= 0 && iPos < sortedRows.size()) {
				TableRowCore row = sortedRows.get(iPos);

				if (row.getIndex() != iPos) {
					row.setTableItem(iPos);
				}
				return row;
			}
		} finally {
			sortedRows_mon.exit();
		}
		return null;
	}

	public TableRowCore getRowQuick(int iPos) {
		try {
			return sortedRows.get(iPos);
		} catch (Exception e) {
			return null;
		}
	}

	public int indexOf(TableRowCore row) {
		return sortedRows.indexOf(row);
	}

	public int getRowCount() {
		// don't use sortedRows here, it's not always up to date 
		return mapDataSourceToRow.size();
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getDataSources()
	public ArrayList<DATASOURCETYPE> getDataSources() {
		return new ArrayList<DATASOURCETYPE>(mapDataSourceToRow.keySet());
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#removeDataSource(java.lang.Object)
	public void removeDataSource(final DATASOURCETYPE dataSource) {
		if (dataSource == null) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.remove(dataSource);
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (DataSourceCallBackUtil.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyRemoveDataSources(new Object[] {
				dataSource
			});
			return;
		}

		try {
			dataSourceToRow_mon.enter();

			dataSourcesToAdd.remove(dataSource); // override any pending addition
			dataSourcesToRemove.add(dataSource);

			if (DEBUGADDREMOVE) {
				debug("Queued 1 dataSource to remove.  Total Queued: "
						+ dataSourcesToRemove.size());
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	/** Remove the specified dataSource from the table.
	 *
	 * @param dataSources data sources to be removed
	 * @param bImmediate Remove immediately, or queue and remove at next refresh
	 */
	public void removeDataSources(final DATASOURCETYPE[] dataSources) {
		if (dataSources == null || dataSources.length == 0) {
			return;
		}

		listUnfilteredDatasources_mon.enter();
		try {
			listUnfilteredDataSources.removeAll(Arrays.asList(dataSources));
		} finally {
			listUnfilteredDatasources_mon.exit();
		}

		if (DataSourceCallBackUtil.IMMEDIATE_ADDREMOVE_DELAY == 0) {
			reallyRemoveDataSources(dataSources);
			return;
		}

		try {
			dataSourceToRow_mon.enter();

			for (int i = 0; i < dataSources.length; i++) {
				DATASOURCETYPE dataSource = dataSources[i];
				dataSourcesToAdd.remove(dataSource); // override any pending addition
				dataSourcesToRemove.add(dataSource);
			}

			if (DEBUGADDREMOVE) {
				debug("Queued " + dataSources.length
						+ " dataSources to remove.  Total Queued: "
						+ dataSourcesToRemove.size() + " via " + Debug.getCompressedStackTrace(4));
			}
		} finally {
			dataSourceToRow_mon.exit();
		}

		refreshenProcessDataSourcesTimer();
	}

	private void refreshenProcessDataSourcesTimer() {
		if (bReallyAddingDataSources || processDataSourceQueueCallback == null) {
			// when processDataSourceQueueCallback is null, we are disposing
			return;
		}

/////////////////////////////////////////////////////////////////////////////////		if (cellEditNotifier != null) {
/////////////////////////////////////////////////////////////////////////////////			cellEditNotifier.sourcesChanged();
/////////////////////////////////////////////////////////////////////////////////		}

		boolean processQueueImmediately = DataSourceCallBackUtil.addDataSourceAggregated(processDataSourceQueueCallback);

		if (processQueueImmediately) {
			processDataSourceQueue();
		}
	}

	public void reallyAddDataSources(final Object dataSources[]) {
		// Note: We assume filterCheck has already run, and the list of dataSources
		//       all passed the filter

		if (isDisposed()) {
			return;
		}

		bReallyAddingDataSources = true;
		if (DEBUGADDREMOVE) {
			debug(">>" + " Add " + dataSources.length + " rows;");
		}

		// Create row, and add to map immediately
		try {
			dataSourceToRow_mon.enter();

			//long lStartTime = SystemTime.getCurrentTime();

			for (int i = 0; i < dataSources.length; i++) {
				Object ds = dataSources[i];
				if (ds == null) {
					if (DEBUGADDREMOVE) {
						debug("-- Null DS for " + i);
					}
					continue;
				}

				if (mapDataSourceToRow.containsKey(ds)) {
					debug("-- " + i + " already addded: " + ds.getClass());
					ds = null;
				} else {
					TableRowCore rowCore = createNewRow(ds);
					mapDataSourceToRow.put((DATASOURCETYPE) ds, rowCore);
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while added row to Table "
					+ getTableID(), e));
		} finally {
			dataSourceToRow_mon.exit();
		}

		if (DEBUGADDREMOVE) {
			debug("--" + " Add " + dataSources.length + " rows;");
		}
		
		addSortedDataSource(dataSources);

		bReallyAddingDataSources = false;
	}

	public abstract TableRowCore createNewRow(Object object);

	public void delete() {
		processDataSourceQueueCallback = null;
	}

	public void lockRows() {
		sortedRows_mon.enter();
	}

	public void unlockRows() {
		sortedRows_mon.exit();
	}

	public void generate(IndentWriter writer) {
		writer.println("Diagnostics for " + this + " (" + getTableID() + ")");

		try {
			dataSourceToRow_mon.enter();

			writer.println("DataSources scheduled to Add/Remove: "
					+ dataSourcesToAdd.size() + "/" + dataSourcesToRemove.size());

			writer.println("TableView: " + mapDataSourceToRow.size() + " datasources");
			Iterator<DATASOURCETYPE> it = mapDataSourceToRow.keySet().iterator();

			while (it.hasNext()) {

				Object key = it.next();

				writer.println("  " + key + " -> " + mapDataSourceToRow.get(key));
			}

		} finally {

			dataSourceToRow_mon.exit();
		}
	}

	public void removeAllTableRows() {
		try {
			dataSourceToRow_mon.enter();
			sortedRows_mon.enter();
			listUnfilteredDatasources_mon.enter();

			mapDataSourceToRow.clear();
			sortedRows.clear();
			
			dataSourcesToAdd.clear();
			dataSourcesToRemove.clear();
			
			listUnfilteredDataSources.clear();

			if (DEBUGADDREMOVE) {
				debug("removeAll");
			}

		} finally {

			listUnfilteredDatasources_mon.exit();
			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();
		}
	}

	@SuppressWarnings("null")
	public void reallyRemoveDataSources(final Object[] dataSources) {
		final long lStart = SystemTime.getCurrentTime();

		StringBuffer sbWillRemove = null;
		if (DEBUGADDREMOVE) {
			debug(">>> Remove rows.  Start w/" + getRowCount()
					+ "ds;"
					+ (SystemTime.getCurrentTime() - lStart) + "ms wait");

			sbWillRemove = new StringBuffer("Will soon remove row #");
		}

		ArrayList<TableRowCore> itemsToRemove = new ArrayList<TableRowCore>();
		ArrayList<Integer> indexesToRemove = new ArrayList<Integer>();

		int numRemovedHavingSelection = 0;
		for (int i = 0; i < dataSources.length; i++) {
			if (dataSources[i] == null) {
				continue;
			}

			TableRowCore item = mapDataSourceToRow.get(dataSources[i]);
			if (item != null) {
				// use sortedRows position instead of item.getIndex(), because
				// getIndex may have a wrong value (unless we fillRowGaps() which
				// is more time consuming and we do afterwards anyway)
				int index = sortedRows.indexOf(item);
				indexesToRemove.add(index);
				if (DEBUGADDREMOVE) {
					if (i != 0) {
						sbWillRemove.append(", ");
					}
					sbWillRemove.append(index);
				}

				if (item.isSelected()) {
					numRemovedHavingSelection++;
				}
				itemsToRemove.add(item);
				mapDataSourceToRow.remove(dataSources[i]);
				triggerListenerRowRemoved(item);
				sortedRows.remove(item);
			}
		}

		if (DEBUGADDREMOVE) {
			debug(sbWillRemove.toString());
			debug("#itemsToRemove=" + itemsToRemove.size());
		}

		uiRemoveRows(itemsToRemove.toArray(new TableRowCore[0]),
				indexesToRemove.toArray(new Integer[0]));

		// Finally, delete the rows
		for (Iterator<TableRowCore> iter = itemsToRemove.iterator(); iter.hasNext();) {
			TableRowCore row = iter.next();
			row.delete();
		}

		if (DEBUGADDREMOVE) {
			debug("<< Remove " + itemsToRemove.size() + " rows. now "
					+ mapDataSourceToRow.size() + "ds");
		}
	}

	protected void fillRowGaps(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, true, false);
	}

	public void sortColumn(boolean bForceDataRefresh) {
		_sortColumn(bForceDataRefresh, false, false);
	}

	protected void _sortColumn(final boolean bForceDataRefresh, final boolean bFillGapsOnly,
			final boolean bFollowSelected) {
		if (isDisposed()) {
			return;
		}

		try {
			sortColumn_mon.enter();

			long lTimeStart;
			if (DEBUG_SORTER) {
				//System.out.println(">>> Sort.. ");
				lTimeStart = System.currentTimeMillis();
			}

			int iNumMoves = 0;


			boolean needsUpdate = false;

			try {
				sortedRows_mon.enter();

				if (bForceDataRefresh && sortColumn != null) {
					int i = 0;
					String sColumnID = sortColumn.getName();
					for (Iterator<TableRowCore> iter = sortedRows.iterator(); iter.hasNext();) {
						TableRowCore row = iter.next();
						TableCellCore cell = row.getSortColumnCell(sColumnID);
						if (cell != null) {
							cell.refresh(true);
						}
						i++;
					}
				}

				if (!bFillGapsOnly) {
					if (sortColumn != null
							&& sortColumn.getLastSortValueChange() >= lLastSortedOn) {
						lLastSortedOn = SystemTime.getCurrentTime();
						Collections.sort(sortedRows, sortColumn);
						if (DEBUG_SORTER) {
							long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
							if (lTimeDiff >= 0) {
								debug("--- Build & Sort took " + lTimeDiff + "ms");
							}
						}
					} else {
						if (DEBUG_SORTER) {
							debug("Skipping sort :)");
						}
					}
				}

				for (int i = 0; i < sortedRows.size(); i++) {
					TableRowCore row = sortedRows.get(i);
					boolean visible = row.isVisible();
					if (row.setTableItem(i, visible)) {
						if (visible) {
							needsUpdate = true;
						}
						iNumMoves++;
					}
				}
			} finally {
				sortedRows_mon.exit();
			}

			if (DEBUG_SORTER && iNumMoves > 0) {
				debug("Sort: numMoves= " + iNumMoves + ";needUpdate?" + needsUpdate);
			}

			if (needsUpdate) {
				visibleRowsChanged();
			}

			if (DEBUG_SORTER) {
				long lTimeDiff = (System.currentTimeMillis() - lTimeStart);
				if (lTimeDiff >= 500) {
					debug("<<< Sort & Assign took " + lTimeDiff + "ms with "
							+ iNumMoves + " rows (of " + sortedRows.size() + ") moved. "
							+ Debug.getCompressedStackTrace());
				}
			}
		} finally {
			sortColumn_mon.exit();
		}
	}

	public abstract void visibleRowsChanged();

	public abstract int uiGetTopIndex();

	public abstract int uiGetBottomIndex(int iTopIndex);

	public abstract void uiRemoveRows(TableRowCore[] rows, Integer[] rowIndexes);
	
	public abstract int uiGuessMaxVisibleRows();

	public void resetLastSortedOn() {
		lLastSortedOn = 0;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getColumnCells(java.lang.String)
	public TableCellCore[] getColumnCells(String sColumnName) {
		TableCellCore[] cells = new TableCellCore[sortedRows.size()];

		try {
			sortedRows_mon.enter();

			int i = 0;
			for (Iterator<TableRowCore> iter = sortedRows.iterator(); iter.hasNext();) {
				TableRowCore row = iter.next();
				cells[i++] = row.getTableCellCore(sColumnName);
			}

		} finally {
			sortedRows_mon.exit();
		}

		return cells;
	}
	
	private void addSortedDataSource(final Object dataSources[]) {

		if (isDisposed()) {
			return;
		}

		TableRowCore[] selectedRows = getSelectedRows();
			
		boolean bWas0Rows = getRowCount() == 0;
		dataSourceToRow_mon.enter();
		sortedRows_mon.enter();
		try {

			if (DEBUGADDREMOVE) {
				debug("--" + " Add " + dataSources.length + " rows to SWT");
			}

			long lStartTime = SystemTime.getCurrentTime();
			
			final List<TableRowCore> rowsAdded = new ArrayList<TableRowCore>();
			
			// add to sortedRows list in best position.  
			// We need to be in the SWT thread because the rowSorter may end up
			// calling SWT objects.
			for (int i = 0; i < dataSources.length; i++) {
				Object dataSource = dataSources[i];
				if (dataSource == null) {
					continue;
				}

				TableRowCore row = mapDataSourceToRow.get(dataSource);
				//if ((row == null) || row.isRowDisposed() || sortedRows.indexOf(row) >= 0) {
				if (row == null || row.isRowDisposed()) {
					continue;
				}
//				if (sortColumn != null) {
//					TableCellCore cell = row.getTableCellCore(sortColumn.getName());
//					if (cell != null) {
//						try {
//							cell.invalidate();
//							cell.refresh(true);
//						} catch (Exception e) {
//							Logger.log(new LogEvent(LOGID,
//									"Minor error adding a row to table " + sTableID, e));
//						}
//					}
//				}

				try {
					int index = 0;
					if (sortedRows.size() > 0) {
						// If we are >= to the last item, then just add it to the end
						// instead of relying on binarySearch, which may return an item
						// in the middle that also is equal.
						TableRowCore lastRow = sortedRows.get(sortedRows.size() - 1);
						if (sortColumn == null || sortColumn.compare(row, lastRow) >= 0) {
							index = sortedRows.size();
							sortedRows.add(row);
							if (DEBUGADDREMOVE) {
								debug("Adding new row to bottom");
							}
						} else {
							index = Collections.binarySearch(sortedRows, row, sortColumn);
							if (index < 0) {
								index = -1 * index - 1; // best guess
							}

							if (index > sortedRows.size()) {
								index = sortedRows.size();
							}

							if (DEBUGADDREMOVE) {
								debug("Adding new row at position " + index + " of "
										+ (sortedRows.size() - 1));
							}
							sortedRows.add(index, row);
						}
					} else {
						if (DEBUGADDREMOVE) {
							debug("Adding new row to bottom (1st Entry)");
						}
						index = sortedRows.size();
						sortedRows.add(row);
					}

					rowsAdded.add(row);

					// XXX Don't set table item here, it will mess up selected rows
					//     handling (which is handled in fillRowGaps called later on)
					//row.setTableItem(index);
					
					
					//row.setIconSize(ptIconSize);
				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "Error adding a row to table "
							+ getTableID(), e));
					try {
						if (!sortedRows.contains(row)) {
							sortedRows.add(row);
						}
					} catch (Exception e2) {
						Debug.out(e2);
					}
				}
			} // for dataSources

			// NOTE: if the listener tries to do something like setSelected,
			// it will fail because we aren't done adding.
			// we should trigger after fillRowGaps()
			triggerListenerRowAdded(rowsAdded.toArray(new TableRowCore[0]));


			if (DEBUGADDREMOVE) {
				debug("Adding took " + (SystemTime.getCurrentTime() - lStartTime)
						+ "ms");
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Error while adding row to Table "
					+ getTableID(), e));
		} finally {
			sortedRows_mon.exit();
			dataSourceToRow_mon.exit();

			refreshenProcessDataSourcesTimer();
		}

		visibleRowsChanged();
		fillRowGaps(false);

		setSelectedRows(selectedRows);
		if (DEBUGADDREMOVE) {
			debug("<< " + size(false));
		}

	}

	// @see com.aelitis.azureus.ui.common.table.TableStructureModificationListener#cellInvalidate(com.aelitis.azureus.ui.common.table.TableColumnCore, java.lang.Object)
	public void cellInvalidate(TableColumnCore tableColumn,
			DATASOURCETYPE data_source) {
		cellInvalidate(tableColumn, data_source, true);
	}

	public void cellInvalidate(TableColumnCore tableColumn,
			final DATASOURCETYPE data_source, final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellCore cell = row.getTableCellCore(sColumnName);
				if (cell != null && cell.getDataSource() != null
						&& cell.getDataSource().equals(data_source)) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
	}

	// see common.TableView
	public void columnInvalidate(final String sColumnName) {
		TableColumnCore tc = TableColumnManager.getInstance().getTableColumnCore(
				getTableID(), sColumnName);
		if (tc != null) {
			columnInvalidate(tc, tc.getType() == TableColumnCore.TYPE_TEXT_ONLY);
		}
	}

	public void columnInvalidate(TableColumnCore tableColumn,
			final boolean bMustRefresh) {
		final String sColumnName = tableColumn.getName();

		runForAllRows(new TableGroupRowRunner() {
			public void run(TableRowCore row) {
				TableCellCore cell = row.getTableCellCore(sColumnName);
				if (cell != null) {
					cell.invalidate(bMustRefresh);
				}
			}
		});
		resetLastSortedOn();
		tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
	}

	// ITableStructureModificationListener
	// TableView
	public void columnInvalidate(TableColumnCore tableColumn) {
		// We are being called from a plugin (probably), so we must refresh
		columnInvalidate(tableColumn, true);
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getPropertiesPrefix()
	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}


	// @see com.aelitis.azureus.ui.common.table.TableView#getTableID()
	public String getTableID() {
		return tableID;
	}


	// @see com.aelitis.azureus.ui.common.table.TableView#getDataSourceType()
	public Class<?> getDataSourceType() {
		return classPluginDataSourceType;
	}

	public void tableStructureChanged(boolean columnAddedOrRemoved,
			Class forPluginDataSourceType) {
		if (forPluginDataSourceType != null
				&& !forPluginDataSourceType.equals(getDataSourceType())) {
			return;
		}
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_DESTROYED);

		DATASOURCETYPE[] unfilteredDS = (DATASOURCETYPE[]) listUnfilteredDataSources.toArray();

		if (DEBUGADDREMOVE) {
			debug("TSC: #Unfiltered=" + unfilteredDS.length);
		}
		removeAllTableRows();
		processDataSourceQueueSync();

		if (columnAddedOrRemoved) {
			tableColumns = TableColumnManager.getInstance().getAllTableColumnCoreAsArray(
					getDataSourceType(), tableID);
			ArrayList<TableColumnCore> listVisibleColumns = new ArrayList<TableColumnCore>();
			for (TableColumnCore column : tableColumns) {
				if (column.isVisible()) {
					listVisibleColumns.add(column);
				}
			}
			Collections.sort(listVisibleColumns, new Comparator<TableColumnCore>() {
				public int compare(TableColumnCore o1, TableColumnCore o2) {
					if (o1 == o2) {
						return 0;
					}
					int diff = o1.getPosition() - o2.getPosition();
					return diff;
				}
			});
			columnsOrdered = listVisibleColumns.toArray(new TableColumnCore[0]);
		}
		
		//initializeTableColumns()
		
		refreshTable(false);
		triggerLifeCycleListener(TableLifeCycleListener.EVENT_INITIALIZED);
		
		addDataSources(unfilteredDS);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableView#getTableColumn(java.lang.String)
	 */
	public org.gudy.azureus2.plugins.ui.tables.TableColumn getTableColumn(
			String sColumnName) {
		for (int i = 0; i < tableColumns.length; i++) {
			TableColumnCore tc = tableColumns[i];
			if (tc.getName().equals(sColumnName)) {
				return tc;
			}
		}
		return null;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getVisibleColumns()
	public TableColumnCore[] getVisibleColumns() {
		return columnsOrdered;
	}
	
	public TableColumnCore[] getAllColumns() {
		return tableColumns;
	}

	protected void setColumnsOrdered(TableColumnCore[] columnsOrdered) {
		this.columnsOrdered = columnsOrdered;
	}

	public boolean isColumnVisible(
			org.gudy.azureus2.plugins.ui.tables.TableColumn column) {
		return column.isVisible();
	}
	
	public void refreshTable(boolean bForceSort) {
		triggerTableRefreshListeners();
	}

	/* various selected rows functions */
	/***********************************/

	public List<Object> getSelectedDataSourcesList() {
		if (listSelectedCoreDataSources != null) {
			return listSelectedCoreDataSources;
		}
		synchronized (selectedRows) {
			if (isDisposed() || selectedRows.size() == 0) {
  			return Collections.emptyList();
  		}
  
  		final ArrayList<Object> l = new ArrayList<Object>(
  				selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				Object ds = row.getDataSource(true);
  				if (ds != null) {
  					l.add(ds);
  				}
  			}
  		}
 
  		listSelectedCoreDataSources = l;
  		return l;
		}
	}

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 * 
	 * @TODO TuxPaper: Virtual row not created when using getSelection?
	 *                  computePossibleActions isn't being calculated right
	 *                  because of non-created rows when select user selects all
	 */
	public List<Object> getSelectedPluginDataSourcesList() {
		synchronized (selectedRows) {
  		if (isDisposed() || selectedRows.size() == 0) {
  			return Collections.emptyList();
  		}
  
  		final ArrayList<Object> l = new ArrayList<Object>(selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				Object ds = row.getDataSource(false);
  				if (ds != null) {
  					l.add(ds);
  				}
  			}
  		}
  		return l;
		}
	}

	/** Returns an array of all selected Data Sources.  Null data sources are
	 * ommitted.
	 *
	 * @return an array containing the selected data sources
	 *
	 **/
	// see common.TableView
	public List<Object> getSelectedDataSources() {
		return new ArrayList<Object>(getSelectedDataSourcesList());
	}

	// see common.TableView
	public Object[] getSelectedDataSources(boolean bCoreDataSource) {
		if (bCoreDataSource) {
			return getSelectedDataSourcesList().toArray();
		}
		return getSelectedPluginDataSourcesList().toArray();
	}

	/** @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRows() */
	public TableRowCore[] getSelectedRows() {
		synchronized (selectedRows) {
			return selectedRows.toArray(new TableRowCore[0]);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getSelectedRowsSize()
	public int getSelectedRowsSize() {
		synchronized (selectedRows) {
			return selectedRows.size();
		}
	}

	/** Returns an list of all selected TableRowSWT objects.  Null data sources are
	 * ommitted.
	 *
	 * @return an list containing the selected TableRowSWT objects
	 */
	public List<TableRowCore> getSelectedRowsList() {
		synchronized (selectedRows) {
  		final ArrayList<TableRowCore> l = new ArrayList<TableRowCore>(
  				selectedRows.size());
  		for (TableRowCore row : selectedRows) {
  			if (row != null && !row.isRowDisposed()) {
  				l.add(row);
  			}
  		}
  
  		return l;
		}
	}
	
	public boolean isSelected(TableRow row) {
		synchronized (selectedRows) {
			return selectedRows.contains(row);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFocusedRow()
	public TableRowCore getFocusedRow() {
		synchronized (selectedRows) {
			if (selectedRows.size() == 0) {
				return null;
			}
			return selectedRows.get(0);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getFirstSelectedDataSource()
	public Object getFirstSelectedDataSource() {
		return getFirstSelectedDataSource(true);
	}
	
	/** Returns the first selected data sources.
	 *
	 * @return the first selected data source, or null if no data source is 
	 *         selected
	 */
	public Object getFirstSelectedDataSource(boolean bCoreObject) {
		synchronized (selectedRows) {
			if (selectedRows.size() > 0) {
				return selectedRows.get(0).getDataSource(bCoreObject);
			}
		}
		return null;
	}


	/////


	/**
	 * Invalidate and refresh whole table
	 */
	public void tableInvalidate() {
		runForAllRows(new TableGroupRowVisibilityRunner() {
			public void run(TableRowCore row, boolean bVisible) {
				row.invalidate();
				row.refresh(true, bVisible);
			}
		});
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#getHeaderVisible()
	public boolean getHeaderVisible() {
		return headerVisible;
	}

	// @see com.aelitis.azureus.ui.common.table.TableView#setHeaderVisible(boolean)
	public void setHeaderVisible(boolean visible) {
		headerVisible  = visible;
	}


	// @see org.gudy.azureus2.ui.swt.views.TableViewSWT#getSortColumn()
	public TableColumnCore getSortColumn() {
		return sortColumn;
	}

	protected boolean setSortColumn(TableColumnCore newSortColumn, boolean allowOrderChange) {
		if (newSortColumn == null) {
			return false;
		}
		sortColumn_mon.enter();
		try {
  		boolean isSameColumn = newSortColumn.equals(sortColumn);
 			if (allowOrderChange) {
  			if (!isSameColumn) {
  				sortColumn = newSortColumn;
  				
  				int iSortDirection = configMan.getIntParameter(CFG_SORTDIRECTION);
  				if (iSortDirection == 0) {
  					sortColumn.setSortAscending(true);
  				} else if (iSortDirection == 1) {
  					sortColumn.setSortAscending(false);
  				} else {
  					sortColumn.setSortAscending(!sortColumn.isSortAscending());
  				}

  				TableColumnManager.getInstance().setDefaultSortColumnName(tableID,
  						sortColumn.getName());
  			} else {
  				sortColumn.setSortAscending(!sortColumn.isSortAscending());
  			}
			} else {
				sortColumn = newSortColumn;
			}
 			if (!isSameColumn) {
				sortedRows_mon.enter();
				try {
					String name = sortColumn.getName();
					for (Iterator<TableRowCore> iter = sortedRows.iterator(); iter.hasNext();) {
						TableRowCore row = iter.next();
						row.setSortColumn(name);
					}
				} finally {
					sortedRows_mon.exit();
				}
 			}
 			uiChangeColumnIndicator();
 			resetLastSortedOn();
 			sortColumn(!isSameColumn);
			return !isSameColumn;
		} finally {
			sortColumn_mon.exit();
		}
	}

	public void setRowSelected(final TableRowCore row, boolean selected, boolean trigger) {
		if (row == null || row.isRowDisposed()) {
			return;
		}
		synchronized (selectedRows) {
			if (selected) {
    		if (selectedRows.contains(row)) {
    			return;
    		}
    		selectedRows.add(row);
			} else {
				if (!selectedRows.remove(row)) {
					return;
				}
			}
  		
  		listSelectedCoreDataSources = null;
		}
		
		if (trigger) {
			triggerSelectionListeners(new TableRowCore[] { row });
			triggerTabViewsDataSourceChanged(false);
		}
	}

	public void setSelectedRows(final TableRowCore[] newSelectionArray,
			final boolean trigger) {
		if (isDisposed()) {
			return;
		}

		/**
		System.out.print(newSelectionArray.length + " Selected Rows: ");
		for (TableRowCore row : newSelectionArray) {
			System.out.print(indexOf(row));
			System.out.print(", ");
		}
		System.out.println(" via " + Debug.getCompressedStackTrace(4));
		/**/

		final List<TableRowCore> oldSelectionList = new ArrayList<TableRowCore>();

		List<TableRowCore> listNewlySelected;
		boolean somethingChanged;
		synchronized (selectedRows) {
			oldSelectionList.addAll(selectedRows);
			listSelectedCoreDataSources = null;
			selectedRows.clear();

			listNewlySelected = new ArrayList<TableRowCore>(1);

			// We'll remove items still selected from oldSelectionLeft, leaving
			// it with a list of items that need to fire the deselection event.
			for (TableRowCore row : newSelectionArray) {
				if (row == null || row.isRowDisposed()) {
					continue;
				}

				boolean existed = false;
				for (TableRowCore oldRow : oldSelectionList) {
					if (oldRow == row) {
						existed = true;
						selectedRows.add(row);
						oldSelectionList.remove(row);
						break;
					}
				}
				if (!existed) {
					selectedRows.add(row);
					listNewlySelected.add(row);
				}
			}

			somethingChanged = listNewlySelected.size() > 0
					|| oldSelectionList.size() > 0;
			if (DEBUG_SELECTION) {
				System.out.println(somethingChanged + "] +"
						+ listNewlySelected.size() + "/-" + oldSelectionList.size()
						+ ";  UpdateSelectedRows via " + Debug.getCompressedStackTrace());
			}
		}
		
		if (somethingChanged) {
			uiSelectionChanged(listNewlySelected.toArray(new TableRowCore[0]), oldSelectionList.toArray(new TableRowCore[0]));
		}

		if (trigger && somethingChanged) {
			if (listNewlySelected.size() > 0) {
				triggerSelectionListeners(listNewlySelected.toArray(new TableRowCore[0]));
			}
			if (oldSelectionList.size() > 0) {
				triggerDeselectionListeners(oldSelectionList.toArray(new TableRowCore[0]));
			}

			triggerTabViewsDataSourceChanged(false);
		}

	}
	
	public abstract void uiSelectionChanged(TableRowCore[] newlySelectedRows, TableRowCore[] deselectedRows);

	// @see com.aelitis.azureus.ui.common.table.TableView#setSelectedRows(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void setSelectedRows(TableRowCore[] rows) {
		setSelectedRows(rows, true);
	}

	public void selectAll() {
		setSelectedRows(getRows(), true);
	}

	public String getFilterText() {
		return filter == null ? "" : filter.text;
	}

	public boolean isMenuEnabled() {
		return menuEnabled;
	}

	public void setMenuEnabled(boolean menuEnabled) {
		this.menuEnabled = menuEnabled;
	}

	protected boolean isLastRow(TableRowCore row) {
		sortedRows_mon.enter();
		try {
			int size = sortedRows.size();
			return size == 0 ? false : sortedRows.get(size - 1) == row;
		} finally {
			sortedRows_mon.exit();
		}
	}

	public abstract void triggerTabViewsDataSourceChanged(boolean sendParent);

	protected abstract void uiChangeColumnIndicator();
}