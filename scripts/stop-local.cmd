@echo off
setlocal

set SCRIPT_DIR=%~dp0
set POWERSHELL_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe

if not exist "%POWERSHELL_EXE%" (
  echo [ERROR] PowerShell not found: %POWERSHELL_EXE%
  pause
  exit /b 1
)

echo Stopping local stack...
"%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%stop-local.ps1" %*
set EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
  echo.
  echo [ERROR] Stop script failed with exit code %EXIT_CODE%.
  pause
  exit /b %EXIT_CODE%
)

echo.
echo Local stack stop command finished.
ping -n 3 127.0.0.1 >nul
exit /b 0
