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
package Paras_Presets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultCaret;


public class Preset_Panel extends javax.swing.JPanel {

    private Presets presets = new Presets();
    private boolean initialized = false;
    private boolean hideadv = false;

    public static String resource_preset_filename = "all_presets_js";

    public static void main(String args[]) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Preset_Panel preset_panel = new Preset_Panel();
        frame.add(preset_panel);
        frame.pack();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }
        });
    }
    
    // ---------------------------------    ---------------------------------  
    public String get_ta_current_paras_jstr(){
        return ta_current_paras_jstr.getText();
    }

    // ---------------------------------    ---------------------------------    ---------------------------------    ---------------------------------    
    public void jsonto_current_settings(String jstr) {
        Gson gson = new Gson();
        String logText = "";
        HashMap<String, Object> json_dict = new HashMap<>();
        Type type_dict = new TypeToken<HashMap<String, Object>>() {
        }.getType();

        try {
            json_dict = gson.fromJson(jstr, type_dict);
        } catch (Exception ex) {
            logText = logText.concat("Failed to convert json string into a map of para_name->para_value: "
                    + "\n" + ex.toString());
        }

        if (json_dict != null && !json_dict.isEmpty() && current_paras_table != null) {
            int para_names_cnt = current_paras_table.getColumnCount();

            if (para_names_cnt > 0) {
                for (int i = 0; i < para_names_cnt; i++) {
                    String col_name = current_paras_table.getColumnName(i);

                    if (json_dict.containsKey(col_name)) {
                        try {
                            current_paras_table.setValueAt(json_dict.get(col_name), 0, i);
                            json_dict.remove(col_name);
                        } catch (Exception ex) {
                        }
                    }
                }

                if (!json_dict.isEmpty()) {
                    ta_current_paras_jstr.setText(gson.toJson(json_dict, type_dict));
                } else {
                    ta_current_paras_jstr.setText("");
                }

            } else {
                logText = "No paras exist in column";
            }
        }

        ta_log.setText(logText);
    }

    public void set_editable_hideadv(boolean flag) {
        cbx_show_advparams.setEnabled(flag);
    }

    public String get_current_settings_json() {
        return presets.get_current_preset_json(!hideadv);
    }

    public void setEditable_tf_preset_filepath(boolean editable) {
        tf_preset_filepath.setEditable(editable);
        bt_choose_preset_filepath.setEnabled(editable);
    }

    private String init_empty_presets() {
        presets = new Presets();
        on_load_new_allpresets();
        tf_preset_filepath.setText("null");
        ta_log.setText("");
        return tf_preset_filepath.getText();
    }

    private String update_presets_jstr_from_resource() {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        this.getClass().getResourceAsStream("/Resources/" + resource_preset_filename)
                )
        );
        in.lines().forEach(line -> {
            lines.add(line);
        });
        String jstr = String.join("\n", lines);
        presets.update_allpresets_from_jstr(jstr);

        on_load_new_allpresets();

        tf_preset_filepath.setText("");
        ta_log.setText("");
        return tf_preset_filepath.getText();
    }

    public void on_load_new_allpresets() {
        updateModel_combo_current_preset_name();
        updateModel_combo_this_para_name();
        onSelectedItemChanged_this_para_name();

        String current_presetname = presets.get_current_preset_name();
        combo_current_preset_name.setSelectedItem(current_presetname);
        if (!String.valueOf(combo_current_preset_name.getSelectedItem())
                .equals(current_presetname)) {
            combo_current_preset_name.setSelectedIndex(-1);
        }

        ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
    }

    public String set_preset_filepath(String filepath) {
        String logText = "";
        if (filepath != null && !filepath.equalsIgnoreCase("null") && !filepath.isEmpty()) {
            File preset_file = new File(filepath);
            if (!preset_file.isFile()) {
                logText = logText.concat("Invalid filepath: " + filepath + "\n\nTrying to update from Resources...\n\n");
                filepath = "";
            }

            if (!filepath.isEmpty()) {
                try {
                    String jstr = String.join("\n", Files.readAllLines(preset_file.toPath()));

                    presets.update_allpresets_from_jstr(jstr);

                    on_load_new_allpresets();

                    tf_preset_filepath.setText(preset_file.getAbsolutePath());
                    ta_log.setText(logText);
                    return tf_preset_filepath.getText();
                } catch (Exception ex) {
                    logText = logText.concat("Failed to update presets from filepath: "
                            + filepath + "\n" + ex.toString()
                            + "\n\nTrying to update from Resources...\n\n");
                    filepath = "";
                }
            }
        }

        if (filepath != null && filepath.isEmpty()) {
            try {
                update_presets_jstr_from_resource();
                ta_log.setText(logText);
                return tf_preset_filepath.getText();
            } catch (Exception e) {
                logText = logText.concat("Failed to update presets from Resources: \n")
                        .concat(e.toString()).concat("\n\nUsing empty preset...");
                filepath = null;
            }
        }

        init_empty_presets();
        ta_log.setText(logText);
        return tf_preset_filepath.getText();
    }

    public void init_current_paras_table() {
        current_paras_table.setModel(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return 1;
            }

            @Override
            public int getColumnCount() {
                return presets.get_para_names_by_mode(hideadv).size();
            }

            @Override
            public String getColumnName(int columnIndex) {
                return presets.get_para_names_by_mode(hideadv).get(columnIndex);
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return tbt_advance_mode_on.isSelected();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return presets.get_current_value(getColumnName(columnIndex));
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                try {
                    presets.set_current_value(getColumnName(columnIndex), String.valueOf(aValue));

                    if (getColumnName(columnIndex).equals(presets.preset_name_str)) {
                        String current_presetname = presets.get_current_preset_name();
                        combo_current_preset_name.setSelectedItem(current_presetname);
                        if (!String.valueOf(combo_current_preset_name.getSelectedItem())
                                .equals(current_presetname)) {
                            combo_current_preset_name.setSelectedIndex(-1);
                        }
                    }

                    ta_log.setText("");
                } catch (Exception e) {
                    ta_log.setText(e.toString());
                }
                fireTableStructureChanged();
                fireTableDataChanged();
            }
        }
        );
        current_paras_table.getTableHeader().setReorderingAllowed(false);
    }

    public void init_all_presets_table() {
        all_presets_table.setModel(new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return presets.get_all_preset_names().size();
            }

            @Override
            public int getColumnCount() {
                return presets.get_para_names_by_mode(hideadv).size();
            }

            @Override
            public String getColumnName(int columnIndex) {
                return presets.get_para_names_by_mode(hideadv).get(columnIndex);
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                String preset_name = presets.get_all_preset_names().get(rowIndex);
                return presets.get_preset(preset_name).get(getColumnName(columnIndex));
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                fireTableStructureChanged();
                fireTableDataChanged();
            }
        });
        all_presets_table.getTableHeader().setReorderingAllowed(false);

    }

    public void onTextChanged_tf_preset_filepath() {
        set_preset_filepath(tf_preset_filepath.getText());
    }

    public void onPress_bt_choose_preset_filepath() {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File currentFile = new File(tf_preset_filepath.getText());
        filechooser.setSelectedFile(currentFile);
        int retVal = filechooser.showOpenDialog(this);

        if (retVal == JFileChooser.APPROVE_OPTION) {
            set_preset_filepath(filechooser.getSelectedFile().getAbsolutePath());
        }
    }

    public boolean set_advance_mode_on(boolean flag) {
        tbt_advance_mode_on.setSelected(flag);
        onAdvanceMode();
        return tbt_advance_mode_on.isSelected();
    }

    public boolean set_tbt_para_edit_on(boolean flag) {
        flag = tbt_para_edit_on.isEnabled() && tbt_para_edit_on.isSelected();
        tbt_para_edit_on.setSelected(flag);
        onParaEditor();
        return tbt_para_edit_on.isSelected();
    }

    // on advance mode on clicked
    private void onAdvanceMode() {
        boolean flag = tbt_advance_mode_on.isSelected();

        bt_save_this_preset.setEnabled(flag);
        bt_remove_preset.setEnabled(flag);
        bt_save_allpresets.setEnabled(flag);
        tbt_para_edit_on.setEnabled(flag);

        bt_save_this_preset.setSelected(false);
        bt_remove_preset.setSelected(false);
        bt_save_allpresets.setSelected(false);
        tbt_para_edit_on.setSelected(false);

        onParaEditor();
    }

    private void setEnabled_para_editor_pane(boolean enabled) {
        for (Component component : para_editor_pane.getComponents()) {
            component.setEnabled(enabled);
        }
        if (enabled && hideadv) {
            this_para_isadv.setEnabled(false);
        }
    }

    private void updateModel_combo_current_preset_name() {
        combo_current_preset_name.setModel(
                new javax.swing.DefaultComboBoxModel<String>(
                        presets.get_all_preset_names().toArray(new String[0])
                )
        );
    }

    private void updateModel_combo_this_para_name() {
        ArrayList<String> allparanames = presets.get_para_names_by_mode(hideadv);
        allparanames.remove(presets.preset_name_str);
        this_para_name.setModel(
                new javax.swing.DefaultComboBoxModel<String>(
                        allparanames.toArray(new String[0])
                )
        );
    }

// on para editor on clicked
    private void onParaEditor() {
        boolean flag = tbt_para_edit_on.isEnabled() && tbt_para_edit_on.isSelected();

        setEnabled_para_editor_pane(flag);

        bt_set_this_para.setSelected(false);
        bt_remove_this_para.setSelected(false);
    }

    public void onSelectedItemChanged_combo_current_preset_name() {
        if (combo_current_preset_name.getSelectedIndex() != -1) {
            String current_preset_name = String.valueOf(combo_current_preset_name.getSelectedItem());
            presets.load_preset_to_current_preset(current_preset_name);
        }

        ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
    }

    public void onPress_bt_save_this_preset() {
        presets.addupdate_current_preset_to_allpresets();
        updateModel_combo_current_preset_name();
        combo_current_preset_name.setSelectedItem(presets.get_current_preset_name());

        ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
    }

    public void onPress_bt_remove_this_preset() {
        String preset_name = presets.get_current_preset_name();
        presets.remove_preset(preset_name);
        updateModel_combo_current_preset_name();
        combo_current_preset_name.setSelectedIndex(-1);
        presets.clear_current_values();

        ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
    }

    public void onSelectedItemChanged_this_para_name() {
        if (this_para_name.getSelectedIndex() != -1) {
            boolean enabled = tbt_para_edit_on.isSelected();
            String paraname = String.valueOf(this_para_name.getSelectedItem());
            this_para_type.setEnabled(false);

            Para thispara = presets.get_para(paraname);
            this_para_type.setSelectedItem(thispara.get_type_name());
            this_para_min.setText(thispara.get_min_value());
            this_para_max.setText(thispara.get_max_value());
            this_para_isadv.setSelected(thispara.get_isadvparam());

        } else {
            boolean enabled = tbt_para_edit_on.isSelected();
            this_para_type.setEnabled(enabled);

            String paraname = String.valueOf(this_para_name.getSelectedItem());
            if (paraname.equals(presets.preset_name_str)
                    || (hideadv && presets.get_adv_paranames().contains(paraname))) {
                this_para_name.setSelectedItem("");
            } else {

                if (!paraname.isEmpty()) {
                    try {
                        Para temp = new Para(paraname, "String", "", "", "", false);
                        if (!temp.get_name().equals(paraname)) {
                            this_para_name.setSelectedItem(temp.get_name());
                        }
                    } catch (Exception e) {
                        this_para_name.setSelectedItem("");
                    }
                }
            }

            this_para_type.setSelectedIndex(0);
            this_para_min.setText("");
            this_para_max.setText("");
            this_para_isadv.setSelected(false);
        }
    }

    public void onTextChanged_this_para_max() {
        Para thispara = new Para("whatever",
                String.valueOf(this_para_type.getSelectedItem()),
                "",
                "",
                this_para_min.getText(),
                this_para_isadv.isSelected());
        thispara.set_max_value(this_para_max.getText());

        this_para_max.setText(thispara.get_max_value());
    }

    public void onTextChanged_this_para_min() {
        Para thispara = new Para("whatever",
                String.valueOf(this_para_type.getSelectedItem()),
                "",
                this_para_max.getText(),
                "",
                this_para_isadv.isSelected());
        thispara.set_min_value(this_para_min.getText());

        this_para_min.setText(thispara.get_min_value());
    }

    public void onPress_bt_remove_this_para() {
        String paraname = String.valueOf(this_para_name.getSelectedItem());
        if (!paraname.isEmpty() && !paraname.equals(presets.preset_name_str)) {
            presets.remove_para(paraname);

            updateModel_combo_this_para_name();
            this_para_name.setSelectedIndex(-1);
            onSelectedItemChanged_this_para_name();

            ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
            ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
            ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
            ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
        }
    }

    public void onPress_bt_set_this_para() {
        String paraname = String.valueOf(this_para_name.getSelectedItem());
        if (!paraname.isEmpty() && !paraname.equals(presets.preset_name_str)) {
            Para temp = new Para(paraname,
                    String.valueOf(this_para_type.getSelectedItem()),
                    "",
                    this_para_max.getText(),
                    this_para_min.getText(),
                    this_para_isadv.isSelected());
            Para Para_added = presets.add_update_para(temp);

            updateModel_combo_this_para_name();
            if (Para_added != null) {
                this_para_name.setSelectedItem(Para_added.get_name());
            } else {
                this_para_name.setSelectedIndex(-1);
            }
            onSelectedItemChanged_this_para_name();

            ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
            ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
            ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
            ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();
        }
    }

    public void onPress_save_all_presets() {
        String savepath = tf_preset_filepath.getText();
        if (savepath != null && !savepath.isEmpty() && !savepath.equalsIgnoreCase("null")) {
            try {
                File savefile = new File(savepath);
                if (!savefile.exists()) {
                    savefile.createNewFile();
                }

                String jstr = presets.toString();
                BufferedWriter writer = new BufferedWriter(new FileWriter(savefile));
                writer.write(jstr);
                writer.close();

            } catch (Exception e) {
                ta_log.setText(e.toString());
            }
        } else {
            ta_log.setText("Cannot save to invalid save path: " + savepath);
        }
    }

    public void onPress_bt_jsonto_current_paras() {
        String jstr = ta_current_paras_jstr.getText();
        jsonto_current_settings(jstr);
    }

// ---------------------------------    ---------------------------------    ---------------------------------    ---------------------------------    
    public void onPanelStart() {
        if (!initialized) {
            initComponents();

            set_preset_filepath(tf_preset_filepath.getText());

            init_current_paras_table();
            init_all_presets_table();
        }
        initialized = true;
    }

    public void onPanelExit() {
        if (initialized) {
            initialized = false;
        }
    }

    public boolean set_hideadv(boolean hide) {
        hideadv = hide;
        cbx_show_advparams.setSelected(!hide);

        Object olditem = this_para_name.getSelectedItem();
        updateModel_combo_this_para_name();
        this_para_name.setSelectedItem(olditem);
        onSelectedItemChanged_this_para_name();

        if (hideadv && this_para_isadv.isEnabled()) {
            this_para_isadv.setEnabled(false);
        } else {
            if (!hideadv && tbt_para_edit_on.isSelected() && !this_para_isadv.isEnabled()) {
                this_para_isadv.setEnabled(true);
            }
        }

        ((AbstractTableModel) current_paras_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) current_paras_table.getModel()).fireTableDataChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableStructureChanged();
        ((AbstractTableModel) all_presets_table.getModel()).fireTableDataChanged();

        return hideadv;
    }

// ---------------------------------    ---------------------------------    ---------------------------------    ---------------------------------    
    /**
     * Creates new form Preset_Panel
     */
    public Preset_Panel() {
        onPanelStart();
    }

// ---------------------------------    ---------------------------------    ---------------------------------    ---------------------------------    
// ---------------------------------    ---------------------------------    ---------------------------------    ---------------------------------    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        all_presets_scrollpane = new javax.swing.JScrollPane();
        all_presets_table = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        advanced_panel = new javax.swing.JPanel();
        tbt_para_edit_on = new javax.swing.JToggleButton();
        para_editor_pane = new javax.swing.JPanel();
        this_para_type = new javax.swing.JComboBox<>();
        this_para_name = new javax.swing.JComboBox<>();
        this_para_min = new javax.swing.JTextField();
        this_para_max = new javax.swing.JTextField();
        label1 = new java.awt.Label();
        label2 = new java.awt.Label();
        label3 = new java.awt.Label();
        label4 = new java.awt.Label();
        label5 = new java.awt.Label();
        this_para_isadv = new javax.swing.JCheckBox();
        bt_set_this_para = new javax.swing.JButton();
        bt_remove_this_para = new javax.swing.JButton();
        combo_current_preset_name = new javax.swing.JComboBox<>();
        label7 = new java.awt.Label();
        tbt_advance_mode_on = new javax.swing.JToggleButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta_current_paras_jstr = new javax.swing.JTextArea();
        cbx_show_advparams = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        ta_log = new javax.swing.JTextArea();
        bt_print_current_paras = new javax.swing.JButton();
        bt_save_this_preset = new javax.swing.JButton();
        bt_remove_preset = new javax.swing.JButton();
        bt_save_allpresets = new javax.swing.JButton();
        bt_jsonto_current_paras = new javax.swing.JButton();
        current_paras_scrollpane = new javax.swing.JScrollPane();
        current_paras_table = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        tf_preset_filepath = new javax.swing.JTextField();
        bt_choose_preset_filepath = new javax.swing.JButton();

        all_presets_scrollpane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        all_presets_scrollpane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        all_presets_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1"
            }
        ));
        all_presets_scrollpane.setViewportView(all_presets_table);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 1, 16)); // NOI18N
        jLabel1.setText("Current settings");

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 16)); // NOI18N
        jLabel2.setText("All presets");

        tbt_para_edit_on.setText("Parameter editor on");
        tbt_para_edit_on.setEnabled(tbt_advance_mode_on.isSelected());
        tbt_para_edit_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_para_edit_onActionPerformed(evt);
            }
        });

        this_para_type.setModel(new javax.swing.DefaultComboBoxModel<>(Para.STR_Allowed_Para_Type));
        this_para_type.setEnabled(tbt_para_edit_on.isSelected());
        this_para_type.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                this_para_typeItemStateChanged(evt);
            }
        });

        this_para_name.setEditable(true);
        ArrayList<String> allparanames = presets.get_para_names_by_mode(hideadv);
        allparanames.remove(presets.preset_name_str);
        this_para_name.setModel(new javax.swing.DefaultComboBoxModel<>(allparanames.toArray(new String[0]))
        );
        this_para_name.setEnabled(tbt_para_edit_on.isSelected());
        this_para_name.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                this_para_nameItemStateChanged(evt);
            }
        });

        this_para_min.setEnabled(tbt_para_edit_on.isSelected());
        this_para_min.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                this_para_minActionPerformed(evt);
            }
        });

        this_para_max.setEnabled(tbt_para_edit_on.isSelected());
        this_para_max.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                this_para_maxActionPerformed(evt);
            }
        });

        label1.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        label1.setText("<Parameter editor>");

        label2.setText("Name");

        label3.setText("Type");

        label4.setText("Max value");

        label5.setText("Min value");

        this_para_isadv.setText("is Adv param");
        this_para_isadv.setEnabled(false);

        bt_set_this_para.setText("Add/Update parameter");
        bt_set_this_para.setEnabled(false);
        bt_set_this_para.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_set_this_paraActionPerformed(evt);
            }
        });

        bt_remove_this_para.setText("Remove parameter");
        bt_remove_this_para.setEnabled(false);
        bt_remove_this_para.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_remove_this_paraActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout para_editor_paneLayout = new javax.swing.GroupLayout(para_editor_pane);
        para_editor_pane.setLayout(para_editor_paneLayout);
        para_editor_paneLayout.setHorizontalGroup(
            para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(para_editor_paneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, para_editor_paneLayout.createSequentialGroup()
                        .addComponent(this_para_isadv)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bt_set_this_para))
                    .addGroup(para_editor_paneLayout.createSequentialGroup()
                        .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(this_para_max, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(this_para_name, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(para_editor_paneLayout.createSequentialGroup()
                                .addComponent(label5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 71, Short.MAX_VALUE)
                                .addComponent(bt_remove_this_para))
                            .addGroup(para_editor_paneLayout.createSequentialGroup()
                                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(this_para_min, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(this_para_type, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        para_editor_paneLayout.setVerticalGroup(
            para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(para_editor_paneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(this_para_isadv)
                    .addComponent(bt_set_this_para))
                .addGap(10, 10, 10)
                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(label5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_remove_this_para))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(this_para_max, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(this_para_min, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(para_editor_paneLayout.createSequentialGroup()
                        .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addGroup(para_editor_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(this_para_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(this_para_type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        combo_current_preset_name.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        combo_current_preset_name.setModel(new javax.swing.DefaultComboBoxModel(presets.get_all_preset_names().toArray(new String[0])));
        combo_current_preset_name.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                combo_current_preset_nameItemStateChanged(evt);
            }
        });

        label7.setText("Current Preset");

        tbt_advance_mode_on.setText("Advance mode on");
        tbt_advance_mode_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_advance_mode_onActionPerformed(evt);
            }
        });

        ta_current_paras_jstr.setColumns(20);
        ta_current_paras_jstr.setLineWrap(true);
        ta_current_paras_jstr.setRows(5);
        ((DefaultCaret) ta_current_paras_jstr.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        jScrollPane1.setViewportView(ta_current_paras_jstr);

        cbx_show_advparams.setSelected(true);
        cbx_show_advparams.setText("Show Adv params");
        cbx_show_advparams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_show_advparamsActionPerformed(evt);
            }
        });

        jLabel4.setText("Logs:");

        ta_log.setEditable(false);
        ta_log.setColumns(20);
        ta_log.setLineWrap(true);
        ta_log.setRows(4);
        ta_log.setWrapStyleWord(true);
        jScrollPane2.setViewportView(ta_log);

        bt_print_current_paras.setText("Print current setting as json");
        bt_print_current_paras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_print_current_parasActionPerformed(evt);
            }
        });

        bt_save_this_preset.setText("Add/Update preset");
        bt_save_this_preset.setEnabled(false);
        bt_save_this_preset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_save_this_presetActionPerformed(evt);
            }
        });

        bt_remove_preset.setText("Remove preset");
        bt_remove_preset.setEnabled(false);
        bt_remove_preset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_remove_presetActionPerformed(evt);
            }
        });

        bt_save_allpresets.setText("Save all presets");
        bt_save_allpresets.setEnabled(false);
        bt_save_allpresets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_save_allpresetsActionPerformed(evt);
            }
        });

        bt_jsonto_current_paras.setText("Update json to current setting");
        bt_jsonto_current_paras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_jsonto_current_parasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout advanced_panelLayout = new javax.swing.GroupLayout(advanced_panel);
        advanced_panel.setLayout(advanced_panelLayout);
        advanced_panelLayout.setHorizontalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(advanced_panelLayout.createSequentialGroup()
                            .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(combo_current_preset_name, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jLabel4)
                        .addGroup(advanced_panelLayout.createSequentialGroup()
                            .addComponent(bt_print_current_paras)
                            .addGap(18, 18, 18)
                            .addComponent(bt_jsonto_current_paras)))
                    .addComponent(jScrollPane2))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tbt_advance_mode_on)
                            .addComponent(bt_save_this_preset))
                        .addGap(18, 18, 18)
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbx_show_advparams)
                            .addComponent(bt_remove_preset))
                        .addGap(18, 18, 18)
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(tbt_para_edit_on)
                            .addComponent(bt_save_allpresets)))
                    .addComponent(para_editor_pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16))
        );

        advanced_panelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {bt_jsonto_current_paras, bt_print_current_paras});

        advanced_panelLayout.setVerticalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(combo_current_preset_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbt_advance_mode_on)
                    .addComponent(tbt_para_edit_on)
                    .addComponent(cbx_show_advparams))
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE))
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bt_print_current_paras)
                            .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(bt_save_this_preset)
                                .addComponent(bt_jsonto_current_paras)
                                .addComponent(bt_remove_preset)
                                .addComponent(bt_save_allpresets)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(para_editor_pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        advanced_panelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {bt_jsonto_current_paras, bt_print_current_paras});

        current_paras_scrollpane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        current_paras_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        current_paras_scrollpane.setViewportView(current_paras_table);

        jLabel3.setText("Preset filepath:");

        tf_preset_filepath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_preset_filepathActionPerformed(evt);
            }
        });

        bt_choose_preset_filepath.setText("...");
        bt_choose_preset_filepath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_choose_preset_filepathActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(advanced_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(all_presets_scrollpane)
                    .addComponent(current_paras_scrollpane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(tf_preset_filepath)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bt_choose_preset_filepath))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(tf_preset_filepath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_choose_preset_filepath))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(advanced_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addGap(12, 12, 12)
                .addComponent(current_paras_scrollpane, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(all_presets_scrollpane, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbt_advance_mode_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_advance_mode_onActionPerformed
        // TODO add your handling code here:
        set_advance_mode_on(tbt_advance_mode_on.isSelected());
    }//GEN-LAST:event_tbt_advance_mode_onActionPerformed

    private void tbt_para_edit_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_para_edit_onActionPerformed
        // TODO add your handling code here:
        set_tbt_para_edit_on(tbt_para_edit_on.isSelected());
    }//GEN-LAST:event_tbt_para_edit_onActionPerformed

    private void combo_current_preset_nameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_combo_current_preset_nameItemStateChanged
        // TODO add your handling code here:
        onSelectedItemChanged_combo_current_preset_name();
    }//GEN-LAST:event_combo_current_preset_nameItemStateChanged

    private void this_para_nameItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_this_para_nameItemStateChanged
        // TODO add your handling code here: 
        onSelectedItemChanged_this_para_name();
    }//GEN-LAST:event_this_para_nameItemStateChanged

    private void this_para_maxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_this_para_maxActionPerformed
        // TODO add your handling code here: 
        onTextChanged_this_para_max();
    }//GEN-LAST:event_this_para_maxActionPerformed

    private void this_para_minActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_this_para_minActionPerformed
        // TODO add your handling code here: 
        onTextChanged_this_para_min();
    }//GEN-LAST:event_this_para_minActionPerformed

    private void this_para_typeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_this_para_typeItemStateChanged
        // TODO add your handling code here: 
    }//GEN-LAST:event_this_para_typeItemStateChanged

    private void cbx_show_advparamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_show_advparamsActionPerformed
        // TODO add your handling code here:
        set_hideadv(!cbx_show_advparams.isSelected());
    }//GEN-LAST:event_cbx_show_advparamsActionPerformed

    private void tf_preset_filepathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_preset_filepathActionPerformed
        // TODO add your handling code here:
        onTextChanged_tf_preset_filepath();
    }//GEN-LAST:event_tf_preset_filepathActionPerformed

    private void bt_choose_preset_filepathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_choose_preset_filepathActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_choose_preset_filepath.getText())) {
            onPress_bt_choose_preset_filepath();
        }
    }//GEN-LAST:event_bt_choose_preset_filepathActionPerformed

    private void bt_print_current_parasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_print_current_parasActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_print_current_paras.getText())) {
            ta_current_paras_jstr.setText(get_current_settings_json());
        }
    }//GEN-LAST:event_bt_print_current_parasActionPerformed

    private void bt_save_this_presetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_save_this_presetActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_save_this_preset.getText())) {
            onPress_bt_save_this_preset();
        }
    }//GEN-LAST:event_bt_save_this_presetActionPerformed

    private void bt_remove_presetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_remove_presetActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_remove_preset.getText())) {
            onPress_bt_remove_this_preset();
        }
    }//GEN-LAST:event_bt_remove_presetActionPerformed

    private void bt_set_this_paraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_set_this_paraActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_set_this_para.getText())) {
            onPress_bt_set_this_para();
        }
    }//GEN-LAST:event_bt_set_this_paraActionPerformed

    private void bt_remove_this_paraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_remove_this_paraActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_remove_this_para.getText())) {
            onPress_bt_remove_this_para();
        }
    }//GEN-LAST:event_bt_remove_this_paraActionPerformed

    private void bt_save_allpresetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_save_allpresetsActionPerformed
        // TODO add your handling code here:
        onPress_save_all_presets();
    }//GEN-LAST:event_bt_save_allpresetsActionPerformed

    private void bt_jsonto_current_parasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_jsonto_current_parasActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_jsonto_current_paras.getText())) {
            onPress_bt_jsonto_current_paras();
        }
    }//GEN-LAST:event_bt_jsonto_current_parasActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel advanced_panel;
    private javax.swing.JScrollPane all_presets_scrollpane;
    private javax.swing.JTable all_presets_table;
    private javax.swing.JButton bt_choose_preset_filepath;
    private javax.swing.JButton bt_jsonto_current_paras;
    private javax.swing.JButton bt_print_current_paras;
    private javax.swing.JButton bt_remove_preset;
    private javax.swing.JButton bt_remove_this_para;
    private javax.swing.JButton bt_save_allpresets;
    private javax.swing.JButton bt_save_this_preset;
    private javax.swing.JButton bt_set_this_para;
    private javax.swing.JCheckBox cbx_show_advparams;
    private javax.swing.JComboBox<String> combo_current_preset_name;
    private javax.swing.JScrollPane current_paras_scrollpane;
    private javax.swing.JTable current_paras_table;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private java.awt.Label label1;
    private java.awt.Label label2;
    private java.awt.Label label3;
    private java.awt.Label label4;
    private java.awt.Label label5;
    private java.awt.Label label7;
    private javax.swing.JPanel para_editor_pane;
    private javax.swing.JTextArea ta_current_paras_jstr;
    private javax.swing.JTextArea ta_log;
    private javax.swing.JToggleButton tbt_advance_mode_on;
    private javax.swing.JToggleButton tbt_para_edit_on;
    private javax.swing.JTextField tf_preset_filepath;
    private javax.swing.JCheckBox this_para_isadv;
    private javax.swing.JTextField this_para_max;
    private javax.swing.JTextField this_para_min;
    private javax.swing.JComboBox<String> this_para_name;
    private javax.swing.JComboBox<String> this_para_type;
    // End of variables declaration//GEN-END:variables

}
