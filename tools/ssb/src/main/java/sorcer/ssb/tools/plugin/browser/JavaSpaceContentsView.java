/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.ssb.tools.plugin.browser;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;

/**
 * 
 * @author dev
 * @version
 */

public class JavaSpaceContentsView extends JPanel implements Runnable {

	/*
	 * Reference to the JavaSpaceAdmin proxy
	 */
	private JavaSpace05 javaSpace;
	/*
	 * inner class that extends AbstractTableModel
	 */
	private EntryCountTableModel entryCountTableModel = new EntryCountTableModel();
	/*
	 * refreshRate how often to poll the admin proxy.
	 */
	private long refreshRate = 5000;
	/*
	 * paused if setValue by the GUI then polling is suspended
	 */
	private boolean paused = false;

	private final String UNUSABLE_ENTRY = "<unusable entry>";

	// private static int maxEntries=100;

	private Thread _updater;
	private JEditorPane htmlView = new JEditorPane();
	private JScrollPane introView = new JScrollPane(htmlView);
	private boolean firstTime = true;
	private Frame frame;
	private ActionListener updateAction;
	private boolean entriesDeleted;
	private ServiceBrowserUI _sbui;

	/*
	 * Construct the OutriggerView
	 */
	public JavaSpaceContentsView(Frame f, JavaSpace05 jspace,
			ServiceBrowserUI sbui) {
		javaSpace = jspace;
		frame = f;
		_sbui = sbui;
		initUI();
	}

	/**
	 * Initialize the GUI and start the monitor thread which polls the
	 * JavaSpaceAdmin for the entry iterator
	 */
	private void initUI() {
		setLayout(new BorderLayout());

		try {
			htmlView.setEditable(false);
			htmlView.setPage(getClass().getClassLoader().getResource(
					"html/outrigger-admin.html"));
			add(introView, BorderLayout.CENTER);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		add(createControls(), BorderLayout.SOUTH);
		/*
		 * SwingUtilities.invokeLater( new Runnable(){ public void run(){ try{
		 * doUpdates();
		 * 
		 * }catch(Exception ex){ ex.printStackTrace(); } } });
		 */
		// kick off the monitor thread
		// new Thread(this).start();
	}

	/*
	 * public void addNotify(){ super.addNotify(); if(_updater!=null){
	 * _updater.interrupt(); } _updater=new Thread(this); _updater.start(); }
	 */
	public void removeNotify() {
		super.removeNotify();
		if (_updater != null) {
			_updater.interrupt();
		}
	}

	/*
	 * Poll the JavaSpaceAdmin proxy every <code>refreshRate</code> millis If we
	 * getValue an Exception, drop out of the loop and report it The most likely
	 * cause of an Exception occuring will be down to a retrieved entry not
	 * having a valid codebase set for it
	 * 
	 * If the client application that is writing the args to the space is
	 * being run from within the SSB environment, makes sure you have set a
	 * codebase alias, but right clicking on the project icon and selecting
	 * "Add alias to webserver" and then selecting "Yes" when prompted.
	 */
	public void run() {
		System.out.println("Starting JavaSpace05 reader thread");

		while (!_updater.isInterrupted()) {
			try {

				doUpdates();

				Thread.sleep(refreshRate);

			} catch (InterruptedException ex) {
				System.out.println("Stopping JavaSpace05 reader thread");
				_updater = null;
				return;
			} catch (Exception ex) {
				// drop out here if we getValue an exception,
				// likely causes are UnusableEntryExceptions
				// because the client program that wrote the entry
				// didn't set a codebase (or set a wrong one) or the HTTPD of
				// the actual
				// codebase isn't working
				// JOptionPane.showMessageDialog(this,ex);
				ex.printStackTrace();
				_updater = null;
				return;
			}
		}

	}

	/**
	 * Request all the args currently in the space.
	 * 
	 * @throws TransactionException
	 *             ,RemoteException,UnusableEntryException
	 */
	private void doUpdates() throws TransactionException,
			UnusableEntryException, RemoteException {

		if (firstTime) {
			firstTime = false;
			JTable table = new JTable(entryCountTableModel);
			JScrollPane scrollPane = new JScrollPane(table);
			remove(introView);
			add(scrollPane, BorderLayout.CENTER);
			invalidate();
			getParent().validate();
			addMouseListener(table);

		}
		// here we're asking for all the args in the space
		// you could modify this sample GUI to allow user input of an entry
		// class key
		// AdminIterator iter=javaSpaceAdmin.contents(null/*Entry
		// template*/,null/*Transaction*/);
		ArrayList tmplist = new ArrayList();
		tmplist.add(null);
		MatchSet iter = javaSpace.contents(tmplist, null, 5L * 60000L,
				Integer.MAX_VALUE);

		List list = new ArrayList();
		Map entryMap = new HashMap();
		int ueCount = 0;
		int got = 0;
		while (iter != null && true) {
			try {
				Entry e = iter.next();
				if (e == null)/* || got>=maxEntries) */{
					break;
				}
				got++;
				Class entryClass = e.getClass();

				String entryClassName = entryClass.getName();

				Object[] data = (Object[]) entryMap.get(entryClassName);
				if (data == null) {
					Object template = null;
					try {
						template = entryClass.newInstance();

					} catch (Exception ex) {
						System.out.println(ex);
					}
					entryMap.put(entryClassName, new Object[] { entryClassName,
							new Integer(1), template });
				} else {
					Integer count = (Integer) data[1];
					data[1] = new Integer(count.intValue() + 1);
				}

			} catch (UnusableEntryException uee) {
				// add as UnusableEnrty
				uee.printStackTrace();
				ueCount++;
				// throw uee;
			}
		}

		try {
			if (iter != null) {
				Lease lease = iter.getLease();
				if (lease != null) {
					lease.cancel();
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println("JavaSpace05 contents viewer retreived " + got
				+ " args");

		if (ueCount > 0) {
			entryMap.put(UNUSABLE_ENTRY, new Object[] { UNUSABLE_ENTRY,
					new Integer(ueCount), null });
		}
		// getValue the object counts
		Collection col = entryMap.values();
		List counterList = new ArrayList();
		counterList.addAll(col);

		entryCountTableModel.update(counterList);
	}

	/*
	 * Create the buttons that getValue displayed at the bottom of the GUI
	 */
	private JComponent createControls() {
		final JPanel panel = new JPanel();
		// final JTextField max=new JTextField(5);
		// max.setText("100   ");
		final JButton auto = new JButton("Auto refresh");
		final JButton update = new JButton("Get args");
		updateAction = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				// maxEntries=Integer.parseInt(max.getText().trim());
				Thread t = new Thread(_sbui.wrap(new Runnable() {
					public void run() {
						try {
							update.setEnabled(false);
							auto.setEnabled(false);

							ArrayList tmp = new ArrayList();
							tmp.add(new Object[] { "Loading...", "" });
							entryCountTableModel.update(tmp);
							doUpdates();

						} catch (Exception ex) {
							entryCountTableModel.update(new ArrayList());
							JOptionPane.showMessageDialog(panel, ex);
						} finally {
							update.setEnabled(true);
							update.setText("  Refresh   ");
							auto.setEnabled(true);
						}
					}
				}));
				t.start();

			}
		};
		update.addActionListener(updateAction);

		auto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (_updater != null) {
					_updater.interrupt();
					auto.setText("Auto refresh");
					update.setEnabled(true);
				} else {
					update.setEnabled(false);
					auto.setText("    Stop    ");
					// maxEntries=Integer.parseInt(max.getText().trim());
					_updater = new Thread(_sbui
							.wrap(JavaSpaceContentsView.this));
					_updater.start();
				}
			}
		});

		// panel.add(new JLabel("Max args"));
		// panel.add(max);
		panel.add(update);
		panel.add(auto);
		return panel;
	}

	/*
	 * Table model for the entry data retrieved in doUpdate()
	 */
	private class EntryCountTableModel extends AbstractTableModel {
		private List _data = new ArrayList();

		private String[] _cols = new String[] { "Entry Type", "Instance Count" };

		public int getRowCount() {
			return _data.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public String getColumnName(int col) {
			return _cols[col];
		}

		public Object getValueAt(int r, int c) {
			Object[] rowData = (Object[]) _data.get(r);

			return rowData[c];
		}

		public void update(List data) {
			_data = data;
			fireTableDataChanged();
		}
	}

	private void addMouseListener(final JTable table) {
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() != 2) {
					return;
				}
				int sel = table.getSelectedRow();
				// disable is running updates
				if (sel == -1 || _updater != null) {
					return;
				}
				showEntryBrowser(sel);
			}
		});
	}

	private void showEntryBrowser(int row) {
		try {
			Object tmpl = entryCountTableModel.getValueAt(row, 2);

			if (tmpl == null) {
				return;// unusable entry count
			}
			ArrayList list = new ArrayList();
			list.add(tmpl);
			final MatchSet iter = javaSpace.contents(list, null, 15L * 60000L,
					Integer.MAX_VALUE);

			entriesDeleted = false;

			final JDialog dlg = new JDialog(frame, tmpl.getClass().getName(),
					true);
			final WindowListener wl = new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					try {
						// System.out.println("Closing admin iterator");
						try {
							iter.getLease().cancel();
						} catch (Throwable t) {
							t.printStackTrace();
						}
						dlg.dispose();
						if (entriesDeleted) {
							updateAction.actionPerformed(null);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};
			dlg.addWindowListener(wl);

			JComponent view = createEntryPanel(iter, wl);

			dlg.getContentPane().add(view, BorderLayout.CENTER);
			dlg.setSize(400, 300);
			OutriggerViewer.centreDialog(dlg, frame);
			dlg.setVisible(true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex);
		}
	}

	private class EntryPropsTable extends AbstractTableModel {

		private ArrayList _data = new ArrayList();

		EntryPropsTable(Object entry) {
			parseEntry(entry);
		}

		private String[] _cols = new String[] { "Type", "Tag", "Value" };

		public int getRowCount() {
			return _data.size();
		}

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int col) {
			return _cols[col];
		}

		public Object getValueAt(int r, int c) {
			Object[] rowData = (Object[]) _data.get(r);

			return rowData[c];
		}

		public void update(Object nextEntry) {
			_data = new ArrayList();
			parseEntry(nextEntry);
			fireTableDataChanged();
		}

		private void parseEntry(Object entry) {
			try {
				Class ec = entry.getClass();
				Field[] f = ec.getFields();
				for (int i = 0; i < f.length; i++) {
					Object[] fData = { f[i].getType().getName(),
							f[i].getName(), f[i].get(entry) };
					_data.add(fData);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void clear() {
			_data = new ArrayList();

			fireTableDataChanged();
		}
	}

	private JComponent createEntryPanel(final MatchSet iter,
			final WindowListener wl) throws Exception {
		final JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout());

		final EntryPropsTable model = new EntryPropsTable(iter.next());

		JTable jt = new JTable(model);
		jt.setToolTipText("Click a listing to view entry values");
		jp.add(new JScrollPane(jt), BorderLayout.CENTER);
		// add ctrls
		final JButton next = new JButton(" Next ");
		final JButton del = new JButton("Delete");
		final JButton close = new JButton("Close ");
		final ActionListener nextAl = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				try {

					Object nextEntry = iter.next();
					if (nextEntry == null) {
						next.setEnabled(false);
						del.setEnabled(false);
						model.clear();
					} else {
						model.update(nextEntry);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		next.addActionListener(nextAl);
		/*
		 * del.addActionListener( new ActionListener(){ public void
		 * actionPerformed(ActionEvent evt){ try{ int
		 * ok=JOptionPane.showConfirmDialog
		 * (jp,"Are you sure you want to delete this entry?" ,"Delete Entry",
		 * JOptionPane.YES_NO_OPTION); if(ok==JOptionPane.YES_OPTION){
		 * iter.delete(); entriesDeleted=true; } nextAl.actionPerformed(null);
		 * 
		 * }catch(Exception ex){ JOptionPane.showMessageDialog(jp,ex); } } });
		 */
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				wl.windowClosing(null);
			}
		});
		JPanel ctrls = new JPanel();

		// ctrls.add(del);
		ctrls.add(next);
		ctrls.add(close);
		jp.add(ctrls, BorderLayout.SOUTH);
		return jp;
	}

}
