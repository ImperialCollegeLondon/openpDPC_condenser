/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package MM2Plugin_pDPC;

/**
 *
 * @author h.liu
 */
import javax.swing.JFrame;
import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

@Plugin(type = MenuPlugin.class)
public class pluginClass implements MenuPlugin, SciJavaPlugin {
    private static final String MenuName = "pDPC"; // plugin name shown in menu
    private Studio gui_; // mmstudio (core can be called from it)
    public static JFrame frame_; // all program share same frame_, thus static;

    @Override
    public String getSubMenu() { // where the plugin shows in micromanager Plugins bar
        // if not exist, will automatically create one 
        // if "", will directly put inside Plugins
        return "";
    }

    @Override
    public void onPluginSelected() { // run when the plugin is pressed in micromanager
        System.out.println("Plugin " + MenuName + " has been launched.");
        // setframe 
        frame_ = mainPluginFrame.getInstance(gui_); // _frame is created as JFrom Form in the same package of this .java
        frame_.setVisible(true);
    }

    @Override
    public void setContext(Studio studio) { // receive the studio object needed to make api calls
        gui_ = studio;
    }

    @Override
    public String getName() { // return name of this plugin shown in micromanager menubar
        return MenuName;
    }

    @Override
    public String getHelpText() {
        return  "...";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public String getCopyright() {
        return "";
    }
}
