#!/bin/bash
set -e

ROOTDIR=$PWD
VERSION=$(jq -r 'if .[0]?.version? then .[0].version else error("Malformed mcmod.info; no version field present") end' "$ROOTDIR/src/main/resources/mcmod.info")
MCVERSION=$(jq -r 'if .[0]?.mcversion? then .[0].mcversion else error("Malformed mcmod.info; no mcversion field present") end' "$ROOTDIR/src/main/resources/mcmod.info")
OUTNAME=mapwriter-$MCVERSION-$VERSION.zip

echo "Package will be saved as '$OUTNAME'"
echo "Compiling..."

cd "$ROOTDIR/forge/mcp"
./recompile.sh

echo "Reobfuscating..."

cd "$ROOTDIR/forge/mcp"
./reobfuscate.sh

echo "Packing..."

cd "$ROOTDIR/forge/mcp/reobf/minecraft"
zip -r output.zip mapwriter
mv output.zip "$ROOTDIR/src/main/resources/output.zip"
cd "$ROOTDIR/src/main/resources"
zip -ru output.zip assets
zip -u output.zip mcmod.info
rm -f "$ROOTDIR/$OUTNAME"
mv output.zip "$ROOTDIR/$OUTNAME"

echo "Done"
