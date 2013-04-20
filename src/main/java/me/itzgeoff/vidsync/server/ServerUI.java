package me.itzgeoff.vidsync.server;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUI {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerUI.class);
    
    public static void main(String[] args) {
        ServerMain.initLogging();
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            logger.error("Failed to setup L&F: {}", e.getMessage());
            System.exit(1);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createUI();
                ServerMain.start();
            }
        });
    }

    private static void createUI() {
        if (!SystemTray.isSupported()) {
            logger.error("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(createImage("/SysTrayIcon.png"), "Vidsync Server");
        final SystemTray tray = SystemTray.getSystemTray();
        
        MenuItem configItem = new MenuItem("Configure");
        MenuItem exitItem = new MenuItem("Exit");
        
        popup.add(configItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon.setPopupMenu(popup);
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("Unable to add system tray icon: {}", e.getMessage());
            System.exit(1);
        }
        
        configItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ConfigUI().setVisible(true);
            }
        });
        
        exitItem.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);
            }
        });
    }

    private static Image createImage(String resourcePath) {
        return new ImageIcon(ServerUI.class.getResource(resourcePath)).getImage();
    }
}
