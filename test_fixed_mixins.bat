@echo off
echo Тестирование мода с исправленными миксинами...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Остановка Gradle...
call gradlew --stop
timeout /t 2 /nobreak > nul

echo Запуск клиента Minecraft с исправленными миксинами...
call gradlew runClient --stacktrace

echo Готово!
pause