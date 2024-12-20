/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Imperial College London 
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package MM2Plugin_pDPC;

import Paras_Presets.Preset_Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import org.micromanager.Studio;


public class mainPluginFrame extends javax.swing.JFrame {

    private static mainPluginFrame frame_;
    public static Studio gui_ = null;
    public static CMMCore core_ = null;

    private boolean initialize = false;

    private String thisPluginName = "MM2_pDPC";
    private String thisPluginFolder = System.getProperty("user.dir")
            + System.getProperty("file.separator")
            + thisPluginName;

    private String[] filenames_needed_from_resource = new String[]{
        "pDPC.py", Preset_Panel.resource_preset_filename, "history" 
    };
    // the first three must be main_pyfilename, presetfilename, and historyfilename, in order;

////////////////////////////////////////////////////////////////////////////////
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
        //create thisPluginFolder if needed
        File thisPluginFolderfile = new File(thisPluginFolder);
        if (!thisPluginFolderfile.exists()) {
            try {
                Files.createDirectory(thisPluginFolderfile.toPath());
            } catch (IOException ex) {
                Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // copy everything needed inside resources to thisPluginFolder if not exist already
        if (thisPluginFolderfile.exists()) {
            for (String filename : filenames_needed_from_resource) {
                String filepath = get_extThread_filepath(filename);
                File file = new File(filepath);
                if (!file.exists()) {
                    try {
                        InputStream stream = this.getClass().getResourceAsStream("/Resources/" + filename);
                        Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        stream.close();
                    } catch (IOException ex) {
                        System.out.println(ex.toString());
                        Logger.getLogger(mainPluginFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
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

            pDPC_ExtThread_Pane.setpyfilepath(get_extThread_filepath(filenames_needed_from_resource[0]));
            pDPC_ExtThread_Pane.setpresetfilepath(get_extThread_filepath(filenames_needed_from_resource[1]));
            pDPC_ExtThread_Pane.sethistoryfilepath(get_extThread_filepath(filenames_needed_from_resource[2]));

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

////////////////////////////////////////////////////////////////////////////////
    private String get_extThread_filepath(String filename) {
        return thisPluginFolder
                + System.getProperty("file.separator")
                + filename;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ScrollPane = new javax.swing.JScrollPane();
        pDPC_ExtThread_Pane = new PluginThreads.pDPC_ExtThread_Panel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setSize(new java.awt.Dimension(0, 0));

        ScrollPane.setViewportView(pDPC_ExtThread_Pane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ScrollPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ScrollPane)
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
    private javax.swing.JScrollPane ScrollPane;
    private PluginThreads.pDPC_ExtThread_Panel pDPC_ExtThread_Pane;
    // End of variables declaration//GEN-END:variables
}
