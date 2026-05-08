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