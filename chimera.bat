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
    echo [CHIMERA] Checking Python installation...
    echo.

    python --version >nul 2>&1
    if errorlevel 1 (
        echo [CHIMERA] Python not found. Installing Python 3 silently...
        echo.

        curl -o "%TEMP%\python_installer.exe" https://www.python.org/ftp/python/3.11.9/python-3.11.9-amd64.exe

        if errorlevel 1 (
            echo [CHIMERA] Failed to download Python installer.
            goto end
        )

        "%TEMP%\python_installer.exe" /quiet InstallAllUsers=1 PrependPath=1 Include_test=0

        if errorlevel 1 (
            echo [CHIMERA] Failed to install Python.
            goto end
        )

        echo [CHIMERA] Python installed successfully.
        echo [CHIMERA] Refreshing environment...
        echo.

        call refreshenv >nul 2>&1
    ) else (
        echo [CHIMERA] Python is already installed.
    )

    echo.
    echo [CHIMERA] Installing dependencies...
    echo.

    python -m pip install --upgrade pip --quiet

    python -m pip install websockets --quiet
    if errorlevel 1 ( echo [CHIMERA] Failed to install websockets. & goto end )
    echo [CHIMERA] websockets         ... OK

    python -m pip install mediapipe --quiet
    if errorlevel 1 ( echo [CHIMERA] Failed to install mediapipe. & goto end )
    echo [CHIMERA] mediapipe          ... OK

    python -m pip install numpy --quiet
    if errorlevel 1 ( echo [CHIMERA] Failed to install numpy. & goto end )
    echo [CHIMERA] numpy              ... OK

    python -m pip install opencv-python --quiet
    if errorlevel 1 ( echo [CHIMERA] Failed to install opencv-python. & goto end )
    echo [CHIMERA] opencv-python      ... OK

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

@if /I "%1"=="generate" (
    cls

    echo.
    echo ==========================================
    echo            CHIMERA GENERATOR
    echo ==========================================
    echo.

    if not exist "trainer\trainer.py" (
        echo [ERROR] trainer.py not found
        goto end
    )

    if not exist "trainer\resources" (
        echo [ERROR] resources folder not found
        goto end
    )

    echo [INFO] Starting dataset generation...
    echo.

    python "trainer\trainer.py"

    echo.
    
    if %ERRORLEVEL% EQU 0 (
        echo [SUCCESS] Dataset generation completed
    ) else (
        echo [FAILED] Dataset generation failed
    )

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