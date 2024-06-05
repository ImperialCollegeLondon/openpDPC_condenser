/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Socket;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author h.liu
 */
public class SocketComm_Panel extends javax.swing.JPanel {

    public void set_ConnServerToClient_timeoutSec(double timeoutSec) {
        jsp_conn_server_to_client_timeout_sec.setValue(timeoutSec);
    }

    public double get_ConnServerToClient_timeoutSec() {
        return (double) jsp_conn_server_to_client_timeout_sec.getValue();
    }

    public boolean set_UseServer(boolean use_server) {
        if (cbx_use_server.isEnabled()) {
            cbx_use_server.setSelected(use_server);
        }

        return cbx_use_server.isSelected();
    }

    public String get_SocketPort_string() {
        return String.valueOf(jsp_port.getValue());
    }

    public boolean set_SocketPort(int port) {
        if (jsp_port.isEnabled()) {
            jsp_port.setValue(port);
        }

        return ((int) jsp_port.getValue()) == port;
    }

    public String get_SocketPort() {
        return String.valueOf(sockcomm_.getSocketPort());
    }

    public boolean Send(String str) {
        jtf_send_line.setText(str);
        return onPress_SendLine();
    }

    public ArrayList<String> waitUntilRecv(String endsign, double timeOutSec) { 
        String endsign_ = endsign == null ? "null" : endsign;
        jtf_recv_endsign.setText(endsign_);
        jsp_recv_timeout_sec.setValue(timeOutSec); 
        ArrayList<String> output = onPress_Recv();  
        return output;
    }

// ---------------------------- ---------------------------------- -------------------------
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        SocketComm_Panel sock_panel = new SocketComm_Panel();

        frame.add(sock_panel);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    sock_panel.onPanelExit();// stop live thread and turn off command running process
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

// ------------------------------- ------------------------------- --------------------------
    public SocketComm sockcomm_ = new SocketComm();
    private boolean initialized = false;

    public void onPanelStart() {
        if (!initialized) {
            initComponents();
        }
        initialized = true;
    }

    public void onPanelExit() {
        tbt_conn_server_to_client.setSelected(false);
        onUnSelect_ConnServerToClient();

        tbt_socket_on.setSelected(false);
        onUNSelect_SocketON();

        initialized = false;
    }

// ------------------------------- ------------------------------- --------------------------
    public boolean onSelect_SocketON() {
        int port = (int) jsp_port.getValue();
        boolean use_server = cbx_use_server.isSelected();
        boolean sock_on = sockcomm_.socket_ON(port, use_server, jcbx_verbose.isSelected());

        tbt_socket_on.setSelected(sock_on);
        cbx_use_server.setEnabled(!sock_on);
        if (sock_on) {
            jsp_port.setForeground(Color.black);
            jsp_port.setValue(sockcomm_.getSocketPort());
        } else {
            jsp_port.setForeground(Color.red);
        }
        jsp_port.setEnabled(!sock_on);

        return sock_on;
    }

    public boolean onUNSelect_SocketON() {
        onUnSelect_ConnServerToClient();

        boolean issockoff = sockcomm_.socket_OFF(jcbx_verbose.isSelected());

        tbt_socket_on.setSelected(!issockoff);
        cbx_use_server.setEnabled(issockoff);
        jsp_port.setEnabled(issockoff);

        return issockoff;
    }

    public boolean onSelect_ConnServerToClient() {
        double timeOutsec = (double) jsp_conn_server_to_client_timeout_sec.getValue();
        boolean isServerConnToClient = sockcomm_.ConnServerToClient(timeOutsec, jcbx_verbose.isSelected());

        tbt_conn_server_to_client.setSelected(isServerConnToClient);
        jsp_conn_server_to_client_timeout_sec.setEnabled(!isServerConnToClient);

        return isServerConnToClient;
    }

    public boolean onUnSelect_ConnServerToClient() {
        boolean serverNotConnToClient = true;
        if (sockcomm_.use_server_) {
            serverNotConnToClient = sockcomm_.DisconnectClientSock(jcbx_verbose.isSelected());
        }

        this.tbt_conn_server_to_client.setSelected(!serverNotConnToClient);
        this.jsp_conn_server_to_client_timeout_sec.setEnabled(serverNotConnToClient);

        return serverNotConnToClient;
    }

    public boolean onPress_SendLine() {
        boolean send_succeed = sockcomm_.Send(jtf_send_line.getText(), jcbx_verbose.isSelected());
        if (!send_succeed) {
            jtf_send_line.setForeground(Color.red);
        } else {
            jtf_send_line.setForeground(Color.BLACK);
        }
        return send_succeed;
    }

    public ArrayList<String> onPress_Recv() {
        double timeOutsec = (double) jsp_recv_timeout_sec.getValue();
        String endsign = jtf_recv_endsign.getText().equals("null") ? null : jtf_recv_endsign.getText();

        jsp_recv_timeout_sec.setEnabled(false);
        jtf_recv_endsign.setEnabled(false);

        ArrayList<String> recvstrs = sockcomm_.waitUntilRecv(endsign, timeOutsec, jcbx_verbose.isSelected());

        jsp_recv_timeout_sec.setEnabled(true);
        jtf_recv_endsign.setEnabled(true);

        jta_recvstrs.setText("");
        if (!recvstrs.isEmpty()) {
            recvstrs.forEach(line -> jta_recvstrs.append(line + "\n"));
        }

        return recvstrs;
    }

// ------------------------------- ------------------------------- --------------------------
    /**
     * Creates new form ServerSockComm_Panel
     */
    public SocketComm_Panel() {
        onPanelStart();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jsp_port = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jsp_conn_server_to_client_timeout_sec = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jsp_recv_timeout_sec = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        tbt_socket_on = new javax.swing.JToggleButton();
        tbt_conn_server_to_client = new javax.swing.JToggleButton();
        bt_send_line = new javax.swing.JButton();
        bt_recv_w_endsign = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jtf_send_line = new javax.swing.JTextField();
        cbx_use_server = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jta_recvstrs = new javax.swing.JTextArea();
        jtf_recv_endsign = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jcbx_verbose = new javax.swing.JCheckBox();

        jsp_port.setModel(new javax.swing.SpinnerNumberModel(9999, 0, 65535, 1));
        jsp_port.setEditor(new javax.swing.JSpinner.NumberEditor(jsp_port, "#"));

        jLabel1.setText("Port #:");

        jsp_conn_server_to_client_timeout_sec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.1d));

        jLabel2.setText("ConnServerToClient TimeOut [sec]:");

        jsp_recv_timeout_sec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.0010000000000000009d));

        jLabel3.setText("Recv TimeOut [sec]:");

        tbt_socket_on.setText("Socket ON");
        tbt_socket_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_socket_onActionPerformed(evt);
            }
        });

        tbt_conn_server_to_client.setText("Connect Server to Client");
        tbt_conn_server_to_client.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_conn_server_to_clientActionPerformed(evt);
            }
        });

        bt_send_line.setText("Send line");
        bt_send_line.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_send_lineActionPerformed(evt);
            }
        });

        bt_recv_w_endsign.setText("Recv");
        bt_recv_w_endsign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_recv_w_endsignActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 3, 18)); // NOI18N
        jLabel4.setText("!!! zero/negative TimeOut = infinite TimeOut !!!");

        cbx_use_server.setText("use as server");

        jta_recvstrs.setColumns(20);
        jta_recvstrs.setRows(5);
        jScrollPane1.setViewportView(jta_recvstrs);

        jLabel7.setText("Recv Endsign:");

        jLabel5.setText("empty -- one line; null -- loop until timeout");

        jcbx_verbose.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jcbx_verbose.setSelected(true);
        jcbx_verbose.setText("verbose");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jtf_send_line)
                    .addComponent(jSeparator3)
                    .addComponent(jScrollPane1)
                    .addComponent(jSeparator4)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtf_recv_endsign)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel5))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tbt_socket_on)
                            .addComponent(tbt_conn_server_to_client)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_conn_server_to_client_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(bt_send_line)
                            .addComponent(bt_recv_w_endsign)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_recv_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(cbx_use_server)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jcbx_verbose)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                        .addComponent(jLabel4)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsp_port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(cbx_use_server))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbt_socket_on)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsp_conn_server_to_client_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbt_conn_server_to_client)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jtf_send_line, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bt_send_line)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jtf_recv_endsign, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(3, 3, 3)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jsp_recv_timeout_sec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bt_recv_w_endsign)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jcbx_verbose)
                    .addComponent(jLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbt_socket_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_socket_onActionPerformed
        // TODO add your handling code here:
        if (tbt_socket_on.isSelected()) {
            onSelect_SocketON();
        } else {
            onUNSelect_SocketON();
        }
    }//GEN-LAST:event_tbt_socket_onActionPerformed

    private void tbt_conn_server_to_clientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_conn_server_to_clientActionPerformed
        // TODO add your handling code here:
        if (tbt_conn_server_to_client.isSelected()) {
            onSelect_ConnServerToClient();
        } else {
            onUnSelect_ConnServerToClient();
        }
    }//GEN-LAST:event_tbt_conn_server_to_clientActionPerformed

    private void bt_send_lineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_send_lineActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_send_line.getText())) {
            onPress_SendLine();
        }
    }//GEN-LAST:event_bt_send_lineActionPerformed

    private void bt_recv_w_endsignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_recv_w_endsignActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_recv_w_endsign.getText())) {
            onPress_Recv();
        }
    }//GEN-LAST:event_bt_recv_w_endsignActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_recv_w_endsign;
    private javax.swing.JButton bt_send_line;
    private javax.swing.JCheckBox cbx_use_server;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JCheckBox jcbx_verbose;
    private javax.swing.JSpinner jsp_conn_server_to_client_timeout_sec;
    private javax.swing.JSpinner jsp_port;
    private javax.swing.JSpinner jsp_recv_timeout_sec;
    private javax.swing.JTextArea jta_recvstrs;
    private javax.swing.JTextField jtf_recv_endsign;
    private javax.swing.JTextField jtf_send_line;
    private javax.swing.JToggleButton tbt_conn_server_to_client;
    private javax.swing.JToggleButton tbt_socket_on;
    // End of variables declaration//GEN-END:variables
}
