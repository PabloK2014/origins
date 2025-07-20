@echo off
echo Запуск Minecraft с Java 17...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Запуск клиента Minecraft...
"C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot\bin\java.exe" -Dfabric.dli.config=.gradle\loom-cache\launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient -cp .gradle\loom-cache\launch_dependencies.jar net.fabricmc.devlaunchinjector.Main

pause