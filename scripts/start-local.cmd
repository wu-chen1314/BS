@echo off
setlocal

set SCRIPT_DIR=%~dp0
set POWERSHELL_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe

if not exist "%POWERSHELL_EXE%" (
  echo [ERROR] PowerShell not found: %POWERSHELL_EXE%
  pause
  exit /b 1
)

echo Starting local stack...
"%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%start-local.ps1" -Restart %*
set EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
  echo.
  echo [ERROR] Start script failed with exit code %EXIT_CODE%.
  echo Check logs under D:\BS\ich-frontend\.tmp
  echo If ports are still occupied, run D:\BS\ich-frontend\Stop Project.cmd and try again.
  pause
  exit /b %EXIT_CODE%
)

echo.
echo Local stack start command finished.
echo You can close this window now.
ping -n 4 127.0.0.1 >nul
exit /b 0
