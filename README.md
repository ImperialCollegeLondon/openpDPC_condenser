# openpDPC_condenser
This repository is for information and code relating to open source implementation of phase-based Differential Phase Contrast using a condenser mask
s
## MM2_pDPC - MicroManager 2 plugin for pDPC
This plugin is for use with MicroManager 2 (tested with MM2.0.0 but should also work with nightly-builds). It is a GUI that allows for live pDPC imaging and post-processing of pDPC raw images. 

### Installation
1. Download dist/MM2_pDPC.jar file and copy it to mmplugins folder of MM2
2. restart MM2 and you should see a new plugin called "pDPC" in the plugins menu

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