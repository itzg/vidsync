package me.itzgeoff.vidsync.server;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class PathsToScanPanel extends JPanel {

	private File previousDirectory;
	private JList<File> pathList;
	private DefaultListModel<File> pathDataModel;

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
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JButton btnAdd = new JButton("Add...");
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAdd();
			}
		});
		panel.add(btnAdd);

	}

	private JList createPathList() {
		if (pathList == null) {
			pathDataModel = ConfigFactory.createPathDataModel();
			pathList = new JList<>(pathDataModel);
		}

		return pathList;
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

}
