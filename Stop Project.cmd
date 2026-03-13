@echo off
setlocal

call "%~dp0scripts\stop-local.cmd" %*
exit /b %ERRORLEVEL%
