/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package PluginThreads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.micromanager.Studio;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.data.Datastore;
import org.micromanager.internal.MMStudio;

/**
 *
 * @author localuser
 */
public class pDPCrecon_path_Panel extends javax.swing.JPanel {

    public static void main(String[] args) {
        MMStudio gui = new MMStudio(false);

        JFrame frame = new JFrame();

        pDPCrecon_path_Panel img_Panel = new pDPCrecon_path_Panel();

        frame.add(img_Panel);
        frame.pack();

        img_Panel.set_parent(gui);
        System.out.println(img_Panel.find_latest_MDAsv_folder());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
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

    private boolean initialized = false;
    private Studio parent = null;
    public boolean verbose = true;

////////////////////////////////////////////////////////////////////////////////
    public boolean isallpathready() {
        return isString_notnull_notEmpty(get_rawpath())
                & isString_notnull_notEmpty(get_phasepath());
    }

    public String get_rawpath() {
        String rawpath = "";

        String raw_folder = get_raw_folder();
        String raw_name = get_rawname();

        if (isDirExists(raw_folder) && istif(raw_name)) {
            rawpath = getPathstr(Paths.get(getPathstr(raw_folder), raw_name).toString());
        } else {
            if (verbose) {
                System.out.println("Invalid raw path");
            }
        }

        return rawpath;
    }

    public String get_phasepath() {
        String phasepath = "";

        String phase_folder = get_phase_folder();
        String phase_name = get_phasename();

        if (isParentDirExists(phase_folder) && istif(phase_name)) {
            phasepath = getPathstr(Paths.get(getPathstr(phase_folder), phase_name).toString());
        } else {
            if (verbose) {
                System.out.println("Invalid phase path");
            }
        }

        return phasepath;
    }

    public void setPane_enabled(boolean enabled) {
        for (Component component : getComponents()) {
            component.setEnabled(enabled);
        }
    }

////////////////////////////////////////////////////////////////////////////////
    public void set_parent(Studio parent_) {
        parent = parent_;
    }

    public String get_raw_folder() {
        return tf_rawfolder.getText();
    }

    public String set_raw_folder(String folderpath) {
        Dimension current_textsize = tf_rawfolder.getSize();
        if (folderpath != null) {
            cb_rawfilename.removeAllItems();

            if (isPathstr(folderpath)) {
                folderpath = getPathstr(folderpath);
                if (isDirExists(folderpath)) {
                    File[] alltiffiles = new File(folderpath).listFiles((File dir, String name1)
                            -> istif(name1));

                    if (alltiffiles.length > 0) {
                        for (File tifffile : alltiffiles) {
                            cb_rawfilename.addItem(tifffile.getName());
                        }
                        tf_rawfolder.setForeground(UIManager.getColor("TextField.foreground"));
                    } else {
                        tf_rawfolder.setForeground(Color.red);
                        if (verbose) {
                            System.out.println("No Tiff images found in raw path");
                        }
                    }

                } else {
                    tf_rawfolder.setForeground(Color.red);
                    if (verbose) {
                        System.out.println("Raw folder path is not an existing folder");
                    }
                }
            } else {
                tf_rawfolder.setForeground(Color.red);
                if (verbose) {
                    System.out.println("Invalid raw folder path");
                }
            }

            tf_rawfolder.setText(folderpath);
            if (cbx_samefolder_rawphase.isSelected()) {
                String phasefolder = set_phase_folder(folderpath);
                if (verbose) {
                    System.out.println("Use same path for phase folder: " + phasefolder);
                }
            }
        }
        tf_rawfolder.setSize(current_textsize);
        return get_raw_folder();
    }

    public String get_phase_folder() {
        return tf_phasefolder.getText();
    }

    public String set_phase_folder(String folderpath) {
        Dimension current_size = tf_phasefolder.getSize();
        if (folderpath != null) {
            if (cbx_samefolder_rawphase.isSelected() && !folderpath.equals(get_raw_folder())) {
                folderpath = get_raw_folder();
            }

            if (isPathstr(folderpath)) {
                folderpath = getPathstr(folderpath);
                if (isParentDirExists(folderpath)) {
                    tf_phasefolder.setForeground(UIManager.getColor("TextField.foreground"));
                } else {
                    tf_phasefolder.setForeground(Color.red);
                    if (verbose) {
                        System.out.println("Parent of phase folder path not exists");
                    }
                }
            } else {
                tf_phasefolder.setForeground(Color.red);
                if (verbose) {
                    System.out.println("Invalid phase folder path");
                }
            }

            tf_phasefolder.setText(folderpath);
        }
        tf_phasefolder.setSize(current_size);
        return get_phase_folder();
    }

    public String get_rawname() {
        String rawname = "";
        if (cb_rawfilename.getItemCount() > 0 && cb_rawfilename.getSelectedIndex() > -1) {
            rawname = String.valueOf(cb_rawfilename.getSelectedItem());
            rawname = isString_notnull_notEmpty(rawname) ? rawname : "";
        }

        if ("".equals(rawname)) {
            if (verbose) {
                System.out.println("No valid raw filename selected now");
            }
        }
        return rawname;
    }

    public String set_rawname(String rawname) {
        if (rawname != null) {
            int size = cb_rawfilename.getItemCount();
            if (size > 0) {
                ArrayList<String> list_rawnames = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    list_rawnames.add(cb_rawfilename.getItemAt(i));
                }

                int index = list_rawnames.indexOf(rawname);
                if (index == -1) {
                    cb_rawfilename.setSelectedItem(null);
                    if (verbose) {
                        System.out.println(rawname + " not in list, set combobox to unselected.");
                    }
                } else {
                    if (index != cb_rawfilename.getSelectedIndex()) {
                        cb_rawfilename.setSelectedIndex(index);
                    }
                }
            }

            if (cbx_defaultphasename.isSelected()) {
                set_phasename(get_default_phasename());
                if (verbose) {
                    System.out.println("Use default phase filename: " + get_phasename());
                }
            }
        }
        return get_rawname();
    }

    public String get_phasename() {
        return tf_phasefilename.getText();
    }

    public String set_phasename(String phasename) {
        if (phasename != null) {
            if (this.cbx_defaultphasename.isSelected() && !phasename.equals(get_default_phasename())) {
                phasename = get_default_phasename();
            }

            tf_phasefilename.setText(phasename);

            boolean valid = istif(phasename);
            tf_phasefilename.setForeground(valid ? UIManager.getColor("TextField.foreground") : Color.red);

            if (!valid && verbose) {
                System.out.println(phasename + " is not a valid TIFF image name.");
            }
        }
        return get_phasename();
    }

////////////////////////////////////////////////////////////////////////////////
    public String onClick_bt_useMDArawfolder() {
        return set_raw_folder(find_latest_MDAsv_folder());
    }

    public String onTextChanged_tf_rawfolder() {
        return set_raw_folder(tf_rawfolder.getText());
    }

    public String onItemChanged_cb_rawfilename() {
        return set_rawname(String.valueOf(cb_rawfilename.getSelectedItem()));
    }

    public String onTextChanged_tf_phasefolder() {
        return set_phase_folder(tf_phasefolder.getText());
    }

    public void onTextChanged_tf_phasefilename() {
        set_phasename(tf_phasefilename.getText());
    }

    public void onSelect_cbx_samefolder_rawphase() {
        set_phase_folder(get_raw_folder());
        setEnabled_phasefolder_oncheck_cbx_samefolder_rawphase();
    }

    public void onUNSelect_cbx_samefolder_rawphase() {
        setEnabled_phasefolder_oncheck_cbx_samefolder_rawphase();
    }

    public void onSelect_cbx_defaultphasename() {
        set_phasename(get_default_phasename());
        setEnabled_phasename_oncheck_cbx_defaultphasename();
    }

    public void onUNSelect_cbx_defaultphasename() {
        setEnabled_phasename_oncheck_cbx_defaultphasename();
    }

    public void onClick_bt_select_rawfolder() {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        filechooser.setDialogType(JFileChooser.OPEN_DIALOG);

        String currentFilepath = tf_rawfolder.getText();
        File currentFile = new File("".equals(currentFilepath) ? System.getProperty("user.home") : currentFilepath);
        filechooser.setSelectedFile(currentFile);
        filechooser.setCurrentDirectory(currentFile);
        int retVal = filechooser.showOpenDialog(this);

        if (retVal == JFileChooser.APPROVE_OPTION) {
            String path = filechooser.getSelectedFile().getAbsolutePath();
            set_raw_folder(path);
        }
    }

    public void onClick_bt_select_phasefolder() {
        if (bt_select_phasefolder.isEnabled()) {
            JFileChooser filechooser = new JFileChooser();
            filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            filechooser.setDialogType(JFileChooser.SAVE_DIALOG);

            String currentFilepath = tf_phasefolder.getText();
            File currentFile = new File("".equals(currentFilepath) ? System.getProperty("user.home") : currentFilepath);
            filechooser.setSelectedFile(currentFile);
            filechooser.setCurrentDirectory(currentFile);
            int retVal = filechooser.showOpenDialog(this);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                String path = filechooser.getSelectedFile().getAbsolutePath();
                set_phase_folder(path);
            }
        }
    }

//////////////////////////////////////////////////////////////////////////////// 
    private boolean isPathstr(String pathstr) {
        return isString_notnull_notEmpty(getPathstr(pathstr));
    }

    private String getPathstr(String pathstr) {
        if (isString_notnull_notEmpty(pathstr)) {
            try {
                return new File(Paths.get(pathstr).toString()).getCanonicalPath();
            } catch (Exception e) {
                return "";
            }
        } else {
            return "";
        }
    }

    private boolean isDirExists(String dirpath) {
        boolean flag = false;
        if (isPathstr(dirpath)) {
            flag = new File(getPathstr(dirpath)).isDirectory();
        } else {
            if (verbose) {
                System.out.println("Input is not a valid path string");
            }
        }

        return flag;
    }

    private boolean isParentDirExists(String dirpath) {
        boolean flag = false;

        if (isPathstr(dirpath)) {
            Path parentpath = Paths.get(getPathstr(dirpath)).getParent();
            if (parentpath != null) {
                flag = Files.exists(parentpath);
            } else {
                if (verbose) {
                    System.out.println("Parent path not found");
                }
            }
        } else {
            if (verbose) {
                System.out.println("Input is not a valid path string");
            }
        }

        return flag;
    }

    private String get_default_phasename() {
        String rawname = get_rawname();
        return isString_notnull_notEmpty(rawname) ? ("phase_" + rawname) : "";
    }

    private void setEnabled_phasename_oncheck_cbx_defaultphasename() {
        tf_phasefilename.setEditable(!cbx_defaultphasename.isSelected());
    }

    private void setEnabled_phasefolder_oncheck_cbx_samefolder_rawphase() {
        tf_phasefolder.setEditable(!cbx_samefolder_rawphase.isSelected());
        bt_select_phasefolder.setEnabled(!cbx_samefolder_rawphase.isSelected());
    }

    private boolean isString_notnull_notEmpty(String test) {
        if (test == null) {
            return false;
        }
        if (test.isEmpty()) {
            return false;
        }
        if (test.equalsIgnoreCase("null")) {
            return false;
        }
        return true;
    }

    private boolean istif(String filename) {
        boolean isTif = false;
        if (isString_notnull_notEmpty(filename)) {
            String[] parts = filename.split("\\.");
            if (parts.length > 1) {
                if (parts.length > 2) {
                    isTif = parts[parts.length - 2].equals("ome") & parts[parts.length - 1].equals("tif");
                } else {
                    isTif = parts[parts.length - 1].equals("tif") | parts[parts.length - 1].equals("tiff");
                }
            }
        }
        return isTif;
    }

    private String find_latest_MDAsv_folder() {
        String folderpath = null;

        if (parent != null) {
            SequenceSettings setting = parent.getAcquisitionManager().getAcquisitionSettings();
            if (setting != null) {

                if (!setting.saveMode().equals(Datastore.SaveMode.SINGLEPLANE_TIFF_SERIES)) {
                    if (isDirExists(setting.root())) {
                        File[] allsubdirs = new File(getPathstr(setting.root())).listFiles(File::isDirectory);
                        if (allsubdirs.length > 0) {
                            int largestid = 0;

                            for (File subdir : allsubdirs) {
                                if (subdir.getName().matches(setting.prefix() + "_" + "\\d+$")) {
                                    int id = Integer.parseUnsignedInt(
                                            subdir.getName().substring((setting.prefix() + "_").length())
                                    );
                                    if (id > largestid) {
                                        largestid = id;
                                        folderpath = subdir.getAbsolutePath();
                                    }
                                }
                            }

                            if (largestid == 0) {
                                if (verbose) {
                                    System.out.println("No subfolder w. name=prefix_[1+] found under save root folder");
                                }
                            }

                        } else {
                            if (verbose) {
                                System.out.println("No subfolder found under save root folder");
                            }
                        }
                    } else {
                        if (verbose) {
                            System.out.println("MM2 MDA save root folder not exists");
                        }
                    }
                } else {
                    if (verbose) {
                        System.out.println("MM2 MDA separate save mode not supported");
                    }
                }
            } else {
                if (verbose) {
                    System.out.println("No MM2 MDA setting found");
                }
            }
        } else {
            if (verbose) {
                System.out.println("No MM2 Studio object found");
            }
        }
        return folderpath;
    }

////////////////////////////////////////////////////////////////////////////////
    public void onPanelStart() {
        if (!initialized) {
            initComponents();
            initialized = true;
        }
    }

    public void onPanelExit() {
        initialized = false;
    }
////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates new form pDPCrecon_path_Panel
     */
    public pDPCrecon_path_Panel() {
        onPanelStart();
    }
////////////////////////////////////////////////////////////////////////////////

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        tf_rawfolder = new javax.swing.JTextField();
        bt_select_rawfolder = new javax.swing.JButton();
        cb_rawfilename = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        tf_phasefolder = new javax.swing.JTextField();
        bt_select_phasefolder = new javax.swing.JButton();
        tf_phasefilename = new javax.swing.JTextField();
        bt_useMDArawfolder = new javax.swing.JButton();
        cbx_samefolder_rawphase = new javax.swing.JCheckBox();
        cbx_defaultphasename = new javax.swing.JCheckBox();

        jLabel6.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jLabel6.setText("Raw image:");

        jLabel4.setText("folder:");

        tf_rawfolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_rawfolderActionPerformed(evt);
            }
        });

        bt_select_rawfolder.setText("...");
        bt_select_rawfolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_select_rawfolderActionPerformed(evt);
            }
        });

        cb_rawfilename.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cb_rawfilenameItemStateChanged(evt);
            }
        });

        jLabel7.setText("imgFile:");

        jLabel8.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jLabel8.setText("Phase image:");

        jLabel5.setText("folder:");

        jLabel9.setText("imgFile:");

        tf_phasefolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_phasefolderActionPerformed(evt);
            }
        });

        bt_select_phasefolder.setText("...");
        bt_select_phasefolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_select_phasefolderActionPerformed(evt);
            }
        });

        tf_phasefilename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_phasefilenameActionPerformed(evt);
            }
        });

        bt_useMDArawfolder.setText("Update raw folder from MDA");
        bt_useMDArawfolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_useMDArawfolderActionPerformed(evt);
            }
        });

        cbx_samefolder_rawphase.setText("Use same folder for phase & raw");
        cbx_samefolder_rawphase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_samefolder_rawphaseActionPerformed(evt);
            }
        });

        cbx_defaultphasename.setText("Use default phase image name");
        cbx_defaultphasename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_defaultphasenameActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cb_rawfilename, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tf_rawfolder)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bt_select_rawfolder))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(bt_useMDArawfolder)
                            .addComponent(jLabel8))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cbx_samefolder_rawphase)
                                .addGap(18, 18, 18)
                                .addComponent(cbx_defaultphasename)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tf_phasefilename))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tf_phasefolder)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bt_select_phasefolder)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bt_useMDArawfolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(tf_rawfolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_select_rawfolder))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cb_rawfilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbx_samefolder_rawphase)
                    .addComponent(cbx_defaultphasename))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(tf_phasefolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_select_phasefolder))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(tf_phasefilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tf_rawfolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_rawfolderActionPerformed
        // TODO add your handling code here:
        onTextChanged_tf_rawfolder();
    }//GEN-LAST:event_tf_rawfolderActionPerformed

    private void bt_select_rawfolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_select_rawfolderActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_select_rawfolder.getText())) {
            onClick_bt_select_rawfolder();
        }
    }//GEN-LAST:event_bt_select_rawfolderActionPerformed

    private void cb_rawfilenameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cb_rawfilenameItemStateChanged
        // TODO add your handling code here:
        onItemChanged_cb_rawfilename();
    }//GEN-LAST:event_cb_rawfilenameItemStateChanged

    private void tf_phasefolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_phasefolderActionPerformed
        // TODO add your handling code here:
        onTextChanged_tf_phasefolder();
    }//GEN-LAST:event_tf_phasefolderActionPerformed

    private void bt_select_phasefolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_select_phasefolderActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_select_phasefolder.getText())) {
            onClick_bt_select_phasefolder();
        }
    }//GEN-LAST:event_bt_select_phasefolderActionPerformed

    private void tf_phasefilenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_phasefilenameActionPerformed
        // TODO add your handling code here:
        onTextChanged_tf_phasefilename();
    }//GEN-LAST:event_tf_phasefilenameActionPerformed

    private void bt_useMDArawfolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_useMDArawfolderActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_useMDArawfolder.getText())) {
            onClick_bt_useMDArawfolder();
        }
    }//GEN-LAST:event_bt_useMDArawfolderActionPerformed

    private void cbx_samefolder_rawphaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_samefolder_rawphaseActionPerformed
        // TODO add your handling code here:
        if (cbx_samefolder_rawphase.isSelected()) {
            onSelect_cbx_samefolder_rawphase();
        } else {
            onUNSelect_cbx_samefolder_rawphase();
        }
    }//GEN-LAST:event_cbx_samefolder_rawphaseActionPerformed

    private void cbx_defaultphasenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_defaultphasenameActionPerformed
        // TODO add your handling code here:
        if (cbx_defaultphasename.isSelected()) {
            onSelect_cbx_defaultphasename();;
        } else {
            onUNSelect_cbx_defaultphasename();
        }
    }//GEN-LAST:event_cbx_defaultphasenameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_select_phasefolder;
    private javax.swing.JButton bt_select_rawfolder;
    private javax.swing.JButton bt_useMDArawfolder;
    private javax.swing.JComboBox<String> cb_rawfilename;
    private javax.swing.JCheckBox cbx_defaultphasename;
    private javax.swing.JCheckBox cbx_samefolder_rawphase;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JTextField tf_phasefilename;
    private javax.swing.JTextField tf_phasefolder;
    private javax.swing.JTextField tf_rawfolder;
    // End of variables declaration//GEN-END:variables
}
