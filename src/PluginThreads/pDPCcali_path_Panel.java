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
package PluginThreads;

import com.google.gson.Gson;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;


public class pDPCcali_path_Panel extends javax.swing.JPanel {

    private boolean initialized = false;

///////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) {

        JFrame frame = new JFrame();

        pDPCcali_path_Panel img_Panel = new pDPCcali_path_Panel();

        frame.add(img_Panel);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    System.out.println(img_Panel.get_cali_paths());
                    img_Panel.onPanelExit();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }
        });
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> get_cali_paths() {
        Gson gson = new Gson();

        String[] STR_cali_paths = {
            "quad1_only:" + gson.toJson(tf_quad1_only_path.getText()),
            "quad2_only:" + gson.toJson(tf_quad2_only_path.getText()),
            "quad3_only:" + gson.toJson(tf_quad3_only_path.getText()),
            "quad4_only:" + gson.toJson(tf_quad4_only_path.getText()),
            "quad_only_dark_bkg:" + gson.toJson(tf_darkbkg_quadonly_path.getText()),
            "light_bkg:" + gson.toJson(tf_lightbkg_path.getText()),
            "dark_bkg:" + gson.toJson(tf_darkbkg_path.getText())
        };

        ArrayList<String> paths_str = new ArrayList<String>(
                Arrays.asList(STR_cali_paths)
        );

        return paths_str;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void onClick_bt_cali_path(JTextField tf_cali_path) {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        filechooser.setDialogType(JFileChooser.OPEN_DIALOG);
        FileNameExtensionFilter img_filter = new FileNameExtensionFilter("Tiff file (*.tif, *.tiff, *.ome.tif)", "tif", "tiff", "ome.tif");
        filechooser.addChoosableFileFilter(img_filter);
        filechooser.setFileFilter(img_filter);

        String currentFilepath = tf_cali_path.getText();
        File currentFile = new File("".equals(currentFilepath) ? System.getProperty("user.home") : currentFilepath);
        filechooser.setSelectedFile(currentFile);
        int retVal = filechooser.showOpenDialog(this);

        if (retVal == JFileChooser.APPROVE_OPTION) {
            String path = filechooser.getSelectedFile().getAbsolutePath();
            tf_cali_path.setText(path);
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Creates new form pDPCcali_path_Panel
     */
    public pDPCcali_path_Panel() {
        onPanelStart();
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void onPanelStart() {
        if (!initialized) {
            initComponents();
            initialized = true;
        }
    }

    public void onPanelExit() {
        initialized = false;
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tf_quad1_only_path = new javax.swing.JTextField();
        tf_quad2_only_path = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        tf_quad3_only_path = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        tf_quad4_only_path = new javax.swing.JTextField();
        tf_lightbkg_path = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        bt_lightbkg_path = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        tf_darkbkg_quadonly_path = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        bt_quad1_only_path = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        bt_quad2_only_path = new javax.swing.JButton();
        tf_darkbkg_path = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        bt_quad3_only_path = new javax.swing.JButton();
        bt_darkbkg_path = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        bt_quad4_only_path = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        bt_darkbkg_quadonly_path = new javax.swing.JButton();

        tf_quad1_only_path.setPreferredSize(new java.awt.Dimension(356, 22));

        tf_quad2_only_path.setPreferredSize(new java.awt.Dimension(356, 22));

        jLabel11.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jLabel11.setText("All quadrants raw:");

        tf_quad3_only_path.setPreferredSize(new java.awt.Dimension(356, 22));

        jLabel12.setText("light_bkg:");

        tf_quad4_only_path.setPreferredSize(new java.awt.Dimension(356, 22));

        tf_lightbkg_path.setPreferredSize(new java.awt.Dimension(356, 22));

        jLabel10.setText("dark_bkg:");

        bt_lightbkg_path.setText("...");
        bt_lightbkg_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_lightbkg_pathActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jLabel2.setText("Quadrant-only raw:");

        tf_darkbkg_quadonly_path.setPreferredSize(new java.awt.Dimension(356, 22));

        jLabel5.setText("Quad1_Only:");

        bt_quad1_only_path.setText("...");
        bt_quad1_only_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_quad1_only_pathActionPerformed(evt);
            }
        });

        jLabel13.setText("dark_bkg:");

        bt_quad2_only_path.setText("...");
        bt_quad2_only_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_quad2_only_pathActionPerformed(evt);
            }
        });

        tf_darkbkg_path.setPreferredSize(new java.awt.Dimension(356, 22));

        jLabel7.setText("Quad2_Only:");

        bt_quad3_only_path.setText("...");
        bt_quad3_only_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_quad3_only_pathActionPerformed(evt);
            }
        });

        bt_darkbkg_path.setText("...");
        bt_darkbkg_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_darkbkg_pathActionPerformed(evt);
            }
        });

        jLabel8.setText("Quad3_Only:");

        bt_quad4_only_path.setText("...");
        bt_quad4_only_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_quad4_only_pathActionPerformed(evt);
            }
        });

        jLabel9.setText("Quad4_Only:");

        bt_darkbkg_quadonly_path.setText("...");
        bt_darkbkg_quadonly_path.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_darkbkg_quadonly_pathActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel11)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10)
                            .addComponent(jLabel12)
                            .addComponent(jLabel13))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tf_quad1_only_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_quad2_only_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_quad3_only_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_quad4_only_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_lightbkg_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_darkbkg_quadonly_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(tf_darkbkg_path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bt_lightbkg_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_quad1_only_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_quad2_only_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_quad3_only_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_darkbkg_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_quad4_only_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bt_darkbkg_quadonly_path, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(tf_quad1_only_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_quad1_only_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(tf_quad2_only_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_quad2_only_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(tf_quad3_only_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_quad3_only_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(tf_quad4_only_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_quad4_only_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(tf_darkbkg_quadonly_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_darkbkg_quadonly_path))
                .addGap(18, 18, 18)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(tf_lightbkg_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_lightbkg_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(tf_darkbkg_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_darkbkg_path))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void bt_quad1_only_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_quad1_only_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_quad1_only_path.getText())) {
            onClick_bt_cali_path(tf_quad1_only_path);
        }
    }//GEN-LAST:event_bt_quad1_only_pathActionPerformed

    private void bt_quad2_only_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_quad2_only_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_quad2_only_path.getText())) {
            onClick_bt_cali_path(tf_quad2_only_path);
        }
    }//GEN-LAST:event_bt_quad2_only_pathActionPerformed

    private void bt_quad3_only_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_quad3_only_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_quad3_only_path.getText())) {
            onClick_bt_cali_path(tf_quad3_only_path);
        }
    }//GEN-LAST:event_bt_quad3_only_pathActionPerformed

    private void bt_quad4_only_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_quad4_only_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_quad4_only_path.getText())) {
            onClick_bt_cali_path(tf_quad4_only_path);
        }
    }//GEN-LAST:event_bt_quad4_only_pathActionPerformed

    private void bt_darkbkg_quadonly_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_darkbkg_quadonly_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_darkbkg_quadonly_path.getText())) {
            onClick_bt_cali_path(tf_darkbkg_quadonly_path);
        }
    }//GEN-LAST:event_bt_darkbkg_quadonly_pathActionPerformed

    private void bt_lightbkg_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_lightbkg_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_lightbkg_path.getText())) {
            onClick_bt_cali_path(tf_lightbkg_path);
        }
    }//GEN-LAST:event_bt_lightbkg_pathActionPerformed

    private void bt_darkbkg_pathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_darkbkg_pathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_darkbkg_path.getText())) {
            onClick_bt_cali_path(tf_darkbkg_path);
        }
    }//GEN-LAST:event_bt_darkbkg_pathActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_darkbkg_path;
    private javax.swing.JButton bt_darkbkg_quadonly_path;
    private javax.swing.JButton bt_lightbkg_path;
    private javax.swing.JButton bt_quad1_only_path;
    private javax.swing.JButton bt_quad2_only_path;
    private javax.swing.JButton bt_quad3_only_path;
    private javax.swing.JButton bt_quad4_only_path;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField tf_darkbkg_path;
    private javax.swing.JTextField tf_darkbkg_quadonly_path;
    private javax.swing.JTextField tf_lightbkg_path;
    private javax.swing.JTextField tf_quad1_only_path;
    private javax.swing.JTextField tf_quad2_only_path;
    private javax.swing.JTextField tf_quad3_only_path;
    private javax.swing.JTextField tf_quad4_only_path;
    // End of variables declaration//GEN-END:variables
}
