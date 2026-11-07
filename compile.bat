@echo off
setlocal EnableDelayedExpansion

if not exist bin mkdir bin

if not exist lib\mysql-connector-j.jar (
    echo [WARNING] lib\mysql-connector-j.jar was not found.
    echo           Compilation can still succeed, but the application
    echo           will not connect to MySQL until the jar is added.
    echo.
)

rem Build a source list with forward slashes and quotes. This keeps folder
rem names containing spaces (for example "New folder") working in @sources.txt.
break > sources.txt
for /r "src" %%F in (*.java) do (
    set "SRC=%%F"
    set "SRC=!SRC:\=/!"
    echo "!SRC!" >> sources.txt
)

javac -encoding UTF-8 -cp ".;lib\mysql-connector-j.jar" -d bin @sources.txt
if errorlevel 1 (
    echo.
    echo Compilation FAILED. Check the errors above.
    exit /b 1
)

del sources.txt
echo.
echo Compilation successful. Class files are in the bin folder.
endlocal
