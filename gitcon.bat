@echo off
setlocal enabledelayedexpansion

:: =========================================================
::  GitCon - Local Git Commit CLI Tool
::
::  GitCon is a lightweight local CLI tool that helps enforce
::  meaningful and standardized commit messages using a
::  Conventional Commit format.
::
::  It ensures consistency, improves readability, and makes
::  collaboration easier across teams by validating commit
::  types and structuring messages automatically.
::
::  Usage:
::     gitcon <type> <scope> "<message>"
::
::  Example:
::     gitcon feat auth "add login system"
::
:: =========================================================

:: =========================
:: ENUM: Allowed commit types
:: =========================
set VALID_TYPES=feat fix docs style refactor test chore

:: =========================
:: HELP COMMAND
:: =========================
if "%1"=="help" goto :help

:: =========================
:: ARGUMENTS
:: =========================
set TYPE=%1
set SCOPE=%2
set MESSAGE=%~3

:: =========================
:: VALIDATION
:: =========================

:: Check required args
if "%TYPE%"=="" goto :usage
if "%SCOPE%"=="" goto :usage
if "%MESSAGE%"=="" (
    echo ❌ Error: Message must be wrapped in quotes
    echo Example: gitcon feat auth "add login system"
    exit /b 1
)

:: Validate TYPE (enum check)
set FOUND=false
for %%t in (%VALID_TYPES%) do (
    if /I "%%t"=="%TYPE%" set FOUND=true
)

if "!FOUND!"=="false" (
    echo ❌ Unknown commit type: %TYPE%
    echo Use "gitcon help" to see valid types.
    exit /b 1
)

:: =========================
:: COMMIT
:: =========================
set COMMIT_MSG=%TYPE%(%SCOPE%): %MESSAGE%

git add .
git commit -m "%COMMIT_MSG%"

echo ✔ Commit created: %COMMIT_MSG%
exit /b 0

:: =========================
:: HELP SECTION
:: =========================
:help
echo.
echo GitCon - Conventional Commit Helper
echo.
echo Usage:
echo   gitcon ^<type^> ^<scope^> "^<message^>"
echo.
echo Example:
echo   gitcon feat auth "add login system"
echo.
echo Available Types:
echo.
echo   feat      - A new feature
echo   fix       - A bug fix
echo   docs      - Documentation changes
echo   style     - Code style/formatting (no logic change)
echo   refactor  - Code restructuring (no feature/fix)
echo   test      - Adding or updating tests
echo   chore     - Maintenance tasks
echo.
exit /b 0

:: =========================
:: USAGE FALLBACK
:: =========================
:usage
echo Usage: gitcon ^<type^> ^<scope^> "^<message^>"
echo Try: gitcon help
exit /b 1