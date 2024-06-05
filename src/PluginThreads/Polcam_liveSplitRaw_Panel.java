/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package PluginThreads;

import com.google.common.primitives.Booleans;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import static java.lang.Integer.min;
import static java.lang.Math.abs;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.micromanager.data.Image;
import org.micromanager.Studio;
import org.micromanager.data.Coords;
import org.micromanager.data.Metadata;
import org.micromanager.data.RewritableDatastore;
import org.micromanager.display.DisplayWindow;
import org.micromanager.internal.MMStudio;

/**
 *
 * @author localuser
 */
public class Polcam_liveSplitRaw_Panel extends javax.swing.JPanel {

    public static void main(String[] args) {
        Polcam_liveSplitRaw_Panel pane = new Polcam_liveSplitRaw_Panel();

        JFrame frame = new JFrame();
        frame.add(pane);
        frame.pack();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                int n = JOptionPane.showConfirmDialog(frame, "Quit: Are you sure?", "Quit", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.YES_OPTION) {
                    pane.onPaneExit();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                } else {
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        MMStudio gui = new MMStudio(true);
        CMMCore core = gui.getCMMCore();
        pane.set_gui(gui);

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }
        });
    }

//////////////////////////////////////////////////////////////////////////////////////////////
    private Studio gui_ = null;
    private boolean initialize = false;
    private Thread acqThread = null;

    private RewritableDatastore pols[] = new RewritableDatastore[]{null, null, null, null};
    private boolean showpols[] = new boolean[]{false, false, false, false};

    private RewritableDatastore polall[] = new RewritableDatastore[]{null, null, null, null};
    private String polall_name_suffixes[] = new String[]{"", "_Quads", "_Hori", "_Raw"};
    private int showpolall = 0;

    private boolean show_arithmetics[] = new boolean[]{false, false};
    private String arithmetics_title = "Arithmetics";
    private String arithmetics_labels[] = new String[]{"(Pol1-Pol4)/(Pol1+Pol4)", "(Pol2-Pol3)/(Pol2+Pol3)"};
    private StackWindow arithmetics_win = null;
    private boolean arithmetics_win_usable = false;

    private boolean use_external_thread = false;

//////////////////////////////////////////////////////////////////////////////////////////////
    public boolean get_livesplit_on() {
        return tbt_livesplit_ON.isSelected();
    }

    public void set_use_external_thread(boolean flag) {
        if (flag != use_external_thread) {
            boolean is_liveON = tbt_livesplit_ON.isSelected();

            // stop existing live
            if (tbt_livesplit_ON.isSelected()) {
                onUNSelect_tbt_liveON();
            }

            // set flag
            use_external_thread = flag;

            // restore liveON mode
            tbt_livesplit_ON.setSelected(is_liveON);
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////////
    private class liveSplit_Thread implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (!tbt_livesplit_ON.isSelected()) {
                    break;
                }

                if (gui_ != null) {
                    try {
                        gui_.getCMMCore().snapImage();
                        gui_.getCMMCore().waitForSystem();
                        TaggedImage image = gui_.getCMMCore().getTaggedImage();
                        Image live_img = gui_.getDataManager().convertTaggedImage(image);
                        update_liveWins(live_img);
                    } catch (Exception ex) {
                        Logger.getLogger(Polcam_liveSplitRaw_Panel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    // no gui_ available
                    Logger.getLogger(Polcam_liveSplitRaw_Panel.class.getName()).info("No MMStudio available - exit LiveSplit mode");
                    onUNSelect_tbt_liveON();
                }
            }
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////////
    public DisplayWindow createImgWin(RewritableDatastore Dtst) {
        if (Dtst == null || gui_ == null) {
            return null;
        }

        DisplayWindow imgDisplay = gui_.displays().createDisplay(Dtst);
        imgDisplay.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                onUNSelect_tbt_liveON();
                try {
                    Dtst.freeze();
                } catch (IOException ex) {
                    Logger.getLogger(Polcam_liveSplitRaw_Panel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        return imgDisplay;
    }

    public void updateDtst(ImageProcessor img, RewritableDatastore ds_img, Coords coord, Metadata mtdt) {
        if (ds_img != null && img != null) {
            try {
                ds_img.putImage(gui_.data().ij().createImage(img, coord, mtdt));
            } catch (IOException ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    public RewritableDatastore generateDtst(String name, int nChannels, int nSlices, int nFrames, int nPoses) {
        RewritableDatastore Dtst = null;

        if (gui_ != null) {
            try {
                Dtst = gui_.data().createRewritableRAMDatastore();
                Dtst.setName(name);
                Dtst.setSummaryMetadata(Dtst.getSummaryMetadata()
                        .copyBuilder()
                        .intendedDimensions(gui_.data().coordsBuilder()
                                .c(nChannels)
                                .z(nSlices)
                                .t(nFrames)
                                .p(nPoses)
                                .build()
                        )
                        .build()
                );
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        return Dtst;
    }

    public ImageProcessor get_sub_image(int sub_id, ImageProcessor raw) {
        // copy processor 
        ImageProcessor img_copy_proc = raw.duplicate();

        // calculate translation before resize according to sub_id;
        int translate_x = -1 * sub_id % 2;
        int translate_y = -1 * Math.round((sub_id - (sub_id % 2)) / 2);
        if (translate_x == -0) {
            translate_x = 0;
        }
        if (translate_y == -0) {
            translate_y = 0;
        }

        // translate and resize wo avg to get sub-image
        img_copy_proc.translate(translate_x, translate_y);
        ImageProcessor img_sub_proc = img_copy_proc.resize(
                img_copy_proc.getWidth() / 2, img_copy_proc.getHeight() / 2, false
        );

        return img_sub_proc;
    }

    public ImageProcessor get_imgproc_from_image(Image raw) {
        if (gui_ == null) {
            return null;
        }

        ImageProcessor raw_proc = gui_.data().ij().createProcessor(
                raw.copyAtCoords(
                        raw.getCoords().copyBuilder().build()
                )
        );
        return raw_proc;
    }

//////////////////////////////////////////////////////////////////////////////////////////////     
    public void init_liveWins() {
        boolean somewin_ON = update_liveWin_flags();

        if (!somewin_ON) {
            onUNSelect_tbt_liveON();
        } else {
            for (int i = 0; i < 4; i++) {
                if (showpols[i] && !is_Datastore_usable(pols[i])) {
                    pols[i] = generateDtst("Pol" + String.valueOf(i + 1), 1, 1, 1, 1);
                    createImgWin(pols[i]);
                }
            }

            if (showpolall > 0 && !is_Datastore_usable(polall[showpolall])) {
                polall[showpolall] = generateDtst("Pol_All" + polall_name_suffixes[showpolall], 1, 1, 1, 1);
                createImgWin(polall[showpolall]);
            }

            if (Booleans.contains(show_arithmetics, true)) {
                arithmetics_win_usable = true;
            }
        }
    }

    public void update_liveWins(Image raw) {
        if (raw != null) {
            ImageProcessor raw_proc = get_imgproc_from_image(raw);
            if (raw_proc == null) {
                // no gui available
                onUNSelect_tbt_liveON();
                return;
            }

            Coords coord = raw.getCoords().copyBuilder().build();
            Metadata mtdt = raw.getMetadata().copyBuilderPreservingUUID()
                    .pixelSizeUm(raw.getMetadata().getPixelSizeUm() * 2)
                    .build();

            ImageProcessor pol_imgs[] = new ImageProcessor[4];
            for (int i = 0; i < 4; i++) {
                if (showpols[i] || showpolall > 0 || Booleans.contains(show_arithmetics, true)) {
                    pol_imgs[i] = get_sub_image(i, raw_proc);
                    if (showpols[i] && is_Datastore_usable(pols[i])) {
                        updateDtst(
                                pol_imgs[i],
                                pols[i],
                                coord.copyBuilder().build(),
                                mtdt.copyBuilderWithNewUUID().build()
                        );
                    }
                }
            }

            int sub_width = Math.floorDiv(raw_proc.getWidth(), 2);
            int sub_height = Math.floorDiv(raw_proc.getHeight(), 2);

            if (showpolall > 0 && is_Datastore_usable(polall[showpolall])) {
                ImageProcessor img_all = null;
                switch (showpolall) {
                    case 1:
                        // montage as coords
                        img_all = raw_proc.createProcessor(sub_width * 2, sub_height * 2);
                        for (int i = 0; i < 4; i++) {
                            pol_imgs[i].setRoi(0, 0, sub_width, sub_height);
                            img_all.copyBits(
                                    pol_imgs[i].duplicate(),
                                    (i % 2) * sub_width,
                                    Math.floorDiv(i, 2) * sub_height,
                                    Blitter.COPY_ZERO_TRANSPARENT
                            );
                        }
                        break;
                    case 2:
                        // concatenate horizontally 1-2-3-4 
                        img_all = raw_proc.createProcessor(sub_width * 4, sub_height);
                        for (int i = 0; i < 4; i++) {
                            pol_imgs[i].setRoi(0, 0, sub_width, sub_height);
                            img_all.copyBits(pol_imgs[i].duplicate(), i * sub_width, 0, Blitter.COPY_ZERO_TRANSPARENT);
                        }
                        break;
                    case 3:
                        img_all = raw_proc.duplicate();
                        mtdt = mtdt.copyBuilderPreservingUUID().pixelSizeUm(raw.getMetadata().getPixelSizeUm()).build();
                        break;
                    default:
                        break;
                }

                if (img_all != null) {
                    updateDtst(
                            img_all,
                            polall[showpolall],
                            coord.copyBuilder().build(),
                            mtdt.copyBuilderWithNewUUID().build()
                    );
                }
            }

            if (Booleans.contains(show_arithmetics, true) && arithmetics_win_usable) {
                if (arithmetics_win == null || arithmetics_win.isClosed()) {
                    System.out.println("Initialize " + arithmetics_title + " ImageWindow");
                    arithmetics_win = new StackWindow(
                            ij.IJ.createImage(
                                    arithmetics_title, "32-bit Grayscale", sub_width,
                                    sub_height, show_arithmetics.length, 1, 1
                            )
                    );

                    for (int i = 0; i < arithmetics_win.getImagePlus().getStackSize(); i++) {
                        arithmetics_win.getImagePlus().getStack()
                                .setSliceLabel(arithmetics_labels[i], i + 1);
                    }

                    arithmetics_win.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent we) {
                            arithmetics_win_usable = false;
                            onUNSelect_tbt_liveON();
                        }
                    });
                    arithmetics_win.setVisible(true);
                }

                ImageCalculator img_calc = new ImageCalculator();
                for (int i = 0; i < show_arithmetics.length; i++) {
                    if (show_arithmetics[i]) {
                        ImageProcessor imp = null;
                        switch (i) {
                            case 0:
                            case 1:
                                pol_imgs[0].setRoi(0, 0, sub_width, sub_height);
                                pol_imgs[3].setRoi(0, 0, sub_width, sub_height);

                                imp = img_calc.run(
                                        "Divide create 32-bit",
                                        img_calc.run(
                                                "Subtract create 32-bit",
                                                new ImagePlus("", pol_imgs[i]),
                                                new ImagePlus("", pol_imgs[3 - i])
                                        ),
                                        img_calc.run(
                                                "Add create 32-bit",
                                                new ImagePlus("", pol_imgs[i]),
                                                new ImagePlus("", pol_imgs[3 - i])
                                        )
                                ).getProcessor();
                                break;

                        }

                        if (imp != null) {
                            arithmetics_win.getImagePlus()
                                    .getStack().getProcessor(i + 1)
                                    .copyBits(imp.duplicate(), 0, 0,
                                            Blitter.COPY_ZERO_TRANSPARENT);
                        }
                    } else {
                        ImageStatistics stats = arithmetics_win.getImagePlus()
                                .getStack().getProcessor(i + 1)
                                .getStatistics();
                        double minv = stats.min;
                        double maxv = stats.max;

                        if (minv != maxv) {
                            System.out.println("Set unused channel "
                                    + String.valueOf(i + 1)
                                    + " to 0 (original range: "
                                    + String.valueOf(minv)
                                    + "-" + String.valueOf(maxv)
                                    + ")"
                            );
                            arithmetics_win.getImagePlus()
                                    .getStack().getProcessor(i + 1)
                                    .set(0);
                        }
                    }
                }

                arithmetics_win.getImagePlus().updateAndRepaintWindow();
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////

    public void update_Pane_by_tbt_liveON() {
        for (Component comp : live_setting_Pane.getComponents()) {
            comp.setEnabled(!tbt_livesplit_ON.isSelected());
        }
        for (Component comp : arithmetic_Pane.getComponents()) {
            comp.setEnabled(!tbt_livesplit_ON.isSelected());
        }
    }

    public boolean onSelect_tbt_liveON() {
        tbt_livesplit_ON.setSelected(true);
        update_Pane_by_tbt_liveON();

        init_liveWins();

        if (!use_external_thread && tbt_livesplit_ON.isSelected()) {
            // start thread  
            acqThread = new Thread(new liveSplit_Thread());
            acqThread.start();
        }

        return tbt_livesplit_ON.isSelected();
    }

    public boolean onUNSelect_tbt_liveON() {
        // stop thread by set tbt_liveON to false.
        tbt_livesplit_ON.setSelected(false);

        // update gui
        update_Pane_by_tbt_liveON();

        return !tbt_livesplit_ON.isSelected();
    }

//////////////////////////////////////////////////////////////////////////////////////////////
    public void set_gui(Studio gui) {
        gui_ = gui;
    }

    public void onPaneStart() {
        if (!initialize) {
            initComponents();
        }
        initialize = true;
    }

    public void onPaneExit() {
        onUNSelect_tbt_liveON();
        initialize = false;
    }

//////////////////////////////////////////////////////////////////////////////////////////////    
    private boolean update_liveWin_flags() {
        for (int i = 0; i < show_arithmetics.length; i++) {
            show_arithmetics[i] = Arrays.asList(rb_pol_14, rb_pol_23).get(i).isSelected();
        }

        boolean showpols_from_polall = cbx_all.isSelected() & cb_all_form.getSelectedIndex() == 0;
        for (int i = 0; i < 4; i++) {
            showpols[i] = showpols_from_polall
                    | Arrays.asList(cbx_pol1, cbx_pol2, cbx_pol3, cbx_pol4).get(i).isSelected();
        }

        showpolall = cbx_all.isSelected() ? cb_all_form.getSelectedIndex() : 0;

        return Booleans.contains(showpols, true)
                | showpolall > 0
                | Booleans.contains(show_arithmetics, true);
    }

    private boolean is_Datastore_usable(RewritableDatastore Dtst) {
        if (Dtst == null) {
            return false;
        } else {
            return !Dtst.isFrozen();
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////////   
    /**
     * Creates new form Polcam_liveSplitRaw
     */
    public Polcam_liveSplitRaw_Panel() {
        onPaneStart();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tbt_livesplit_ON = new javax.swing.JToggleButton();
        live_setting_Pane = new javax.swing.JPanel();
        cbx_pol1 = new javax.swing.JCheckBox();
        cbx_pol3 = new javax.swing.JCheckBox();
        cbx_pol2 = new javax.swing.JCheckBox();
        cbx_pol4 = new javax.swing.JCheckBox();
        cbx_all = new javax.swing.JCheckBox();
        cb_all_form = new javax.swing.JComboBox<>();
        arithmetic_Pane = new javax.swing.JPanel();
        rb_pol_14 = new javax.swing.JRadioButton();
        rb_pol_23 = new javax.swing.JRadioButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  LiveSplit Polcam Raw  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 2, 13))); // NOI18N

        tbt_livesplit_ON.setText("LiveSplit on");
        tbt_livesplit_ON.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbt_livesplit_ONActionPerformed(evt);
            }
        });

        cbx_pol1.setText("Pol1");

        cbx_pol3.setText("Pol3");

        cbx_pol2.setText("Pol2");

        cbx_pol4.setText("Pol4");

        cbx_all.setText("Pol All");

        cb_all_form.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Separately", "Montage as quads", "Concatenate horizontally", "Original" }));
        cb_all_form.setSelectedIndex(2);

        arithmetic_Pane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "  On-the-fly Arithmetic  ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 3, 13))); // NOI18N

        rb_pol_14.setText(arithmetics_labels[0]);

        rb_pol_23.setText(arithmetics_labels[1]);

        javax.swing.GroupLayout arithmetic_PaneLayout = new javax.swing.GroupLayout(arithmetic_Pane);
        arithmetic_Pane.setLayout(arithmetic_PaneLayout);
        arithmetic_PaneLayout.setHorizontalGroup(
            arithmetic_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(arithmetic_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(arithmetic_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rb_pol_14)
                    .addComponent(rb_pol_23))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        arithmetic_PaneLayout.setVerticalGroup(
            arithmetic_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(arithmetic_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rb_pol_14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rb_pol_23)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout live_setting_PaneLayout = new javax.swing.GroupLayout(live_setting_Pane);
        live_setting_Pane.setLayout(live_setting_PaneLayout);
        live_setting_PaneLayout.setHorizontalGroup(
            live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(live_setting_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(arithmetic_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(live_setting_PaneLayout.createSequentialGroup()
                        .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbx_pol1)
                            .addComponent(cbx_pol3))
                        .addGap(18, 18, 18)
                        .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbx_pol2)
                            .addComponent(cbx_pol4)))
                    .addGroup(live_setting_PaneLayout.createSequentialGroup()
                        .addComponent(cbx_all)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cb_all_form, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        live_setting_PaneLayout.setVerticalGroup(
            live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(live_setting_PaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbx_pol1)
                    .addComponent(cbx_pol2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbx_pol3)
                    .addComponent(cbx_pol4))
                .addGap(18, 18, 18)
                .addGroup(live_setting_PaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbx_all)
                    .addComponent(cb_all_form, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(arithmetic_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(live_setting_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tbt_livesplit_ON)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(live_setting_Pane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tbt_livesplit_ON)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tbt_livesplit_ONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbt_livesplit_ONActionPerformed
        // TODO add your handling code here: 
        if (tbt_livesplit_ON.isSelected()) {
            onSelect_tbt_liveON();
        } else {
            onUNSelect_tbt_liveON();
        }
    }//GEN-LAST:event_tbt_livesplit_ONActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel arithmetic_Pane;
    private javax.swing.JComboBox<String> cb_all_form;
    private javax.swing.JCheckBox cbx_all;
    private javax.swing.JCheckBox cbx_pol1;
    private javax.swing.JCheckBox cbx_pol2;
    private javax.swing.JCheckBox cbx_pol3;
    private javax.swing.JCheckBox cbx_pol4;
    private javax.swing.JPanel live_setting_Pane;
    private javax.swing.JRadioButton rb_pol_14;
    private javax.swing.JRadioButton rb_pol_23;
    private javax.swing.JToggleButton tbt_livesplit_ON;
    // End of variables declaration//GEN-END:variables
}
