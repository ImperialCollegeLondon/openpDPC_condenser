/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package CMDrun;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultCaret;
import Utils.utils;
import java.util.ArrayList;

/**
 *
 * @author h.liu
 */
public class CMDrun_Panel extends javax.swing.JPanel {

    public boolean Send(String inputline) {
        tf_inputline.setText(inputline);
        return onPressWriteToCMD();
    }

    public ArrayList<String> waitUntilRecv(String endsign, double timeOutSec) {
        return cmdrun.waitUntilRecv(endsign, timeOutSec, cbx_verbose.isSelected());

    }

// -----------------------  ----------------------- ----------------------- -----------------------
    public static void main(String args[]) {

        JFrame frame = new JFrame();

        CMDrun_Panel cmdrun_panel = new CMDrun_Panel();
        frame.add(cmdrun_panel);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    cmdrun_panel.onPanelExit();
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

// -----------------------  ----------------------- ----------------------- -----------------------
    String writeprefix = String.format("%-3s", ">>");
    String readprefix = String.format("%-3s", "<<");

    CMDrun cmdrun = new CMDrun();
    Thread Thread_outputs;

    utils ut = new utils();

    private boolean initialized = false;

    private class sync_output implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (!cmdrun.isProcessAlive()) {
                    break;
                }

                int start = ta_outputs.getLineCount();
                int end = cmdrun.Readers.size();

                if (end == 1) {
                    ta_outputs.setText(String.valueOf(ta_inputs.getLineCount() - 1) + readprefix + cmdrun.Readers.get(0));
                } else {
                    if (start < end) {
                        for (int i = start; i < end; i++) {
                            ta_outputs.append("\n" + String.valueOf(ta_inputs.getLineCount() - 1) + readprefix + cmdrun.Readers.get(i));
                        }
                    }
                }
            }
            System.out.println("sync_output: runnable normally terminated.");
        }
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    /**
     * Creates new form CMDrun1_Panel
     */
    public CMDrun_Panel() {
        onPanelStart();
    }

    public void onPanelExit() {
        onUnSelectCMDrun();
        initialized = false;
    }

    public void onPanelStart() {
        if (!initialized) {
            initComponents();

            ta_outputs.setLineWrap(false);
            ta_outputs.setWrapStyleWord(true);
            ta_inputs.setLineWrap(false);
            ta_inputs.setWrapStyleWord(true);

            DefaultCaret caret = (DefaultCaret) ta_outputs.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            ta_outputs.setCaret(caret);
            caret = (DefaultCaret) ta_inputs.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            ta_inputs.setCaret(caret);

        }
        initialized = true;
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    public boolean onSelectCMDrun() {
        // stop previous Thread_outputs if still alive
        ut.stop_thread(Thread_outputs, 0.1, cbx_verbose.isSelected());

        // start cmd
        cmdrun.startProcess(cbx_verbose.isSelected());
        boolean isCMDAlive = cmdrun.isProcessAlive();

        // start Thread_outputs & change GUI accordingly
        tbt_cmd_run.setSelected(isCMDAlive);
        bt_write_to_cmd.setEnabled(isCMDAlive);

        if (isCMDAlive) {
            ta_outputs.setText("");
            ta_inputs.setText("");
            tf_inputline.setText("");
            tf_inputline.setForeground(Color.black);

            Thread_outputs = new Thread(new sync_output());
            Thread_outputs.start();
        }

        return isCMDAlive;
    }

    public boolean onUnSelectCMDrun() {
        // stop cmd
        double timeoutSec = (double) jsp_timeout_sec.getValue();
        boolean isallstopped = cmdrun.stopProcess(timeoutSec, cbx_verbose.isSelected());

        // change GUI accordingly
        tbt_cmd_run.setSelected(!isallstopped);
        bt_write_to_cmd.setEnabled(!isallstopped);

        // stop Thread_outputs accordingly
        if (!isallstopped) {
            isallstopped &= ut.stop_thread(Thread_outputs, 0.1, cbx_verbose.isSelected());
        }

        return isallstopped;
    }

    public boolean onPressWriteToCMD() {
        String sendstr = tf_inputline.getText();
        boolean sent = cmdrun.Write(sendstr, cbx_verbose.isSelected());
        if (sent) {
            tf_inputline.setText("");
            tf_inputline.setForeground(Color.black);
            ta_inputs.append(String.valueOf(ta_inputs.getLineCount()) + writeprefix + sendstr + "\n");
        } else {
            tf_inputline.setForeground(Color.red);
        }
        return sent;
    }

// -----------------------  ----------------------- ----------------------- -----------------------
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tbt_cmd_run = new javax.swing.JToggleButton();
        jLabel1 = new javax.swing.JLabel();
        jsp_timeout_sec = new javax.swing.JSpinner();
        tf_inputline = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        bt_write_to_cmd = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta_inputs = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        ta_outputs = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        cbx_verbose = new javax.swing.JCheckBox();

        tbt_cmd_run.setText("CMD run");
        tbt_cmd_run.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_cmd_runActionPerformed(evt);
            }
        });

        jLabel1.setText("CMDoff TimeOut [sec]:");

        jsp_timeout_sec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));

        jLabel2.setText("InputLine");

        bt_write_to_cmd.setText("Write To CMD");
        bt_write_to_cmd.setEnabled(tbt_cmd_run.isSelected());
        bt_write_to_cmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_write_to_cmdActionPerformed(evt);
            }
        });

        ta_inputs.setEditable(false);
        ta_inputs.setColumns(20);
        ta_inputs.setRows(5);
        jScrollPane1.setViewportView(ta_inputs);

        jLabel3.setText("All OutputLines");

        jLabel4.setText("All InputLines");

        ta_outputs.setEditable(false);
        ta_outputs.setColumns(20);
        ta_outputs.setRows(5);
        jScrollPane2.setViewportView(ta_outputs);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("!!! CMDrun updates outputs NOT real-time BUT @std in/out !!!");

        cbx_verbose.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        cbx_verbose.setSelected(true);
        cbx_verbose.setText("verbose");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tf_inputline)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(bt_write_to_cmd))
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tbt_cmd_run)
                                .addGap(30, 30, 30)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(50, 50, 50)
                                .addComponent(cbx_verbose))
                            .addComponent(jLabel5))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_cmd_run)
                    .addComponent(jLabel1)
                    .addComponent(jsp_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbx_verbose))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tf_inputline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bt_write_to_cmd)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbt_cmd_runActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_cmd_runActionPerformed
        // TODO add your handling code here:
        if (tbt_cmd_run.isSelected()) {
            onSelectCMDrun();
        } else {
            onUnSelectCMDrun();
        }
    }//GEN-LAST:event_tbt_cmd_runActionPerformed

    private void bt_write_to_cmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_write_to_cmdActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_write_to_cmd.getText())) {
            onPressWriteToCMD();
        }
    }//GEN-LAST:event_bt_write_to_cmdActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_write_to_cmd;
    private javax.swing.JCheckBox cbx_verbose;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner jsp_timeout_sec;
    private javax.swing.JTextArea ta_inputs;
    private javax.swing.JTextArea ta_outputs;
    private javax.swing.JToggleButton tbt_cmd_run;
    private javax.swing.JTextField tf_inputline;
    // End of variables declaration//GEN-END:variables
}
