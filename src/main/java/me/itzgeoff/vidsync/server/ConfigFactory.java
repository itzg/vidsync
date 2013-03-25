package me.itzgeoff.vidsync.server;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigFactory {

	private static final VideoFileFilter VIDEO_FILE_FILTER = new VideoFileFilter();

	protected static final String PREF_PATHS = "paths";
	
	/**
	 * @wbp.factory
	 */
	public static PathsToScanPanel createPathsToScanPanel() {
		PathsToScanPanel pathsToScanPanel = new PathsToScanPanel();
		return pathsToScanPanel;
	}

	public static DefaultListModel<File> createPathDataModel() {
		final Preferences serverPrefs = createServerPreferences();
		
		DefaultListModel<File> model = new DefaultListModel<>();

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			@SuppressWarnings("unchecked")
			ArrayList<String> savedFilePaths = objectMapper.readValue(serverPrefs.get(PREF_PATHS, "[]"), ArrayList.class);
			for (String path : savedFilePaths) {
				model.addElement(new File(path));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		model.addListDataListener(new ListDataListener() {
			
			@Override
			public void intervalRemoved(ListDataEvent e) {
				contentsChanged(e);
			}
			@Override
			public void intervalAdded(ListDataEvent e) {
				contentsChanged(e);
			}
			
			@Override
			public void contentsChanged(ListDataEvent e) {
				@SuppressWarnings("unchecked")
				ListModel<File> source = (ListModel<File>) e.getSource();

				JsonFactory jsonFactory = new JsonFactory();
				StringWriter prefValue = new StringWriter();
				try {
					JsonGenerator jsonGen = jsonFactory.createJsonGenerator(prefValue);
					jsonGen.writeStartArray();
					for (int i = 0; i < source.getSize(); ++i) {
						jsonGen.writeString(source.getElementAt(i).getAbsolutePath());
					}
					jsonGen.writeEndArray();
					jsonGen.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				serverPrefs.put(PREF_PATHS, prefValue.toString());
			}
		});
		return model;
	}

	static Preferences createServerPreferences() {
		return Preferences.userNodeForPackage(ConfigFactory.class);
	}

	public static boolean hasFilesToScan(File directory) {
		File[] contents = directory.listFiles(VIDEO_FILE_FILTER);

		return contents != null && contents.length > 0;
	}
}
