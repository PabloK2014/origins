@echo off
echo Тестирование исправления доски объявлений...
echo.

echo Компилируем проект...
call gradlew build
if %errorlevel% neq 0 (
    echo Ошибка компиляции!
    pause
    exit /b 1
)

echo.
echo Запускаем клиент для тестирования...
call gradlew runClient

pause