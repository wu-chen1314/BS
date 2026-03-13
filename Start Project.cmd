@echo off
setlocal

call "%~dp0scripts\start-local.cmd" %*
set EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
  exit /b %EXIT_CODE%
)

start "" http://localhost:5173
exit /b 0
