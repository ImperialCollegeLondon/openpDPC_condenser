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

import CMDrun.CMDrun_Panel;
import Paras_Presets.Preset_Panel;
import Socket.SocketComm_Panel;
import Utils.MM2_utils;
import Utils.utils;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.internal.MMStudio;


public class pDPC_ExtThread_Panel extends javax.swing.JPanel {

    public static void main(String[] args) {
        MMStudio gui = new MMStudio(false);

        JFrame frame = new JFrame();

        pDPC_ExtThread_Panel img_Panel = new pDPC_ExtThread_Panel();
        img_Panel.set_parent(gui);

        frame.add(img_Panel);
        frame.pack();

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

////////////////////////////////////////////////////////////////////////////////
    utils ut = new utils();
    MM2_utils MM2ut = new MM2_utils();

    JFrame cmdRun_Frame = new JFrame();
    JFrame Socket_Frame = new JFrame();
    JFrame preset_Frame = new JFrame();
    JFrame livesplit_Frame = new JFrame();

    CMDrun_Panel cmdRun_Pane = new CMDrun_Panel();
    SocketComm_Panel Socket_Pane = new SocketComm_Panel();
    Preset_Panel preset_Pane = new Preset_Panel();
    Polcam_liveSplitRaw_Panel livesplit_Pane = new Polcam_liveSplitRaw_Panel();

    Thread loopThread = null;
    Studio parent = null;

    private boolean initialized = false;
    private boolean verbose = true;

    private String sep = System.getProperty("file.separator");
    private String pyfilepath = null;
    private String imgsaving_directory = null;
    private String historypath = null;

    private String live_rawname = "raw.tif";
    private String live_phasename = "phase.tif";
    private String live_dispname = "Phase";

    private String cali_settingname = "cali_paths.txt";

    private String goodsign = "proceed";
    private String badsign = "error";
    private String exitsign = "exit";
    private String pystartsign = "python program started";
    
    private boolean do_absorption = false;
    private int sock_bufsize = 1024; 
    private double loopThread_timeOutSec = 5;
    private double pDPClive_timeOutSec = 1;
    private double pDPCrecon_timeOutSec = 1;
    private double pDPCcali_timeOutSec = 5;
    private boolean is_pDPCrecon_running = false;
    private boolean is_pDPCcali_running = false;

    public Icon get_running_Icon(int width, int height) { 
        ImageIcon icon = (ImageIcon) UIManager.getIcon("OptionPane.informationIcon"); 
        
        icon.setImage(
            icon.getImage().getScaledInstance(
                width, height, java.awt.Image.SCALE_SMOOTH
            )
        );
        
        return icon; 
    }

////////////////////////////////////////////////////////////////////////////////
    private class RunLoopThread implements Runnable {

        public boolean onStop_loop() {

            // set all tbt to unselected
            tbt_pDPClive_on.setSelected(false);
            onUNSelect_tbt_pDPClive_on();

            is_pDPCrecon_running = false;
            tbt_pDPCrecon_on.setSelected(false);
            onUNSelect_tbt_pDPCrecon_on();

            is_pDPCcali_running = false;
            tbt_pDPCcali_on.setSelected(false);
            onUNSelect_tbt_pDPCcali_on();

            tbt_loopThread_on.setSelected(false);

            boolean stopped = true;

            // exit python, sock, cmd 
            Socket_Pane.Send(exitsign);
            stopped &= Socket_Pane.onUnSelect_ConnServerToClient();
            stopped &= Socket_Pane.onUNSelect_SocketON();
            stopped &= cmdRun_Pane.onUnSelectCMDrun();

            // update gui accordingly
            tbt_loopThread_on.setSelected(!stopped);
            is_pDPCrecon_running = !stopped;
            is_pDPCcali_running = !stopped;
            tbt_pDPClive_on.setEnabled(!stopped);
            tbt_pDPCrecon_on.setEnabled(!stopped);
            tbt_pDPCcali_on.setEnabled(!stopped);
            setEnabled_choose_Python_Pane(stopped);
            tbp_allbutlog.setIconAt(0, null);

            return stopped;
        }

        public boolean onStart_loop() {
            boolean proceed = true;
            String errorinfo = "Failed to start loopThread: ";
            ta_log.setText("");

            // 1. check if imgsaving directory & python program exists at given folder
            if (pyfilepath == null || imgsaving_directory == null) {
                proceed = false;
            } else {
                proceed &= (new File(pyfilepath).isFile()
                        & new File(imgsaving_directory).isDirectory());
            }
            if (!proceed) {
                ta_log.setText(errorinfo
                        + "python file or image saving directory not exists\n"
                        + String.valueOf(pyfilepath)
                        + "\n" + String.valueOf(imgsaving_directory));
                tbt_loopThread_on.setSelected(proceed);
                onStop_loop();
                return proceed;
            }

            // 2. start a server socket
            Socket_Pane.set_UseServer(true);
            proceed &= Socket_Pane.onSelect_SocketON();
            if (!proceed) {
                ta_log.setText(
                        errorinfo
                        + "cannot create server socket on port"
                        + Socket_Pane.get_SocketPort()
                );
                tbt_loopThread_on.setSelected(proceed);
                onStop_loop();
                return proceed;
            }

            // 3. activate python env and run pDPC.py with server port
            proceed &= choose_Python_Pane.onSelect_current_env(true);
            if (!proceed) {
                ta_log.setText(errorinfo + "failed to select a valid conda env");
                tbt_loopThread_on.setSelected(proceed);
                onStop_loop();
                return proceed;
            }

            String activate_conda = choose_Python_Pane.get_conda_activate_cmd(choose_Python_Pane.get_conda_path());
            String activate_env = choose_Python_Pane.get_env_activate_cmd(choose_Python_Pane.get_current_conda_env()); 
            String run_pyfile = "python" + " "
                    + "\"" + pyfilepath + "\""
                    + " " + String.valueOf(Socket_Pane.get_SocketPort())
                    + " " + goodsign + " " + badsign + " " + exitsign 
                    + " " + String.valueOf(sock_bufsize) 
                    + " " + String.valueOf(do_absorption);
            cmdRun_Pane.onSelectCMDrun();
            cmdRun_Pane.Send(activate_conda);
            cmdRun_Pane.Send(activate_env);
            cmdRun_Pane.Send(run_pyfile);
            // cannot use goodsign as it's included in input cmds 
            ArrayList<String> outputs = cmdRun_Pane.waitUntilRecv(pystartsign,
                    loopThread_timeOutSec);
            proceed &= check_Recv(pystartsign, outputs);
            if (!proceed) {
                String listString = String.join("\n", outputs);
                ta_log.setText(errorinfo + "failed to run python file.\nOutputs:\n" + listString);
                tbt_loopThread_on.setSelected(proceed);
                onStop_loop();
                return proceed;
            }
            cmdRun_Pane.Send(goodsign);

            // 4. connect python client socket to java server socket
            double default_sock_timeoutsec = Socket_Pane.get_ConnServerToClient_timeoutSec();
            Socket_Pane.set_ConnServerToClient_timeoutSec(10);
            proceed &= Socket_Pane.onSelect_ConnServerToClient();
            Socket_Pane.set_ConnServerToClient_timeoutSec(default_sock_timeoutsec);
            if (!proceed) {
                ta_log.setText(errorinfo + "failed to connect python client socket to server");
                tbt_loopThread_on.setSelected(proceed);
                onStop_loop();
                return proceed;
            }

            // 5. update gui enable & selection accordingly
            tbt_loopThread_on.setSelected(proceed);
            tbt_pDPClive_on.setEnabled(proceed);
            tbt_pDPCrecon_on.setEnabled(proceed);
            tbt_pDPCcali_on.setEnabled(proceed);
            setEnabled_choose_Python_Pane(!proceed);
            tbp_allbutlog.setIconAt(0, get_running_Icon(10, 10));

            return proceed;
        }

        @Override
        public void run() {
            boolean proceed = onStart_loop();

            if (proceed) {
                ta_log.setText("");
                if (verbose) {
                    System.out.println("RunLoopThread: Thread loop starts now");
                }

                while (!Thread.currentThread().isInterrupted()) {
                    // check break conditions -- can add more as needed
                    if (!tbt_loopThread_on.isSelected()) {
                        break;
                    }

                    // loop main contents
                    if (tbt_pDPClive_on.isSelected()) {
                        if (parent != null) {
                            if (parent.getSnapLiveManager().isLiveModeOn()) {
                                parent.getSnapLiveManager().setLiveModeOn(false);
                            }
                            livesplit_Pane.set_use_external_thread(true);

                            if (parent.getAcquisitionManager().isAcquisitionRunning()) {
                                ta_log.setText("pDPClive failed to start: MDA is running");
                                tbt_pDPClive_on.setSelected(false);
                                onUNSelect_tbt_pDPClive_on();
                            } else {
                                Image raw = MM2ut.SnapImg(parent, verbose);
                                ImageProcessor live_img_proc = MM2ut.get_imgproc_from_image(parent, raw, verbose);
                                boolean rawready = MM2ut.SaveImg(live_img_proc, get_MM2pDPClive_rawpath(), verbose);

//                                boolean rawready = MM2ut.SnapAndSave_Img(parent, get_MM2pDPClive_rawpath(), verbose);
                                if (rawready) {
                                    Socket_Pane.Send(get_pDPClive_message());
                                    ArrayList<String> outputs = Socket_Pane.waitUntilRecv("", pDPClive_timeOutSec);
                                    boolean good = check_Recv(goodsign, outputs);

                                    if (good) {
//                                        ImageProcessor imgproc = new ImagePlus(get_MM2pDPClive_phasepath()).getProcessor();
//                                        imgproc.subtract(imgproc.getStatistics().dmode);
//                                        ImagePlus imgtoshow = new ImagePlus(live_dispname, imgproc);

                                        ImagePlus imgtoshow = new ImagePlus(get_MM2pDPClive_phasepath());
                                        imgtoshow.setTitle(live_dispname);
                                        ImagePlus current_win = ij.WindowManager.getImage(live_dispname);
                                        if (current_win == null) {
                                            ImageWindow imgWin = new ImageWindow(imgtoshow);
                                            imgWin.setVisible(true);
                                        } else {
                                            LUT[] lut = current_win.getLuts();

                                            double cmin = current_win.getDisplayRangeMin();
                                            double cmax = current_win.getDisplayRangeMax();

                                            current_win.setImage(imgtoshow);

                                            current_win.setLut(lut[0]);
                                            current_win.setDisplayRange(cmin, cmax);
                                        }

                                        // update live split wins as well
                                        if (livesplit_Pane.get_livesplit_on()) {
                                            livesplit_Pane.update_liveWins(raw);
                                        }

                                        ta_log.setText("");
                                    } else {
                                        if (check_Recv(badsign, outputs)) {
                                            String line = outputs.get(outputs.size() - 1);
                                            ta_log.setText(line);
                                        } else {
                                            ta_log.setText("pDPClive TimeOut");
                                        }
                                        tbt_pDPClive_on.setSelected(false);
                                        onUNSelect_tbt_pDPClive_on();
                                    }
                                } else {
                                    ta_log.setText("Failed to snap & save image to " + get_MM2pDPClive_rawpath());
                                }
                            }
                        } else {
                            ta_log.setText("MM2 GUI & Core not available");
                        }
                    } else {
                        if (is_pDPCrecon_running) {
                            ArrayList<String> outputs = Socket_Pane.waitUntilRecv("", pDPCrecon_timeOutSec);
                            boolean good = check_Recv(goodsign, outputs);
                            if (good) {
                                ta_log.setText("");
                                String line = outputs.get(outputs.size() - 1);
                                int progress = Integer.parseUnsignedInt(line.substring(goodsign.length() + 1));
                                pb_recon.setValue(progress); // this will automatically call onStateChanged_pb_recon
                            } else {
                                if (check_Recv(badsign, outputs)) {
                                    String line = outputs.get(outputs.size() - 1);
                                    is_pDPCrecon_running = false;
                                    tbt_pDPCrecon_on.setSelected(false);
                                    onUNSelect_tbt_pDPCrecon_on();
                                    ta_log.setText(line);
                                } else {
                                    ta_log.setText("Waiting message from pDPCrecon...");
                                }
                            }
                        } else {
                            if (is_pDPCcali_running) {
                                ArrayList<String> outputs = Socket_Pane.waitUntilRecv("", pDPCcali_timeOutSec);
                                boolean good = check_Recv(goodsign, outputs);
                                boolean bad = check_Recv(badsign, outputs);
                                if (good || bad) {
                                    // finished
                                    String line = outputs.get(outputs.size() - 1);
                                    if (good) {
                                        ta_pDPCcali_result.setText(line.substring(goodsign.length() + 1));
                                        ta_log.setText("");
                                    } else {
                                        ta_pDPCcali_result.setText("");
                                        ta_log.setText(line);
                                    }

                                    is_pDPCcali_running = false;
                                    tbt_pDPCcali_on.setSelected(false);
                                    onUNSelect_tbt_pDPCcali_on();
                                } else {
                                    // still waiting 
                                    String pre_str = "Waiting message from pDPCcali...\n";
                                    String received_str = outputs.isEmpty() ? "" : outputs.get(outputs.size() - 1);

                                    System.out.println(pre_str + received_str);

                                    String Q2S_prefix = "Q2S_cali=";
                                    int Q2S_pb = 20;
                                    String part_normCoeffs_prefix = "quadNorm_coeff loop";

                                    if (received_str.length() > 0 && received_str.contains(Q2S_prefix)) {
                                        if (pb_cali.getValue() < Q2S_pb) {
                                            pb_cali.setValue(Q2S_pb);
                                        }
                                    } else {
                                        if (received_str.length() > 0 && received_str.contains(part_normCoeffs_prefix)) {
                                            String[] strs = received_str.split("/");
                                            if (strs.length > 0) {
                                                try {
                                                    int v = Integer.parseInt(strs[0].substring(part_normCoeffs_prefix.length() + 1));
                                                    int progress = (int) ((pb_cali.getMaximum() - Q2S_pb) * 0.01 * v + Q2S_pb);

                                                    pb_cali.setValue(progress);
                                                } catch (Exception e) {
                                                    System.out.println("Failed to read quadNorm_coeff progress.\n" + String.valueOf(e));
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }

                // if normal exit loopthread, close everything needed
                boolean stopped = onStop_loop();
                if (stopped && verbose) {
                    System.out.println("RunLoopThread: Thread loop terminated normally");
                }
            } else {
                if (verbose) {
                    System.out.println("RunLoopThread: Thread loop failed to start");
                }
            }
        }

    }

////////////////////////////////////////////////////////////////////////////////
    public void set_parent(Studio parent_) {
        parent = parent_;
        pDPCrecon_path_Pane.set_parent(parent);
        livesplit_Pane.set_gui(parent);
    }

    public void setpluginfolder(String pathstr) {
        imgsaving_directory = pathstr;
    }

    public void setpyfilepath(String pathstr) {
        pyfilepath = pathstr;
    }

    public void setpresetfilepath(String pathstr) {
        preset_Pane.set_preset_filepath(pathstr);
        preset_Pane.setEditable_tf_preset_filepath(false);
    }

    public void sethistoryfilepath(String extThread_historyfilepath) {
        historypath = extThread_historyfilepath;

        if (historypath != null) {
            File historyfile = new File(historypath);
            if (historyfile.exists()) {
                try {
                    List<String> lines = Files.readAllLines(historyfile.toPath());

                    String conda_path = null;
                    String conda_env = null;
                    double loopThreadTimeOutSec = -1;
                    double liveImgTimeOutSec = -1;
                    double reconImgTimeOutSec = -1;
                    double caliImgTimeOutSec = -1;
                    int socketPort = -1;
                    int sockBufSize = -1;

                    for (String line : lines) {
                        if (line.startsWith("conda_path=")) {
                            conda_path = line.replaceFirst("conda_path=", "");
                        }
                        if (line.startsWith("conda_env=")) {
                            conda_env = line.replaceFirst("conda_env=", "");
                        }
                        if (line.startsWith("loopThreadTimeOutSec=")) {
                            try {
                                loopThreadTimeOutSec = Double.parseDouble(line.replaceFirst("loopThreadTimeOutSec=", ""));
                            } catch (Exception e) {
                                loopThreadTimeOutSec = -1;
                            }
                        }
                        if (line.startsWith("liveImgTimeOutSec=")) {
                            try {
                                liveImgTimeOutSec = Double.parseDouble(line.replaceFirst("liveImgTimeOutSec=", ""));
                            } catch (Exception e) {
                                liveImgTimeOutSec = -1;
                            }
                        }
                        if (line.startsWith("recontimeout_str=")) {
                            try {
                                reconImgTimeOutSec = Double.parseDouble(line.replaceFirst("recontimeout_str=", ""));
                            } catch (Exception e) {
                                reconImgTimeOutSec = -1;
                            }
                        }
                        if (line.startsWith("calitimeout_str=")) {
                            try {
                                caliImgTimeOutSec = Double.parseDouble(line.replaceFirst("calitimeout_str=", ""));
                            } catch (Exception e) {
                                caliImgTimeOutSec = -1;
                            }
                        }
                        if (line.startsWith("socketPort=")) {
                            try {
                                socketPort = Integer.parseInt(line.replaceFirst("socketPort=", ""));
                            } catch (Exception e) {
                                socketPort = -1;
                            }
                        }
                        if (line.startsWith("sockBufSize=")) {
                            try {
                                sockBufSize = Integer.parseInt(line.replaceFirst("sockBufSize=", ""));
                            } catch (Exception e) {
                                sockBufSize = -1;
                            }
                        }
                    }

                    if (conda_path != null) {
                        choose_Python_Pane.set_conda_path(conda_path);
                    }
                    if (conda_env != null) {
                        choose_Python_Pane.set_current_conda_env(conda_env);
                    }
                    if (loopThreadTimeOutSec > 0) {
                        jsp_loopThread_timeOutSec.setValue(loopThreadTimeOutSec);
                        update_loopThread_timeOutSec();
                    }
                    if (liveImgTimeOutSec > 0) {
                        jsp_pDPClive_timeOutSec.setValue(liveImgTimeOutSec);
                        update_pDPCLive_timeOutSec();
                    }
                    if (reconImgTimeOutSec > 0) {
                        jsp_pDPCrecon_timeOutSec.setValue(reconImgTimeOutSec);
                        update_pDPCrecon_timeOutSec();
                    }
                    if (caliImgTimeOutSec > 0) {
                        jsp_pDPCcali_timeOutSec.setValue(caliImgTimeOutSec);
                        update_pDPCcali_timeOutSec();
                    }
                    if (socketPort > 0) {
                        Socket_Pane.set_SocketPort(socketPort);
                    }
                    if (sockBufSize > 0){
                        jsp_sock_bufsize.setValue(sockBufSize);
                        update_sock_bufsize();
                    }

                } catch (Exception e) {
                    if (verbose) {
                        System.out.println("Failed to read history conda path & env");
                        e.printStackTrace(System.out);
                    }
                }
            }
        }

    }

    public void save_history_onExit() {
        if (historypath != null) {
            File historyfile = new File(historypath);
            if (historyfile.exists()) {
                String conda_path_str = "conda_path=" + choose_Python_Pane.get_conda_path();
                String conda_env_str = "conda_env=" + choose_Python_Pane.get_current_conda_env();
                String loopthreadtimeout_str = "loopThreadTimeOutSec=" + String.valueOf(loopThread_timeOutSec);
                String imglivetimeout_str = "liveImgTimeOutSec=" + String.valueOf(pDPClive_timeOutSec);
                String recontimeout_str = "reconImgTimeOutSec=" + String.valueOf(pDPCrecon_timeOutSec);
                String calitimeout_str = "caliImgTimeOutSec=" + String.valueOf(pDPCcali_timeOutSec);
                String sock_port_str = "socketPort=" + Socket_Pane.get_SocketPort_string();
                String sock_bufsize_str = "sockBufSize=" + String.valueOf(sock_bufsize);

                String jstr = conda_path_str + "\n" + conda_env_str
                        + "\n" + loopthreadtimeout_str
                        + "\n" + imglivetimeout_str
                        + "\n" + recontimeout_str
                        + "\n" + calitimeout_str
                        + "\n" + sock_port_str
                        + "\n" + sock_bufsize_str;
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(historyfile));
                    writer.write(jstr);
                    writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(pDPC_ExtThread_Panel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean check_Recv(String sign, ArrayList<String> recvstrs) {
        boolean good = true;
        if (!String.valueOf(sign).equalsIgnoreCase("null")) {
            if (recvstrs == null || recvstrs.isEmpty()) {
                good = false;
            } else {
                if (sign.length() > 0 && !recvstrs.get(recvstrs.size() - 1).contains(sign)) {
                    good = false;
                }
            }
        }
        return good;
    }

////////////////////////////////////////////////////////////////////////////////
    public void OnSelect_tbt_show_livesplit_Pane() {
        livesplit_Frame.setVisible(true);
        tbt_show_livesplit_Pane.setSelected(livesplit_Frame.isVisible());
    }

    public void OnUNSelect_tbt_show_livesplit_Pane() {
        livesplit_Frame.setVisible(false);
        tbt_show_livesplit_Pane.setSelected(livesplit_Frame.isVisible());
    }

    public void OnSelect_tbt_show_preset_Pane() {
        preset_Frame.setVisible(true);
        tbt_show_preset_Pane.setSelected(preset_Frame.isVisible());
    }

    public void OnUNSelect_tbt_show_preset_Pane() {
        preset_Frame.setVisible(false);
        tbt_show_preset_Pane.setSelected(preset_Frame.isVisible());
    }

    public void OnSelect_tbt_show_serverSock_Pane() {
        Socket_Frame.setVisible(true);
        tbt_show_Socket_Pane.setSelected(Socket_Frame.isVisible());
    }

    public void OnUNSelect_tbt_show_Socket_Pane() {
        Socket_Frame.setVisible(false);
        tbt_show_Socket_Pane.setSelected(Socket_Frame.isVisible());
    }

    public void OnSelect_tbt_show_cmdRun_Pane() {
        cmdRun_Frame.setVisible(true);
        tbt_show_cmdRun_Pane.setSelected(cmdRun_Frame.isVisible());
    }

    public void OnUNSelect_tbt_show_cmdRun_Pane() {
        cmdRun_Frame.setVisible(false);
        tbt_show_cmdRun_Pane.setSelected(cmdRun_Frame.isVisible());
    }

    public void onChanged_cbx_verbose() {
        update_verbose();
    }

    public void onCheck_cbx_cancheckadv() {
        tbt_show_cmdRun_Pane.setEnabled(true);
        tbt_show_Socket_Pane.setEnabled(true);

        cbx_cancheckadv.setSelected(true);
    }

    public void onUNCheck_cbx_cancheckadv() {
        tbt_show_cmdRun_Pane.setSelected(false);
        OnUNSelect_tbt_show_cmdRun_Pane();
        tbt_show_cmdRun_Pane.setEnabled(false);

        tbt_show_Socket_Pane.setSelected(false);
        OnUNSelect_tbt_show_Socket_Pane();
        tbt_show_Socket_Pane.setEnabled(false);

        cbx_cancheckadv.setSelected(false);
    }

    public void onSelect_bt_update_pDPCcali_result_to_current_settings() {
        String mtx_str = ta_pDPCcali_result.getText();

        if (mtx_str.length() > 0) {
            boolean useadvparams = cbx_useadvparams.isSelected();
            if (!useadvparams) {
                cbx_useadvparams.setSelected(true);
                onChange_cbx_useadvparams();
            }
            preset_Pane.jsonto_current_settings(mtx_str);
            if (!useadvparams) {
                cbx_useadvparams.setSelected(false);
                onChange_cbx_useadvparams();
            }
            ta_pDPCcali_result.setText(preset_Pane.get_ta_current_paras_jstr());
        }

    }

////////////////////////////////////////////////////////////////////////////////  
    public void update_sock_bufsize(){
        sock_bufsize = (int) jsp_sock_bufsize.getValue();  
    }
    
    public void update_loopThread_timeOutSec() { 
        loopThread_timeOutSec = (double) jsp_loopThread_timeOutSec.getValue();
    }

    public void update_pDPCLive_timeOutSec() {
        pDPClive_timeOutSec = (double) jsp_pDPClive_timeOutSec.getValue();
    }

    public void update_pDPCrecon_timeOutSec() {
        pDPCrecon_timeOutSec = (double) jsp_pDPCrecon_timeOutSec.getValue();
    }

    public void update_pDPCcali_timeOutSec() {
        pDPCcali_timeOutSec = (double) jsp_pDPCcali_timeOutSec.getValue();
    }

    public void update_verbose() {
        verbose = cbx_verbose.isSelected();
        pDPCrecon_path_Pane.verbose = verbose;
    }

////////////////////////////////////////////////////////////////////////////////
    public void on_Select_tbt_loopThread_on() {
        if (loopThread != null && loopThread.isAlive()) {
            ut.stop_thread(loopThread, loopThread_timeOutSec, verbose);
        }

        loopThread = new Thread(new RunLoopThread());
        loopThread.start();
    }

    public void on_UNSelect_tbt_loopThread_on() {
        tbt_loopThread_on.setSelected(false);

        ut.stop_thread(loopThread, loopThread_timeOutSec, verbose);
    }

    public void onSelect_tbt_pDPClive_on() {
        if (tbt_loopThread_on.isSelected() && !tbt_pDPCrecon_on.isSelected() && !is_pDPCrecon_running
                && !tbt_pDPCcali_on.isSelected() && !is_pDPCcali_running) {
            // disable whatever needed 
            tbt_pDPCrecon_on.setEnabled(false);
            tbt_pDPCcali_on.setEnabled(false);
            jsp_pDPClive_timeOutSec.setEnabled(false);

            tbt_pDPClive_on.setSelected(true);
            tbp_modes.setIconAt(0, this.get_running_Icon(10, 10));
        } else {
            tbt_pDPClive_on.setSelected(false);
            tbp_modes.setIconAt(0, null);
        }
    }

    public void onUNSelect_tbt_pDPClive_on() {
        tbt_pDPClive_on.setSelected(false);
        tbp_modes.setIconAt(0, null);

        livesplit_Pane.onUNSelect_tbt_liveON();
        livesplit_Pane.set_use_external_thread(false);

        tbt_pDPCrecon_on.setEnabled(tbt_loopThread_on.isSelected());
        tbt_pDPCcali_on.setEnabled(tbt_loopThread_on.isSelected());
        jsp_pDPClive_timeOutSec.setEnabled(true);
    }

    public void onSelect_tbt_pDPCrecon_on() {
        if (tbt_loopThread_on.isSelected()
                && !tbt_pDPClive_on.isSelected()
                && !tbt_pDPCcali_on.isSelected()
                && !is_pDPCcali_running
                && pDPCrecon_path_Pane.isallpathready()) {
            // disable whatever needed
            tbt_pDPClive_on.setEnabled(false);
            tbt_pDPCcali_on.setEnabled(false);
            pDPCrecon_path_Pane.setPane_enabled(false);
            jsp_pDPCrecon_timeOutSec.setEnabled(false);
            cbx_openphase_afrecon.setEnabled(false);

            // initiate progress bar 
            pb_recon.setValue(0);
            onStateChanged_pb_recon();

            // start recon by sending info to python and set flags 
            Socket_Pane.Send(get_pDPCrecon_message());

            is_pDPCrecon_running = true;
            tbt_pDPCrecon_on.setSelected(true);

            tbp_modes.setIconAt(1, get_running_Icon(10, 10));
        } else {
            tbt_pDPCrecon_on.setSelected(false);
            tbp_modes.setIconAt(1, null);
            if (!pDPCrecon_path_Pane.isallpathready()) {
                ta_log.setText("Failed to start pDPCrecon: invalid raw & phase path(s)");
            }
        }
    }

    public void onUNSelect_tbt_pDPCrecon_on() {
        // if pDPCrecon still running, do not unselect tbt_pDPCrecon_on
        tbt_pDPCrecon_on.setSelected(tbt_loopThread_on.isSelected() & is_pDPCrecon_running);

        if (!tbt_pDPCrecon_on.isSelected()) {
            // if successfully unselected, enable everything needed
            tbt_pDPClive_on.setEnabled(tbt_loopThread_on.isSelected());
            tbt_pDPCcali_on.setEnabled(tbt_loopThread_on.isSelected());
            pDPCrecon_path_Pane.setPane_enabled(true);
            jsp_pDPCrecon_timeOutSec.setEnabled(true);
            cbx_openphase_afrecon.setEnabled(true);

            tbp_modes.setIconAt(1, null);
        }

    }

    public void onSelect_tbt_pDPCcali_on() {
        if (tbt_loopThread_on.isSelected()
                && !tbt_pDPClive_on.isSelected()
                && !tbt_pDPCrecon_on.isSelected()
                && !is_pDPCrecon_running) {
            // disable whatever needed
            tbt_pDPClive_on.setEnabled(false);
            tbt_pDPCrecon_on.setEnabled(false);
            jsp_pDPCcali_timeOutSec.setEnabled(false);

            // initiate progress bar 
            pb_cali.setValue(0);
            onStateChanged_pb_cali();

            // save current path strs to setting file 
            String svpath_str = imgsaving_directory + sep + cali_settingname;
            ArrayList<String> paths = pDPCcali_path_Pane.get_cali_paths();
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(svpath_str));
                writer.write(String.join("\n", paths));
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(pDPC_ExtThread_Panel.class.getName()).log(Level.SEVERE, null, ex);
                ta_log.setText("Failed to store calibration paths.");
                tbt_pDPCcali_on.setSelected(false);
                onUNSelect_tbt_pDPCcali_on();
                return;
            }

            // start recon by sending info to python and set flags
            Socket_Pane.Send("cali:" + svpath_str);

            is_pDPCcali_running = true;
            tbt_pDPCcali_on.setSelected(true);

            tbp_modes.setIconAt(2, get_running_Icon(10, 10));
        } else {
            tbt_pDPCcali_on.setSelected(false);
            tbp_modes.setIconAt(2, null);
        }
    }

    public void onUNSelect_tbt_pDPCcali_on() {
        // if pDPCcali still running, do not unselect tbt_pDPCcali_on
        tbt_pDPCcali_on.setSelected(tbt_loopThread_on.isSelected() & is_pDPCcali_running);

        if (!tbt_pDPCcali_on.isSelected()) {
            // if successfully unselected, enable everything needed
            tbt_pDPClive_on.setEnabled(tbt_loopThread_on.isSelected());
            tbt_pDPCrecon_on.setEnabled(tbt_loopThread_on.isSelected());
            jsp_pDPCcali_timeOutSec.setEnabled(true);

            tbp_modes.setIconAt(2, null);
        }

    }

    public void onStateChanged_pb_recon() {
        int pbv = pb_recon.getValue();
        lb_pbvalue.setText(String.format("%3d", pbv).concat("%"));

        if (pb_recon.getValue() >= 100) {
            boolean openafrecon = cbx_openphase_afrecon.isSelected();
            String reconphasepath = pDPCrecon_path_Pane.get_phasepath();

            is_pDPCrecon_running = false;
            tbt_pDPCrecon_on.setSelected(false);
            onUNSelect_tbt_pDPCrecon_on();

            if (openafrecon) {
                try {
                    ImagePlus reconedimage = ij.IJ.openImage(reconphasepath);
                    reconedimage.show();
                } catch (Exception e) {
                    ta_log.setText("Recon completed but failed to open phase image.\n" + e.getMessage());
                }
            }
        }
    }

    public void onStateChanged_pb_cali() {
        int pbv = pb_cali.getValue();
    }

    public void onChange_cbx_useadvparams() {
        preset_Pane.set_hideadv(!cbx_useadvparams.isSelected());
    }
    
    public void onChanged_cbx_doAbsorption() {
        do_absorption = cbx_doAbsorption.isSelected();
    }
    
////////////////////////////////////////////////////////////////////////////////
    private void setEnabled_choose_Python_Pane(boolean enabled) {
        for (Component item : choose_Python_Pane.getComponents()) {
            item.setEnabled(enabled);
        }
    }

    private String get_pDPCrecon_message() {
        String message = "recon:"
                + "rawPath:" + pDPCrecon_path_Pane.get_rawpath()
                + "; phasePath:" + pDPCrecon_path_Pane.get_phasepath()
                + "; Params:" + preset_Pane.get_current_settings_json()
                + "; useAdvParams:" + cbx_useadvparams.isSelected();
        return message;
    }

    private String get_pDPClive_message() {
        String rawpath = get_MM2pDPClive_rawpath();
        String phasepath = get_MM2pDPClive_phasepath();
        String message = "live:"
                + "rawPath:" + rawpath
                + "; phasePath:" + phasepath
                + "; Params:" + preset_Pane.get_current_settings_json()
                + "; useAdvParams:" + cbx_useadvparams.isSelected();
        return message;
    }

    private void get_pDPCcali_message() {
        // not in use now
    }

    private String get_MM2pDPClive_rawpath() {
        String rawpath = imgsaving_directory + sep + live_rawname;
        return rawpath;
    }

    private String get_MM2pDPClive_phasepath() {
        String phasepath = imgsaving_directory + sep + live_phasename;
        return phasepath;
    }

////////////////////////////////////////////////////////////////////////////////
    public void onPanelStart() {
        if (!initialized) {
            initComponents();

            JScrollPane preset_scrollPane = new JScrollPane();
            preset_scrollPane.add(preset_Pane);
            preset_scrollPane.setViewportView(preset_Pane);
            preset_Frame.add(preset_scrollPane);
            preset_Frame.pack();
            preset_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            preset_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_preset_Pane.setSelected(false);
                    OnUNSelect_tbt_show_preset_Pane();
                }
            });

            JScrollPane socket_scrollPane = new JScrollPane();
            socket_scrollPane.add(Socket_Pane);
            socket_scrollPane.setViewportView(Socket_Pane);
            Socket_Frame.add(socket_scrollPane);
            Socket_Frame.pack();
            Socket_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            Socket_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_Socket_Pane.setSelected(false);
                    OnUNSelect_tbt_show_Socket_Pane();
                }
            });

            JScrollPane cmdRun_scrollPane = new JScrollPane();
            cmdRun_scrollPane.add(cmdRun_Pane);
            cmdRun_scrollPane.setViewportView(cmdRun_Pane);
            cmdRun_Frame.add(cmdRun_scrollPane);
            cmdRun_Frame.pack();
            cmdRun_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            cmdRun_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_cmdRun_Pane.setSelected(false);
                    OnUNSelect_tbt_show_cmdRun_Pane();
                }
            });

            JScrollPane livesplit_scrollPane = new JScrollPane();
            livesplit_scrollPane.add(livesplit_Pane);
            livesplit_scrollPane.setViewportView(livesplit_Pane);
            livesplit_Frame.add(livesplit_Pane);
            livesplit_Frame.pack();
            livesplit_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            livesplit_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_livesplit_Pane.setSelected(false);
                    OnUNSelect_tbt_show_livesplit_Pane();
                }
            });

            preset_Pane.onPanelStart();
            Socket_Pane.onPanelStart();
            cmdRun_Pane.onPanelStart();
            livesplit_Pane.onPaneStart();

            choose_Python_Pane.onPanelStart();
            pDPCrecon_path_Pane.onPanelStart();

            preset_Pane.set_editable_hideadv(false);
            cbx_useadvparams.setSelected(false);
            onChange_cbx_useadvparams();
        }
        initialized = true;
    }

    public void onPanelExit() {
        is_pDPCrecon_running = false;
        is_pDPCcali_running = false;
        tbt_pDPClive_on.setSelected(false);
        tbt_pDPCrecon_on.setSelected(false);
        tbt_pDPCcali_on.setSelected(false);
        tbt_loopThread_on.setSelected(false);
        on_UNSelect_tbt_loopThread_on();

        tbt_show_cmdRun_Pane.setSelected(false);
        OnUNSelect_tbt_show_cmdRun_Pane();
        cmdRun_Pane.onPanelExit();

        tbt_show_Socket_Pane.setSelected(false);
        OnUNSelect_tbt_show_Socket_Pane();
        Socket_Pane.onPanelExit();

        tbt_show_preset_Pane.setSelected(false);
        OnUNSelect_tbt_show_preset_Pane();
        preset_Pane.onPanelExit();

        tbt_show_livesplit_Pane.setSelected(false);
        OnUNSelect_tbt_show_livesplit_Pane();
        livesplit_Pane.onPaneExit();

        save_history_onExit();

        choose_Python_Pane.onPanelExit();
        pDPCrecon_path_Pane.onPanelExit();

        initialized = false;
    }

////////////////////////////////////////////////////////////////////////////////  
    /**
     * Creates new form loop_Panel
     */
    public pDPC_ExtThread_Panel() {
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

        log_scrollPane = new javax.swing.JScrollPane();
        ta_log = new javax.swing.JTextArea();
        tbp_allbutlog = new javax.swing.JTabbedPane();
        launch_stop_python_Pane = new javax.swing.JPanel();
        choose_Python_Pane = new Utils.Choose_Python_Panel();
        launch_stop_Pane = new javax.swing.JPanel();
        tbt_loopThread_on = new javax.swing.JToggleButton();
        jLabel3 = new javax.swing.JLabel();
        jsp_loopThread_timeOutSec = new javax.swing.JSpinner();
        jsp_sock_bufsize = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        cbx_doAbsorption = new javax.swing.JCheckBox();
        operate_modes_Pane = new javax.swing.JPanel();
        tbp_modes = new javax.swing.JTabbedPane();
        live_scrollPane = new javax.swing.JScrollPane();
        live_Pane = new javax.swing.JPanel();
        tbt_pDPClive_on = new javax.swing.JToggleButton();
        jLabel1 = new javax.swing.JLabel();
        jsp_pDPClive_timeOutSec = new javax.swing.JSpinner();
        recon_scrollPane = new javax.swing.JScrollPane();
        recon_Pane = new javax.swing.JPanel();
        pDPCrecon_path_Pane = new PluginThreads.pDPCrecon_path_Panel();
        doRecon_Pane = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        pb_recon = new javax.swing.JProgressBar();
        lb_pbvalue = new javax.swing.JLabel();
        cbx_openphase_afrecon = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        tbt_pDPCrecon_on = new javax.swing.JToggleButton();
        jsp_pDPCrecon_timeOutSec = new javax.swing.JSpinner();
        cali_scrollPane = new javax.swing.JScrollPane();
        cali_Pane = new javax.swing.JPanel();
        doCali_Pane = new javax.swing.JPanel();
        tbt_pDPCcali_on = new javax.swing.JToggleButton();
        jLabel14 = new javax.swing.JLabel();
        jsp_pDPCcali_timeOutSec = new javax.swing.JSpinner();
        scrollPane_pDPCcali_result = new javax.swing.JScrollPane();
        ta_pDPCcali_result = new javax.swing.JTextArea();
        bt_update_pDPCcali_result_to_current_settings = new javax.swing.JButton();
        pb_cali = new javax.swing.JProgressBar();
        pDPCcali_path_Pane = new PluginThreads.pDPCcali_path_Panel();
        general_setting_Pane = new javax.swing.JPanel();
        cbx_useadvparams = new javax.swing.JCheckBox();
        cbx_verbose = new javax.swing.JCheckBox();
        tbt_show_preset_Pane = new javax.swing.JToggleButton();
        cbx_cancheckadv = new javax.swing.JCheckBox();
        tbt_show_cmdRun_Pane = new javax.swing.JToggleButton();
        tbt_show_Socket_Pane = new javax.swing.JToggleButton();
        tbt_show_livesplit_Pane = new javax.swing.JToggleButton();

        ta_log.setEditable(false);
        ta_log.setColumns(20);
        ta_log.setLineWrap(true);
        ta_log.setRows(5);
        ta_log.setWrapStyleWord(true);
        ta_log.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  Log  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N
        log_scrollPane.setViewportView(ta_log);

        tbp_allbutlog.setPreferredSize(new java.awt.Dimension(735, 316));

        choose_Python_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  1.1 Choose python environment  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        launch_stop_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  1.2 Launch/Stop background python process  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        tbt_loopThread_on.setText("Background process on");
        tbt_loopThread_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_loopThread_onActionPerformed(evt);
            }
        });

        jLabel3.setText("Bkg Process TimeOut [Sec]:");

        jsp_loopThread_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_loopThread_timeOutSec.setValue(loopThread_timeOutSec);
        jsp_loopThread_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_loopThread_timeOutSecStateChanged(evt);
            }
        });

        jsp_sock_bufsize.setModel(new javax.swing.SpinnerNumberModel(1024, 2, null, 1024));
        jsp_sock_bufsize.setValue(this.sock_bufsize);
        jsp_sock_bufsize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_sock_bufsizeStateChanged(evt);
            }
        });

        jLabel2.setText("Socket BufSize:");

        cbx_doAbsorption.setSelected(this.do_absorption);
        cbx_doAbsorption.setText("Do Absorption");
        cbx_doAbsorption.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_doAbsorptionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout launch_stop_PaneLayout = new javax.swing.GroupLayout(launch_stop_Pane);
        launch_stop_Pane.setLayout(launch_stop_PaneLayout);
        launch_stop_PaneLayout.setHorizontalGroup(
            launch_stop_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(launch_stop_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbt_loopThread_on)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jsp_loopThread_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jsp_sock_bufsize, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cbx_doAbsorption)
                .addContainerGap(9, Short.MAX_VALUE))
        );
        launch_stop_PaneLayout.setVerticalGroup(
            launch_stop_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(launch_stop_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(launch_stop_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_loopThread_on)
                    .addComponent(jLabel3)
                    .addComponent(jsp_loopThread_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jsp_sock_bufsize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbx_doAbsorption))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout launch_stop_python_PaneLayout = new javax.swing.GroupLayout(launch_stop_python_Pane);
        launch_stop_python_Pane.setLayout(launch_stop_python_PaneLayout);
        launch_stop_python_PaneLayout.setHorizontalGroup(
            launch_stop_python_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(launch_stop_python_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(launch_stop_python_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(choose_Python_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(launch_stop_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        launch_stop_python_PaneLayout.setVerticalGroup(
            launch_stop_python_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(launch_stop_python_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(choose_Python_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(launch_stop_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tbp_allbutlog.addTab("1. Launch/Stop python", launch_stop_python_Pane);

        tbp_modes.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  Modes  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N
        tbp_modes.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        tbp_modes.setPreferredSize(new java.awt.Dimension(690, 235));

        tbt_pDPClive_on.setText("pDPC Live on");
        tbt_pDPClive_on.setEnabled(false);
        tbt_pDPClive_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_pDPClive_onActionPerformed(evt);
            }
        });

        jLabel1.setText("Live TimeOut [Sec]:");

        jsp_pDPClive_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_pDPClive_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_pDPClive_timeOutSecStateChanged(evt);
            }
        });

        javax.swing.GroupLayout live_PaneLayout = new javax.swing.GroupLayout(live_Pane);
        live_Pane.setLayout(live_PaneLayout);
        live_PaneLayout.setHorizontalGroup(
            live_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(live_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbt_pDPClive_on)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jsp_pDPClive_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        live_PaneLayout.setVerticalGroup(
            live_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(live_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(live_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_pDPClive_on)
                    .addComponent(jLabel1)
                    .addComponent(jsp_pDPClive_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        live_scrollPane.setViewportView(live_Pane);

        tbp_modes.addTab("Live", live_scrollPane);

        pDPCrecon_path_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  A. Set raw & phase paths  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        doRecon_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  B. Do recon  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        jLabel6.setText("Recon TimeOut [Sec]:");

        pb_recon.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                pb_reconStateChanged(evt);
            }
        });

        lb_pbvalue.setText("  0%");
        lb_pbvalue.setBorder(new javax.swing.border.MatteBorder(null));

        cbx_openphase_afrecon.setText("Open phase after recon");

        jLabel4.setFont(new java.awt.Font("Tahoma", 3, 13)); // NOI18N
        jLabel4.setText("Progress:");

        tbt_pDPCrecon_on.setText("pDPC Recon on");
        tbt_pDPCrecon_on.setEnabled(false);
        tbt_pDPCrecon_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_pDPCrecon_onActionPerformed(evt);
            }
        });

        jsp_pDPCrecon_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_pDPCrecon_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_pDPCrecon_timeOutSecStateChanged(evt);
            }
        });

        javax.swing.GroupLayout doRecon_PaneLayout = new javax.swing.GroupLayout(doRecon_Pane);
        doRecon_Pane.setLayout(doRecon_PaneLayout);
        doRecon_PaneLayout.setHorizontalGroup(
            doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(doRecon_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(doRecon_PaneLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(12, 12, 12)
                        .addComponent(pb_recon, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lb_pbvalue))
                    .addGroup(doRecon_PaneLayout.createSequentialGroup()
                        .addComponent(tbt_pDPCrecon_on)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jsp_pDPCrecon_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cbx_openphase_afrecon)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        doRecon_PaneLayout.setVerticalGroup(
            doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(doRecon_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(doRecon_PaneLayout.createSequentialGroup()
                        .addGroup(doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tbt_pDPCrecon_on)
                            .addComponent(cbx_openphase_afrecon)
                            .addComponent(jsp_pDPCrecon_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addGap(20, 20, 20)
                        .addGroup(doRecon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4)
                            .addComponent(pb_recon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(lb_pbvalue))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout recon_PaneLayout = new javax.swing.GroupLayout(recon_Pane);
        recon_Pane.setLayout(recon_PaneLayout);
        recon_PaneLayout.setHorizontalGroup(
            recon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(recon_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(recon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pDPCrecon_path_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE)
                    .addComponent(doRecon_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        recon_PaneLayout.setVerticalGroup(
            recon_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(recon_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pDPCrecon_path_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(doRecon_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        recon_scrollPane.setViewportView(recon_Pane);

        tbp_modes.addTab("Recon", recon_scrollPane);

        doCali_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  B. Do cali  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        tbt_pDPCcali_on.setText("pDPC Cali On");
        tbt_pDPCcali_on.setEnabled(false);
        tbt_pDPCcali_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_pDPCcali_onActionPerformed(evt);
            }
        });

        jLabel14.setText("Cali TimeOut [Sec]:");

        jsp_pDPCcali_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_pDPCcali_timeOutSec.setValue(this.pDPCcali_timeOutSec);
        jsp_pDPCcali_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_pDPCcali_timeOutSecStateChanged(evt);
            }
        });

        ta_pDPCcali_result.setEditable(false);
        ta_pDPCcali_result.setColumns(20);
        ta_pDPCcali_result.setLineWrap(true);
        ta_pDPCcali_result.setRows(5);
        ta_pDPCcali_result.setTabSize(4);
        ta_pDPCcali_result.setToolTipText("");
        ta_pDPCcali_result.setWrapStyleWord(true);
        ta_pDPCcali_result.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  Cali results  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N
        scrollPane_pDPCcali_result.setViewportView(ta_pDPCcali_result);

        bt_update_pDPCcali_result_to_current_settings.setText("Update cali results to current setting");
        bt_update_pDPCcali_result_to_current_settings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_update_pDPCcali_result_to_current_settingsActionPerformed(evt);
            }
        });

        pb_cali.setBackground(new java.awt.Color(0, 0, 0));
        pb_cali.setForeground(new java.awt.Color(0, 0, 153));
        pb_cali.setToolTipText("");
        pb_cali.setStringPainted(true);

        javax.swing.GroupLayout doCali_PaneLayout = new javax.swing.GroupLayout(doCali_Pane);
        doCali_Pane.setLayout(doCali_PaneLayout);
        doCali_PaneLayout.setHorizontalGroup(
            doCali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(doCali_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(doCali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane_pDPCcali_result)
                    .addGroup(doCali_PaneLayout.createSequentialGroup()
                        .addComponent(tbt_pDPCcali_on)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jsp_pDPCcali_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(doCali_PaneLayout.createSequentialGroup()
                        .addComponent(bt_update_pDPCcali_result_to_current_settings)
                        .addGap(18, 18, 18)
                        .addComponent(pb_cali, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        doCali_PaneLayout.setVerticalGroup(
            doCali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(doCali_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(doCali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_pDPCcali_on)
                    .addComponent(jsp_pDPCcali_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addGap(18, 18, 18)
                .addComponent(scrollPane_pDPCcali_result, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(doCali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pb_cali, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bt_update_pDPCcali_result_to_current_settings))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pDPCcali_path_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  A. Set raw paths for cali  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        javax.swing.GroupLayout cali_PaneLayout = new javax.swing.GroupLayout(cali_Pane);
        cali_Pane.setLayout(cali_PaneLayout);
        cali_PaneLayout.setHorizontalGroup(
            cali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cali_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pDPCcali_path_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE)
                    .addComponent(doCali_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        cali_PaneLayout.setVerticalGroup(
            cali_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cali_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pDPCcali_path_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(doCali_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cali_scrollPane.setViewportView(cali_Pane);

        tbp_modes.addTab("Cali", cali_scrollPane);

        javax.swing.GroupLayout operate_modes_PaneLayout = new javax.swing.GroupLayout(operate_modes_Pane);
        operate_modes_Pane.setLayout(operate_modes_PaneLayout);
        operate_modes_PaneLayout.setHorizontalGroup(
            operate_modes_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(operate_modes_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbp_modes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        operate_modes_PaneLayout.setVerticalGroup(
            operate_modes_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(operate_modes_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbp_modes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tbp_allbutlog.addTab("2. Choose mode & operate", operate_modes_Pane);

        general_setting_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  General  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        cbx_useadvparams.setText("Use advanced pDPC params");
        cbx_useadvparams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_useadvparamsActionPerformed(evt);
            }
        });

        cbx_verbose.setSelected(true);
        cbx_verbose.setText("verbose");
        cbx_verbose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_verboseActionPerformed(evt);
            }
        });

        tbt_show_preset_Pane.setText("show Preset Panel");
        tbt_show_preset_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_preset_PaneActionPerformed(evt);
            }
        });

        cbx_cancheckadv.setText("Check background processes");
        cbx_cancheckadv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_cancheckadvActionPerformed(evt);
            }
        });

        tbt_show_cmdRun_Pane.setText("show CMDrun Panel");
        tbt_show_cmdRun_Pane.setEnabled(false);
        tbt_show_cmdRun_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_cmdRun_PaneActionPerformed(evt);
            }
        });

        tbt_show_Socket_Pane.setText("show Socket Panel");
        tbt_show_Socket_Pane.setEnabled(false);
        tbt_show_Socket_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_Socket_PaneActionPerformed(evt);
            }
        });

        tbt_show_livesplit_Pane.setText("show LiveSplit Panel");
        tbt_show_livesplit_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_livesplit_PaneActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout general_setting_PaneLayout = new javax.swing.GroupLayout(general_setting_Pane);
        general_setting_Pane.setLayout(general_setting_PaneLayout);
        general_setting_PaneLayout.setHorizontalGroup(
            general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_setting_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(general_setting_PaneLayout.createSequentialGroup()
                        .addComponent(cbx_useadvparams)
                        .addGap(18, 18, 18)
                        .addComponent(cbx_verbose))
                    .addGroup(general_setting_PaneLayout.createSequentialGroup()
                        .addComponent(tbt_show_preset_Pane)
                        .addGap(18, 18, 18)
                        .addComponent(tbt_show_livesplit_Pane)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(general_setting_PaneLayout.createSequentialGroup()
                        .addComponent(tbt_show_cmdRun_Pane)
                        .addGap(18, 18, 18)
                        .addComponent(tbt_show_Socket_Pane))
                    .addComponent(cbx_cancheckadv))
                .addContainerGap())
        );
        general_setting_PaneLayout.setVerticalGroup(
            general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_setting_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbx_useadvparams)
                    .addComponent(cbx_verbose)
                    .addComponent(cbx_cancheckadv))
                .addGap(18, 18, 18)
                .addGroup(general_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_show_preset_Pane)
                    .addComponent(tbt_show_cmdRun_Pane)
                    .addComponent(tbt_show_Socket_Pane)
                    .addComponent(tbt_show_livesplit_Pane))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tbp_allbutlog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(general_setting_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(log_scrollPane, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbp_allbutlog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(general_setting_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(log_scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbt_show_preset_PaneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_show_preset_PaneActionPerformed
        // TODO add your handling code here:
        if (tbt_show_preset_Pane.isSelected()) {
            OnSelect_tbt_show_preset_Pane();
        } else {
            OnUNSelect_tbt_show_preset_Pane();
        }
    }//GEN-LAST:event_tbt_show_preset_PaneActionPerformed

    private void tbt_show_Socket_PaneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_show_Socket_PaneActionPerformed
        // TODO add your handling code here:
        if (tbt_show_Socket_Pane.isSelected()) {
            OnSelect_tbt_show_serverSock_Pane();
        } else {
            OnUNSelect_tbt_show_Socket_Pane();
        }
    }//GEN-LAST:event_tbt_show_Socket_PaneActionPerformed

    private void tbt_show_cmdRun_PaneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_show_cmdRun_PaneActionPerformed
        // TODO add your handling code here:
        if (tbt_show_cmdRun_Pane.isSelected()) {
            OnSelect_tbt_show_cmdRun_Pane();
        } else {
            OnUNSelect_tbt_show_cmdRun_Pane();
        }
    }//GEN-LAST:event_tbt_show_cmdRun_PaneActionPerformed

    private void tbt_loopThread_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_loopThread_onActionPerformed
        // TODO add your handling code here:
        if (tbt_loopThread_on.isSelected()) {
            on_Select_tbt_loopThread_on();
        } else {
            on_UNSelect_tbt_loopThread_on();
        }
    }//GEN-LAST:event_tbt_loopThread_onActionPerformed

    private void jsp_loopThread_timeOutSecStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_loopThread_timeOutSecStateChanged
        // TODO add your handling code here:
        update_loopThread_timeOutSec();
    }//GEN-LAST:event_jsp_loopThread_timeOutSecStateChanged

    private void cbx_cancheckadvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_cancheckadvActionPerformed
        // TODO add your handling code here:
        if (cbx_cancheckadv.isSelected()) {
            onCheck_cbx_cancheckadv();
        } else {
            onUNCheck_cbx_cancheckadv();
        }
    }//GEN-LAST:event_cbx_cancheckadvActionPerformed

    private void cbx_verboseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_verboseActionPerformed
        // TODO add your handling code here:
        onChanged_cbx_verbose();
    }//GEN-LAST:event_cbx_verboseActionPerformed

    private void cbx_useadvparamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_useadvparamsActionPerformed
        // TODO add your handling code here:
        onChange_cbx_useadvparams();
    }//GEN-LAST:event_cbx_useadvparamsActionPerformed

    private void pb_reconStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_pb_reconStateChanged
        // TODO add your handling code here:
        onStateChanged_pb_recon();
    }//GEN-LAST:event_pb_reconStateChanged

    private void jsp_pDPCrecon_timeOutSecStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_pDPCrecon_timeOutSecStateChanged
        // TODO add your handling code here:
        update_pDPCrecon_timeOutSec();
    }//GEN-LAST:event_jsp_pDPCrecon_timeOutSecStateChanged

    private void tbt_pDPCrecon_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_pDPCrecon_onActionPerformed
        // TODO add your handling code here:
        if (tbt_pDPCrecon_on.isSelected()) {
            onSelect_tbt_pDPCrecon_on();
        } else {
            onUNSelect_tbt_pDPCrecon_on();
        }
    }//GEN-LAST:event_tbt_pDPCrecon_onActionPerformed

    private void jsp_pDPClive_timeOutSecStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_pDPClive_timeOutSecStateChanged
        // TODO add your handling code here:
        update_pDPCLive_timeOutSec();
    }//GEN-LAST:event_jsp_pDPClive_timeOutSecStateChanged

    private void tbt_pDPClive_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_pDPClive_onActionPerformed
        // TODO add your handling code here:
        if (tbt_pDPClive_on.isSelected()) {
            onSelect_tbt_pDPClive_on();
        } else {
            onUNSelect_tbt_pDPClive_on();
        }
    }//GEN-LAST:event_tbt_pDPClive_onActionPerformed

    private void jsp_pDPCcali_timeOutSecStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_pDPCcali_timeOutSecStateChanged
        // TODO add your handling code here:
        update_pDPCcali_timeOutSec();
    }//GEN-LAST:event_jsp_pDPCcali_timeOutSecStateChanged

    private void tbt_show_livesplit_PaneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_show_livesplit_PaneActionPerformed
        // TODO add your handling code here:
        if (tbt_show_livesplit_Pane.isSelected()) {
            OnSelect_tbt_show_livesplit_Pane();
        } else {
            OnUNSelect_tbt_show_livesplit_Pane();
        }
    }//GEN-LAST:event_tbt_show_livesplit_PaneActionPerformed

    private void tbt_pDPCcali_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_pDPCcali_onActionPerformed
        // TODO add your handling code here:
        if (tbt_pDPCcali_on.isSelected()) {
            onSelect_tbt_pDPCcali_on();
        } else {
            onUNSelect_tbt_pDPCcali_on();
        }
    }//GEN-LAST:event_tbt_pDPCcali_onActionPerformed

    private void bt_update_pDPCcali_result_to_current_settingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_update_pDPCcali_result_to_current_settingsActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_update_pDPCcali_result_to_current_settings.getText())) {
            onSelect_bt_update_pDPCcali_result_to_current_settings();
        }
    }//GEN-LAST:event_bt_update_pDPCcali_result_to_current_settingsActionPerformed

    private void jsp_sock_bufsizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_sock_bufsizeStateChanged
        // TODO add your handling code here:
        update_sock_bufsize();
    }//GEN-LAST:event_jsp_sock_bufsizeStateChanged

    private void cbx_doAbsorptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_doAbsorptionActionPerformed
        // TODO add your handling code here:
        onChanged_cbx_doAbsorption();
    }//GEN-LAST:event_cbx_doAbsorptionActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_update_pDPCcali_result_to_current_settings;
    private javax.swing.JPanel cali_Pane;
    private javax.swing.JScrollPane cali_scrollPane;
    private javax.swing.JCheckBox cbx_cancheckadv;
    private javax.swing.JCheckBox cbx_doAbsorption;
    private javax.swing.JCheckBox cbx_openphase_afrecon;
    private javax.swing.JCheckBox cbx_useadvparams;
    private javax.swing.JCheckBox cbx_verbose;
    private Utils.Choose_Python_Panel choose_Python_Pane;
    private javax.swing.JPanel doCali_Pane;
    private javax.swing.JPanel doRecon_Pane;
    private javax.swing.JPanel general_setting_Pane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSpinner jsp_loopThread_timeOutSec;
    private javax.swing.JSpinner jsp_pDPCcali_timeOutSec;
    private javax.swing.JSpinner jsp_pDPClive_timeOutSec;
    private javax.swing.JSpinner jsp_pDPCrecon_timeOutSec;
    private javax.swing.JSpinner jsp_sock_bufsize;
    private javax.swing.JPanel launch_stop_Pane;
    private javax.swing.JPanel launch_stop_python_Pane;
    private javax.swing.JLabel lb_pbvalue;
    private javax.swing.JPanel live_Pane;
    private javax.swing.JScrollPane live_scrollPane;
    private javax.swing.JScrollPane log_scrollPane;
    private javax.swing.JPanel operate_modes_Pane;
    private PluginThreads.pDPCcali_path_Panel pDPCcali_path_Pane;
    private PluginThreads.pDPCrecon_path_Panel pDPCrecon_path_Pane;
    private javax.swing.JProgressBar pb_cali;
    private javax.swing.JProgressBar pb_recon;
    private javax.swing.JPanel recon_Pane;
    private javax.swing.JScrollPane recon_scrollPane;
    private javax.swing.JScrollPane scrollPane_pDPCcali_result;
    private javax.swing.JTextArea ta_log;
    private javax.swing.JTextArea ta_pDPCcali_result;
    private javax.swing.JTabbedPane tbp_allbutlog;
    private javax.swing.JTabbedPane tbp_modes;
    private javax.swing.JToggleButton tbt_loopThread_on;
    private javax.swing.JToggleButton tbt_pDPCcali_on;
    private javax.swing.JToggleButton tbt_pDPClive_on;
    private javax.swing.JToggleButton tbt_pDPCrecon_on;
    private javax.swing.JToggleButton tbt_show_Socket_Pane;
    private javax.swing.JToggleButton tbt_show_cmdRun_Pane;
    private javax.swing.JToggleButton tbt_show_livesplit_Pane;
    private javax.swing.JToggleButton tbt_show_preset_Pane;
    // End of variables declaration//GEN-END:variables
}
