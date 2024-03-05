# openpDPC_condenser
This repository is for information and code relating to open source implementation of phase-based Differential Phase Contrast using a condenser mask

## MM2_pDPC - MicroManager 2 plugin for pDPC
This plugin is for use with MicroManager 2 (tested with MM2.0.0 but should also work with nightly-builds). It is a GUI that allows for live pDPC imaging and post-processing of pDPC raw images. 

### Installation
1. Download dist/MM2_pDPC.jar file and copy it to mmplugins folder of MM2
2. restart MM2 and you should see a new plugin called "pDPC" in the plugins menu

#### Update 2024-03-05
The plugin now use a different preset convention to allow for input valid dark and light background image paths.  

To update from any older versions (before this date) to any versions after this date:

**NOTE: you will lose ALL presets in the old version while updating to the new ones!!!**

1. Download the latest MM2_pDPC.jar file and replace the old one in the mmplugins folder of MM2.
2. It is recommended, before the following steps, to back up old presets by copying the "all_preset_js" file in MM2/MM2_pDPC folder to a different location.  
3. Delete the "all_preset_js" file in MM2/MM2_pDPC folder. 
4. Restart MM2 and launch the pDPC plugin. This will create a new "all_preset_js" file with the new preset convention in  MM2/MM2_pDPC folder but with default preset only.

### Pre-requisites
Software:
- MicroManager 2.0.0 (or nightly-builds)
- Anaconda3

Python packages:
- opencv-python
- numpy
- tifffile
- PyQt5
- magicgui
- scipy