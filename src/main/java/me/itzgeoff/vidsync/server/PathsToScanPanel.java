package me.itzgeoff.vidsync.server;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class PathsToScanPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private File previousDirectory;
	private JList<File> pathList;
	private DefaultListModel<File> pathDataModel;
	private JButton btnRemove;

	/**
	 * Create the panel.
	 */
	public PathsToScanPanel() {
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		pathList = createPathList();
		pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(pathList);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.EAST);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{71, 0};
		gbl_panel.rowHeights = new int[]{23, 23, 0};
		gbl_panel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JButton btnAdd = new JButton("Add...");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAdd();
			}
		});
		GridBagConstraints gbc_btnAdd = new GridBagConstraints();
		gbc_btnAdd.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnAdd.gridx = 0;
		gbc_btnAdd.gridy = 0;
		panel.add(btnAdd, gbc_btnAdd);
		
		btnRemove = new JButton("Remove");
		btnRemove.setEnabled(false);
		btnRemove.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        doRemove();
		    }
		});
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemove.gridx = 0;
		gbc_btnRemove.gridy = 1;
		panel.add(btnRemove, gbc_btnRemove);

	}

	protected void doRemove() {
        File selectedValue = pathList.getSelectedValue();
        pathDataModel.removeElement(selectedValue);
    }

    private JList createPathList() {
		if (pathList == null) {
			pathDataModel = ConfigFactory.createPathDataModel();
			pathList = new JList<>(pathDataModel);
			pathList.addListSelectionListener(new ListSelectionListener() {
			    public void valueChanged(ListSelectionEvent e) {
			        if (pathList.getMinSelectionIndex() < 0) {
			            doPathSelected(false);
			        }
			        else {
			            doPathSelected(true);
			        }
			    }
			});
		}

		return pathList;
	}

	protected void doPathSelected(boolean b) {
	    getBtnRemove().setEnabled(b);
    }

    protected void doAdd() {
		JFileChooser chooser = new JFileChooser(previousDirectory);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			previousDirectory = selectedFile.getParentFile();
			if (!ConfigFactory.hasFilesToScan(selectedFile)) {
				int confirmResult = JOptionPane.showConfirmDialog(this, "The selected directory doesn't contain any" +
						" video files to scan. Are you sure you want to use this?", "Are you sure?", JOptionPane.YES_NO_OPTION);
				if (confirmResult != JOptionPane.YES_OPTION) {
					return;
				}
			}
			pathDataModel.addElement(selectedFile);
		}
	}

    protected JButton getBtnRemove() {
        return btnRemove;
    }
}
