@echo off
setlocal
cd /d "%~dp0"

if not exist out mkdir out

javac -encoding UTF-8 -d out -sourcepath src src\app\Main.java
if errorlevel 1 goto :error

java -cp out app.Main
goto :eof

:error
echo.
echo Build failed. Review the compiler errors above.
pause
