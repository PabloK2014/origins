@echo off
echo Запуск Minecraft с Java 17...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Очистка кэша Gradle...
call gradlew --stop
timeout /t 2 /nobreak > nul

echo Запуск Gradle с Java 17...
call gradlew clean
call gradlew --refresh-dependencies
call gradlew runClient

echo Готово!
pause