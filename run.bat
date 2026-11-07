@echo off
setlocal

if not exist lib\mysql-connector-j.jar (
    echo [ERROR] lib\mysql-connector-j.jar is missing.
    echo         Download MySQL Connector/J and copy the jar to the lib folder.
    echo         Default download page: https://dev.mysql.com/downloads/connector/j/
    pause
    exit /b 1
)

if not exist bin\Main.class (
    echo [ERROR] bin\Main.class is missing.
    echo         Run compile.bat first.
    pause
    exit /b 1
)

java -cp ".;bin;lib\mysql-connector-j.jar" Main
endlocal
