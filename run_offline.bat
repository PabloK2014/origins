@echo off
echo Starting Minecraft in offline mode for development...
gradlew runClient --offline --args="--username DevPlayer --uuid 00000000-0000-0000-0000-000000000000"
pause