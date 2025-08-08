@echo off
echo Запуск Minecraft с Java 17 и минимальной конфигурацией...
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Используемая версия Java:
java -version

echo Остановка Gradle...
call gradlew --stop
timeout /t 2 /nobreak > nul

echo Очистка кэша...
rmdir /s /q build
rmdir /s /q .gradle

echo Запуск клиента Minecraft...
call gradlew runClient --stacktrace

echo Готово!
pause