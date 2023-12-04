/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package MM2Plugin_pDPC;

import Paras_Presets.Preset_Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import org.micromanager.Studio;

/**
 *
 * @author h.liu
 */
public class mainPluginFrame extends javax.swing.JFrame {

    private static mainPluginFrame frame_;
    public static Studio gui_ = null;
    public static CMMCore core_ = null;

    private boolean initialize = false;

    private String thisPluginName = "MM2_pDPC";
    private String extThread_pyfilename = "pDPC.py";
    private String extThread_presetfilename = Preset_Panel.resource_preset_filename;
    private String extThread_historyfilename = "history";

    private String thisPluginFolder = System.getProperty("user.dir")
            + System.getProperty("file.separator")
            + thisPluginName;
    private String extThread_pyfilepath = thisPluginFolder
            + System.getProperty("file.separator")
            + extThread_pyfilename;
    private String extThread_presetfilepath = thisPluginFolder
            + System.getProperty("file.separator")
            + extThread_presetfilename;
    private String extThread_historyfilepath = thisPluginFolder
            + System.getProperty("file.separator")
            + extThread_historyfilename;

    public static mainPluginFrame getInstance(Studio gui_ref) {
        if (frame_ == null) {
            synchronized (mainPluginFrame.class) {
                if (frame_ == null) {
                    frame_ = new mainPluginFrame(gui_ref);
                }
            }
        } else {
            frame_.onFrameStart(gui_ref);
        }
        return frame_;
    }

    private void prepare_files() {
        // copy whatever need in resources to thisPluginFolder
        File thisPluginFolderfile = new File(thisPluginFolder);
        if (!thisPluginFolderfile.exists()) {
            try {
                Files.createDirectory(thisPluginFolderfile.toPath());
            } catch (IOException ex) {
                Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (thisPluginFolderfile.exists()) {
            File pyfile = new File(extThread_pyfilepath);
            if (!pyfile.exists()) {
                try {
                    InputStream stream = this.getClass().getResourceAsStream("/Resources/" + extThread_pyfilename);
                    Files.copy(stream, pyfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stream.close();
                } catch (IOException ex) {
                    System.out.println(ex.toString());
                    Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            File presetfile = new File(extThread_presetfilepath);
            if (!presetfile.exists()) {
                try {
                    InputStream stream = this.getClass().getResourceAsStream("/Resources/" + extThread_presetfilename);
                    Files.copy(stream, presetfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stream.close();
                } catch (IOException ex) {
                    System.out.println(ex.toString());
                    Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            File historyfile = new File(extThread_historyfilepath);
            if (!historyfile.exists()) {
                try {
                    InputStream stream = this.getClass().getResourceAsStream("/Resources/" + extThread_historyfilename);
                    Files.copy(stream, historyfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stream.close();
                } catch (IOException ex) {
                    System.out.println(ex.toString());
                    Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    public void onFrameStart(Studio gui_ref) {
        if (!initialize) {
            prepare_files();
            initComponents();
            pDPC_ExtThread_Pane.onPanelStart();

            pDPC_ExtThread_Pane.setpluginfolder(thisPluginFolder);
            pDPC_ExtThread_Pane.setpyfilepath(extThread_pyfilepath);
            pDPC_ExtThread_Pane.setpresetfilepath(extThread_presetfilepath);
            pDPC_ExtThread_Pane.sethistoryfilepath(extThread_historyfilepath);

            pDPC_ExtThread_Pane.set_parent(gui_);
        }
        initialize = true;
    }

    public void onFrameExit() {
        pDPC_ExtThread_Pane.onPanelExit();
        initialize = false;
    }

    /**
     * Creates new form mainPluginFrame
     */
    public mainPluginFrame(Studio gui_ref) {
        gui_ = gui_ref;
        core_ = gui_.getCMMCore();

        frame_ = this;
        frame_.setTitle(thisPluginName);

        frame_.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame_, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    onFrameExit();// stop live thread and turn off command running process
                    frame_.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                } else {
                    frame_.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        onFrameStart(gui_ref);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pDPC_ExtThread_Pane = new PluginThreads.pDPC_ExtThread_Panel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(pDPC_ExtThread_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(1, 1, 1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(pDPC_ExtThread_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(1, 1, 1))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(mainPluginFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(mainPluginFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(mainPluginFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(mainPluginFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new mainPluginFrame(gui_).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private PluginThreads.pDPC_ExtThread_Panel pDPC_ExtThread_Pane;
    // End of variables declaration//GEN-END:variables
}
