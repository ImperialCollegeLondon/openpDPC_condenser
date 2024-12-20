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
package Utils;

import CMDrun.CMDrun;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * 1. choose path to anaconda3 2. get all conda environments from anaconda3 and
 * store in an arraylist 3. if anaconda3 path invalid or failed to get conda env
 * list, set env list as empty 4. if env list is empty, cannot choose current
 * env 4. once choose current env, set conda path and conda env as not enabled
 * 5. need be able to read current env choosed and path to anaconda activate
 * script
 * 
 */
public class Choose_Python_Panel extends javax.swing.JPanel {

    private boolean initialized = false;
    private String sep = System.getProperty("file.separator");
    private String condapath = "";
    private ArrayList<String> conda_envs = new ArrayList<>();
    private CMDrun cmdrun = new CMDrun();

    public String set_conda_path(String conda_path) {
        if (!conda_path.equals(condapath)) {
            condapath = conda_path;
            condapath_tf.setText(condapath);
            update_conda_envs();
//            conda_envs.forEach(System.out::println);
        }
        return get_conda_path();
    }

    public String get_conda_path() {
        return condapath;
    }

    public void select_current_conda_env(String envname) {
        set_current_conda_env(envname);
        onSelect_current_env(true);
    }

    public void unselect_current_conda_env() {
        onSelect_current_env(false);
    }

    public String get_selected_current_conda_env() {
        if (use_current_pyenv_tbt.isSelected() & use_current_pyenv_tbt.isEnabled()) {
            return get_current_conda_env();
        } else {
            return null;
        }
    }

    public String set_current_conda_env(String envname) {
        if (conda_envs.contains(envname)) {
            pythonenv_cbx.setSelectedItem(envname);
        }

        return get_current_conda_env();
    }

    public String get_current_conda_env() {
        if (conda_envs.isEmpty()) {
            return null;
        } else {
            return String.valueOf(pythonenv_cbx.getSelectedItem());
        }
    }

    public boolean onSelect_current_env(boolean selected) {
        use_current_pyenv_tbt.setSelected(selected & use_current_pyenv_tbt.isEnabled());
        setEnabled_python_env_pane(!use_current_pyenv_tbt.isSelected());
        return use_current_pyenv_tbt.isSelected();
    }

    public ArrayList<String> get_conda_env_list() {
        return conda_envs;
    }

    public String get_conda_activate_filepath(String conda_path) {
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            return conda_path + sep + "Scripts" + sep + "activate";
        } else {
            return conda_path + sep + "etc" + sep + "profile.d" + sep + "conda.sh";
        }
    }

    public String get_conda_activate_runfilecmd(String conda_path) {
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            return "";
        } else {
            return "source ";
        }
    }

    public String get_conda_activate_cmd(String conda_path) {
        return get_conda_activate_runfilecmd(conda_path)
                + get_conda_activate_filepath(conda_path);
    }

    public String get_conda_deactivate_cmd() {
        return "conda deactivate";
    }

    public String get_conda_env_list_cmd() {
        return "conda env list";
    }

    public String get_env_activate_cmd(String envname) {
        return "conda activate " + envname;

    }

    public String get_run_pyfile_cmd(String pyfile_path) {
        return "python3 " + String.format(pyfile_path);
    }

    /**
     * private utility functions
     */
    private void update_conda_envs() {
        conda_envs = get_pyenv_from_conda_path(condapath);

        pythonenv_cbx.removeAllItems();
        for (String env : conda_envs) {
            pythonenv_cbx.addItem(env);
        }

        if (conda_envs.isEmpty()) {
            this.use_current_pyenv_tbt.setSelected(false);
            this.use_current_pyenv_tbt.setEnabled(false);
        } else {
            this.use_current_pyenv_tbt.setEnabled(true);
        }
    }

    private void setEnabled_python_env_pane(boolean enabled) {
        for (Component component : python_env_pane.getComponents()) {
            component.setEnabled(enabled);
        }
    }

    private ArrayList<String> get_pyenv_from_conda_path(String conda_path) {
        String conda_activate_filepathstr = get_conda_activate_filepath(conda_path);
        String conda_activate_str = get_conda_activate_cmd(conda_path);
        String conda_get_pyenv_str = get_conda_env_list_cmd();
        String conda_deactivate_str = get_conda_deactivate_cmd();
        String endsign = "finished";

        if (new File(conda_path).exists() && new File(conda_activate_filepathstr).exists()) {
            cmdrun.startProcess(true);
            cmdrun.Write(conda_activate_str, true);
            cmdrun.Write(conda_get_pyenv_str, true);
            cmdrun.Write(conda_deactivate_str, true);
            cmdrun.Write("echo " + endsign, true);

            ArrayList<String> lines = cmdrun.waitUntilRecv(endsign, 30, true);

            cmdrun.stopProcess(1, true);

            if (!pythonenv_cbx.getToolTipText().isEmpty()) {
                pythonenv_cbx.setToolTipText("");
            }

            return get_pyenv_from_arylist(lines);
        } else {
            String errtext = "Conda activation file not exist\n"
                    + "conda_path: " + conda_path + "\n"
                    + "conda_activate_filepath: " + conda_activate_filepathstr;
            Logger.getLogger(Choose_Python_Panel.class.getName())
                    .log(Level.WARNING, errtext);
            pythonenv_cbx.setToolTipText(
                    "<html>" + errtext.replaceAll("\n", "<br>") + "</html>");
            return new ArrayList<>();
        }
    }

    private ArrayList<String> get_pyenv_from_arylist(ArrayList<String> lines) {
        int start_id = -1;
        String start_sign = "# conda environments:";

        ArrayList<String> pyenvs = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line != null && !line.isEmpty()) {
                if (line.contains(start_sign)) {
                    start_id = i + 2;
                }
            }

            if (start_id > -1 && i >= start_id) {
                if (line.isEmpty()) {
                    break;
                } else {
                    pyenvs.add(line.split("\\s+")[0]);
                }
            }
        }

        return pyenvs;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        Choose_Python_Panel test_pane = new Choose_Python_Panel();

        frame.add(test_pane);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    test_pane.onPanelExit();
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

    /**
     * Creates new form Choose_Python_Panel
     */
    public Choose_Python_Panel() {
        onPanelStart();
    }

    public void onPanelStart() {
        if (!initialized) {
            initComponents();
//            set_conda_path(System.getProperty("user.home") + sep + "anaconda3");
            initialized = true;
        }
    }

    public void onPanelExit() {
        onSelect_current_env(false);
        cmdrun.stopProcess(1, true);
        initialized = false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        python_env_pane = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        condapath_tf = new javax.swing.JTextField();
        choose_condapath_bt = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        pythonenv_cbx = new javax.swing.JComboBox<>();
        jSeparator1 = new javax.swing.JSeparator();
        use_current_pyenv_tbt = new javax.swing.JToggleButton();

        jLabel1.setText("Anaconda3 path:");

        condapath_tf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                condapath_tfActionPerformed(evt);
            }
        });

        choose_condapath_bt.setText("...");
        choose_condapath_bt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                choose_condapath_btActionPerformed(evt);
            }
        });

        jLabel2.setText("Python environment:");

        pythonenv_cbx.setToolTipText("");

        javax.swing.GroupLayout python_env_paneLayout = new javax.swing.GroupLayout(python_env_pane);
        python_env_pane.setLayout(python_env_paneLayout);
        python_env_paneLayout.setHorizontalGroup(
            python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(python_env_paneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(python_env_paneLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, python_env_paneLayout.createSequentialGroup()
                        .addGroup(python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(python_env_paneLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(16, 16, 16)
                                .addComponent(pythonenv_cbx, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 189, Short.MAX_VALUE))
                            .addComponent(condapath_tf))
                        .addGap(18, 18, 18)
                        .addComponent(choose_condapath_bt, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        python_env_paneLayout.setVerticalGroup(
            python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(python_env_paneLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jLabel1)
                .addGap(2, 2, 2)
                .addGroup(python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(condapath_tf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(choose_condapath_bt))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(python_env_paneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pythonenv_cbx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        use_current_pyenv_tbt.setText("Use current python environment");
        use_current_pyenv_tbt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                use_current_pyenv_tbtActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(use_current_pyenv_tbt)
                .addContainerGap(429, Short.MAX_VALUE))
            .addComponent(python_env_pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(python_env_pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(use_current_pyenv_tbt)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void choose_condapath_btActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_choose_condapath_btActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand() == choose_condapath_bt.getText()) {
            JFileChooser filechooser = new JFileChooser();
            filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            File currenrDir = new File(condapath_tf.getText());
            filechooser.setSelectedFile(currenrDir);
            filechooser.setCurrentDirectory(currenrDir);
            int retVal = filechooser.showOpenDialog(this);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                set_conda_path(filechooser.getSelectedFile().getAbsolutePath());
            }
        }
    }//GEN-LAST:event_choose_condapath_btActionPerformed

    private void condapath_tfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_condapath_tfActionPerformed
        // TODO add your handling code here: 
        set_conda_path(condapath_tf.getText());
    }//GEN-LAST:event_condapath_tfActionPerformed

    private void use_current_pyenv_tbtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_use_current_pyenv_tbtActionPerformed
        // TODO add your handling code here:
        onSelect_current_env(use_current_pyenv_tbt.isSelected());
    }//GEN-LAST:event_use_current_pyenv_tbtActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton choose_condapath_bt;
    private javax.swing.JTextField condapath_tf;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel python_env_pane;
    private javax.swing.JComboBox<String> pythonenv_cbx;
    private javax.swing.JToggleButton use_current_pyenv_tbt;
    // End of variables declaration//GEN-END:variables
}
