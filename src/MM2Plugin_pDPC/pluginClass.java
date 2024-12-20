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
package MM2Plugin_pDPC;

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
        return "Documentation: https://imperialcollegelondon.github.io/openpDPC_condenser/";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public String getCopyright() {
        return "BSD 3-Clause License\n" + "\n"
                + "Copyright (c) 2023, Imperial College London \n"
                + "All rights reserved.\n" + "\n"
                + "Copyright (c) 2018, Waller Lab\n"
                + "All rights reserved.\n" + "\n"
                + "Redistribution and use in source and binary forms, with or without\n"
                + "modification, are permitted provided that the following conditions are met:\n"
                + "\n"
                + "* Redistributions of source code must retain the above copyright notice, this\n"
                + "  list of conditions and the following disclaimer.\n"
                + "\n"
                + "* Redistributions in binary form must reproduce the above copyright notice,\n"
                + "  this list of conditions and the following disclaimer in the documentation\n"
                + "  and/or other materials provided with the distribution.\n"
                + "\n"
                + "* Neither the name of the copyright holder nor the names of its\n"
                + "  contributors may be used to endorse or promote products derived from\n"
                + "  this software without specific prior written permission.\n"
                + "\n"
                + "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n"
                + "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n"
                + "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n"
                + "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE\n"
                + "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n"
                + "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR\n"
                + "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER\n"
                + "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,\n"
                + "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
                + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
    }
}
