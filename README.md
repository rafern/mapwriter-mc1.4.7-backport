## IMPORTANT

This is the MapWriter mod version 2.1.1 (master branch) for Minecraft 1.7.10,
backported to Minecraft 1.4.7 used in Tekkit Lite.

Issues when setting up Forge:

- All scripts fail to run because of syntax errors:
  - All scripts use Python 2, but most systems now use Python 3 by default.
    Switch the system default to use Python 2 instead of Python 3, or manually
    change all scripts to explicitly use `python2` instead of `python` as the
    executable.

- Forge install script fails to download mcp726a.zip:
  - The MCP download links are dead. Use a newer one. The one provided in the
    Minecraft Wiki is
    `https://minecraft.gamepedia.com/Programs_and_editors/Mod_Coder_Pack`.
    The link can be changed in `forge/fml/mc_versions.cfg`, scroll down to the
    `[1.4.7]` section and change the field `mcp_url` to the new URL. Mediafire
    gives you a temporary direct link when you try to download it, use that one.

- Some Forge patches fail:
  - I don't know why MCP decompiles with different variable names, maybe it's
    because of the java version, maybe because it's openjdk instead of oracle,
    breaking the patches, but you can check the log to see which failed and
    manually apply the patches. Normally they always fail because of different
    temporary variable names (varXXX). I fixed the patches for my system and
    made a patch for the patches which can be found in `forge_patches.patch`, it
    might work for other systems

- Minecraft fails to download resources:
  - Old asset links have been dead for years, you have to manually supply the
    resources folder with all the sounds, etc...

- Minecraft crashes on Linux at startup:
  - Forge supplies an old version of LWJGL which parses xrandr parameters wrong.
    To fix this, replace the lib jars with newer versions, including natives.
    I'm not sure if its neccessary, but the natives also seem to be jar'ed
    together into a single file.

If you are using Linux, all of the issues can be fixed automatically by setting
up the Forge environment with the `setup_forge.sh` script. Dependencies are
Java (for `jar`), Python 2, `jq`, `sed`, `curl`, `patch` and `unzip`.
To run, put the Forge and MCP zips in the script's folder, as well as
(optionally) a folder named `mc_resources` with all the resources to be copied,
and run the script.

### Original README with instructions for building with MCP, slightly updated:

mapwriter
=========

MapWriter: A minimap mod for Minecraft


Instructions for development:

1) Set up your Forge/MCP environment.

2) Move or copy the mapwriter folder found in `src/main/java` to
   `forge/mcp/src/minecraft/`

3) Copy or move the textures from
   `src/main/resources/assets/mapwriter/textures/map/*.png` to
   `forge/mcp/bin/minecraft/assets/mapwriter/textures/map/*.png`

4) Modify the code, and use recompile.bat and startclient.bat to test.
   Alternatively use Eclipse and recompile and test by pressing the run button.

Reobfuscation and Packaging:

1) Run the recompile.bat script in your mcp directory.

2) Run the reobfuscate.bat script.

3) Create a zip file of the `forge/mcp/reobf/minecraft/mapwriter` folder.

4) Add the textures to the zip file in the folder
   [MapWriter.zip]/assets/mapwriter/textures/map/*.png
   
   The final structure should look like:
       MapWriter.zip
       | assets/mapwriter/textures/map/
       | | arrow_north.png
       | | arrow_player.png
       | | ...
       |
       | mapwriter/
         | api/
         | forge/
         | gui/
         | map/
         | ...
         | Mw.class
         | MwUtil.class
         | ... 

5) Optionally, add `mcmod.info` to the archive so that there is Forge mod info.
   This file is in `resources/mcmod.info`

Acknowledgements:

* Chrixian for the code to get death markers working.
* ProfMobius for the overlay API.
* taelnia for extrautils compatibility patch.
* LoneStar144 for minimap border and arrow textures.
* Melodeiro for updated marker dialog
