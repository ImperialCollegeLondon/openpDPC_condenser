/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Utils;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public ImageProcessor get_imgproc_from_image(Studio gui, Image raw, boolean verbose) {
        if (gui == null) {
            return null;
        }

        try {
            ImageProcessor raw_proc = gui.data().ij().createProcessor(
                    raw.copyAtCoords(
                            raw.getCoords().copyBuilder().build()
                    )
            );
            return raw_proc;
        } catch (Exception ex) {
            if (verbose) {
                System.out.println("Failed to get imageprocessor from snapped image");
                ex.printStackTrace(System.out);
            }
            Logger.getLogger(MM2_utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * snap an image in MM2 and return image processor of the snapped image if
     * succeed otherwise return null
     *
     * @param gui
     * @param verbose
     * @return
     */
    public Image SnapImg(Studio gui, boolean verbose) {
        try {
            gui.getCMMCore().snapImage();
            gui.getCMMCore().waitForSystem();

            TaggedImage image = gui.getCMMCore().getTaggedImage();
            Image live_img = gui.getDataManager().convertTaggedImage(image);

            return live_img;
        } catch (Exception ex) {
            if (verbose) {
                System.out.println("Failed to take the snapped image");
                ex.printStackTrace(System.out);
            }
            Logger.getLogger(MM2_utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public boolean SaveImg(ImageProcessor live_img_proc, String ImgPathStr, boolean verbose) {
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
                assert live_img_proc != null;
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
     * snap & save an image in MM2 to a path return if the process has been
     * successful
     *
     * @param gui
     * @param ImgPathStr
     * @param verbose
     * @return
     */
    public boolean SnapAndSave_Img(Studio gui, String ImgPathStr, boolean verbose) {
        ImageProcessor live_img_proc = null;
        try {
            live_img_proc = get_imgproc_from_image(gui, SnapImg(gui, verbose), verbose);
        } catch (Exception ex) {
            if (verbose) {
                System.out.println("Failed to take the snapped image");
                ex.printStackTrace(System.out);
            }
            return false;
        }

        return SaveImg(live_img_proc, ImgPathStr, verbose); 
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
