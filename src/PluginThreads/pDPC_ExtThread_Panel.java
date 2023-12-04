/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
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
import ij.process.ImageStatistics;
import ij.process.LUT;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.micromanager.Studio;
import org.micromanager.internal.MMStudio;

/**
 *
 * @author localuser
 */
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

    CMDrun_Panel cmdRun_Pane = new CMDrun_Panel();
    SocketComm_Panel Socket_Pane = new SocketComm_Panel();
    Preset_Panel preset_Pane = new Preset_Panel();

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

    private String goodsign = "proceed";
    private String badsign = "error";
    private String exitsign = "exit";
    private String pystartsign = "python program started";

    private double loopThread_timeOutSec = 5;
    private double pDPClive_timeOutSec = 1;
    private double pDPCrecon_timeOutSec = 1;
    private boolean is_pDPCrecon_running = false;

////////////////////////////////////////////////////////////////////////////////
    private class RunLoopThread implements Runnable {

        public boolean onStop_loop() {

            // set all tbt to unselected
            tbt_pDPClive_on.setSelected(false);
            onUNSelect_tbt_pDPClive_on();

            is_pDPCrecon_running = false;
            tbt_pDPCrecon_on.setSelected(false);
            onUNSelect_tbt_pDPCrecon_on();

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
            tbt_pDPClive_on.setEnabled(!stopped);
            tbt_pDPCrecon_on.setEnabled(!stopped);
            setEnabled_choose_Python_Pane(stopped);

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
                    + " " + goodsign + " " + badsign + " " + exitsign;
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
            setEnabled_choose_Python_Pane(!proceed);

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
                            if (parent.getAcquisitionManager().isAcquisitionRunning()) {
                                ta_log.setText("pDPClive failed to start: MDA is running");
                                tbt_pDPClive_on.setSelected(false);
                                onUNSelect_tbt_pDPClive_on();
                            } else {
                                boolean rawready = MM2ut.SnapAndSave_Img(parent, get_MM2pDPClive_rawpath(), verbose);
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

                String jstr = conda_path_str + "\n" + conda_env_str
                        + "\n" + loopthreadtimeout_str
                        + "\n" + imglivetimeout_str;
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

    public void onClick_bt_gotoMDA() {
        if (parent != null) {
            // TO-DO: try find way to display MDA window
        }
    }

////////////////////////////////////////////////////////////////////////////////
    public void update_loopThread_timeOutSec() {
        loopThread_timeOutSec = (double) jsp_loopThread_timeOutSec.getValue();
    }

    public void update_pDPCLive_timeOutSec() {
        pDPClive_timeOutSec = (double) jsp_pDPClive_timeOutSec.getValue();
    }

    public void update_pDPCrecon_timeOutSec() {
        pDPCrecon_timeOutSec = (double) jsp_pDPCrecon_timeOutSec.getValue();
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
        if (tbt_loopThread_on.isSelected() && !tbt_pDPCrecon_on.isSelected() && !is_pDPCrecon_running) {
            // disable whatever needed 
            tbt_pDPCrecon_on.setEnabled(false);
            jsp_pDPClive_timeOutSec.setEnabled(false);

            tbt_pDPClive_on.setSelected(true);
        } else {
            tbt_pDPClive_on.setSelected(false);
        }
    }

    public void onUNSelect_tbt_pDPClive_on() {
        tbt_pDPClive_on.setSelected(false);

        tbt_pDPCrecon_on.setEnabled(tbt_loopThread_on.isSelected());
        jsp_pDPClive_timeOutSec.setEnabled(true);
    }

    public void onSelect_tbt_pDPCrecon_on() {
        if (tbt_loopThread_on.isSelected() && !tbt_pDPClive_on.isSelected() && pDPCrecon_path_Pane.isallpathready()) {
            // disable whatever needed
            tbt_pDPClive_on.setEnabled(false);
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
        } else {
            tbt_pDPCrecon_on.setSelected(false);
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
            pDPCrecon_path_Pane.setPane_enabled(true);
            jsp_pDPCrecon_timeOutSec.setEnabled(true);
            cbx_openphase_afrecon.setEnabled(true);
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

    public void onChange_cbx_useadvparams() {
        preset_Pane.set_hideadv(!cbx_useadvparams.isSelected());
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

            preset_Frame.add(preset_Pane);
            preset_Frame.pack();
            preset_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            preset_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_preset_Pane.setSelected(false);
                    OnUNSelect_tbt_show_preset_Pane();
                }
            });

            Socket_Frame.add(Socket_Pane);
            Socket_Frame.pack();
            Socket_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            Socket_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_Socket_Pane.setSelected(false);
                    OnUNSelect_tbt_show_Socket_Pane();
                }
            });

            cmdRun_Frame.add(cmdRun_Pane);
            cmdRun_Frame.pack();
            cmdRun_Frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            cmdRun_Frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {
                    tbt_show_cmdRun_Pane.setSelected(false);
                    OnUNSelect_tbt_show_cmdRun_Pane();
                }
            });

            preset_Pane.onPanelStart();
            Socket_Pane.onPanelStart();
            cmdRun_Pane.onPanelStart();

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
        tbt_pDPClive_on.setSelected(false);
        tbt_pDPCrecon_on.setSelected(false);
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

        tbt_pDPClive_on = new javax.swing.JToggleButton();
        choose_Python_Pane = new Utils.Choose_Python_Panel();
        tbt_show_cmdRun_Pane = new javax.swing.JToggleButton();
        jLabel2 = new javax.swing.JLabel();
        tbt_show_preset_Pane = new javax.swing.JToggleButton();
        tbt_show_Socket_Pane = new javax.swing.JToggleButton();
        cbx_verbose = new javax.swing.JCheckBox();
        jsp_pDPClive_timeOutSec = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        tbt_loopThread_on = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jsp_loopThread_timeOutSec = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta_log = new javax.swing.JTextArea();
        cbx_cancheckadv = new javax.swing.JCheckBox();
        jSeparator4 = new javax.swing.JSeparator();
        pb_recon = new javax.swing.JProgressBar();
        lb_pbvalue = new javax.swing.JLabel();
        bt_gotoMDA = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JSeparator();
        pDPCrecon_path_Pane = new PluginThreads.pDPCrecon_path_Panel();
        cbx_openphase_afrecon = new javax.swing.JCheckBox();
        cbx_useadvparams = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        tbt_pDPCrecon_on = new javax.swing.JToggleButton();
        jsp_pDPCrecon_timeOutSec = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();

        tbt_pDPClive_on.setText("pDPC Live on");
        tbt_pDPClive_on.setEnabled(false);
        tbt_pDPClive_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_pDPClive_onActionPerformed(evt);
            }
        });

        choose_Python_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "    Setup python environment    ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        tbt_show_cmdRun_Pane.setText("show CMDrun Panel");
        tbt_show_cmdRun_Pane.setEnabled(false);
        tbt_show_cmdRun_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_cmdRun_PaneActionPerformed(evt);
            }
        });

        jLabel2.setText("Log:");

        tbt_show_preset_Pane.setText("show Preset Panel");
        tbt_show_preset_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_preset_PaneActionPerformed(evt);
            }
        });

        tbt_show_Socket_Pane.setText("show Socket Panel");
        tbt_show_Socket_Pane.setEnabled(false);
        tbt_show_Socket_Pane.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_show_Socket_PaneActionPerformed(evt);
            }
        });

        cbx_verbose.setSelected(true);
        cbx_verbose.setText("verbose");
        cbx_verbose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_verboseActionPerformed(evt);
            }
        });

        jsp_pDPClive_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_pDPClive_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_pDPClive_timeOutSecStateChanged(evt);
            }
        });

        jLabel1.setText("pDPCLive TimeOut [Sec]:");

        tbt_loopThread_on.setText("loopThread on");
        tbt_loopThread_on.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_loopThread_onActionPerformed(evt);
            }
        });

        jLabel3.setText("loopThread TimeOut [Sec]:");

        jsp_loopThread_timeOutSec.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.001d, null, 0.001d));
        jsp_loopThread_timeOutSec.setValue(loopThread_timeOutSec);
        jsp_loopThread_timeOutSec.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jsp_loopThread_timeOutSecStateChanged(evt);
            }
        });

        ta_log.setEditable(false);
        ta_log.setColumns(20);
        ta_log.setLineWrap(true);
        ta_log.setRows(5);
        ta_log.setWrapStyleWord(true);
        jScrollPane1.setViewportView(ta_log);

        cbx_cancheckadv.setText("Check Bkg processes");
        cbx_cancheckadv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_cancheckadvActionPerformed(evt);
            }
        });

        pb_recon.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                pb_reconStateChanged(evt);
            }
        });

        lb_pbvalue.setText("  0%");
        lb_pbvalue.setBorder(new javax.swing.border.MatteBorder(null));

        bt_gotoMDA.setText("Go to MDA panel");
        bt_gotoMDA.setEnabled(false);
        bt_gotoMDA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt_gotoMDAActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        cbx_openphase_afrecon.setText("Open phase af recon");

        cbx_useadvparams.setSelected(true);
        cbx_useadvparams.setText("Use advanced pDPC params");
        cbx_useadvparams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_useadvparamsActionPerformed(evt);
            }
        });

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

        jLabel6.setText("pDPCRecon TimeOut [Sec]:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator3)
            .addComponent(jSeparator5, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(tbt_loopThread_on)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_loopThread_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(cbx_useadvparams)
                                .addGap(18, 18, 18)
                                .addComponent(cbx_verbose)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(choose_Python_Pane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator4)
                            .addComponent(pDPCrecon_path_Pane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tbt_pDPClive_on)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_pDPClive_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jLabel4)
                                .addGap(12, 12, 12)
                                .addComponent(pb_recon, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lb_pbvalue))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tbt_pDPCrecon_on)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_pDPCrecon_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(cbx_openphase_afrecon)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbx_cancheckadv)
                            .addComponent(tbt_show_cmdRun_Pane)
                            .addComponent(tbt_show_Socket_Pane)
                            .addComponent(bt_gotoMDA)
                            .addComponent(tbt_show_preset_Pane)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(choose_Python_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbt_loopThread_on)
                    .addComponent(jsp_loopThread_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cbx_verbose)
                    .addComponent(cbx_useadvparams))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tbt_show_preset_Pane)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(bt_gotoMDA)
                                .addGap(161, 161, 161)
                                .addComponent(cbx_cancheckadv)
                                .addGap(18, 18, 18)
                                .addComponent(tbt_show_cmdRun_Pane)
                                .addGap(18, 18, 18)
                                .addComponent(tbt_show_Socket_Pane))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(tbt_pDPClive_on)
                                    .addComponent(jLabel1)
                                    .addComponent(jsp_pDPClive_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(tbt_pDPCrecon_on)
                                            .addComponent(cbx_openphase_afrecon)
                                            .addComponent(jsp_pDPCrecon_timeOutSec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel6))
                                        .addGap(20, 20, 20)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel4)
                                            .addComponent(pb_recon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(lb_pbvalue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(pDPCrecon_path_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel2))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
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

    private void cbx_cancheckadvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_cancheckadvActionPerformed
        // TODO add your handling code here:
        if (cbx_cancheckadv.isSelected()) {
            onCheck_cbx_cancheckadv();
        } else {
            onUNCheck_cbx_cancheckadv();
        }
    }//GEN-LAST:event_cbx_cancheckadvActionPerformed

    private void bt_gotoMDAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bt_gotoMDAActionPerformed
        // TODO add your handling code here:
        if (evt.getActionCommand().equals(bt_gotoMDA.getText())) {
            onClick_bt_gotoMDA();
        }
    }//GEN-LAST:event_bt_gotoMDAActionPerformed

    private void cbx_verboseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_verboseActionPerformed
        // TODO add your handling code here:
        onChanged_cbx_verbose();
    }//GEN-LAST:event_cbx_verboseActionPerformed

    private void tbt_pDPCrecon_onActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_pDPCrecon_onActionPerformed
        // TODO add your handling code here:
        if (tbt_pDPCrecon_on.isSelected()) {
            onSelect_tbt_pDPCrecon_on();
        } else {
            onUNSelect_tbt_pDPCrecon_on();
        }
    }//GEN-LAST:event_tbt_pDPCrecon_onActionPerformed

    private void pb_reconStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_pb_reconStateChanged
        // TODO add your handling code here:
        onStateChanged_pb_recon();
    }//GEN-LAST:event_pb_reconStateChanged

    private void jsp_pDPCrecon_timeOutSecStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jsp_pDPCrecon_timeOutSecStateChanged
        // TODO add your handling code here:
        update_pDPCrecon_timeOutSec();
    }//GEN-LAST:event_jsp_pDPCrecon_timeOutSecStateChanged

    private void cbx_useadvparamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_useadvparamsActionPerformed
        // TODO add your handling code here:
        onChange_cbx_useadvparams();
    }//GEN-LAST:event_cbx_useadvparamsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bt_gotoMDA;
    private javax.swing.JCheckBox cbx_cancheckadv;
    private javax.swing.JCheckBox cbx_openphase_afrecon;
    private javax.swing.JCheckBox cbx_useadvparams;
    private javax.swing.JCheckBox cbx_verbose;
    private Utils.Choose_Python_Panel choose_Python_Pane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSpinner jsp_loopThread_timeOutSec;
    private javax.swing.JSpinner jsp_pDPClive_timeOutSec;
    private javax.swing.JSpinner jsp_pDPCrecon_timeOutSec;
    private javax.swing.JLabel lb_pbvalue;
    private PluginThreads.pDPCrecon_path_Panel pDPCrecon_path_Pane;
    private javax.swing.JProgressBar pb_recon;
    private javax.swing.JTextArea ta_log;
    private javax.swing.JToggleButton tbt_loopThread_on;
    private javax.swing.JToggleButton tbt_pDPClive_on;
    private javax.swing.JToggleButton tbt_pDPCrecon_on;
    private javax.swing.JToggleButton tbt_show_Socket_Pane;
    private javax.swing.JToggleButton tbt_show_cmdRun_Pane;
    private javax.swing.JToggleButton tbt_show_preset_Pane;
    // End of variables declaration//GEN-END:variables
}
