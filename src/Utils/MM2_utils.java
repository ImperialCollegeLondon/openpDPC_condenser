/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Utils;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.process.ImageProcessor;
import java.nio.file.Path;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.micromanager.Studio;
import org.micromanager.data.Image;
import org.micromanager.internal.MMStudio;

/**
 *
 * @author localuser
 */
public class MM2_utils {

    public static void main(String[] args) {
        MMStudio gui = new MMStudio(false);
        CMMCore core = gui.getCMMCore();
        MM2_utils ut = new MM2_utils();

    }

    utils ut = new utils();

    /**
     * snap & save an image in MM2 to a path return if the process has been
     * successful
     *
     * @param gui
     * @param ImgPathStr
     * @param verbose
     * @return
     */
    public boolean SnapAndSave_Img(Studio gui, String ImgPathStr, boolean verbose) {

        Path ImgPath = ut.getPathFromStr(ImgPathStr, verbose);
        if (ImgPath == null || ImgPath.getParent() == null) {
            if (verbose) {
                System.out.println("Invalid ImgPathStr: " + ImgPathStr);
            }
            return false;
        }

        boolean DirExist = ut.CreateifTargetDirsNotExist(ImgPath.getParent().toString(), verbose) != null;

        if (DirExist) {
            ImagePlus live_img_plus = null;
            try {
                gui.getCMMCore().snapImage();
                gui.getCMMCore().waitForSystem();
                TaggedImage image = gui.getCMMCore().getTaggedImage();
                Image live_img = gui.getDataManager().convertTaggedImage(image);
//                Image live_img = gui.acquisitions().snap().get(0);
                ImageProcessor live_img_proc = gui.data().ij().createProcessor(live_img);
                live_img_plus = new ImagePlus("", live_img_proc);
            } catch (Exception ex) {
                if (verbose) {
                    System.out.println("Failed to take the snapped image");
                    ex.printStackTrace(System.out);
                }
                return false;
            }

            try {
                ij.IJ.save(live_img_plus, ImgPathStr);
            } catch (Exception ex) {
                if (verbose) {
                    System.out.println("Failed to save the snapped image");
                    ex.printStackTrace(System.out);
                }
                return false;
            }
            return true;
        } else {
            if (verbose) {
                System.out.println("Failed to create target directory");
            }
            return false;
        }

    }

    /**
     * close an image window known its title
     *
     * @param title
     * @param verbose
     */
    public void close_ImgWindow(String title, boolean verbose) {
        if (ij.WindowManager.getWindow(title) != null) {
            ij.IJ.run(ij.WindowManager.getImage(title), "Close", "");
            if (verbose) {
                System.out.println("ImageWindow " + title + " closed");
            }
        } else {
            if (verbose) {
                System.out.println("ImageWindow " + title + " not exists");
            }
        }
    }

}
