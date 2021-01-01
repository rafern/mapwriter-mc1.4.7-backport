#!/bin/bash
set -e

# Linux-only script for setting up development environment

# Abort if forge folder already exists
if [ -d "forge" ]; then
    echo "Forge/MCP environment already exists. Aborting"
    exit 1
fi

# Find Python 2 command
echo "Finding Python 2 command..."
if command -v python > /dev/null; then
    # Check if default Python is Python 2
    if python --version 2>&1 | grep "Python 2."; then
        PYTHONCOMMAND=python
    fi
fi

if [ ! -v PYTHONCOMMAND ]; then
    # Check if explicit Python 2 is available
    if command -v python2 > /dev/null; then
        PYTHONCOMMAND=python2
    fi
fi

if [ ! -v PYTHONCOMMAND ]; then
    echo "Can't find Python 2. Aborting"
    exit 1
fi

echo "Using '$PYTHONCOMMAND' as the Python 2 command"

# These are not directly downloaded since it is against the EULA of these
# websites
FORGEZIP=forge-1.4.7-6.6.2.534-src.zip
FORGEMD5=3d98a02d38afca25266e925126348466
FORGEURL=https://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.4.7.html
MCPZIP=mcp726a.zip
MCPMD5=5f11fbccd857b43a0f54117253b3e4dd
MCPURL=https://minecraft.gamepedia.com/Programs_and_editors/Mod_Coder_Pack

# Make sure that Forge and MCP archives are present
echo "Checking if Forge and MCP archives are present..."
if [ ! -f "$FORGEZIP" ]; then
    echo "'$FORGEZIP' is missing. Please download it from '$FORGEURL' and place it in this folder."
    exit 1
fi
if [ ! -f "$MCPZIP" ]; then
    echo "'$MCPZIP' is missing. Please download it from '$MCPURL' and place it in this folder."
    exit 1
fi

# Verify checksums of Forge and MCP archives
echo "Verifying MD5 checksums..."
if ! echo "$FORGEMD5" "$FORGEZIP" | md5sum --quiet -c -; then
    echo "Checksum failed for Forge zip. Make sure you are using the correct version (6.6.2.534 for MC 1.4.7)"
    exit 1
fi
if ! echo "$MCPMD5" "$MCPZIP" | md5sum --quiet -c -; then
    echo "Checksum failed for MCP zip. Make sure you are using the correct version (7.26a for MC 1.4.7)"
    exit 1
fi

# Download game libraries, Forge install script downloads outdated ones. These
# can be downloaded directly, since they are hosted officially
echo "Downloading game libraries metadata..."
VERSIONMANIFEST=$(curl https://launchermeta.mojang.com/mc/game/version_manifest.json)
MCMETAURL=$(echo "$VERSIONMANIFEST" | jq -r '.versions | map(select(.id == "1.4.7")) | first.url')

if [ "$MCMETAURL" = "null" ]; then
    echo "Minecraft 1.4.7 launcher metadata no longer available"
    exit 1
fi

MCMETA=$(curl "$MCMETAURL")
JARS=$(echo "$MCMETA" | jq -r '.libraries | map(select(.name | test("^(net\\.java\\.jinput:jinput:|org\\.lwjgl\\.lwjgl:lwjgl(_util)?:)((?!nightly).)+$", "sg"))) | map(.downloads.artifact.url) | unique')
JINPUTURL=$(echo "$JARS" | jq -r 'map(select(test("jinput","sg"))) | first')
LWJGLURL=$(echo "$JARS" | jq -r 'map(select(test("^.+lwjgl((?!_util).)+$","sg"))) | first')
LWJGLUTILURL=$(echo "$JARS" | jq -r 'map(select(test("lwjgl_util","sg"))) | first')
NATIVES=$(echo "$MCMETA" | jq -r '.libraries | map(select(.name | test("^(net\\.java\\.jinput:jinput-platform:|org\\.lwjgl\\.lwjgl:lwjgl-platform:)((?!nightly).)+$", "sg"))) | map(.downloads.classifiers."natives-linux".url) | unique')
JINPUTPLATFORMURL=$(echo "$NATIVES" | jq -r 'map(select(test("jinput-platform","sg"))) | first')
LWJGLPLATFORMURL=$(echo "$NATIVES" | jq -r 'map(select(test("lwjgl-platform","sg"))) | first')

echo "Downloading game libraries..."
curl "$JINPUTURL" -o jinput.jar
curl "$LWJGLURL" -o lwjgl.jar
curl "$LWJGLUTILURL" -o lwjgl_util.jar
curl "$JINPUTPLATFORMURL" -o jinput-platform.jar
curl "$LWJGLPLATFORMURL" -o lwjgl-platform.jar

echo "Extracting platform natives..."
mkdir -p tmp_natives
cd tmp_natives
cat ../jinput-platform.jar | jar -x
cat ../lwjgl-platform.jar | jar -x
rm -rf META-INF

echo "Re-packaging platform natives..."
jar cfM linux_natives.jar ./*
cd ..

echo "Deleting original platform natives jars..."
rm -f jinput-platform.jar
rm -f lwjgl-platform.jar

# Extract Forge
echo "Extracting Forge archive..."
unzip -q "$FORGEZIP"

# Patch broken Forge patches... yeah...
echo "Patching Forge patches..."
patch -s -p0 < forge_patches.patch

# Extract MCP in forge folder so that Forge doesn't download from outdated URL
echo "Extracting MCP archive..."
cd forge
mkdir mcp
unzip -q "../$MCPZIP" -d mcp

# Install forge
echo "Installing forge..."
$PYTHONCOMMAND install.py

# Overwrite game libraries
echo "Overwriting game libraries..."
cd ..
cp tmp_natives/* forge/mcp/jars/bin/natives
cp jinput.jar forge/mcp/jars/bin/jinput.jar
cp lwjgl.jar forge/mcp/jars/bin/lwjgl.jar
cp lwjgl_util.jar forge/mcp/jars/bin/lwjgl_util.jar

# Remote downloaded game libraries
echo "Removing downloaded game libraries..."
rm -rf tmp_natives
rm -f jinput.jar
rm -f lwjgl.jar
rm -f lwjgl_util.jar

# Patch MCP scripts to use Python 2
echo "Patching MCP scripts..."
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/cleanup.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/decompile.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/getchangedsrc.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/recompile.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/reformat.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/reobfuscate.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/startclient.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/startserver.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/updateids.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/updatemcp.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/updatemd5.sh
sed -i "s/python/$PYTHONCOMMAND/g" forge/mcp/updatenames.sh

# Create symlinks to source code and assets
echo "Creating symlinks to source code in MCP environment..."
ln -s ../../../../src/main/java/mapwriter forge/mcp/src/minecraft/mapwriter
ln -s ../../../../src/main/resources/assets forge/mcp/bin/minecraft/assets

# Copy resources folder if present
if [ -d mc_resources ]; then
    echo "Copying resources folder..."
    mkdir -p forge/mcp/jars/resources
    cp -Lr mc_resources/* forge/mcp/jars/resources
else
    echo "No mc_resources folder present. Resource folder copy skipped"
fi

echo "All done"