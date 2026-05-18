@echo off
setlocal enabledelayedexpansion

:: =====================================================================
::   CHIMERA CLI - CONFIG
:: =====================================================================

set "JAVA_MIN_VERSION=23.0.2"
set "MAVEN_MIN_VERSION=3.9.15"
set "PYTHON_MIN_VERSION=3.13.2"

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "PROJECT_DIR=%ROOT%\chimera"
set "OPENCV_JAR=%PROJECT_DIR%\lib\opencv\opencv-4120.jar"
set "DATASET_DEST=%PROJECT_DIR%\src\main\resources\datasets"
set "MEDIAPIPE_DEST=%PROJECT_DIR%\src\main\java\org\sozotech\stager"

if "%1"=="" goto help

:: =====================================================================
::   COMMAND ROUTER
:: =====================================================================

if /I "%1"=="run"      goto cmd_run
if /I "%1"=="setup"    goto cmd_setup
if /I "%1"=="clean"    goto cmd_clean
if /I "%1"=="generate" goto cmd_generate
if /I "%1"=="validate" goto cmd_validate
goto help

:cmd_run
echo.
echo [CHIMERA] Running JavaFX Project...
echo.
cd /d "%PROJECT_DIR%"
mvn clean javafx:run
goto end

:cmd_setup
echo.
echo =====================================
echo          CHIMERA SETUP
echo =====================================
echo.

set "SETUP_TOTAL=8"
set "SETUP_STEP=0"

call :progress_step "Checking Python"
call :check_python

call :progress_step "Checking Java JDK"
call :check_java

call :progress_step "Checking Maven"
call :check_maven

echo.
echo [CHIMERA] Checking Python packages...
echo.

call :progress_step "pip (upgrade)"
python -m pip install --upgrade pip --quiet

call :progress_step "websockets"
call :pip_ensure websockets
if errorlevel 1 goto end

call :progress_step "mediapipe"
call :pip_ensure mediapipe
if errorlevel 1 goto end

call :progress_step "numpy"
call :pip_ensure numpy
if errorlevel 1 goto end

call :progress_step "opencv-python"
call :pip_ensure opencv-python
if errorlevel 1 goto end

echo.
echo [CHIMERA] Installing OpenCV JAR into local Maven repo...
echo.

mvn install:install-file ^
    -Dfile="%OPENCV_JAR%" ^
    -DgroupId=org.opencv ^
    -DartifactId=opencv ^
    -Dversion=4.12.0 ^
    -Dpackaging=jar ^
    -q

if errorlevel 1 (
    echo [CHIMERA] Failed to install OpenCV JAR.
    goto end
)
echo [CHIMERA] OpenCV JAR ... OK

echo.
echo [CHIMERA] Building project...
echo.

cd /d "%PROJECT_DIR%"
mvn clean install
cd /d "%ROOT%"

echo.
echo [CHIMERA] Setup complete.
echo.
goto end

:cmd_clean
echo.
echo =====================================
echo          CHIMERA CLEAN
echo =====================================
echo.

echo [CHIMERA] Terminating Java processes...
taskkill /F /IM java.exe  >nul 2>&1
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

:cmd_generate
cls
echo.
echo ==========================================
echo            CHIMERA GENERATOR
echo ==========================================
echo.

set "GEN_DATASET=0"
set "GEN_MEDIAPIPE=0"

:parse_gen_args
shift
if "%1"=="" goto validate_gen_args
if /I "%1"=="--dataset"   set "GEN_DATASET=1"
if /I "%1"=="--mediapipe" set "GEN_MEDIAPIPE=1"
goto parse_gen_args

:validate_gen_args
if "%GEN_DATASET%"=="0" if "%GEN_MEDIAPIPE%"=="0" (
    echo [ERROR] No target specified.
    echo.
    echo   Usage:
    echo     chimera generate --dataset             Generate AI training dataset
    echo     chimera generate --mediapipe           Generate MediaPipe Java server
    echo     chimera generate --dataset --mediapipe Generate both
    echo.
    goto end
)

if "%GEN_DATASET%"=="1" (
    echo [STEP] Generating dataset...
    echo.

    if not exist "%ROOT%\trainer\trainer.py" (
        echo [ERROR] trainer\trainer.py not found.
        goto skip_dataset
    )
    if not exist "%ROOT%\trainer\resources" (
        echo [ERROR] trainer\resources folder not found.
        goto skip_dataset
    )

    echo   ^> Running trainer.py ...
    python "%ROOT%\trainer\trainer.py"
    if !ERRORLEVEL! NEQ 0 (
        echo [FAILED] Dataset generation failed.
        goto skip_dataset
    )

    set "CSV_SRC=%ROOT%\trainer\dataset.csv"
    if not exist "!CSV_SRC!" (
        echo [WARN] dataset.csv not found at: !CSV_SRC!
        goto skip_dataset
    )

    if not exist "%DATASET_DEST%" mkdir "%DATASET_DEST%"
    move /Y "!CSV_SRC!" "%DATASET_DEST%\dataset.csv" >nul
    if !ERRORLEVEL! NEQ 0 (
        echo [FAILED] Could not move dataset.csv to %DATASET_DEST%
        goto skip_dataset
    )

    echo [ OK ] Dataset moved to:
    echo        %DATASET_DEST%\dataset.csv
    echo.
    :skip_dataset
)

if "%GEN_MEDIAPIPE%"=="1" (
    echo [STEP] Generating MediaPipe Java server...
    echo.

    set "MP_SCRIPT=%ROOT%\mediapipe\generate_java.py"
    set "MP_OUT=%ROOT%\mediapipe\MediaPipeServer.java"

    if not exist "!MP_SCRIPT!" (
        echo [ERROR] mediapipe\generate_java.py not found.
        goto skip_mediapipe
    )

    echo   ^> Running generate_java.py ...
    python "!MP_SCRIPT!"
    if !ERRORLEVEL! NEQ 0 (
        echo [FAILED] MediaPipe generation failed.
        goto skip_mediapipe
    )

    if not exist "!MP_OUT!" (
        echo [WARN] MediaPipeServer.java not found at: !MP_OUT!
        goto skip_mediapipe
    )

    if not exist "%MEDIAPIPE_DEST%" mkdir "%MEDIAPIPE_DEST%"
    copy /Y "!MP_OUT!" "%MEDIAPIPE_DEST%\MediaPipeServer.java" >nul
    if !ERRORLEVEL! NEQ 0 (
        echo [FAILED] Could not copy MediaPipeServer.java to %MEDIAPIPE_DEST%
        goto skip_mediapipe
    )

    echo [ OK ] MediaPipeServer.java copied to:
    echo        %MEDIAPIPE_DEST%\MediaPipeServer.java
    echo.
    :skip_mediapipe
)

goto end

:cmd_validate
cls
echo.
echo ============================================================
echo                    CHIMERA VALIDATE
echo ============================================================
echo.
echo   Checking all dependencies...
echo.

set "V_PASS=0"
set "V_WARN=0"
set "V_FAIL=0"

echo   Runtimes
echo   ------------------------------------------------------------

set "_LABEL=Python (>= %PYTHON_MIN_VERSION%)"
python --version >nul 2>&1
if errorlevel 1 (
    call :val_row "!_LABEL!" "NOT INSTALLED" FAIL "Run: chimera setup"
    set /a V_FAIL+=1
) else (
    for /f "tokens=2" %%V in ('python --version 2^>^&1') do set "_FOUND=%%V"
    call :ver_gte "!_FOUND!" "%PYTHON_MIN_VERSION%" _OK
    if "!_OK!"=="1" (
        call :val_row "!_LABEL!" "!_FOUND!" OK ""
        set /a V_PASS+=1
    ) else (
        call :val_row "!_LABEL!" "!_FOUND! (outdated)" WARN "Run: chimera setup"
        set /a V_WARN+=1
    )
)

set "_LABEL=Java JDK (>= %JAVA_MIN_VERSION%)"
java -version >nul 2>&1
if errorlevel 1 (
    call :val_row "!_LABEL!" "NOT INSTALLED" FAIL "Run: chimera setup"
    set /a V_FAIL+=1
) else (
    call :get_java_version _FOUND
    call :ver_gte "!_FOUND!" "%JAVA_MIN_VERSION%" _OK
    if "!_OK!"=="1" (
        call :val_row "!_LABEL!" "!_FOUND!" OK ""
        set /a V_PASS+=1
    ) else (
        call :val_row "!_LABEL!" "!_FOUND! (outdated)" WARN "Run: chimera setup"
        set /a V_WARN+=1
    )
)

set "_LABEL=Maven (>= %MAVEN_MIN_VERSION%)"
call :get_maven_version _FOUND
if "!_FOUND!"=="" (
    call :val_row "!_LABEL!" "NOT FOUND" FAIL "Run: chimera setup"
    set /a V_FAIL+=1
) else (
    call :ver_gte "!_FOUND!" "%MAVEN_MIN_VERSION%" _OK
    if "!_OK!"=="1" (
        call :val_row "!_LABEL!" "!_FOUND!" OK ""
        set /a V_PASS+=1
    ) else (
        call :val_row "!_LABEL!" "!_FOUND! (outdated)" WARN "Run: chimera setup"
        set /a V_WARN+=1
    )
)

set "_LABEL=OpenCV JAR (4.12.0)"
if exist "%OPENCV_JAR%" (
    call :val_row "!_LABEL!" "Found" OK ""
    set /a V_PASS+=1
) else (
    call :val_row "!_LABEL!" "NOT FOUND" FAIL "Run: chimera setup"
    set /a V_FAIL+=1
)

echo.
echo   Python packages
echo   ------------------------------------------------------------

call :pip_check_row "websockets"    "websockets"
call :pip_check_row "mediapipe"     "mediapipe"
call :pip_check_row "numpy"         "numpy"
call :pip_check_row "opencv-python" "cv2"

echo.
echo   Project files
echo   ------------------------------------------------------------

call :check_file "trainer\trainer.py"     "%ROOT%\trainer\trainer.py"           WARN "Required for: chimera generate --dataset"
call :check_file "trainer\resources"      "%ROOT%\trainer\resources"             WARN "Required for: chimera generate --dataset"
call :check_file "mediapipe\generate_java.py" "%ROOT%\mediapipe\generate_java.py" WARN "Required for: chimera generate --mediapipe"
call :check_file "chimera\pom.xml"        "%PROJECT_DIR%\pom.xml"                FAIL "Project missing or misconfigured"

echo.
echo ============================================================
echo.
set /a V_TOTAL=V_PASS+V_WARN+V_FAIL
echo   Results:  %V_TOTAL% checked   /   %V_PASS% OK   /   %V_WARN% WARNING   /   %V_FAIL% FAILED
echo.
if %V_FAIL% GTR 0 (
    echo   [!] Some dependencies are missing. Run "chimera setup" to fix.
) else if %V_WARN% GTR 0 (
    echo   [~] Some dependencies are outdated. Run "chimera setup" to upgrade.
) else (
    echo   [*] All dependencies satisfied. Ready to run.
)
echo.
goto end


:check_python

python --version >nul 2>&1
if errorlevel 1 goto _do_install_python

for /f "tokens=2" %%V in ('python --version 2^>^&1') do set "_PY_FOUND=%%V"
call :ver_gte "!_PY_FOUND!" "%PYTHON_MIN_VERSION%" _PY_OK
if "!_PY_OK!"=="1" (
    echo [CHIMERA] Python !_PY_FOUND! ... OK (>= %PYTHON_MIN_VERSION%)
    goto :eof
)
echo [CHIMERA] Python !_PY_FOUND! is below %PYTHON_MIN_VERSION%. Upgrading...

:_do_install_python
echo.
call :install_step 1 3 "Downloading Python %PYTHON_MIN_VERSION%"
curl -s -o "%TEMP%\python_installer.exe" "https://www.python.org/ftp/python/%PYTHON_MIN_VERSION%/python-%PYTHON_MIN_VERSION%-amd64.exe"
if errorlevel 1 ( echo [CHIMERA] Download failed. & goto :eof )

call :install_step 2 3 "Running installer (silent)"
"%TEMP%\python_installer.exe" /quiet InstallAllUsers=1 PrependPath=1 Include_test=0
if errorlevel 1 ( echo [CHIMERA] Install failed. & goto :eof )

call :install_step 3 3 "Refreshing environment"
call refreshenv >nul 2>&1
echo [CHIMERA] Python %PYTHON_MIN_VERSION% installed.
echo.
goto :eof

:check_java

java -version >nul 2>&1
if errorlevel 1 goto _do_install_java

call :get_java_version _JV_FOUND
call :ver_gte "!_JV_FOUND!" "%JAVA_MIN_VERSION%" _JV_OK
if "!_JV_OK!"=="1" (
    echo [CHIMERA] Java !_JV_FOUND! ... OK (>= %JAVA_MIN_VERSION%)
    goto :eof
)
echo [CHIMERA] Java !_JV_FOUND! is below %JAVA_MIN_VERSION%. Upgrading...

:_do_install_java
echo.

for /f "tokens=1 delims=." %%M in ("%JAVA_MIN_VERSION%") do set "_JDK_MAJOR=%%M"
set "_JDK_URL=https://download.oracle.com/java/%_JDK_MAJOR%/latest/jdk-%_JDK_MAJOR%_windows-x64_bin.exe"

call :install_step 1 3 "Downloading JDK %JAVA_MIN_VERSION%"
curl -s -o "%TEMP%\jdk_installer.exe" "%_JDK_URL%"
if errorlevel 1 ( echo [CHIMERA] Download failed. & goto :eof )

call :install_step 2 3 "Running installer (silent)"
"%TEMP%\jdk_installer.exe" /s
if errorlevel 1 ( echo [CHIMERA] Install failed. & goto :eof )

call :install_step 3 3 "Registering JAVA_HOME + PATH"
set "_JDK_HOME=C:\Program Files\Java\jdk-%_JDK_MAJOR%"
setx JAVA_HOME "%_JDK_HOME%" /M >nul 2>&1
setx PATH "%_JDK_HOME%\bin;%PATH%" /M >nul 2>&1
call refreshenv >nul 2>&1
echo [CHIMERA] JDK %JAVA_MIN_VERSION% installed.
echo.
goto :eof

:check_maven

call :get_maven_version _MVN_FOUND
if "!_MVN_FOUND!"=="" goto _do_install_maven

call :ver_gte "!_MVN_FOUND!" "%MAVEN_MIN_VERSION%" _MVN_OK
if "!_MVN_OK!"=="1" (
    echo [CHIMERA] Maven !_MVN_FOUND! ... OK (>= %MAVEN_MIN_VERSION%)
    goto :eof
)
echo [CHIMERA] Maven !_MVN_FOUND! is below %MAVEN_MIN_VERSION%. Upgrading...

:_do_install_maven
echo.
set "_MVN_ZIP=apache-maven-%MAVEN_MIN_VERSION%-bin.zip"
set "_MVN_URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_MIN_VERSION%/binaries/%_MVN_ZIP%"
set "_MVN_DIR=C:\maven"

call :install_step 1 4 "Downloading Maven %MAVEN_MIN_VERSION%"
curl -s -o "%TEMP%\%_MVN_ZIP%" "%_MVN_URL%"
if errorlevel 1 ( echo [CHIMERA] Download failed. & goto :eof )

call :install_step 2 4 "Extracting to %_MVN_DIR%"
if not exist "%_MVN_DIR%" mkdir "%_MVN_DIR%"
tar -xf "%TEMP%\%_MVN_ZIP%" -C "%_MVN_DIR%" --strip-components=1 >nul 2>&1
if errorlevel 1 ( echo [CHIMERA] Extraction failed. & goto :eof )

call :install_step 3 4 "Registering MAVEN_HOME + PATH"
setx MAVEN_HOME "%_MVN_DIR%" /M >nul 2>&1
setx PATH "%_MVN_DIR%\bin;%PATH%" /M >nul 2>&1

call :install_step 4 4 "Refreshing environment"
call refreshenv >nul 2>&1
echo [CHIMERA] Maven %MAVEN_MIN_VERSION% installed.
echo.
goto :eof

:get_java_version
set "%~1="
set "_JV_LINE="
for /f "tokens=* delims=" %%L in ('java -version 2^>^&1') do (
    if not defined _JV_LINE set "_JV_LINE=%%L"
)
for /f "tokens=3" %%V in ("!_JV_LINE!") do set "_JV_RAW=%%V"
set "_JV_RAW=!_JV_RAW:"=!"
set "%~1=!_JV_RAW!"
goto :eof

:get_maven_version
set "%~1="
set "_MH="

if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" set "_MH=%MAVEN_HOME%"
)

if not defined _MH (
    if defined M2_HOME (
        if exist "%M2_HOME%\bin\mvn.cmd" set "_MH=%M2_HOME%"
    )
)

if not defined _MH (
    for %%D in ("%PATH:;=" "%") do (
        if not defined _MH (
            if exist "%%~D\mvn.cmd" (
                :: Parent of \bin is the Maven home
                for %%P in ("%%~D\..") do set "_MH=%%~fP"
            )
        )
    )
)

if not defined _MH goto :eof

set "_VER="
for %%F in ("!_MH!\lib\maven-core-*.jar") do (
    if not defined _VER (
        set "_FNAME=%%~nF"
        set "_VER=!_FNAME:maven-core-=!"
    )
)

if not defined _VER (
    for %%D in ("!_MH!") do set "_DNAME=%%~nxD"
    for /f "tokens=3 delims=-" %%V in ("!_DNAME!") do set "_VER=%%V"
)

set "%~1=!_VER!"
goto :eof

:pip_check_row
set "_PCR_PIP=%~1"
set "_PCR_IMP=%~2"
set "_PCR_VER="

for /f "tokens=*" %%V in ('python -c "import importlib.metadata; print(importlib.metadata.version(\"!_PCR_PIP!\"))" 2^>nul') do set "_PCR_VER=%%V"
if not defined _PCR_VER (
    call :val_row "!_PCR_PIP!" "NOT INSTALLED" FAIL "Run: chimera setup"
    set /a V_FAIL+=1
    goto :eof
)

python -c "import !_PCR_IMP!" >nul 2>&1
if errorlevel 1 (
    call :val_row "!_PCR_PIP!" "!_PCR_VER! (import broken)" WARN "Reinstall: pip install !_PCR_PIP!"
    set /a V_WARN+=1
) else (
    call :val_row "!_PCR_PIP!" "!_PCR_VER!" OK ""
    set /a V_PASS+=1
)
goto :eof

:pip_ensure
set "_PKG=%~1"
set "_PKGV="
for /f "tokens=*" %%V in ('python -c "import importlib.metadata; print(importlib.metadata.version(\"!_PKG!\"))" 2^>nul') do set "_PKGV=%%V"
if defined _PKGV (
    echo [CHIMERA] !_PKG! !_PKGV! ... already installed, skipping.
    goto :eof
)
echo [CHIMERA] Installing !_PKG!...
python -m pip install "!_PKG!" --quiet
if errorlevel 1 (
    echo [CHIMERA] Failed to install !_PKG!.
    exit /b 1
)
echo [CHIMERA] !_PKG! ... OK
goto :eof

:check_file
set "_CF_LABEL=%~1"
set "_CF_PATH=%~2"
set "_CF_STATUS=%~3"
set "_CF_HINT=%~4"
if exist "%_CF_PATH%" (
    call :val_row "!_CF_LABEL!" "Found" OK ""
    set /a V_PASS+=1
) else (
    call :val_row "!_CF_LABEL!" "NOT FOUND" %_CF_STATUS% "!_CF_HINT!"
    if /I "%_CF_STATUS%"=="FAIL" ( set /a V_FAIL+=1 ) else ( set /a V_WARN+=1 )
)
goto :eof

:progress_step
set /a SETUP_STEP+=1
echo [!SETUP_STEP!/%SETUP_TOTAL%] %~1...
goto :eof

:install_step
set "_SN=%~1"
set "_ST=%~2"
set "_SD=%~3"
set /a "_FILLED=_SN*16/_ST"
set /a "_EMPTY=16-_FILLED"
set "_BAR="
for /l %%i in (1,1,%_FILLED%) do set "_BAR=!_BAR!#"
for /l %%i in (1,1,%_EMPTY%)  do set "_BAR=!_BAR!-"
echo   [%_SN%/%_ST%] [!_BAR!] %_SD%
goto :eof

:val_row
set "_VL=%~1"
set "_VV=%~2"
set "_VS=%~3"
set "_VH=%~4"
set "_PL=!_VL!                                "
set "_PL=!_PL:~0,32!"
set "_PV=!_VV!                        "
set "_PV=!_PV:~0,24!"
if /I "!_VS!"=="OK"   echo    [  OK  ]  !_PL!  !_PV!
if /I "!_VS!"=="WARN" echo    [ WARN ]  !_PL!  !_PV!  (!_VH!)
if /I "!_VS!"=="FAIL" echo    [ FAIL ]  !_PL!  !_PV!  (!_VH!)
goto :eof

:ver_gte
set "_VA=%~1"
set "_VB=%~2"
set "_RES=%~3"
set /a "_A1=0" & set /a "_A2=0" & set /a "_A3=0"
set /a "_B1=0" & set /a "_B2=0" & set /a "_B3=0"
for /f "tokens=1,2,3 delims=." %%A in ("%_VA%") do (
    set /a "_A1=%%A" 2>nul
    set /a "_A2=%%B" 2>nul
    set /a "_A3=%%C" 2>nul
)
for /f "tokens=1,2,3 delims=." %%A in ("%_VB%") do (
    set /a "_B1=%%A" 2>nul
    set /a "_B2=%%B" 2>nul
    set /a "_B3=%%C" 2>nul
)
set "%_RES%=0"
if !_A1! GTR !_B1! ( set "%_RES%=1" & goto :eof )
if !_A1! LSS !_B1! goto :eof
if !_A2! GTR !_B2! ( set "%_RES%=1" & goto :eof )
if !_A2! LSS !_B2! goto :eof
if !_A3! GEQ !_B3! set "%_RES%=1"
goto :eof

:help
echo.
echo =====================================
echo            CHIMERA CLI
echo =====================================
echo.
echo Commands:
echo.
echo   chimera run
echo       Runs the JavaFX application.
echo.
echo   chimera setup
echo       Checks Java, Maven, Python -- skips if already up to date.
echo       Installs missing pip packages, OpenCV JAR, builds project.
echo.
echo   chimera validate
echo       Reports status of all dependencies.
echo       Shows OK / WARNING (outdated) / FAIL (missing) per item.
echo.
echo   chimera clean
echo       Kills Java processes and frees port 8765.
echo.
echo   chimera generate [--dataset] [--mediapipe]
echo       --dataset    Generate AI training dataset
echo                    Output: chimera\src\main\resources\datasets\dataset.csv
echo       --mediapipe  Generate MediaPipe Java server
echo                    Output: chimera\src\main\java\org\sozotech\stager\MediaPipeServer.java
echo       (Flags can be combined)
echo.

:end
endlocal