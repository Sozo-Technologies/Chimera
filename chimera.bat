@echo off
setlocal enabledelayedexpansion


set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "PROJECT_DIR=%ROOT%\chimera"
set "OPENCV_JAR=%PROJECT_DIR%\lib\opencv\opencv-4120.jar"
if "%1"=="" goto help
if /I "%1"=="run" (
    echo.
    echo [CHIMERA] Running JavaFX Project...
    echo.

    cd /d "%PROJECT_DIR%"
    mvn clean javafx:run
    goto end
)

if /I "%1"=="setup" (
    echo.
    echo [CHIMERA] Installing OpenCV JAR...
    echo.

    mvn install:install-file ^
    -Dfile="%OPENCV_JAR%" ^
    -DgroupId=org.opencv ^
    -DartifactId=opencv ^
    -Dversion=4.12.0 ^
    -Dpackaging=jar

    if errorlevel 1 (
        echo.
        echo [CHIMERA] Failed to install OpenCV dependency.
        goto end
    )

    echo.
    echo [CHIMERA] Building Project...
    echo.

    cd /d "%PROJECT_DIR%"
    mvn clean install
    cd /d ../
    goto end
)

if /I "%1"=="clean" (

    echo.
    echo =====================================
    echo          CHIMERA CLEAN
    echo =====================================
    echo.

    echo [CHIMERA] Terminating Java processes...
    taskkill /F /IM java.exe >nul 2>&1
    taskkill /F /IM javaw.exe >nul 2>&1

    echo [CHIMERA] Cleaning port 8765...

    for /f "tokens=5" %%A in ('netstat -ano ^| findstr :8765') do (
        echo [CHIMERA] Killing PID %%A
        taskkill /F /PID %%A >nul 2>&1
    )

    echo.
    echo [CHIMERA] Cleanup complete.
    echo.

    goto end
)

:help
echo.
echo =====================================
echo            CHIMERA CLI
echo =====================================
echo.
echo Commands:
echo.
echo   chimera run
echo       Runs the JavaFX application
echo.
echo   chimera setup
echo       Installs OpenCV JAR and builds project
echo.

:end
endlocal