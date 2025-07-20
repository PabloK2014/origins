@echo off
echo Запуск Minecraft с Java 17 и дополнительными параметрами...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Остановка Gradle...
call gradlew --stop
timeout /t 2 /nobreak > nul

echo Запуск клиента Minecraft...
call gradlew runClient --stacktrace -Dorg.gradle.jvmargs="-Xmx4G -Dmixin.debug.export=true -Dmixin.checks.interfaces=true -Dmixin.hotSwap=true -Dmixin.debug=true"

echo Готово!
pause