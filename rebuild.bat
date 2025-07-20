@echo off
echo Полная перестройка проекта с Java 17...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Остановка Gradle...
call gradlew --stop
timeout /t 2 /nobreak > nul

echo Удаление кэша Gradle...
rmdir /s /q .gradle
rmdir /s /q build

echo Перестройка проекта...
call gradlew clean
call gradlew --refresh-dependencies
call gradlew build

echo Готово!
pause